package org.andresoviedo.android_3d_model_engine.services;

import android.app.Activity;
import android.app.AlertDialog;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Toast;

import org.andresoviedo.android_3d_model_engine.animation.Animator;
import org.andresoviedo.android_3d_model_engine.collision.CollisionEvent;
import org.andresoviedo.android_3d_model_engine.controller.TouchEvent;
import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Camera;
import org.andresoviedo.android_3d_model_engine.model.Dimensions;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.model.Transform;
import org.andresoviedo.android_3d_model_engine.objects.Point;
import org.andresoviedo.android_3d_model_engine.services.collada.ColladaLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.stl.STLLoaderTask;
import org.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoaderTask;
import org.andresoviedo.android_3d_model_engine.view.ModelSurfaceView;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.event.EventListener;
import org.andresoviedo.util.io.IOUtils;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
/**************************************************************************************************/
public class SceneLoader implements LoadListener, EventListener {
    /**********************************************************************************************/
    private static final float DEFAULT_MAX_MODEL_SIZE = 100;
    /**********************************************************************************************/
    private static final float DEFAULT_CAMERA_POSITION = DEFAULT_MAX_MODEL_SIZE / 2 + 25;
    /**********************************************************************************************/
    protected final Activity parent;
    /**********************************************************************************************/
    private final URI uri;
    /**********************************************************************************************/
    private final int type;
    /**********************************************************************************************/
    private GLSurfaceView glView;
    /**********************************************************************************************/
    private List<Object3DData> objects = new ArrayList<>();
    /**********************************************************************************************/
    private List<Object3DData> guiObjects = new ArrayList<>();
    /**********************************************************************************************/
    private Camera camera = new Camera(DEFAULT_CAMERA_POSITION);
    /**********************************************************************************************/
    private boolean isFixCoordinateSystem = false;
    /**********************************************************************************************/
    private boolean isBlendingEnabled = true;
    /**********************************************************************************************/
    private boolean isBlendingForced = false;
    /**********************************************************************************************/
    private int drawwMode = 0;
    /**********************************************************************************************/
    private boolean drawWireframe = false;
    /**********************************************************************************************/
    private boolean drawPoints = false;
    /**********************************************************************************************/
    private boolean drawBoundingBox = false;
    /**********************************************************************************************/
    private boolean drawNormals = false;
    /**********************************************************************************************/
    private boolean drawTextures = true;
    /**********************************************************************************************/
    private boolean drawColors = true;
    /**********************************************************************************************/
    private boolean rotatingLight = true;
    /**********************************************************************************************/
    private boolean drawLighting = true;
    /**********************************************************************************************/
    private boolean doAnimation = true;
    /**********************************************************************************************/
    private boolean isSmooth = false;
    /**********************************************************************************************/
    private boolean showBindPose = false;
    /**********************************************************************************************/
    private boolean drawSkeleton = false;
    /**********************************************************************************************/
    private boolean isCollision = false;
    /**********************************************************************************************/
    private boolean isStereoscopic = false;
    /**********************************************************************************************/
    private boolean isAnaglyph = false;
    /**********************************************************************************************/
    private boolean isVRGlasses = false;
    /**********************************************************************************************/
    private Object3DData selectedObject = null;
    /**********************************************************************************************/
    private final Object3DData lightBulb = Point.build(new float[]{0, 0, 0}).setId("light");
    /**********************************************************************************************/
    private Animator animator = new Animator();
    /**********************************************************************************************/
    private boolean userHasInteracted;
    /**********************************************************************************************/
    private long startTime;
    /**********************************************************************************************/
    private Map<Object3DData, Dimensions> originalDimensions = new HashMap<>();
    private Map<Object3DData, Transform> originalTransforms = new HashMap<>();

    /**********************************************************************************************/
    public SceneLoader(Activity main, URI uri, int type, GLSurfaceView glView) {
        this.parent = main;
        this.uri = uri;
        this.type = type;
        this.glView = glView;

        lightBulb.setLocation(new float[]{0, 0, DEFAULT_CAMERA_POSITION});
    }

    /**********************************************************************************************/
    public void init() {

        camera.setChanged(true);

        if (uri == null) {
            return;
        }

        Log.i("SceneLoader", "Loading model " + uri + ". async and parallel..");
        if (uri.toString().toLowerCase().endsWith(".obj") || type == 0) {
            new WavefrontLoaderTask(parent, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".stl") || type == 1) {
            Log.i("SceneLoader", "Loading STL object from: " + uri);
            new STLLoaderTask(parent, uri, this).execute();
        } else if (uri.toString().toLowerCase().endsWith(".dae") || type == 2) {
            Log.i("SceneLoader", "Loading Collada object from: " + uri);
            new ColladaLoaderTask(parent, uri, this).execute();
        }
    }

    /**********************************************************************************************/
    public void fixCoordinateSystem() {
        final List<Object3DData> objects = getObjects();
        for (int i = 0; i < objects.size(); i++) {
            final Object3DData objData = objects.get(i);
            if (objData.getAuthoringTool() != null && objData.getAuthoringTool().toLowerCase().contains("blender")) {
                getCamera().rotate(90f, 1, 0, 0);
                Log.i("SceneLoader", "Fixed coordinate system to 90 degrees on x axis. object: " + objData.getId());
                this.isFixCoordinateSystem = true;
                break;
            }
        }
    }

    /**********************************************************************************************/
    public boolean isFixCoordinateSystem() {
        return this.isFixCoordinateSystem;
    }

    /**********************************************************************************************/
    public final Camera getCamera() {
        return camera;
    }

    /**********************************************************************************************/
    private final void makeToastText(final String text, final int toastDuration) {
        parent.runOnUiThread(() -> Toast.makeText(parent.getApplicationContext(), text, toastDuration).show());
    }

    /**********************************************************************************************/
    public final Object3DData getLightBulb() {
        return lightBulb;
    }

    /**********************************************************************************************/
    public final void onDrawFrame() {
        animateLight();

        camera.animate();

        if (!userHasInteracted) {
            animateCamera();
        }

        if (objects.isEmpty()) return;

        if (doAnimation) {
            for (int i = 0; i < objects.size(); i++) {
                Object3DData obj = objects.get(i);
                animator.update(obj, isShowBindPose());
            }
        }
    }

    /**********************************************************************************************/
    private void animateLight() {
        if (!rotatingLight) return;

        long time = SystemClock.uptimeMillis() % 5000L;
        float angleInDegrees = (360.0f / 5000.0f) * ((int) time);
        lightBulb.setRotation1(new float[]{0, angleInDegrees, 0});
    }

    /**********************************************************************************************/
    private void animateCamera() {
        camera.translateCamera(0.0005f, 0f);
    }

    /**********************************************************************************************/
    public final synchronized void addObject(Object3DData obj) {
        Log.i("SceneLoader", "Adding object to scene... " + obj);
        objects.add(obj);
    }

    /**********************************************************************************************/
    public final synchronized void addGUIObject(Object3DData obj) {
        Log.i("SceneLoader", "Adding GUI object to scene... " + obj);

        guiObjects.add(obj);
    }

    /**********************************************************************************************/
    private void requestRender() {
        if (glView != null) {
            glView.requestRender();
        }
    }

    /**********************************************************************************************/
    public final synchronized List<Object3DData> getObjects() {
        return objects;
    }

    /**********************************************************************************************/
    public final synchronized List<Object3DData> getGUIObjects() {
        return guiObjects;
    }

    /**********************************************************************************************/
    public final void toggleWireframe() {
        final int module = 4;

        this.drawwMode = (this.drawwMode + 1) % module;
        this.drawNormals = false;
        this.drawPoints = false;
        this.drawSkeleton = false;
        this.drawWireframe = false;

        switch (drawwMode) {
            case 0:
                makeToastText("Faces", Toast.LENGTH_SHORT);
                break;
            case 1:
                this.drawWireframe = true;
                makeToastText("Wireframe", Toast.LENGTH_SHORT);
                break;
            case 2:
                this.drawPoints = true;
                makeToastText("Points", Toast.LENGTH_SHORT);
                break;
            case 3:
                this.drawSkeleton = true;
                makeToastText("Skeleton", Toast.LENGTH_SHORT);
                break;
            case 4:
                this.drawWireframe = true;
                this.drawNormals = true;
                makeToastText("Normals", Toast.LENGTH_SHORT);
                break;
        }
        requestRender();
    }

    /**********************************************************************************************/
    public final boolean isDrawWireframe() {
        return this.drawWireframe;
    }

    /**********************************************************************************************/
    public final boolean isDrawPoints() {
        return this.drawPoints;
    }

    /**********************************************************************************************/
    public final void toggleBoundingBox() {
        this.drawBoundingBox = !drawBoundingBox;
        requestRender();
    }

    /**********************************************************************************************/
    public final boolean isDrawBoundingBox() {
        return drawBoundingBox;
    }

    /**********************************************************************************************/
    public final boolean isDrawNormals() {
        return drawNormals;
    }

    /**********************************************************************************************/
    public final void toggleTextures() {
        if (drawTextures && drawColors) {
            this.drawTextures = false;
            this.drawColors = true;
            makeToastText("Texture off", Toast.LENGTH_SHORT);
        } else if (drawColors) {
            this.drawTextures = false;
            this.drawColors = false;
            makeToastText("Colors off", Toast.LENGTH_SHORT);
        } else {
            this.drawTextures = true;
            this.drawColors = true;
            makeToastText("Textures on", Toast.LENGTH_SHORT);
        }
    }

    /**********************************************************************************************/
    public final void toggleLighting() {
        if (this.drawLighting && this.rotatingLight) {
            this.rotatingLight = false;
            makeToastText("Light stopped", Toast.LENGTH_SHORT);
        } else if (this.drawLighting && !this.rotatingLight) {
            this.drawLighting = false;
            makeToastText("Lights off", Toast.LENGTH_SHORT);
        } else {
            this.drawLighting = true;
            this.rotatingLight = true;
            makeToastText("Light on", Toast.LENGTH_SHORT);
        }
        requestRender();
    }

    /**********************************************************************************************/
    public final void toggleAnimation() {
        if (!this.doAnimation) {
            this.doAnimation = true;
            this.showBindPose = false;
            makeToastText("Animation on", Toast.LENGTH_SHORT);
        } else {
            this.doAnimation = false;
            this.showBindPose = true;
            makeToastText("Bind pose", Toast.LENGTH_SHORT);
        }
    }

    /**********************************************************************************************/
    public final void toggleSmooth() {
        for (int i = 0; i < getObjects().size(); i++) {
            if (!this.isSmooth) {
                getObjects().get(0).getMeshData().smooth();
            } else {
                getObjects().get(0).getMeshData().unSmooth();
            }
            getObjects().get(0).getMeshData().refreshNormalsBuffer();
        }
        this.isSmooth = !this.isSmooth;
    }

    /**********************************************************************************************/
    public final void showSettingsDialog() {

        final AnimatedModel animatedModel;
        if (objects.get(0) instanceof AnimatedModel) {
            animatedModel = (AnimatedModel) objects.get(0);
        } else return;

        AlertDialog.Builder builderSingle = new AlertDialog.Builder(this.parent);
        builderSingle.setTitle("Scene");
        String[] items = new String[]{"Geometries...", "Joints..."};
        builderSingle.setItems(items, (dialog, which) -> {
            switch (which) {
                case 0:
                    showGeometriesDialog(animatedModel);
                    break;
                case 1:
                    showJointsDialog(animatedModel);
                    break;
            }
        });
        builderSingle.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });

        builderSingle.show();
    }

    /**********************************************************************************************/
    private void showGeometriesDialog(final AnimatedModel animatedModel) {
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this.parent);
        builderSingle.setTitle("Geometries");
        final String[] items = new String[animatedModel.getElements().size()];
        boolean[] selected = new boolean[animatedModel.getElements().size()];
        for (int i = 0; i < items.length; i++) {
            String jointId = "Element #" + i;
            items[i] = jointId;
            selected[i] = true;
        }

        builderSingle.setMultiChoiceItems(items, selected, (dialog, which, isChecked) -> {
            animatedModel.getElements().remove(which);
        });

        builderSingle.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });
        builderSingle.show();
    }

    /**********************************************************************************************/
    private void showJointsDialog(final AnimatedModel animatedModel) {
        final AlertDialog.Builder builderSingle = new AlertDialog.Builder(this.parent);
        builderSingle.setTitle("Joints");
        final String[] items = new String[animatedModel.getElements().size()];
        boolean[] selected = new boolean[animatedModel.getElements().size()];
        for (int i = 0; i < items.length; i++) {
            String jointId = "Joint #" + i;
            items[i] = jointId;
            selected[i] = true;
        }

        builderSingle.setMultiChoiceItems(items, selected, (dialog, which, isChecked) -> {
            animatedModel.getElements().remove(which);
        });

        builderSingle.setPositiveButton("Close", (dialog, which) -> {
            dialog.dismiss();
        });
        builderSingle.show();
    }

    /**********************************************************************************************/
    public final boolean isDoAnimation() {
        return doAnimation;
    }

    /**********************************************************************************************/
    public final boolean isShowBindPose() {
        return showBindPose;
    }

    /**********************************************************************************************/
    public final void toggleCollision() {
        this.isCollision = !isCollision;
        makeToastText("Collisions: " + isCollision, Toast.LENGTH_SHORT);
    }

    /**********************************************************************************************/
    public final void toggleStereoscopic() {
        if (!this.isStereoscopic) {
            this.isStereoscopic = true;
            this.isAnaglyph = true;
            this.isVRGlasses = false;
            makeToastText("Stereoscopic Anaplygh", Toast.LENGTH_SHORT);
        } else if (this.isAnaglyph) {
            this.isAnaglyph = false;
            this.isVRGlasses = true;
            this.userHasInteracted = false;
            makeToastText("Stereoscopic VR Glasses", Toast.LENGTH_SHORT);
        } else {
            this.isStereoscopic = false;
            this.isAnaglyph = false;
            this.isVRGlasses = false;
            makeToastText("Stereoscopic disabled", Toast.LENGTH_SHORT);
        }
        this.camera.setChanged(true);
    }

    /**********************************************************************************************/
    public final boolean isVRGlasses() {
        return isVRGlasses;
    }

    /**********************************************************************************************/
    public final boolean isDrawTextures() {
        return drawTextures;
    }

    /**********************************************************************************************/
    public final boolean isDrawColors() {
        return drawColors;
    }

    /**********************************************************************************************/
    public final boolean isDrawLighting() {
        return drawLighting;
    }

    /**********************************************************************************************/
    public final boolean isDrawSkeleton() {
        return drawSkeleton;
    }

    /**********************************************************************************************/
    public final boolean isCollision() {
        return isCollision;
    }

    /**********************************************************************************************/
    public final boolean isStereoscopic() {
        return isStereoscopic;
    }

    /**********************************************************************************************/
    public final boolean isAnaglyph() {
        return isAnaglyph;
    }

    /**********************************************************************************************/
    public final void toggleBlending() {
        if (this.isBlendingEnabled && !this.isBlendingForced) {
            makeToastText("X-Ray enabled", Toast.LENGTH_SHORT);
            this.isBlendingEnabled = true;
            this.isBlendingForced = true;
        } else if (this.isBlendingForced) {
            makeToastText("Blending disabled", Toast.LENGTH_SHORT);
            this.isBlendingEnabled = false;
            this.isBlendingForced = false;
        } else {
            makeToastText("X-Ray disabled", Toast.LENGTH_SHORT);
            this.isBlendingEnabled = true;
            this.isBlendingForced = false;
        }
    }

    /**********************************************************************************************/
    public final boolean isBlendingEnabled() {
        return isBlendingEnabled;
    }

    /**********************************************************************************************/
    public final boolean isBlendingForced() {
        return isBlendingForced;
    }

    /**********************************************************************************************/
    @Override
    public void onStart() {
        startTime = SystemClock.uptimeMillis();

        ContentUtils.setThreadActivity(parent);
    }

    /**********************************************************************************************/
    @Override
    public void onProgress(String progress) {
    }

    /**********************************************************************************************/
    @Override
    public synchronized void onLoad(Object3DData data) {
        if (doAnimation) {
            animator.update(data, isShowBindPose());
        }
        addObject(data);
    }

    /**********************************************************************************************/
    @Override
    public synchronized void onLoadComplete() {
        final List<Object3DData> objs = getObjects();

        List<String> allErrors = new ArrayList<>();
        for (Object3DData data : objs) {
            allErrors.addAll(data.getErrors());
        }
        if (!allErrors.isEmpty()) {
            makeToastText(allErrors.toString(), Toast.LENGTH_LONG);
        }

        final String elapsed = (SystemClock.uptimeMillis() - startTime) / 1000 + " secs";
        makeToastText("Load complete (" + elapsed + ")", Toast.LENGTH_LONG);

        ContentUtils.setThreadActivity(null);

        rescale(this.getObjects(), DEFAULT_MAX_MODEL_SIZE, new float[3]);

        fixCoordinateSystem();
    }

    /**********************************************************************************************/
    private void rescale(List<Object3DData> objs) {
        Log.v("SceneLoader", "Rescaling objects... " + objs.size());

        float largest = 1;

        for (int i = 0; i < objs.size(); i++) {
            Object3DData data = objs.get(i);
            float candidate = data.getCurrentDimensions().getLargest();
            if (candidate > largest) {
                largest = candidate;
            }
        }

        Log.v("SceneLoader", "Object largest dimension: " + largest);

        float ratio = DEFAULT_MAX_MODEL_SIZE / largest;
        Log.v("SceneLoader", "Scaling " + objs.size() + " objects with factor: " + ratio);
        float[] newScale = new float[]{ratio, ratio, ratio};

        for (Object3DData data : objs) {
            data.setScale(newScale);
        }
    }

    /**********************************************************************************************/
    @Override
    public void onLoadError(Exception ex) {
        Log.e("SceneLoader", ex.getMessage(), ex);
        makeToastText("There was a problem building the model: " + ex.getMessage(), Toast.LENGTH_LONG);
        ContentUtils.setThreadActivity(null);
    }

    /**********************************************************************************************/
    public Object3DData getSelectedObject() {
        return selectedObject;
    }

    /**********************************************************************************************/
    private void setSelectedObject(Object3DData selectedObject) {
        this.selectedObject = selectedObject;
    }

    /**********************************************************************************************/
    public void loadTexture(Object3DData obj, Uri uri) throws IOException {
        if (obj == null && objects.size() != 1) {
            makeToastText("Unavailable", Toast.LENGTH_SHORT);
            return;
        }
        obj = obj != null ? obj : objects.get(0);
        /**********************************************************************************************/
        obj.setTextureData(IOUtils.read(ContentUtils.getInputStream(uri)));

        this.drawTextures = true;
    }

    /**********************************************************************************************/
    public final boolean isRotatingLight() {
        return rotatingLight;
    }

    /**********************************************************************************************/
    public void setView(ModelSurfaceView view) {
        this.glView = view;
    }

    /**********************************************************************************************/
    @Override
    public boolean onEvent(EventObject event) {
        if (event instanceof TouchEvent) {
            userHasInteracted = true;
        } else if (event instanceof CollisionEvent) {
            Object3DData objectToSelect = ((CollisionEvent) event).getObject();
            Object3DData point = ((CollisionEvent) event).getPoint();
            if (isCollision() && point != null) {
                addObject(point);
            } else {
                if (getSelectedObject() == objectToSelect) {
                    Log.i("SceneLoader", "Unselected object " + objectToSelect.getId());
                    Log.d("SceneLoader", "Unselected object " + objectToSelect);
                    setSelectedObject(null);
                } else {
                    Log.i("SceneLoader", "Selected object " + objectToSelect.getId());
                    Log.d("SceneLoader", "Selected object " + objectToSelect);
                    setSelectedObject(objectToSelect);
                }
            }
        }
        return true;
    }

    /**********************************************************************************************/
    private void rescale(List<Object3DData> datas, float newScale, float[] newPosition) {
        if (datas == null || datas.isEmpty()) {
            return;
        }

        Log.d("SceneLoader", "Scaling datas... total: " + datas.size());

        final Object3DData firstObject = datas.get(0);
        final Dimensions currentDimensions;
        if (this.originalDimensions.containsKey(firstObject)) {
            currentDimensions = this.originalDimensions.get(firstObject);
        } else {
            currentDimensions = firstObject.getCurrentDimensions();
            this.originalDimensions.put(firstObject, currentDimensions);
        }
        Log.v("SceneLoader", "Model[0] dimension: " + currentDimensions.toString());

        final float[] corner01 = currentDimensions.getCornerLeftTopNearVector();
        final float[] corner02 = currentDimensions.getCornerRightBottomFar();

        final float[] center01 = currentDimensions.getCenter();

        float maxLeft = corner01[0];
        float maxTop = corner01[1];
        float maxNear = corner01[2];
        float maxRight = corner02[0];
        float maxBottom = corner02[1];
        float maxFar = corner02[2];
        float maxCenterX = center01[0];
        float maxCenterY = center01[1];
        float maxCenterZ = center01[2];

        for (int i = 1; i < datas.size(); i++) {

            final Object3DData obj = datas.get(i);

            final Dimensions original;
            if (this.originalDimensions.containsKey(obj)) {
                original = this.originalDimensions.get(obj);
                Log.v("SceneLoader", "Found dimension: " + original.toString());
            } else {
                original = obj.getCurrentDimensions();
                this.originalDimensions.put(obj, original);
            }

            Log.v("SceneLoader", "Model[" + i + "] '" + obj.getId() + "' dimension: " + original.toString());

            final float[] corner1 = original.getCornerLeftTopNearVector();
            final float[] corner2 = original.getCornerRightBottomFar();
            final float[] center = original.getCenter();

            float maxLeft2 = corner1[0];
            float maxTop2 = corner1[1];
            float maxNear2 = corner1[2];
            float maxRight2 = corner2[0];
            float maxBottom2 = corner2[1];

            float maxFar2 = corner2[2];

            float centerX = center[0];
            float centerY = center[1];
            float centerZ = center[2];

            if (maxRight2 > maxRight) maxRight = maxRight2;
            if (maxLeft2 < maxLeft) maxLeft = maxLeft2;
            if (maxTop2 > maxTop) maxTop = maxTop2;
            if (maxBottom2 < maxBottom) maxBottom = maxBottom2;
            if (maxNear2 > maxNear) maxNear = maxNear2;
            if (maxFar2 < maxFar) maxFar = maxFar2;
            if (maxCenterX < centerX) maxCenterX = centerX;
            if (maxCenterY < centerY) maxCenterY = centerY;
            if (maxCenterZ < centerZ) maxCenterZ = centerZ;
        }
        float lengthX = maxRight - maxLeft;
        float lengthY = maxTop - maxBottom;
        float lengthZ = maxNear - maxFar;

        float maxLength = lengthX;

        if (lengthY > maxLength) maxLength = lengthY;
        if (lengthZ > maxLength) maxLength = lengthZ;

        Log.v("SceneLoader", "Max length: " + maxLength);

        float maxLocation = 0;
        if (datas.size() > 1) {
            maxLocation = maxCenterX;
            if (maxCenterY > maxLocation) maxLocation = maxCenterY;
            if (maxCenterZ > maxLocation) maxLocation = maxCenterZ;
        }

        Log.v("SceneLoader", "Max location: " + maxLocation);

        float scaleFactor = newScale / (maxLength + maxLocation);
        final float[] finalScale = new float[]{scaleFactor, scaleFactor, scaleFactor};

        Log.d("SceneLoader", "New scale: " + scaleFactor);

        float centerX = (maxRight + maxLeft) / 2;
        float centerY = (maxTop + maxBottom) / 2;
        float centerZ = (maxNear + maxFar) / 2;

        Log.d("SceneLoader", "Total center: " + centerX + "," + centerY + "," + centerZ);

        float translationX = -centerX + newPosition[0];
        float translationY = -centerY + newPosition[1];
        float translationZ = -centerZ + newPosition[2];

        final float[] globalDifference = new float[]{translationX * scaleFactor, translationY * scaleFactor, translationZ * scaleFactor};

        Log.d("SceneLoader", "Total translation: " + Arrays.toString(globalDifference));

        for (Object3DData data : datas) {

            final Transform original;

            if (this.originalTransforms.containsKey(data)) {
                original = this.originalTransforms.get(data);
                Log.v("SceneLoader", "Found transform: " + original);
            } else {
                original = data.getTransform();
                this.originalTransforms.put(data, original);
            }

            float localScaleX = scaleFactor * original.getScale()[0];
            float localScaleY = scaleFactor * original.getScale()[1];
            float localScaleZ = scaleFactor * original.getScale()[2];

            data.setScale(new float[]{localScaleX, localScaleY, localScaleZ});

            Log.v("SceneLoader", "Mew model scale: " + Arrays.toString(data.getScale()));

            float localTranlactionX = original.getLocation()[0] * scaleFactor;
            float localTranlactionY = original.getLocation()[1] * scaleFactor;
            float localTranlactionZ = original.getLocation()[2] * scaleFactor;

            data.setLocation(new float[]{localTranlactionX, localTranlactionY, localTranlactionZ});

            Log.v("SceneLoader", "Mew model location: " + Arrays.toString(data.getLocation()));

            data.translate(globalDifference);

            Log.v("SceneLoader", "Mew model translated: " + Arrays.toString(data.getLocation()));
        }
    }
}
