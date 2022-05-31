package org.andresoviedo.android_3d_model_engine.services.wavefront;

import android.net.Uri;
import android.opengl.GLES20;
import androidx.annotation.Nullable;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.Element;
import org.andresoviedo.android_3d_model_engine.model.Material;
import org.andresoviedo.android_3d_model_engine.model.Materials;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.LoadListener;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.MeshData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.Vertex;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.io.IOUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**************************************************************************************************/
public class WavefrontLoader {
    /**********************************************************************************************/
    private final int triangulationMode;
    /**********************************************************************************************/
    private final LoadListener callback;

    /**********************************************************************************************/
    public WavefrontLoader(int triangulationMode, LoadListener callback) {
        this.triangulationMode = triangulationMode;
        this.callback = callback;
    }

    /**********************************************************************************************/
    @Nullable
    public static String getMaterialLib(Uri uri) {
        return getParameter(uri, "mtllib ");
    }

    /**********************************************************************************************/
    @Nullable
    public static String getTextureFile(Uri uri) {
        return getParameter(uri, "map_Kd ");
    }

    /**********************************************************************************************/
    @Nullable
    private static String getParameter(Uri uri, String parameter) {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(ContentUtils.getInputStream(uri)))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith(parameter)) {
                    return line.substring(parameter.length()).trim();
                }
            }
        } catch (IOException e) {
            Log.e("WavefrontLoader", "Problem reading file '" + uri + "': " + e.getMessage(), e);
            throw new RuntimeException(e);
        }
        return null;
    }

    /**********************************************************************************************/
    public List<Object3DData> load(URI modelURI) {
        try {
            Log.i("WavefrontLoader", "Loading model... " + modelURI.toString());

            Log.i("WavefrontLoader", "--------------------------------------------------");
            Log.i("WavefrontLoader", "Parsing geometries... ");
            Log.i("WavefrontLoader", "--------------------------------------------------");

            final InputStream is = modelURI.toURL().openStream();
            final List<MeshData> meshes = loadModel(modelURI.toString(), is);
            is.close();

            final List<Object3DData> ret = new ArrayList<>();

            Log.i("WavefrontLoader", "Processing geometries... ");

            callback.onProgress("Processing geometries...");

            for (MeshData meshData : meshes) {
                callback.onProgress("Processing normals...");

                meshData.fixNormals();

                meshData.validate();

                Object3DData data3D = new Object3DData(meshData.getVertexBuffer());

                data3D.setMeshData(meshData);
                data3D.setId(meshData.getId());
                data3D.setName(meshData.getName());
                data3D.setNormalsBuffer(meshData.getNormalsBuffer());
                data3D.setTextureBuffer(meshData.getTextureBuffer());
                data3D.setElements(meshData.getElements());
                data3D.setId(modelURI.toString());
                data3D.setUri(modelURI);
                data3D.setDrawUsingArrays(false);
                data3D.setDrawMode(GLES20.GL_TRIANGLES);

                callback.onLoad(data3D);

                callback.onProgress("Loading materials...");

                loadMaterials(meshData);

                ret.add(data3D);
            }

            Log.i("WavefrontLoader", "Loaded geometries: " + ret.size());

            return ret;
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**********************************************************************************************/
    private void loadMaterials(MeshData meshData) {
        if (meshData.getMaterialFile() == null) return;

        Log.i("WavefrontLoader", "--------------------------------------------------");
        Log.i("WavefrontLoader", "Parsing materials... ");
        Log.i("WavefrontLoader", "--------------------------------------------------");

        try {
            final InputStream inputStream = ContentUtils.getInputStream(meshData.getMaterialFile());

            final WavefrontMaterialsParser materialsParser = new WavefrontMaterialsParser();
            final Materials materials = materialsParser.parse(meshData.getMaterialFile(), inputStream);

            if (materials.size() > 0) {
                for (int e = 0; e < meshData.getElements().size(); e++) {
                    final Element element = meshData.getElements().get(e);

                    Log.i("WavefrontLoader", "Processing element... " + element.getId());

                    final String elementMaterialId = element.getMaterialId();

                    if (elementMaterialId != null && materials.contains(elementMaterialId)) {

                        final Material elementMaterial = materials.get(elementMaterialId);

                        element.setMaterial(elementMaterial);

                        if (elementMaterial.getTextureFile() != null) {

                            Log.i("WavefrontLoader", "Reading texture file... " + elementMaterial.getTextureFile());

                            try (InputStream stream = ContentUtils.getInputStream(elementMaterial.getTextureFile())) {

                                elementMaterial.setTextureData(IOUtils.read(stream));

                                Log.i("WavefrontLoader", "Texture linked... " + element.getMaterial().getTextureFile());

                            } catch (Exception ex) {
                                Log.e("WavefrontLoader", String.format("Error reading texture file: %s", ex.getMessage()));
                            }
                        }
                    }
                }
            }
        } catch (IOException ex) {
            Log.e("WavefrontLoader", "Error loading materials... " + meshData.getMaterialFile());
        }
    }

    /**********************************************************************************************/
    private List<MeshData> loadModel(String id, InputStream is) {
        Log.i("WavefrontLoader", "Loading model... " + id);

        BufferedReader br = null;

        try {
            br = new BufferedReader(new InputStreamReader(is));

            int lineNum = 0;
            String line = null;

            final List<float[]> vertexList = new ArrayList<>();
            final List<float[]> normalsList = new ArrayList<>();
            final List<float[]> textureList = new ArrayList<>();

            final List<MeshData> meshes = new ArrayList<>();
            final List<Vertex> verticesAttributes = new ArrayList<>();

            String mtllib = null;

            final Map<String, List<Vertex>> smoothingGroups = new HashMap<>();

            List<Vertex> currentSmoothingList = null;

            MeshData.Builder meshCurrent = new MeshData.Builder().id(id);
            Element.Builder elementCurrent = new Element.Builder().id("default");
            List<Integer> indicesCurrent = new ArrayList<>();

            boolean buildNewMesh = false;
            boolean buildNewElement = false;

            try {
                while (((line = br.readLine()) != null)) {
                    lineNum++;
                    line = line.trim();
                    if (line.length() == 0) continue;
                    if (line.startsWith("v ")) {
                        parseVector(vertexList, line.substring(2).trim());
                    } else if (line.startsWith("vn")) {
                        parseVector(normalsList, line.substring(3).trim());
                    } else if (line.startsWith("vt")) {
                        parseVariableVector(textureList, line.substring(3).trim());
                    } else if (line.charAt(0) == 'o') {
                        if (buildNewMesh) {
                            meshCurrent.vertices(vertexList).normals(normalsList).textures(textureList).vertexAttributes(verticesAttributes).materialFile(mtllib).addElement(elementCurrent.indices(indicesCurrent).build());

                            final MeshData build = meshCurrent.build();
                            meshes.add(build);

                            Log.d("WavefrontLoader", "Loaded mesh. id:" + build.getId() + ", indices: " + indicesCurrent.size()
                                    + ", vertices:" + vertexList.size()
                                    + ", normals: " + normalsList.size()
                                    + ", textures:" + textureList.size()
                                    + ", elements: " + build.getElements());

                            meshCurrent = new MeshData.Builder().id(line.substring(1).trim());

                            elementCurrent = new Element.Builder();
                            indicesCurrent = new ArrayList<>();
                        } else {
                            meshCurrent.id(line.substring(1).trim());
                            buildNewMesh = true;
                        }
                    } else if (line.charAt(0) == 'g') {
                        if (buildNewElement && indicesCurrent.size() > 0) {
                            elementCurrent.indices(indicesCurrent);
                            meshCurrent.addElement(elementCurrent.build());

                            Log.d("WavefrontLoader", "New element. indices: " + indicesCurrent.size());

                            indicesCurrent = new ArrayList<>();
                            elementCurrent = new Element.Builder().id(line.substring(1).trim());
                        } else {
                            elementCurrent.id(line.substring(1).trim());
                            buildNewElement = true;
                        }
                    } else if (line.startsWith("f ")) {
                        parseFace(verticesAttributes, indicesCurrent, vertexList, normalsList, textureList, line.substring(2), currentSmoothingList);
                    } else if (line.startsWith("mtllib ")) {
                        mtllib = line.substring(7);
                    } else if (line.startsWith("usemtl ")) {
                        if (elementCurrent.getMaterialId() != null) {

                            elementCurrent.indices(indicesCurrent);
                            meshCurrent.addElement(elementCurrent.build());

                            Log.v("WavefrontLoader", "New material: " + line);

                            indicesCurrent = new ArrayList<>();
                            elementCurrent = new Element.Builder().id(elementCurrent.getId());
                        }

                        elementCurrent.materialId(line.substring(7));
                    } else if (line.charAt(0) == 's') {
                        final String smoothingGroupId = line.substring(1).trim();
                        if ("0".equals(smoothingGroupId) || "off".equals(smoothingGroupId)) {
                            currentSmoothingList = null;
                        } else {
                            currentSmoothingList = new ArrayList<>();
                            smoothingGroups.put(smoothingGroupId, currentSmoothingList);
                        }
                    } else if (line.charAt(0) == '#') {
                        Log.v("WavefrontLoader", line);
                    } else
                        Log.w("WavefrontLoader", "Ignoring line " + lineNum + " : " + line);
                }

                final Element element = elementCurrent.indices(indicesCurrent).build();
                final MeshData meshData = meshCurrent.vertices(vertexList).normals(normalsList).textures(textureList).vertexAttributes(verticesAttributes).materialFile(mtllib).addElement(element).smoothingGroups(smoothingGroups).build();

                Log.i("WavefrontLoader", "Loaded mesh. id:" + meshData.getId() + ", indices: " + indicesCurrent.size()
                        + ", vertices:" + vertexList.size()
                        + ", normals: " + normalsList.size()
                        + ", textures:" + textureList.size()
                        + ", elements: " + meshData.getElements());

                meshes.add(meshData);

                return meshes;
            } catch (Exception e) {
                Log.e("WavefrontLoader", "Error reading line: " + lineNum + ":" + line, e);
                Log.e("WavefrontLoader", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    Log.e("WavefrontLoader", e.getMessage(), e);
                }
            }
        }
    }

    /**********************************************************************************************/
    private void parseVector(List<float[]> vectorList, String line) {
        try {
            final String[] tokens = line.split(" +");
            final float[] vector = new float[3];
            vector[0] = Float.parseFloat(tokens[0]);
            vector[1] = Float.parseFloat(tokens[1]);
            vector[2] = Float.parseFloat(tokens[2]);
            vectorList.add(vector);
        } catch (Exception ex) {
            Log.e("WavefrontLoader", "Error parsing vector '" + line + "': " + ex.getMessage());
            vectorList.add(new float[3]);
        }
    }

    /**********************************************************************************************/
    private void parseVariableVector(List<float[]> textureList, String line) {
        try {
            final String[] tokens = line.split(" +");
            final float[] vector = new float[2];
            vector[0] = Float.parseFloat(tokens[0]);
            if (tokens.length > 1) {
                vector[1] = Float.parseFloat(tokens[1]);
            }
            textureList.add(vector);
        } catch (Exception ex) {
            Log.e("WavefrontLoader", ex.getMessage());
            textureList.add(new float[2]);
        }
    }

    /**********************************************************************************************/
    private void parseFace(List<Vertex> vertexAttributes, List<Integer> indices, List<float[]> vertexList, List<float[]> normalsList, List<float[]> texturesList, String line, List<Vertex> currentSmoothingList) {
        try {
            final String[] tokens;
            if (line.contains("  ")) {
                tokens = line.split(" +");
            } else {
                tokens = line.split(" ");
            }

            final int numTokens = tokens.length;

            for (int i = 0, faceIndex = 0; i < numTokens; i++, faceIndex++) {
                if (faceIndex > 2) {
                    faceIndex = 0;

                    i -= 2;
                }

                final String faceToken;

                if (this.triangulationMode == GLES20.GL_TRIANGLE_FAN) {
                    if (faceIndex == 0) {
                        faceToken = tokens[0];
                    } else {
                        faceToken = tokens[i];
                    }
                } else {
                    faceToken = tokens[i];
                }

                final String[] faceTokens = faceToken.split("/");
                final int numSeps = faceTokens.length;

                int vertIdx = Integer.parseInt(faceTokens[0]);

                if (vertIdx < 0) {
                    vertIdx = vertexList.size() + vertIdx;
                } else {
                    vertIdx--;
                }

                int textureIdx = -1;
                if (numSeps > 1 && faceTokens[1].length() > 0) {
                    textureIdx = Integer.parseInt(faceTokens[1]);
                    if (textureIdx < 0) {
                        textureIdx = texturesList.size() + textureIdx;
                    } else {
                        textureIdx--;
                    }
                }
                int normalIdx = -1;
                if (numSeps > 2 && faceTokens[2].length() > 0) {
                    normalIdx = Integer.parseInt(faceTokens[2]);
                    if (normalIdx < 0) {
                        normalIdx = normalsList.size() + normalIdx;
                    } else {
                        normalIdx--;
                    }
                }

                final Vertex vertexAttribute = new Vertex(vertIdx);
                vertexAttribute.setNormalIndex(normalIdx);
                vertexAttribute.setTextureIndex(textureIdx);

                final int idx = vertexAttributes.size();
                vertexAttributes.add(idx, vertexAttribute);

                indices.add(idx);

                if (currentSmoothingList != null) {
                    currentSmoothingList.add(vertexAttribute);
                }
            }
        } catch (NumberFormatException e) {
            Log.e("WavefrontLoader", e.getMessage(), e);
        }
    }
}