package org.andresoviedo.android_3d_model_engine.services.collada.entities;

import android.util.Log;

import androidx.annotation.NonNull;

import org.andresoviedo.android_3d_model_engine.model.Element;
import org.andresoviedo.util.io.IOUtils;
import org.andresoviedo.util.math.Math3DUtils;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**************************************************************************************************/
public class MeshData {
    /**********************************************************************************************/
    private static final float[] WRONG_NORMAL = {0, -1, 0};

    /**********************************************************************************************/
    public static class Builder {

        private String id;
        private String name;

        private List<float[]> vertices;
        private List<float[]> normals;
        private List<float[]> colors;
        private List<float[]> textures;

        private List<Vertex> vertexAttributes;
        private List<Element> elements;

        private String materialFile;

        private Map<String, List<Vertex>> smoothingGroups;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder vertices(List<float[]> vertices) {
            this.vertices = vertices;
            return this;
        }

        public Builder normals(List<float[]> normals) {
            this.normals = normals;
            return this;
        }

        public Builder colors(List<float[]> colors) {
            this.colors = colors;
            return this;
        }

        public Builder textures(List<float[]> textures) {
            this.textures = textures;
            return this;
        }

        public Builder vertexAttributes(List<Vertex> vertices) {
            this.vertexAttributes = vertices;
            return this;
        }

        public Builder addElement(Element element) {
            if (this.elements == null) {
                this.elements = new ArrayList<>();
            }
            this.elements.add(element);
            return this;
        }

        public Builder materialFile(String materialFile) {
            this.materialFile = materialFile;
            return this;
        }

        public Builder smoothingGroups(Map<String, List<Vertex>> smoothingGroups) {
            this.smoothingGroups = smoothingGroups;
            return this;
        }

        public MeshData build() {
            return new MeshData(id, name, vertices, normals, colors, textures, vertexAttributes, elements, materialFile, smoothingGroups);
        }
    }

    /**********************************************************************************************/
    private final String id;
    private final String name;
    /**********************************************************************************************/
    private List<Vertex> verticesAttributes;
    /**********************************************************************************************/
    private final List<float[]> vertices;
    private final List<float[]> textures;
    /**********************************************************************************************/
    private List<float[]> normals;
    /**********************************************************************************************/
    private final List<float[]> colors;
    /**********************************************************************************************/
    private final List<Element> elements;
    /**********************************************************************************************/
    private FloatBuffer vertexBuffer;
    private FloatBuffer normalsBuffer;
    private FloatBuffer colorsBuffer;
    private FloatBuffer textureBuffer;
    /**********************************************************************************************/
    private float[] bindShapeMatrix;
    private int[] jointsArray;
    private float[] weightsArray;
    private FloatBuffer jointsBuffer;
    private FloatBuffer weightsBuffer;
    /**********************************************************************************************/
    private final String materialFile;
    /**********************************************************************************************/
    private final Map<String, List<Vertex>> smoothingGroups;
    private List<float[]> normalsOriginal;
    private List<Vertex> verticesAttributesOriginal;

    /**********************************************************************************************/
    public MeshData(String id, String name, List<float[]> vertices, List<float[]> normals, List<float[]> colors, List<float[]> textures, List<Vertex> verticesAttributes,
                    List<Element> elements, String materialFile, Map<String, List<Vertex>> smoothingGroups) {
        this.id = id;
        this.name = name;

        this.vertices = vertices;
        this.normals = normals;
        this.colors = colors;
        this.textures = textures;

        this.verticesAttributes = verticesAttributes;
        this.elements = elements;

        this.materialFile = materialFile;

        this.smoothingGroups = smoothingGroups;
    }

    /**********************************************************************************************/
    public String getId() {
        return id;
    }

    /**********************************************************************************************/
    public String getName() {
        return name;
    }

    /**********************************************************************************************/
    public List<float[]> getNormals() {
        return this.normals;
    }

    /**********************************************************************************************/
    public List<Element> getElements() {
        return elements;
    }

    /**********************************************************************************************/
    public void smooth() {
        this.normalsOriginal = new ArrayList<>(this.normals.size());
        for (int i = 0; i < this.normals.size(); i++) {
            this.normalsOriginal.add(this.normals.get(i).clone());
        }
        if (this.verticesAttributes != null) {
            try {
                this.verticesAttributesOriginal = new ArrayList<>(this.verticesAttributes.size());
                for (int i = 0; i < this.verticesAttributes.size(); i++) {
                    this.verticesAttributesOriginal.add(this.verticesAttributes.get(i).clone());
                }
            } catch (CloneNotSupportedException ignored) {
            }
        }

        if (smoothingGroups == null || smoothingGroups.isEmpty()) {
            smoothAuto();
        } else {
            smoothGroups();
        }

        refreshNormalsBuffer();
    }

    /**********************************************************************************************/
    public void unSmooth() {
        if (this.normalsOriginal != null) {
            this.normals.clear();
            this.normals = this.normalsOriginal;
        }
        if (this.verticesAttributesOriginal != null) {
            this.verticesAttributes.clear();
            this.verticesAttributes = verticesAttributesOriginal;
        }

    }

    /**********************************************************************************************/
    private void smoothAuto() {
        if (this.elements != null) {
            smoothAutoForElements();
        } else {
            smoothAutoForArrays();
        }
    }

    /**********************************************************************************************/
    private void smoothGroups() {
        Log.i("MeshData", "Smoothing groups... Total: " + smoothingGroups.size());

        for (Map.Entry<String, List<Vertex>> smoothingGroup : smoothingGroups.entrySet()) {

            Log.v("MeshData", "Smoothing group... Total vertices: " + smoothingGroup.getValue().size());

            float[] smoothNormal = new float[3];

            for (int i = 0; i < smoothingGroup.getValue().size(); i += 3) {
                Vertex va1 = smoothingGroup.getValue().get(i);
                Vertex va2 = smoothingGroup.getValue().get(i + 1);
                Vertex va3 = smoothingGroup.getValue().get(i + 2);
                float[] v1 = this.vertices.get(va1.getVertexIndex());
                float[] v2 = this.vertices.get(va2.getVertexIndex());
                float[] v3 = this.vertices.get(va3.getVertexIndex());
                float[] normal = calculateNormalFailsafe(v1, v2, v3);
                smoothNormal = Math3DUtils.add(smoothNormal, normal);
            }

            Math3DUtils.normalize(smoothNormal);

            final int newSmoothNormalIdx = this.normals.size();
            this.normals.add(newSmoothNormalIdx, smoothNormal);

            for (int i = 0; i < smoothingGroup.getValue().size(); i++) {

                Vertex va = smoothingGroup.getValue().get(i);

                if (va.getNormalIndex() != -1) {
                    continue;
                }

                va.setNormalIndex(newSmoothNormalIdx);
            }
        }
    }

    /**********************************************************************************************/
    private static float[] calculateNormalFailsafe(float[] v1, float[] v2, float[] v3) {
        float[] normal = Math3DUtils.calculateNormal(v1, v2, v3);
        try {
            Math3DUtils.normalize(normal);
        } catch (Exception e) {
            Log.e("MeshData", "Error calculating normal. " + e.getMessage()
                    + "," + Math3DUtils.toString(v1)
                    + "," + Math3DUtils.toString(v2)
                    + "," + Math3DUtils.toString(v3), e);
            normal = WRONG_NORMAL;
        }
        return normal;
    }

    /**********************************************************************************************/
    public void fixNormals() {
        Log.i("MeshData", "Fixing missing or wrong normals...");

        if (this.normals == null || this.normals.isEmpty()) {
            generateNormals();
        } else {
            if (this.elements != null) {
                fixNormalsForElements();
            } else {
                fixNormalsForArrays();
            }
        }
    }

    /**********************************************************************************************/
    private void generateNormals() {
        Log.i("MeshData", "Generating normals...");

        final List<float[]> newNormals = new ArrayList<>();

        int counter = 0;

        for (Element element : getElements()) {

            for (int i = 0; i < element.getIndices().size(); i += 3) {

                final int idx1 = element.getIndices().get(i);
                final int idx2 = element.getIndices().get(i + 1);
                final int idx3 = element.getIndices().get(i + 2);

                final Vertex vertexAttribute1 = this.verticesAttributes.get(idx1);
                final Vertex vertexAttribute2 = this.verticesAttributes.get(idx2);
                final Vertex vertexAttribute3 = this.verticesAttributes.get(idx3);

                final int idxV1 = vertexAttribute1.getVertexIndex();
                final int idxV2 = vertexAttribute2.getVertexIndex();
                final int idxV3 = vertexAttribute3.getVertexIndex();

                final float[] v1 = this.vertices.get(idxV1);
                final float[] v2 = this.vertices.get(idxV2);
                final float[] v3 = this.vertices.get(idxV3);

                if (Arrays.equals(v1, v2) || Arrays.equals(v2, v3) || Arrays.equals(v1, v3)) {

                    vertexAttribute1.setNormalIndex(newNormals.size());
                    vertexAttribute2.setNormalIndex(newNormals.size());
                    vertexAttribute3.setNormalIndex(newNormals.size());

                    newNormals.add(newNormals.size(), WRONG_NORMAL);

                    counter++;
                    continue;
                }

                final float[] calculatedNormal = calculateNormalFailsafe(v1, v2, v3);

                vertexAttribute1.setNormalIndex(newNormals.size());
                vertexAttribute2.setNormalIndex(newNormals.size());
                vertexAttribute3.setNormalIndex(newNormals.size());

                newNormals.add(newNormals.size(), calculatedNormal);

            }
        }

        this.normals = newNormals;

        Log.i("MeshData", "Generated normals. Total: " + this.normals.size() + ", Faces/Lines: " + counter);
    }

    /**********************************************************************************************/
    private void fixNormalsForArrays() {
        Log.i("MeshData", "Fixing normals...");

        final List<float[]> newNormals = new ArrayList<>();

        int counter = 0;
        for (int i = 0; i < vertices.size(); i += 3) {

            final float[] v1 = this.vertices.get(i);
            final float[] v2 = this.vertices.get(i + 1);
            final float[] v3 = this.vertices.get(i + 2);

            if (Arrays.equals(v1, v2) || Arrays.equals(v2, v3) || Arrays.equals(v1, v3)) {

                newNormals.add(newNormals.size(), WRONG_NORMAL);
                newNormals.add(newNormals.size(), WRONG_NORMAL);
                newNormals.add(newNormals.size(), WRONG_NORMAL);

                counter++;
                continue;
            }

            final float[] normalV1 = normals.get(i);
            final float[] normalV2 = normals.get(i + 1);
            final float[] normalV3 = normals.get(i + 2);

            float[] calculatedNormal = calculateNormalFailsafe(v1, v2, v3);

            if (Math3DUtils.length(normalV1) < 0.1f) {

                newNormals.add(calculatedNormal);

                counter++;

            } else {
                newNormals.add(normalV1);
            }

            if (Math3DUtils.length(normalV2) < 0.1f) {
                newNormals.add(calculatedNormal);

                counter++;

            } else {
                newNormals.add(normalV2);
            }

            if (Math3DUtils.length(normalV3) < 0.1f) {
                newNormals.add(calculatedNormal);

                counter++;

            } else {
                newNormals.add(normalV3);
            }
        }

        this.normals = newNormals;

        Log.i("MeshData", "Fixed normals. Total: " + counter);
    }

    /**********************************************************************************************/
    private void fixNormalsForElements() {

        Log.i("MeshData", "Fixing normals for all elements...");

        final List<float[]> newNormals = new ArrayList<>();

        int counter = 0;

        for (Element element : getElements()) {

            for (int i = 0; i < element.getIndices().size(); i += 3) {

                final int idx1 = element.getIndices().get(i);
                final int idx2 = element.getIndices().get(i + 1);
                final int idx3 = element.getIndices().get(i + 2);

                final Vertex vertexAttribute1 = this.verticesAttributes.get(idx1);
                final Vertex vertexAttribute2 = this.verticesAttributes.get(idx2);
                final Vertex vertexAttribute3 = this.verticesAttributes.get(idx3);

                final int idxV1 = vertexAttribute1.getVertexIndex();
                final int idxV2 = vertexAttribute2.getVertexIndex();
                final int idxV3 = vertexAttribute3.getVertexIndex();

                final float[] v1 = this.vertices.get(idxV1);
                final float[] v2 = this.vertices.get(idxV2);
                final float[] v3 = this.vertices.get(idxV3);

                if (Arrays.equals(v1, v2) || Arrays.equals(v2, v3) || Arrays.equals(v1, v3)) {

                    vertexAttribute1.setNormalIndex(newNormals.size());
                    vertexAttribute2.setNormalIndex(newNormals.size());
                    vertexAttribute3.setNormalIndex(newNormals.size());

                    newNormals.add(newNormals.size(), WRONG_NORMAL);

                    counter++;
                    continue;
                }

                final int normalIdxV1 = vertexAttribute1.getNormalIndex();
                final int normalIdxV2 = vertexAttribute2.getNormalIndex();
                final int normalIdxV3 = vertexAttribute3.getNormalIndex();

                final float[] normalV1 = normals.get(normalIdxV1);
                final float[] normalV2 = normals.get(normalIdxV2);
                final float[] normalV3 = normals.get(normalIdxV3);

                float[] calculatedNormal = calculateNormalFailsafe(v1, v2, v3);

                if (normalIdxV1 == -1 || Math3DUtils.length(normalV1) < 0.1f) {

                    vertexAttribute1.setNormalIndex(newNormals.size());

                    newNormals.add(calculatedNormal);

                    counter++;

                } else {
                    vertexAttribute1.setNormalIndex(newNormals.size());

                    newNormals.add(normalV1);
                }

                if (normalIdxV2 == -1 || Math3DUtils.length(normalV2) < 0.1f) {
                    vertexAttribute2.setNormalIndex(newNormals.size());

                    newNormals.add(calculatedNormal);

                    counter++;

                } else {
                    vertexAttribute2.setNormalIndex(newNormals.size());

                    newNormals.add(normalV2);
                }

                if (normalIdxV3 == -1 || Math3DUtils.length(normalV3) < 0.1f) {
                    vertexAttribute3.setNormalIndex(newNormals.size());

                    newNormals.add(calculatedNormal);

                    counter++;

                } else {
                    vertexAttribute3.setNormalIndex(newNormals.size());

                    newNormals.add(normalV3);
                }
            }
        }

        this.normals = newNormals;

        Log.i("MeshData", "Fixed normals. Total: " + counter);
    }

    /**********************************************************************************************/
    private void smoothAutoForArrays() {
        Log.i("MeshData", "Auto smoothing normals for arrays...");

        final Map<String, float[]> smoothNormals = new HashMap<>();

        for (int i = 0; i < vertices.size(); i++) {

            final String idxKey = Arrays.toString(vertices.get(i));
            final float[] smoothNormal = smoothNormals.get(idxKey);
            if (smoothNormal == null) {
                smoothNormals.put(idxKey, this.normals.get(i));
                continue;
            }

            final float[] normal = this.normals.get(i);

            if (normal == smoothNormal || Arrays.equals(normal, smoothNormal)) {
                normals.set(i, smoothNormal);
                continue;
            }

            final float[] newSmoothNormal = Math3DUtils.mean(smoothNormal, normal);

            Math3DUtils.normalize(newSmoothNormal);

            smoothNormal[0] = newSmoothNormal[0];
            smoothNormal[1] = newSmoothNormal[1];
            smoothNormal[2] = newSmoothNormal[2];

            this.normals.set(i, smoothNormal);
        }
    }

    /**********************************************************************************************/
    private void smoothAutoForElements() {
        Log.i("MeshData", "Auto smoothing normals for all elements...");

        final Map<Integer, List<float[]>> vertexNormals = new HashMap<>();

        for (Element element : getElements()) {

            for (int i = 0; i < element.getIndices().size(); i++) {

                final int idx = element.getIndices().get(i);

                final int vertexIndex = this.verticesAttributes.get(idx).getVertexIndex();
                final int normalIndex = this.verticesAttributes.get(idx).getNormalIndex();

                final float[] normal = this.normals.get(normalIndex);

                List<float[]> normals = vertexNormals.get(vertexIndex);
                if (normals == null) {
                    normals = new ArrayList<>();
                    normals.add(normal);
                    vertexNormals.put(vertexIndex, normals);
                } else {
                    normals.add(normal);
                }
            }
        }

        final Map<Integer, float[]> vertexSmooths = new HashMap<>();

        for (Map.Entry<Integer, List<float[]>> entry : vertexNormals.entrySet()) {
            final List<float[]> normals = entry.getValue();

            if (normals.size() == 1) {
                vertexSmooths.put(entry.getKey(), normals.get(0));
                continue;
            }

            final float[] smoothNormal = Math3DUtils.mean(normals);

            Math3DUtils.normalize(smoothNormal);

            vertexSmooths.put(entry.getKey(), smoothNormal);
        }

        this.normals.clear();
        for (Element element : getElements()) {

            for (int i = 0; i < element.getIndices().size(); i++) {
                final int idx = element.getIndices().get(i);

                final int vertexIndex = this.verticesAttributes.get(idx).getVertexIndex();
                final int normalIndex = this.verticesAttributes.get(idx).getNormalIndex();

                float[] smoothNormal = vertexSmooths.get(vertexIndex);

                this.verticesAttributes.get(idx).setNormalIndex(this.normals.size());
                this.normals.add(smoothNormal);
            }
        }
    }

    /**********************************************************************************************/
    private static float[] search(List<float[]> list, float[] search) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == search) {
                return list.get(i);
            }
        }
        return null;
    }

    /**********************************************************************************************/
    public void validate() {

        if (normals == null) return;

        for (int i = 0; i < normals.size(); i++) {
            float[] normal = normals.get(i);
            if (Float.isNaN(normal[0])) throw new IllegalArgumentException("NaN");
            if (Float.isNaN(normal[1])) throw new IllegalArgumentException("NaN");
            if (Float.isNaN(normal[2])) throw new IllegalArgumentException("NaN");

            if (Math3DUtils.length(normal) < 0.9f) {
                throw new IllegalArgumentException("Wrong normal. Length < 1.0");
            }
        }

        for (Element element : elements) {
            for (int i = 0; i < element.getIndices().size(); i++) {
                final int idx = element.getIndices().get(i);
                Vertex vertexAttribute = verticesAttributes.get(idx);

                if (vertexAttribute.getNormalIndex() < 0 || vertexAttribute.getNormalIndex() >= normals.size()) {
                    throw new IllegalArgumentException("Wrong normal index: " + vertexAttribute.getNormalIndex());
                }
            }
        }
    }

    /**********************************************************************************************/
    public FloatBuffer getVertexBuffer() {
        if (this.vertexBuffer == null) {
            if (this.verticesAttributes != null) {
                this.vertexBuffer = IOUtils.createFloatBuffer(verticesAttributes.size() * 3);
                for (int i = 0; i < verticesAttributes.size(); i++)
                    this.vertexBuffer.put(vertices.get(verticesAttributes.get(i).getVertexIndex()));
            } else {
                this.vertexBuffer = IOUtils.createFloatBuffer(vertices.size() * 3);
                for (int i = 0; i < vertices.size(); i++)
                    this.vertexBuffer.put(vertices.get(i));
            }
        }
        return vertexBuffer;
    }

    /**********************************************************************************************/
    public FloatBuffer getNormalsBuffer() {
        if (this.normalsBuffer == null && !this.normals.isEmpty()) {
            if (this.verticesAttributes != null) {
                this.normalsBuffer = IOUtils.createFloatBuffer(this.verticesAttributes.size() * 3);
                for (int i = 0; i < verticesAttributes.size(); i++) {
                    float[] normal = WRONG_NORMAL; // no normal in case of error
                    final int index = verticesAttributes.get(i).getNormalIndex();
                    if (index >= 0 && index < normals.size()) {
                        normal = normals.get(index);
                    } else {
                        Log.e("MeshData", "Wrong normal index: " + index);
                    }
                    this.normalsBuffer.put(normal);
                }
            } else {
                this.normalsBuffer = IOUtils.createFloatBuffer(normals.size() * 3);
                for (int i = 0; i < normals.size(); i++)
                    this.normalsBuffer.put(normals.get(i));
            }
        }
        return normalsBuffer;
    }

    /**********************************************************************************************/
    public void refreshNormalsBuffer() {
        if (this.normalsBuffer == null || this.normals.isEmpty()) {
            Log.e("MeshData", "Can't refresh normals buffer. Either normals or normalsBuffer is empty");
            return;
        } else if (this.verticesAttributes != null && this.verticesAttributes.size() * 3 != this.normalsBuffer.capacity()) {
            Log.e("MeshData", "Can't refresh normals buffer. Buffer size doesn't match actual data");
        } else if (this.verticesAttributes == null && this.normals.size() * 3 != this.normalsBuffer.capacity()) {
            Log.e("MeshData", "Can't refresh normals buffer. Buffer size doesn't match actual data");
        }

        Log.i("MeshData", "Refreshing normals buffer...");
        if (this.verticesAttributes != null) {
            for (int i = 0; i < verticesAttributes.size(); i++) {
                float[] normal = WRONG_NORMAL;
                final int index = verticesAttributes.get(i).getNormalIndex();
                if (index >= 0 && index < normals.size()) {
                    normal = this.normals.get(index);
                } else {
                    Log.e("MeshData", "Wrong normal index: " + index);
                }
                this.normalsBuffer.put(i * 3, normal[0]);
                this.normalsBuffer.put(i * 3 + 1, normal[1]);
                this.normalsBuffer.put(i * 3 + 2, normal[2]);
            }
        } else {
            for (int i = 0; i < this.normals.size(); i++) {
                this.normalsBuffer.put(i * 3, this.normals.get(i)[0]);
                this.normalsBuffer.put(i * 3 + 1, this.normals.get(i)[1]);
                this.normalsBuffer.put(i * 3 + 2, this.normals.get(i)[2]);
            }
        }
    }

    /**********************************************************************************************/
    public FloatBuffer getColorsBuffer() {
        if (this.colorsBuffer == null && !this.colors.isEmpty()) {
            this.colorsBuffer = IOUtils.createFloatBuffer(this.verticesAttributes.size() * 4);
            for (int i = 0; i < verticesAttributes.size() && i < colors.size(); i++) {
                float[] color = new float[]{1, 0, 0, 1}; // red to warn about error
                final int index = verticesAttributes.get(i).getColorIndex();
                if (index >= 0 && index < colors.size()) {
                    color = colors.get(index);
                }
                this.colorsBuffer.put(color);
            }
        }
        return colorsBuffer;
    }

    /**********************************************************************************************/
    public FloatBuffer getTextureBuffer() {
        if (this.textureBuffer == null && !this.textures.isEmpty()) {
            this.textureBuffer = IOUtils.createFloatBuffer(this.verticesAttributes.size() * 2);
            for (int i = 0; i < verticesAttributes.size(); i++) {
                float[] texture = new float[2]; // no texture in case of error
                int index = verticesAttributes.get(i).getTextureIndex();
                if (index >= 0 && index < textures.size()) {
                    texture = textures.get(index);
                    texture = new float[]{texture[0], 1 - texture[1]};
                }
                this.textureBuffer.put(texture);
            }
        }
        return textureBuffer;
    }

    /**********************************************************************************************/
    public String getMaterialFile() {
        return materialFile;
    }

    /**********************************************************************************************/
    public FloatBuffer getJointsBuffer() {
        if (this.jointsBuffer == null && getJointsArray() != null) {
            this.jointsBuffer = IOUtils.createFloatBuffer(getJointsArray().length);
            for (int i = 0; i < getJointsArray().length; i++) {
                this.jointsBuffer.put(getJointsArray()[i]);
            }
        }
        return jointsBuffer;
    }

    /**********************************************************************************************/
    public FloatBuffer getWeightsBuffer() {
        if (this.weightsBuffer == null && getWeightsArray() != null) {
            this.weightsBuffer = IOUtils.createFloatBuffer(getWeightsArray().length);
            this.weightsBuffer.put(getWeightsArray());
        }
        return weightsBuffer;
    }

    /**********************************************************************************************/
    public List<Vertex> getVerticesAttributes() {
        return verticesAttributes;
    }

    /**********************************************************************************************/
    public void setBindShapeMatrix(float[] bindShapeMatrix) {
        this.bindShapeMatrix = bindShapeMatrix;
    }

    /**********************************************************************************************/
    public float[] getBindShapeMatrix() {
        return bindShapeMatrix;
    }

    /**********************************************************************************************/
    public void setJointsArray(int[] jointIdsArray) {
        this.jointsArray = jointIdsArray;
    }

    /**********************************************************************************************/
    public void setWeightsArray(float[] weightsArray) {
        this.weightsArray = weightsArray;
    }

    /**********************************************************************************************/
    public int[] getJointsArray() {
        return jointsArray;
    }

    /**********************************************************************************************/
    public float[] getWeightsArray() {
        return weightsArray;
    }

    /**********************************************************************************************/
    @NonNull
    public MeshData clone() {
        final MeshData ret = new MeshData(getId(), getName(), this.vertices, this.normals, this.colors, this.textures,
                getVerticesAttributes(), getElements(), materialFile, smoothingGroups);
        ret.setBindShapeMatrix(getBindShapeMatrix());
        ret.setJointsArray(getJointsArray());
        ret.setWeightsArray(getWeightsArray());
        return ret;
    }
}
