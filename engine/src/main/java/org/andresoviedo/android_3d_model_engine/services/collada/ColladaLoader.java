package org.andresoviedo.android_3d_model_engine.services.collada;

import android.opengl.GLES20;
import androidx.annotation.NonNull;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.animation.Animation;
import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Element;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.LoadListener;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.JointData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.MeshData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.SkeletonData;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.SkinningData;
import org.andresoviedo.android_3d_model_engine.services.collada.loader.AnimationLoader;
import org.andresoviedo.android_3d_model_engine.services.collada.loader.GeometryLoader;
import org.andresoviedo.android_3d_model_engine.services.collada.loader.MaterialLoader;
import org.andresoviedo.android_3d_model_engine.services.collada.loader.SkeletonLoader;
import org.andresoviedo.android_3d_model_engine.services.collada.loader.SkinLoader;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.io.IOUtils;
import org.andresoviedo.util.xml.XmlNode;
import org.andresoviedo.util.xml.XmlParser;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
/**************************************************************************************************/
public final class ColladaLoader {
    /**********************************************************************************************/
    public static List<String> getImages(InputStream is) {
        try {
            final XmlNode xml = XmlParser.parse(is);
            return new MaterialLoader(xml.getChild("library_materials"),
                    xml.getChild("library_effects"), xml.getChild("library_images")).getImages();
        } catch (Exception ex) {
            Log.e("ColladaLoaderTask", "Error loading materials", ex);
            return null;
        }
    }

    /**********************************************************************************************/
    @NonNull
    public List<Object3DData> load(URI uri, LoadListener callback) {
        final List<Object3DData> ret = new ArrayList<>();
        final List<MeshData> allMeshes = new ArrayList<>();

        try (InputStream is = ContentUtils.getInputStream(uri)) {

            Log.i("ColladaLoaderTask", "Parsing file... " + uri.toString());
            callback.onProgress("Loading file...");
            final XmlNode xml = XmlParser.parse(is);

            String authoring_tool = null;
            try {
                XmlNode child = xml.getChild("asset").getChild("contributor").getChild("authoring_tool");
                authoring_tool = child.getData();
                Log.i("ColladaLoaderTask", "authoring_tool: " + authoring_tool);
            } catch (Exception ignored) {
            }

            Log.i("ColladaLoaderTask", "--------------------------------------------------");
            Log.i("ColladaLoaderTask", "Loading visual nodes...");
            Log.i("ColladaLoaderTask", "--------------------------------------------------");

            callback.onProgress("Loading visual nodes...");

            Map<String, SkeletonData> skeletons = null;
            try {
                SkeletonLoader jointsLoader = new SkeletonLoader(xml);
                skeletons = jointsLoader.loadJoints();

            } catch (Exception ex) {
                Log.e("ColladaLoaderTask", "Error loading visual scene", ex);
            }

            Log.i("ColladaLoaderTask", "--------------------------------------------------");
            Log.i("ColladaLoaderTask", "Loading geometries...");
            Log.i("ColladaLoaderTask", "--------------------------------------------------");

            callback.onProgress("Loading geometries...");
            List<MeshData> meshDatas = null;
            try {
                GeometryLoader g = new GeometryLoader(xml.getChild("library_geometries"));
                List<XmlNode> geometries = xml.getChild("library_geometries").getChildren("geometry");
                meshDatas = new ArrayList<>();
                for (int i = 0; i < geometries.size(); i++) {
                    if (geometries.size() > 1) {
                        callback.onProgress("Loading geometries... " + (i + 1) + " / " + geometries.size());
                    }

                    XmlNode geometry = geometries.get(i);
                    MeshData meshData = g.loadGeometry(geometry);
                    if (meshData == null) continue;
                    meshDatas.add(meshData);
                    allMeshes.add(meshData);

                    AnimatedModel data3D = new AnimatedModel(meshData.getVertexBuffer(), null);
                    data3D.setAuthoringTool(authoring_tool);
                    data3D.setMeshData(meshData);
                    data3D.setId(meshData.getId());
                    data3D.setVertexBuffer(meshData.getVertexBuffer());
                    data3D.setNormalsBuffer(meshData.getNormalsBuffer());
                    data3D.setColorsBuffer(meshData.getColorsBuffer());
                    data3D.setElements(meshData.getElements());

                    data3D.setDrawMode(GLES20.GL_TRIANGLES);
                    data3D.setDrawUsingArrays(false);

                    if (skeletons != null) {
                        JointData jointData = null;

                        SkeletonData skeletonData = skeletons.get(meshData.getId());

                        if (skeletonData == null) {
                            skeletonData = skeletons.get("default");
                        }
                        if (skeletonData != null) {
                            jointData = skeletonData.find(meshData.getId());
                        }
                        if (jointData != null) {
                            Log.d("ColladaLoaderTask", "Mesh joint found. id: " + jointData.getName() + ", bindTransform: " + Arrays.toString(jointData.getBindTransform()));
                            data3D.setName(jointData.getName());
                            data3D.setBindTransform(jointData.getBindTransform());
                        }
                    }

                    callback.onLoad(data3D);
                    ret.add(data3D);
                }
            } catch (Exception ex) {
                Log.e("ColladaLoaderTask", "Error loading geometries", ex);
                return Collections.emptyList();
            }

            Log.i("ColladaLoaderTask", "--------------------------------------------------");
            Log.i("ColladaLoaderTask", "Loading materials...");
            Log.i("ColladaLoaderTask", "--------------------------------------------------");

            callback.onProgress("Loading materials...");

            try {
                final MaterialLoader materialLoader = new MaterialLoader(xml.getChild("library_materials"),
                        xml.getChild("library_effects"), xml.getChild("library_images"));
                for (int i = 0; i < meshDatas.size(); i++) {
                    final MeshData meshData = meshDatas.get(i);
                    final Object3DData data3D = ret.get(i);

                    materialLoader.loadMaterial(meshData);

                    data3D.setTextureBuffer(meshData.getTextureBuffer());
                }
            } catch (Exception ex) {
                Log.e("ColladaLoaderTask", "Error loading materials", ex);
            }

            Log.i("ColladaLoaderTask", "--------------------------------------------------");
            Log.i("ColladaLoaderTask", "Loading visual scene...");
            Log.i("ColladaLoaderTask", "--------------------------------------------------");

            callback.onProgress("Loading visual scene...");

            try {
                final MaterialLoader materialLoader = new MaterialLoader(xml.getChild("library_materials"),
                        xml.getChild("library_effects"), xml.getChild("library_images"));
                for (int i = 0; i < meshDatas.size(); i++) {
                    final MeshData meshData = meshDatas.get(i);
                    final Object3DData data3D = ret.get(i);

                    SkeletonData skeletonData = skeletons.get(meshData.getId());
                    if (skeletonData == null) {
                        skeletonData = skeletons.get("default");
                    }
                    materialLoader.loadMaterialFromVisualScene(meshData, skeletonData);
                }

                Log.d("ColladaLoaderTask", "Loading instance geometries...");

                for (int i = 0; i < meshDatas.size(); i++) {
                    final MeshData meshData = meshDatas.get(i);
                    final AnimatedModel data3D = (AnimatedModel) ret.get(i);

                    SkeletonData skeletonData = skeletons.get(meshData.getId());
                    if (skeletonData == null) {
                        skeletonData = skeletons.get("default");
                    }

                    if (skeletonData == null) continue;

                    List<JointData> allJointData = skeletonData.getHeadJoint().findAll(meshData.getId());
                    if (allJointData.isEmpty()) {
                        Log.d("ColladaLoaderTask", "No joint linked to mesh: " + meshData.getId());
                        continue;
                    }

                    if (allJointData.size() == 1) {
                        final JointData jointData = allJointData.get(0);
                        data3D.setBindTransform(jointData.getBindTransform());
                        continue;
                    }

                    Log.i("ColladaLoaderTask", "Found multiple instances for mesh: " + meshData.getId() + ". Total: " + allJointData.size());
                    boolean isOriginalMeshConfigured = false;
                    for (JointData jd : allJointData) {
                        if (!isOriginalMeshConfigured) {
                            data3D.setBindTransform(jd.getBindTransform());
                            isOriginalMeshConfigured = true;
                            continue;
                        }

                        Log.i("ColladaLoaderTask", "Cloning mesh for joint: " + jd.getName());
                        final AnimatedModel instance_geometry = data3D.clone();
                        instance_geometry.setId(data3D.getId() + "_instance_" + jd.getName());
                        instance_geometry.setBindTransform(jd.getBindTransform());

                        callback.onLoad(instance_geometry);
                        ret.add(instance_geometry);
                        allMeshes.add(meshData.clone());
                    }
                }
            } catch (Exception ex) {
                Log.e("ColladaLoaderTask", "Error loading visual scene", ex);
            }

            Log.i("ColladaLoaderTask", "--------------------------------------------------");
            Log.i("ColladaLoaderTask", "Loading textures...");
            Log.i("ColladaLoaderTask", "--------------------------------------------------");

            callback.onProgress("Loading textures...");

            try {
                for (int i = 0; i < meshDatas.size(); i++) {
                    final MeshData meshData = meshDatas.get(i);
                    for (int e = 0; e < meshData.getElements().size(); e++) {
                        final Element element = meshData.getElements().get(e);
                        if (element.getMaterial() != null && element.getMaterial().getTextureFile() != null) {
                            final String textureFile = element.getMaterial().getTextureFile();

                            Log.i("ColladaLoaderTask", "Reading texture file... " + textureFile);

                            try (InputStream stream = ContentUtils.getInputStream(textureFile)) {

                                element.getMaterial().setTextureData(IOUtils.read(stream));

                                Log.i("ColladaLoaderTask", "Texture linked... " + element.getMaterial().getTextureData().length + " (bytes)");

                            } catch (Exception ex) {
                                Log.e("ColladaLoaderTask", String.format("Error reading texture file: %s", ex.getMessage()));
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                Log.e("ColladaLoaderTask", "Error loading materials", ex);
            }

            Map<String, SkinningData> skins = null;

            try {
                Log.i("ColladaLoaderTask", "--------------------------------------------------");
                Log.i("ColladaLoaderTask", "Loading skinning data...");
                Log.i("ColladaLoaderTask", "--------------------------------------------------");

                XmlNode library_controllers = xml.getChild("library_controllers");
                if (library_controllers != null && !library_controllers.getChildren("controller").isEmpty()) {

                    callback.onProgress("Loading skinning data...");

                    SkinLoader skinLoader = new SkinLoader(library_controllers, 3);

                    skins = skinLoader.loadSkinData();

                    for (int i = 0; i < allMeshes.size(); i++) {
                        SkinningData skinningData = skins.get(allMeshes.get(i).getId());
                        if (skinningData != null) {
                            final MeshData meshData = allMeshes.get(i);
                            final AnimatedModel data3D = (AnimatedModel) ret.get(i);
                            meshData.setBindShapeMatrix(skinningData.getBindShapeMatrix());
                            data3D.setBindShapeMatrix(meshData.getBindShapeMatrix());
                        }
                    }

                } else {
                    Log.i("ColladaLoaderTask", "No skinning data available");
                }
            } catch (Exception ex) {
                Log.e("ColladaLoaderTask", "Error loading skinning data", ex);
            }

            final AnimationLoader loader = new AnimationLoader(xml);

            try {
                if (loader.isAnimated()) {

                    Log.i("ColladaLoaderTask", "--------------------------------------------------");
                    Log.i("ColladaLoaderTask", "Loading joints...");
                    Log.i("ColladaLoaderTask", "--------------------------------------------------");

                    callback.onProgress("Loading joints...");

                    SkeletonLoader skeletonLoader = new SkeletonLoader(xml);

                    skeletonLoader.updateJointData(skins, skeletons);

                    for (int i = 0; i < allMeshes.size(); i++) {
                        final MeshData meshData = allMeshes.get(i);
                        final AnimatedModel data3D = (AnimatedModel) ret.get(i);


                        SkeletonData skeletonData = skeletons.get(meshData.getId());
                        if (skeletonData == null) {
                            skeletonData = skeletons.get("default");
                        }

                        SkinLoader.loadSkinningData(meshData, skins != null ? skins.get(meshData.getId()) : null, skeletonData);

                        SkinLoader.loadSkinningArrays(meshData);

                        data3D.setJointIds(meshData.getJointsBuffer());
                        data3D.setVertexWeights(meshData.getWeightsBuffer());
                        Log.d("ColladaLoader", "Loaded skinning data: "
                                + "jointIds: " + (meshData.getJointsArray() != null ? meshData.getJointsArray().length : 0)
                                + ", weights: " + (meshData.getWeightsArray() != null ? meshData.getWeightsArray().length : 0));
                    }

                }
            } catch (Exception ex) {
                Log.e("ColladaLoaderTask", "Error updating joint data", ex);
            }

            try {
                if (loader.isAnimated()) {

                    Log.i("ColladaLoaderTask", "--------------------------------------------------");
                    Log.i("ColladaLoaderTask", "Loading animation... ");
                    Log.i("ColladaLoaderTask", "--------------------------------------------------");

                    callback.onProgress("Loading animation...");

                    final Animation animation = loader.load();

                    for (int i = 0; i < allMeshes.size(); i++) {
                        final MeshData meshData = allMeshes.get(i);
                        final AnimatedModel data3D = (AnimatedModel) ret.get(i);

                        SkeletonData skeletonData = skeletons.get(meshData.getId());
                        if (skeletonData == null) {
                            skeletonData = skeletons.get("default");
                        }

                        data3D.setJointsData(skeletonData);
                        data3D.doAnimation(animation);

                        data3D.setBindTransform(null);
                    }
                }
            } catch (Exception ex) {
                Log.e("ColladaLoaderTask", "Error loading animation", ex);
            }

            if (ret.isEmpty()) {
                Log.e("ColladaLoaderTask", "Mesh data list empty. Did you exclude any model in GeometryLoader.java?");
            }
            Log.i("ColladaLoaderTask", "Loading model finished. Objects: " + ret.size());

        } catch (Exception ex) {
            Log.e("ColladaLoaderTask", "Problem loading model", ex);
        }
        return ret;
    }
}
