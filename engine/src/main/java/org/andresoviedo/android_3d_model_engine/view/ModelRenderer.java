package org.andresoviedo.android_3d_model_engine.view;

import android.app.Activity;
import android.opengl.GLES20;
import android.opengl.GLSurfaceView;
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.animation.Animator;
import org.andresoviedo.android_3d_model_engine.drawer.Renderer;
import org.andresoviedo.android_3d_model_engine.drawer.RendererFactory;
import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Camera;
import org.andresoviedo.android_3d_model_engine.model.Element;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.objects.Axis;
import org.andresoviedo.android_3d_model_engine.objects.BoundingBox;
import org.andresoviedo.android_3d_model_engine.objects.Grid;
import org.andresoviedo.android_3d_model_engine.objects.Line;
import org.andresoviedo.android_3d_model_engine.objects.Normals;
import org.andresoviedo.android_3d_model_engine.objects.Skeleton;
import org.andresoviedo.android_3d_model_engine.objects.SkyBox;
import org.andresoviedo.android_3d_model_engine.objects.Wireframe;
import org.andresoviedo.android_3d_model_engine.services.SceneLoader;
import org.andresoviedo.util.android.AndroidUtils;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.android.GLUtil;
import org.andresoviedo.util.event.EventListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;
/**************************************************************************************************/
public class ModelRenderer implements GLSurfaceView.Renderer {
    /**********************************************************************************************/
    public static class ViewEvent extends EventObject {

        private final Code code;
        private final int width;
        private final int height;

        public enum Code {SURFACE_CREATED, SURFACE_CHANGED}

        public ViewEvent(Object source, Code code, int width, int height) {
            super(source);
            this.code = code;
            this.width = width;
            this.height = height;
        }

        public Code getCode() {
            return code;
        }

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }
    }

    /**********************************************************************************************/
    public static class FPSEvent extends EventObject {

        private final int fps;

        public FPSEvent(Object source, int fps) {
            super(source);
            this.fps = fps;
        }

        public int getFps() {
            return fps;
        }
    }

    /**********************************************************************************************/
    private final static String TAG = ModelRenderer.class.getSimpleName();
    /**********************************************************************************************/
    private static final float GRID_WIDTH = 100f;
    private static final float GRID_SIZE = 10f;
    private static final float[] GRID_COLOR = {0.25f, 0.25f, 0.25f, 0.5f};
    /**********************************************************************************************/
    private static final float[] BLENDING_MASK_DEFAULT = {1.0f, 1.0f, 1.0f, 1.0f};
    /**********************************************************************************************/
    private static final float[] BLENDING_MASK_FORCED = {1.0f, 1.0f, 1.0f, 0.5f};
    /**********************************************************************************************/
    private static final float near = 1.0f;
    /**********************************************************************************************/
    private static final float far = 5000f;
    /**********************************************************************************************/
    private static float EYE_DISTANCE = 0.64f;
    private static final float[] COLOR_RED = {1.0f, 0.0f, 0.0f, 1f};
    private static final float[] COLOR_BLUE = {0.0f, 1.0f, 0.0f, 1f};
    private static final float[] COLOR_WHITE = {1f, 1f, 1f, 1f};
    private static final float[] COLOR_HALF_TRANSPARENT = {1f, 1f, 1f, 0.5f};
    private static final float[] COLOR_ALMOST_TRANSPARENT = {1f, 1f, 1f, 0.1f};
    /**********************************************************************************************/
    private final float[] backgroundColor;
    /**********************************************************************************************/
    private final SceneLoader scene;
    /**********************************************************************************************/
    private final List<EventListener> listeners = new ArrayList<>();
    /**********************************************************************************************/
    private GLSurfaceView main;
    /**********************************************************************************************/
    private int width;
    /**********************************************************************************************/
    private int height;
    /**********************************************************************************************/
    private float ratio;
    /**********************************************************************************************/
    private final RendererFactory drawer;
    /**********************************************************************************************/
    private long framesPerSecondTime = -1;
    private int framesPerSecond = 0;
    private int framesPerSecondCounter = 0;
    /**********************************************************************************************/
    private Map<Object3DData, Object3DData> wireframes = new HashMap<>();
    /**********************************************************************************************/
    private Map<Object, Integer> textures = new HashMap<>();
    /**********************************************************************************************/
    private Map<Object3DData, Object3DData> boundingBoxes = new HashMap<>();
    /**********************************************************************************************/
    private Map<Object3DData, Object3DData> normals = new HashMap<>();
    /**********************************************************************************************/
    private Map<Object3DData, Object3DData> skeleton = new HashMap<>();
    /**********************************************************************************************/
    private boolean debugSkeleton = false;
    /**********************************************************************************************/
    private final float[] viewMatrix = new float[16];
    private final float[] projectionMatrix = new float[16];
    private final float[] viewProjectionMatrix = new float[16];
    /**********************************************************************************************/
    private final float[] tempVector4 = new float[4];
    private final float[] lightPosInWorldSpace = new float[3];
    private final float[] cameraPosInWorldSpace = new float[3];
    private final float[] lightPosition = new float[]{0, 0, 0, 1};
    /**********************************************************************************************/
    private final List<Object3DData> extras = new ArrayList<>();
    private final Object3DData axis = Axis.build().setId("axis").setSolid(false).setScale(new float[]{50, 50, 50});
    private final Object3DData gridx = Grid.build(-GRID_WIDTH, 0f, -GRID_WIDTH, GRID_WIDTH, 0f, GRID_WIDTH, GRID_SIZE).setColor(GRID_COLOR).setId("grid-x").setSolid(false);
    private final Object3DData gridy = Grid.build(-GRID_WIDTH, -GRID_WIDTH, 0f, GRID_WIDTH, GRID_WIDTH, 0f, GRID_SIZE).setColor(GRID_COLOR).setId("grid-y").setSolid(false);
    private final Object3DData gridz = Grid.build(0, -GRID_WIDTH, -GRID_WIDTH, 0, GRID_WIDTH, GRID_WIDTH, GRID_SIZE).setColor(GRID_COLOR).setId("grid-z").setSolid(false);

    {
        extras.add(axis);
        extras.add(gridx);
        extras.add(gridy);
        extras.add(gridz);
    }

    /**********************************************************************************************/
    private final float[] viewMatrixLeft = new float[16];
    private final float[] projectionMatrixLeft = new float[16];
    private final float[] viewProjectionMatrixLeft = new float[16];
    private final float[] viewMatrixRight = new float[16];
    private final float[] projectionMatrixRight = new float[16];
    private final float[] viewProjectionMatrixRight = new float[16];
    /**********************************************************************************************/
    private boolean lightsEnabled = true;
    private boolean wireframeEnabled = false;
    private boolean texturesEnabled = true;
    private boolean colorsEnabled = true;
    private boolean animationEnabled = true;
    /**********************************************************************************************/
    private boolean isDrawSkyBox = true;
    private int isUseskyBoxId = 0;
    private final float[] projectionMatrixSkyBox = new float[16];
    private final float[] viewMatrixSkyBox = new float[16];
    private SkyBox[] skyBoxes = null;
    private Object3DData[] skyBoxes3D = null;
    /**********************************************************************************************/
    private Map<String, Boolean> infoLogged = new HashMap<>();
    /**********************************************************************************************/
    private boolean anaglyphSwitch = false;
    /**********************************************************************************************/
    private Animator animator = new Animator();
    /**********************************************************************************************/
    private boolean fatalException = false;

    /**********************************************************************************************/
    public ModelRenderer(Activity parent, ModelSurfaceView modelSurfaceView,
                         float[] backgroundColor, SceneLoader scene) throws IOException, IllegalAccessException {
        this.main = modelSurfaceView;
        this.backgroundColor = backgroundColor;
        this.scene = scene;
        this.drawer = new RendererFactory(parent);
    }

    /**********************************************************************************************/
    public ModelRenderer addListener(EventListener listener) {
        this.listeners.add(listener);
        return this;
    }

    /**********************************************************************************************/
    public float getNear() {
        return near;
    }

    /**********************************************************************************************/
    public float getFar() {
        return far;
    }

    /**********************************************************************************************/
    public void toggleLights() {
        lightsEnabled = !lightsEnabled;
    }

    /**********************************************************************************************/
    public void toggleSkyBox() {
        isUseskyBoxId++;

        if (isUseskyBoxId > 1) {
            isUseskyBoxId = -3;
        }

        Log.i("ModelRenderer", "Toggled skybox. Idx: " + isUseskyBoxId);
    }

    /**********************************************************************************************/
    public boolean isLightsEnabled() {
        return lightsEnabled;
    }

    /**********************************************************************************************/
    public void toggleWireframe() {
        this.wireframeEnabled = !wireframeEnabled;
    }

    /**********************************************************************************************/
    public void toggleTextures() {
        this.texturesEnabled = !texturesEnabled;
    }

    /**********************************************************************************************/
    public void toggleColors() {
        this.colorsEnabled = !colorsEnabled;
    }

    /**********************************************************************************************/
    public void toggleAnimation() {
        this.animationEnabled = !animationEnabled;
    }

    /**********************************************************************************************/
    @Override
    public void onSurfaceCreated(GL10 unused, EGLConfig config) {
        Log.d(TAG, "onSurfaceCreated. config: " + config);

        GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

        GLES20.glEnable(GLES20.GL_DEPTH_TEST);

        GLES20.glEnable(GLES20.GL_SCISSOR_TEST);

        AndroidUtils.fireEvent(listeners, new ViewEvent(this, ViewEvent.Code.SURFACE_CREATED, 0, 0));

        ContentUtils.setThreadActivity(main.getContext());
        skyBoxes = new SkyBox[]{SkyBox.getSkyBox1(), SkyBox.getSkyBox2()};
        skyBoxes3D = new Object3DData[skyBoxes.length];
    }

    /**********************************************************************************************/
    @Override
    public void onSurfaceChanged(GL10 unused, int width, int height) {
        this.width = width;
        this.height = height;

        GLES20.glViewport(0, 0, width, height);

        this.ratio = (float) width / height;

        Log.d(TAG, "onSurfaceChanged: projection: [" + -ratio + "," + ratio + ",-1,1]-near/far[1,10]");

        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1, 1, getNear(), getFar());
        Matrix.frustumM(projectionMatrixRight, 0, -ratio, ratio, -1, 1, getNear(), getFar());
        Matrix.frustumM(projectionMatrixLeft, 0, -ratio, ratio, -1, 1, getNear(), getFar());

        Matrix.orthoM(projectionMatrixSkyBox, 0, -ratio, ratio, -1, 1, getNear(), getFar());

        AndroidUtils.fireEvent(listeners, new ViewEvent(this, ViewEvent.Code.SURFACE_CHANGED, width, height));
    }

    /**********************************************************************************************/
    @Override
    public void onDrawFrame(GL10 unused) {
        if (fatalException) {
            return;
        }

        try {

            GLES20.glViewport(0, 0, width, height);
            GLES20.glScissor(0, 0, width, height);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);

            if (scene == null) {
                return;
            }

            float[] colorMask = BLENDING_MASK_DEFAULT;

            if (scene.isBlendingEnabled()) {
                GLES20.glEnable(GLES20.GL_BLEND);
                GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);
                if (scene.isBlendingForced()) {
                    colorMask = BLENDING_MASK_FORCED;
                }
            } else {
                GLES20.glDisable(GLES20.GL_BLEND);
            }

            scene.onDrawFrame();

            Camera camera = scene.getCamera();

            cameraPosInWorldSpace[0] = camera.getxPos();
            cameraPosInWorldSpace[1] = camera.getyPos();
            cameraPosInWorldSpace[2] = camera.getzPos();

            if (camera.hasChanged()) {
                float ratio = (float) width / height;

                if (!scene.isStereoscopic()) {
                    Matrix.setLookAtM(viewMatrix, 0, camera.getxPos(), camera.getyPos(), camera.getzPos(), camera.getxView(), camera.getyView(), camera.getzView(), camera.getxUp(), camera.getyUp(), camera.getzUp());
                    Matrix.multiplyMM(viewProjectionMatrix, 0, projectionMatrix, 0, viewMatrix, 0);
                } else {
                    Camera[] stereoCamera = camera.toStereo(EYE_DISTANCE);
                    Camera leftCamera = stereoCamera[0];
                    Camera rightCamera = stereoCamera[1];

                    Matrix.setLookAtM(viewMatrixLeft, 0, leftCamera.getxPos(), leftCamera.getyPos(), leftCamera.getzPos(), leftCamera.getxView(), leftCamera.getyView(), leftCamera.getzView(), leftCamera.getxUp(), leftCamera.getyUp(), leftCamera.getzUp());

                    Matrix.setLookAtM(viewMatrixRight, 0, rightCamera.getxPos(), rightCamera.getyPos(), rightCamera.getzPos(), rightCamera .getxView(), rightCamera.getyView(), rightCamera.getzView(), rightCamera.getxUp(), rightCamera.getyUp(), rightCamera.getzUp());

                    if (scene.isAnaglyph()) {
                        Matrix.frustumM(projectionMatrixRight, 0, -ratio, ratio, -1, 1, getNear(), getFar());
                        Matrix.frustumM(projectionMatrixLeft, 0, -ratio, ratio, -1, 1, getNear(), getFar());
                    } else if (scene.isVRGlasses()) {
                        float ratio2 = (float) width / 2 / height;
                        Matrix.frustumM(projectionMatrixRight, 0, -ratio2, ratio2, -1, 1, getNear(), getFar());
                        Matrix.frustumM(projectionMatrixLeft, 0, -ratio2, ratio2, -1, 1, getNear(), getFar());
                    }

                    Matrix.multiplyMM(viewProjectionMatrixLeft, 0, projectionMatrixLeft, 0, viewMatrixLeft, 0);
                    Matrix.multiplyMM(viewProjectionMatrixRight, 0, projectionMatrixRight, 0, viewMatrixRight, 0);
                }

                camera.setChanged(false);
            }


            if (!scene.isStereoscopic()) {
                this.onDrawFrame(viewMatrix, projectionMatrix, viewProjectionMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
                return;
            }


            if (scene.isAnaglyph()) {
                if (anaglyphSwitch) {
                    this.onDrawFrame(viewMatrixLeft, projectionMatrixLeft, viewProjectionMatrixLeft, lightPosInWorldSpace, COLOR_RED, cameraPosInWorldSpace);
                } else {
                    this.onDrawFrame(viewMatrixRight, projectionMatrixRight, viewProjectionMatrixRight, lightPosInWorldSpace, COLOR_BLUE, cameraPosInWorldSpace);
                }
                anaglyphSwitch = !anaglyphSwitch;
                return;
            }

            if (scene.isVRGlasses()) {
                GLES20.glViewport(0, 0, width / 2, height);
                GLES20.glScissor(0, 0, width / 2, height);

                this.onDrawFrame(viewMatrixLeft, projectionMatrixLeft, viewProjectionMatrixLeft, lightPosInWorldSpace, null, cameraPosInWorldSpace);

                GLES20.glViewport(width / 2, 0, width / 2, height);
                GLES20.glScissor(width / 2, 0, width / 2, height);

                this.onDrawFrame(viewMatrixRight, projectionMatrixRight, viewProjectionMatrixRight, lightPosInWorldSpace, null, cameraPosInWorldSpace);
            }
        } catch (Exception ex) {
            Log.e("ModelRenderer", "Fatal exception: " + ex.getMessage(), ex);
            fatalException = true;
        } catch (Error err) {
            Log.e("ModelRenderer", "Fatal error: " + err.getMessage(), err);
            fatalException = true;
        }
    }

    /**********************************************************************************************/
    private void onDrawFrame(float[] viewMatrix, float[] projectionMatrix, float[] viewProjectionMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace) {
        final Camera camera = scene.getCamera();

        int skyBoxId = isUseskyBoxId;
        if (skyBoxId == -3) {
            for (int i = 0; i < extras.size(); i++) {
                drawObject(viewMatrix, projectionMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace, false, false, false, false, false, extras, i);
            }
        } else if (skyBoxId == -2) {
            GLES20.glClearColor(backgroundColor[0], backgroundColor[1], backgroundColor[2], backgroundColor[3]);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        } else if (skyBoxId == -1) {
            GLES20.glClearColor(1 - backgroundColor[0], 1 - backgroundColor[1], 1 - backgroundColor[2], 1 - backgroundColor[3]);

            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT | GLES20.GL_DEPTH_BUFFER_BIT);
        } else if (isDrawSkyBox && skyBoxId >= 0 && skyBoxId < skyBoxes3D.length) {
            GLES20.glDepthMask(false);
            try {
                if (skyBoxes3D[skyBoxId] == null) {
                    Log.i("ModelRenderer", "Loading sky box textures to GPU... skybox: " + skyBoxId);
                    int textureId = GLUtil.loadCubeMap(skyBoxes[skyBoxId].getCubeMap());
                    Log.d("ModelRenderer", "Loaded textures to GPU... id: " + textureId);
                    if (textureId != -1) {
                        skyBoxes3D[skyBoxId] = SkyBox.build(skyBoxes[skyBoxId]);
                    } else {
                        Log.e("ModelRenderer", "Error loading sky box textures to GPU. ");
                        isDrawSkyBox = false;
                    }
                }

                Matrix.setLookAtM(viewMatrixSkyBox, 0, 0, 0, 0, camera.getxView() - camera.getxPos(), camera.getyView() - camera.getyPos(), camera.getzView() - camera.getzPos(), camera.getxUp() - camera.getxPos(), camera.getyUp() - camera.getyPos(), camera.getzUp() - camera.getzPos());

                if (scene.isFixCoordinateSystem()) {
                    Matrix.rotateM(viewMatrixSkyBox, 0, 90, 1, 0, 0);
                }
                Renderer basicShader = drawer.getSkyBoxDrawer();

                basicShader.draw(skyBoxes3D[skyBoxId], projectionMatrix, viewMatrixSkyBox, skyBoxes3D[skyBoxId].getMaterial().getTextureId(), null, cameraPosInWorldSpace);
            } catch (Throwable ex) {
                Log.e("ModelRenderer", "Error rendering sky box. " + ex.getMessage(), ex);
                isDrawSkyBox = false;
            }
            GLES20.glDepthMask(true);
        }

        boolean doAnimation = scene.isDoAnimation() && animationEnabled;
        boolean drawLighting = scene.isDrawLighting() && isLightsEnabled();
        boolean drawWireframe = scene.isDrawWireframe() || wireframeEnabled;
        boolean drawTextures = scene.isDrawTextures() && texturesEnabled;
        boolean drawColors = scene.isDrawColors() && colorsEnabled;

        if (drawLighting) {

            Renderer basicShader = drawer.getBasicShader();

            if (scene.isRotatingLight()) {
                Matrix.multiplyMV(tempVector4, 0, scene.getLightBulb().getModelMatrix(), 0, lightPosition, 0);
                lightPosInWorldSpace[0] = tempVector4[0];
                lightPosInWorldSpace[1] = tempVector4[1];
                lightPosInWorldSpace[2] = tempVector4[2];

                basicShader.draw(scene.getLightBulb(), projectionMatrix, viewMatrix, -1, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);

            } else {
                lightPosInWorldSpace[0] = cameraPosInWorldSpace[0];
                lightPosInWorldSpace[1] = cameraPosInWorldSpace[1];
                lightPosInWorldSpace[2] = cameraPosInWorldSpace[2];
            }

            if (scene.isDrawNormals()) {
                basicShader.draw(Line.build(new float[]{lightPosInWorldSpace[0], lightPosInWorldSpace[1], lightPosInWorldSpace[2], 0, 0, 0}).setId("light_line"), projectionMatrix, viewMatrix, -1, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
            }
        }

        List<Object3DData> objects = scene.getObjects();

        for (int i = 0; i < objects.size(); i++) {
            drawObject(viewMatrix, projectionMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace, doAnimation, drawLighting, drawWireframe, drawTextures, drawColors, objects, i);
        }

        List<Object3DData> guiObjects = scene.getGUIObjects();

        for (int i = 0; i < guiObjects.size(); i++) {
            drawObject(viewMatrix, projectionMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace, doAnimation, drawLighting, drawWireframe, drawTextures, drawColors, guiObjects, i);
        }

        if (framesPerSecondTime == -1) {
            framesPerSecondTime = SystemClock.elapsedRealtime();
            framesPerSecondCounter++;
        } else if (SystemClock.elapsedRealtime() > framesPerSecondTime + 1000) {
            framesPerSecond = framesPerSecondCounter;
            framesPerSecondCounter = 1;
            framesPerSecondTime = SystemClock.elapsedRealtime();
            AndroidUtils.fireEvent(listeners, new FPSEvent(this, framesPerSecond));
        } else {
            framesPerSecondCounter++;
        }

        debugSkeleton = !debugSkeleton;
    }

    /**********************************************************************************************/
    private void drawObject(float[] viewMatrix, float[] projectionMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace, boolean doAnimation, boolean drawLighting, boolean drawWireframe, boolean drawTextures, boolean drawColors, List<Object3DData> objects, int i) {
        Object3DData objData = null;

        try {
            objData = objects.get(i);
            if (!objData.isVisible()) {
                return;
            }

            if (!infoLogged.containsKey(objData.getId())) {
                Log.i("ModelRenderer", "Drawing model: " + objData.getId() + ", " + objData.getClass().getSimpleName());
                infoLogged.put(objData.getId(), true);
            }

            Renderer drawerObject = drawer.getDrawer(objData, false, drawTextures, drawLighting, doAnimation, drawColors);

            if (drawerObject == null) {
                if (!infoLogged.containsKey(objData.getId() + "drawer")) {
                    Log.e("ModelRenderer", "No drawer for " + objData.getId());
                    infoLogged.put(objData.getId() + "drawer", true);
                }
                return;
            }

            boolean changed = objData.isChanged();
            objData.setChanged(false);

            Integer textureId = null;

            if (drawTextures) {
                if (objData.getElements() != null) {

                    for (int e = 0; e < objData.getElements().size(); e++) {
                        Element element = objData.getElements().get(e);

                        if (element.getMaterial() == null || element.getMaterial().getTextureData() == null) {
                            continue;
                        }

                        textureId = textures.get(element.getMaterial().getTextureData());
                        if (textureId != null) {
                            continue;
                        }

                        Log.i("ModelRenderer", "Loading material texture for element... '" + element);
                        textureId = GLUtil.loadTexture(element.getMaterial().getTextureData());
                        element.getMaterial().setTextureId(textureId);

                        textures.put(element.getMaterial().getTextureData(), textureId);

                        Log.i("ModelRenderer", "Loaded material texture for element. id: " + textureId);

                        objData.setTextureData(element.getMaterial().getTextureData());
                    }
                } else {
                    textureId = textures.get(objData.getTextureData());

                    if (textureId == null && objData.getTextureData() != null) {
                        Log.i("ModelRenderer", "Loading texture for obj: '" + objData.getId() + "'... bytes: " + objData.getTextureData().length);
                        ByteArrayInputStream textureIs = new ByteArrayInputStream(objData.getTextureData());
                        textureId = GLUtil.loadTexture(textureIs);
                        textureIs.close();
                        textures.put(objData.getTextureData(), textureId);
                        objData.getMaterial().setTextureId(textureId);

                        Log.i("ModelRenderer", "Loaded texture OK. id: " + textureId);
                    }
                }
            }

            if (textureId == null) {
                textureId = -1;
            }

            if (objData.getDrawMode() == GLES20.GL_POINTS) {
                Renderer basicDrawer = drawer.getBasicShader();
                basicDrawer.draw(objData, projectionMatrix, viewMatrix, GLES20.GL_POINTS, lightPosInWorldSpace, cameraPosInWorldSpace);
            } else {
                if (drawWireframe && objData.getDrawMode() != GLES20.GL_POINTS && objData.getDrawMode() != GLES20.GL_LINES && objData.getDrawMode() != GLES20.GL_LINE_STRIP && objData.getDrawMode() != GLES20.GL_LINE_LOOP) {
                    try {
                        Object3DData wireframe = wireframes.get(objData);

                        if (wireframe == null || changed) {
                            Log.i("ModelRenderer", "Building wireframe model...");
                            wireframe = Wireframe.build(objData);
                            wireframe.setColor(objData.getColor());
                            wireframes.put(objData, wireframe);
                            Log.i("ModelRenderer", "Wireframe build: " + wireframe);
                        }

                        animator.update(wireframe, scene.isShowBindPose());
                        drawerObject.draw(wireframe, projectionMatrix, viewMatrix, wireframe.getDrawMode(), wireframe.getDrawSize(), textureId, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
                    } catch (Error e) {
                        Log.e("ModelRenderer", e.getMessage(), e);
                    }
                }

                else if (scene.isDrawPoints()) {
                    drawerObject.draw(objData, projectionMatrix, viewMatrix
                            , GLES20.GL_POINTS, objData.getDrawSize(),
                            textureId, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
                    objData.render(drawer, lightPosInWorldSpace, colorMask);
                }

                else if (scene.isDrawSkeleton() && objData instanceof AnimatedModel && ((AnimatedModel) objData).getAnimation() != null) {
                    drawerObject.draw(objData, projectionMatrix, viewMatrix, textureId, lightPosInWorldSpace, COLOR_HALF_TRANSPARENT, cameraPosInWorldSpace);

                    GLES20.glDisable(GLES20.GL_DEPTH_TEST);
                    Object3DData skeleton = this.skeleton.get(objData);

                    if (skeleton == null || changed) {
                        skeleton = Skeleton.build((AnimatedModel) objData);
                        this.skeleton.put(objData, skeleton);
                    }

                    final Renderer skeletonDrawer = drawer.getDrawer(skeleton, false, false, drawLighting, doAnimation, drawColors);
                    skeletonDrawer.draw(skeleton, projectionMatrix, viewMatrix, -1, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
                    GLES20.glEnable(GLES20.GL_DEPTH_TEST);
                }

                else {
                    if (!infoLogged.containsKey(objData.getId() + "render")) {
                        Log.i("ModelRenderer", "Rendering object... " + objData.getId());
                        Log.d("ModelRenderer", objData.toString());
                        Log.d("ModelRenderer", drawerObject.toString());

                        infoLogged.put(objData.getId() + "render", true);
                    }
                    drawerObject.draw(objData, projectionMatrix, viewMatrix, textureId, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
                    objData.render(drawer, lightPosInWorldSpace, colorMask);
                }
            }

            if (scene.isDrawBoundingBox() && objData.isSolid() || scene.getSelectedObject() == objData) {
                drawBoundingBox(viewMatrix, projectionMatrix, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace, objData, changed);
            }

            if (scene.isDrawNormals()) {
                Object3DData normalData = normals.get(objData);
                if (normalData == null || changed) {
                    normalData = Normals.build(objData);
                    if (normalData != null) {
                        normalData.setId(objData.getId() + "_normals");
                        normals.put(objData, normalData);
                    }
                }
                if (normalData != null) {
                    Renderer normalsDrawer = drawer.getDrawer(normalData, false, false, false, doAnimation, false);
                    animator.update(normalData, scene.isShowBindPose());
                    normalsDrawer.draw(normalData, projectionMatrix, viewMatrix, -1, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
                }
            }
        } catch (Exception ex) {
            if (!infoLogged.containsKey(ex.getMessage())) {
                Log.e("ModelRenderer", "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);

                infoLogged.put(ex.getMessage(), true);
            }
        } catch (Error ex) {
            Log.e("ModelRenderer", "There was a problem rendering the object '" + objData.getId() + "':" + ex.getMessage(), ex);
        }
    }

    /**********************************************************************************************/
    private void drawBoundingBox(float[] viewMatrix, float[] projectionMatrix, float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPosInWorldSpace, Object3DData objData, boolean changed) {
        Object3DData boundingBoxData = boundingBoxes.get(objData);

        if (boundingBoxData == null || changed) {
            Log.i("ModelRenderer", "Building bounding box... id: " + objData.getId());
            boundingBoxData = BoundingBox.build(objData);
            boundingBoxData.setColor(COLOR_WHITE);
            boundingBoxes.put(objData, boundingBoxData);
            Log.i("ModelRenderer", "Bounding box: " + boundingBoxData);
        }

        Renderer boundingBoxDrawer = drawer.getBoundingBoxDrawer();
        boundingBoxDrawer.draw(boundingBoxData, projectionMatrix, viewMatrix, -1, lightPosInWorldSpace, colorMask, cameraPosInWorldSpace);
    }

    /**********************************************************************************************/
    public int getWidth() {
        return width;
    }

    /**********************************************************************************************/
    public int getHeight() {
        return height;
    }

    /**********************************************************************************************/
    public float[] getProjectionMatrix() {
        return projectionMatrix;
    }

    /**********************************************************************************************/
    public float[] getViewMatrix() {
        return viewMatrix;
    }
}