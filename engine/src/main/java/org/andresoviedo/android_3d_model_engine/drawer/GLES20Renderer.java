package org.andresoviedo.android_3d_model_engine.drawer;

import android.opengl.GLES20;
import android.os.SystemClock;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.NonNull;

import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Element;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.android.GLUtil;
import org.andresoviedo.util.io.IOUtils;

import java.nio.Buffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
/**************************************************************************************************/
class GLES20Renderer implements Renderer {
    /**********************************************************************************************/
    private static final int COORDS_PER_VERTEX = 3;
    private static final int TEXTURE_COORDS_PER_VERTEX = 2;
    private static final int COLOR_COORDS_PER_VERTEX = 4;
    /**********************************************************************************************/
    private final static float[] DEFAULT_COLOR = {1.0f, 1.0f, 1.0f, 1.0f};
    private final static float[] NO_COLOR_MASK = {1.0f, 1.0f, 1.0f, 1.0f};
    /**********************************************************************************************/
    private final String id;
    private final Set<String> features;
    /**********************************************************************************************/
    private final int mProgram;
    /**********************************************************************************************/
    private long counter = -1;
    private double shift = -1d;
    /**********************************************************************************************/
    private boolean drawUsingUnsignedInt = true;
    /**********************************************************************************************/
    private Map<Object, Integer> textures = new HashMap<>();
    /**********************************************************************************************/
    private final SparseArray<String> cache1 = new SparseArray<>();
    /**********************************************************************************************/
    private static Map<Object, Object> flags = new HashMap<>();
    /**********************************************************************************************/
    static GLES20Renderer getInstance(String id, String vertexShaderCode, String fragmentShaderCode) {
        Set<String> shaderFeatures = new HashSet<>();
        testShaderFeature(shaderFeatures, vertexShaderCode, "u_MMatrix");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Position");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Normal");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_Color");
        testShaderFeature(shaderFeatures, vertexShaderCode, "a_TexCoordinate");
        testShaderFeature(shaderFeatures, vertexShaderCode, "u_LightPos");
        testShaderFeature(shaderFeatures, vertexShaderCode, "in_jointIndices");
        testShaderFeature(shaderFeatures, vertexShaderCode, "in_weights");
        testShaderFeature(shaderFeatures, fragmentShaderCode, "u_TextureCube");
        return new GLES20Renderer(id, vertexShaderCode, fragmentShaderCode, shaderFeatures);
    }
    /**********************************************************************************************/
    private static void testShaderFeature(Set<String> outputFeatures, String shaderCode, String feature) {
        if (shaderCode.contains(feature)) {
            outputFeatures.add(feature);
        }
    }
    /**********************************************************************************************/
    private GLES20Renderer(String id, String vertexShaderCode, String fragmentShaderCode, Set<String> features) {
        this.id = id;
        this.features = features;

        Log.i("GLES20Renderer", "Compiling 3D Drawer... " + id);

        int vertexShader = GLUtil.loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode);
        int fragmentShader = GLUtil.loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode);

        mProgram = GLUtil.createAndLinkProgram(vertexShader, fragmentShader, features.toArray(new String[0]));

        flags.clear();
        Log.d("GLES20Renderer", "Compiled 3D Drawer (" + id + ") with id " + mProgram);
    }
    /**********************************************************************************************/
    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId, float[] lightPosInWorldSpace, float[] cameraPos) {
        this.draw(obj, pMatrix, vMatrix, obj.getDrawMode(), obj.getDrawSize(), textureId, lightPosInWorldSpace, null, cameraPos);
    }
    /**********************************************************************************************/
    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int textureId, float[] lightPosInWorldSpace, float[]
            colorMask, float[] cameraPos) {
        this.draw(obj, pMatrix, vMatrix, obj.getDrawMode(), obj.getDrawSize(), textureId, lightPosInWorldSpace, colorMask, cameraPos);
    }
    /**********************************************************************************************/
    @Override
    public void draw(Object3DData obj, float[] pMatrix, float[] vMatrix, int drawMode, int drawSize, int textureId,
                     float[] lightPosInWorldSpace, float[] colorMask, float[] cameraPos) {
        if (id != flags.get(obj.getId())) {
            Log.d("GLES20Renderer", "Rendering with shader: " + id + "vert... obj: " + obj);
            flags.put(obj.getId(), this.id);
        }

        GLES20.glUseProgram(mProgram);
        if (GLUtil.checkGlError("glUseProgram")) {
            return;
        }

        if(supportsMMatrix()) {
            setUniformMatrix4(obj.getModelMatrix(), "u_MMatrix");
        }
        setUniformMatrix4(vMatrix, "u_VMatrix");
        setUniformMatrix4(pMatrix, "u_PMatrix");

        int mPositionHandle = setVBO("a_Position", obj.getVertexBuffer(), COORDS_PER_VERTEX);

        int mNormalHandle = -1;
        if (supportsNormals()) {
            mNormalHandle = setVBO("a_Normal", obj.getNormalsBuffer(), COORDS_PER_VERTEX);
        }

        int mColorHandle = -1;
        if (supportsColors()) {
            mColorHandle = setVBO("a_Color", obj.getColorsBuffer(), COLOR_COORDS_PER_VERTEX);
        } else {
            setUniform4(obj.getColor() != null? obj.getColor() : DEFAULT_COLOR,"vColor");
        }

        setUniform4(colorMask != null ? colorMask : NO_COLOR_MASK,"vColorMask");

        int mTextureHandle = -1;
        if (textureId != -1 && supportsTextures()) {
            setTexture(textureId);
            mTextureHandle = setVBO("a_TexCoordinate", obj.getTextureBuffer(), TEXTURE_COORDS_PER_VERTEX);
        }

        if (textureId != -1 && supportsTextureCube()){
            setTextureCube(textureId);
        }

        if (lightPosInWorldSpace != null && supportsLighting()) {
            setUniform3(lightPosInWorldSpace,"u_LightPos");
            setUniform3(cameraPos,"u_cameraPos");
        }

        int in_weightsHandle = -1;
        int in_jointIndicesHandle = -1;
        if (supportsJoints() && obj instanceof AnimatedModel) {
            in_weightsHandle = setVBO("in_weights", ((AnimatedModel) obj).getVertexWeights(), COORDS_PER_VERTEX);
            in_jointIndicesHandle = setVBO("in_jointIndices", ((AnimatedModel) obj).getJointIds(), COORDS_PER_VERTEX);
            setUniformMatrix4(((AnimatedModel) obj).getBindShapeMatrix(), "u_BindShapeMatrix");
            setJointTransforms((AnimatedModel) obj);
        }

        drawShape(obj, drawMode, drawSize);

        disableVBO(mPositionHandle);
        disableVBO(mColorHandle);
        disableVBO(mNormalHandle);
        disableVBO(mTextureHandle);
        disableVBO(in_weightsHandle);
        disableVBO(in_jointIndicesHandle);
    }
    /**********************************************************************************************/
    private int setVBO(final String shaderVariableName, final FloatBuffer vertexBufferObject, int coordsPerVertex) {
        int handler = GLES20.glGetAttribLocation(mProgram, shaderVariableName);
        GLUtil.checkGlError("glGetAttribLocation");

        GLES20.glEnableVertexAttribArray(handler);
        GLUtil.checkGlError("glEnableVertexAttribArray");

        vertexBufferObject.position(0);
        GLES20.glVertexAttribPointer(handler, coordsPerVertex, GLES20.GL_FLOAT, false, 0, vertexBufferObject);
        GLUtil.checkGlError("glVertexAttribPointer");

        return handler;
    }
    /**********************************************************************************************/
    private void setUniform3(float[] uniform3f, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glUniform3fv(handle, 1, uniform3f, 0);
        GLUtil.checkGlError("glUniform3fv");
    }
    /**********************************************************************************************/
    private void setUniform4(float[] uniform4f, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glUniform4fv(handle, 1, uniform4f, 0);
        GLUtil.checkGlError("glUniform4fv");
    }
    /**********************************************************************************************/
    private void setUniformMatrix4(float[] matrix, String variableName) {
        int handle = GLES20.glGetUniformLocation(mProgram, variableName);
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glUniformMatrix4fv(handle, 1, false, matrix, 0);
        GLUtil.checkGlError("glUniformMatrix4fv");
    }
    /**********************************************************************************************/
    private void disableVBO(int handle) {
        if (handle != -1) {
            GLES20.glDisableVertexAttribArray(handle);
            GLUtil.checkGlError("glDisableVertexAttribArray");
        }
    }
    /**********************************************************************************************/
    private boolean supportsMMatrix(){
        return features.contains("u_MMatrix");
    }
    /**********************************************************************************************/
    private boolean supportsTextureCube(){
        return features.contains("u_TextureCube");
    }
    /**********************************************************************************************/
    private boolean supportsColors() {
        return features.contains("a_Color");
    }
    /**********************************************************************************************/
    private boolean supportsNormals() {
        return features.contains("a_Normal");
    }
    /**********************************************************************************************/
    private boolean supportsLighting() {
        return features.contains("u_LightPos");
    }
    /**********************************************************************************************/
    private boolean supportsTextures() {
        return features.contains("a_TexCoordinate");
    }
    /**********************************************************************************************/
    private void setTexture(int textureId) {
        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_Texture");
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLUtil.checkGlError("glActiveTexture");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId);
        GLUtil.checkGlError("glBindTexture");

        GLES20.glUniform1i(mTextureUniformHandle, 0);
        GLUtil.checkGlError("glUniform1i");
    }
    /**********************************************************************************************/
    private void setTextureCube(int textureId) {
        int mTextureUniformHandle = GLES20.glGetUniformLocation(mProgram, "u_TextureCube");
        GLUtil.checkGlError("glGetUniformLocation");

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLUtil.checkGlError("glActiveTexture");

        GLES20.glBindTexture(GLES20.GL_TEXTURE_CUBE_MAP, textureId);
        GLUtil.checkGlError("glBindTexture");

        GLES20.glUniform1i(mTextureUniformHandle, 0);
        GLUtil.checkGlError("glUniform1i");

    }
    /**********************************************************************************************/
    private boolean supportsJoints() {
        return features.contains("in_jointIndices") && features.contains("in_weights");
    }
    /**********************************************************************************************/
    private void setJointTransforms(AnimatedModel animatedModel) {
        float[][] jointTransformsArray = animatedModel.getJointTransforms();

        for (int i = 0; i < jointTransformsArray.length; i++) {
            float[] jointTransform = jointTransformsArray[i];
            String jointTransformHandleName = cache1.get(i);
            if (jointTransformHandleName == null) {
                jointTransformHandleName = "jointTransforms[" + i + "]";
                cache1.put(i, jointTransformHandleName);
            }
            setUniformMatrix4(jointTransform, jointTransformHandleName);
        }
    }
    /**********************************************************************************************/
    private void drawShape(Object3DData obj, int drawMode, int drawSize) {
        int drawBufferType = -1;
        final Buffer drawOrderBuffer;
        final FloatBuffer vertexBuffer;
        if (obj.isDrawUsingArrays()) {
            drawOrderBuffer = null;
            vertexBuffer = obj.getVertexBuffer();
        } else {
            vertexBuffer = obj.getVertexBuffer();
            if (drawUsingUnsignedInt) {
                drawOrderBuffer = obj.getDrawOrder();
                drawBufferType = GLES20.GL_UNSIGNED_INT;
            } else {
                drawOrderBuffer = obj.getDrawOrderAsShort();
                drawBufferType = GLES20.GL_UNSIGNED_SHORT;
            }
        }
        vertexBuffer.position(0);

        List<int[]> drawModeList = obj.getDrawModeList();
        if (drawModeList != null) {
            if (obj.isDrawUsingArrays()) {
                drawPolygonsUsingArrays(drawMode, drawModeList);
            } else {
                drawPolygonsUsingIndex(drawOrderBuffer, drawBufferType, drawModeList);
            }
        } else {
            if (obj.isDrawUsingArrays()) {
                drawTrianglesUsingArrays(drawMode, drawSize, vertexBuffer.capacity() / COORDS_PER_VERTEX);
            } else {
                drawTrianglesUsingIndex(obj, drawMode, drawSize, drawOrderBuffer, drawBufferType);
            }
        }
    }
    /**********************************************************************************************/
    private void drawTrianglesUsingArrays(int drawMode, int drawSize, int drawCount) {
        if (drawSize <= 0) {
            if (this.shift >= 0) {
                double rotation = ((SystemClock.uptimeMillis() % 10000) / 10000f) * (Math.PI * 2);

                if (this.shift == 0d) {
                    this.shift = rotation;
                }
                drawCount = (int) ((Math.sin(rotation - this.shift + Math.PI / 2 * 3) + 1) / 2f * drawCount);
            }
            GLES20.glDrawArrays(drawMode, 0, drawCount);
            GLUtil.checkGlError("glDrawArrays");
        } else {
            for (int i = 0; i < drawCount; i += drawSize) {
                GLES20.glDrawArrays(drawMode, i, drawSize);
                GLUtil.checkGlError("glDrawArrays");
            }
        }
    }
    /**********************************************************************************************/
    private void drawTrianglesUsingIndex(Object3DData obj, int drawMode, int drawSize, Buffer drawOrderBuffer, int drawBufferType) {
        if (drawSize <= 0) {
            if (id != flags.get(obj.getElements())) {
                Log.i("GLES20Renderer", "Rendering elements... obj: " + obj.getId()
                        + ", total:" + obj.getElements().size());
                flags.put(obj.getElements(), this.id);
            }

            for (int i = 0; i < obj.getElements().size(); i++) {

                Element element = obj.getElements().get(i);
                drawOrderBuffer = element.getIndexBuffer();

                if (id != flags.get(element)) {
                    Log.v("GLES20Renderer", "Rendering element " + i + "....  " + element);
                }

                if (element.getMaterial() != null) {
                    if (!supportsColors()) {
                        setUniform4(element.getMaterial().getColor() != null ? element.getMaterial().getColor() :
                                obj.getColor() != null ? obj.getColor() : DEFAULT_COLOR, "vColor");
                    }
                    if (element.getMaterial().getTextureId() != -1 && supportsTextures()) {
                        setTexture(element.getMaterial().getTextureId());
                    }
                }

                if(drawUsingUnsignedInt == false){
                    ShortBuffer indexShortBuffer = null;
                    drawOrderBuffer.position(0);
                    if (indexShortBuffer == null && drawOrderBuffer != null) {
                        indexShortBuffer = IOUtils.createShortBuffer(((IntBuffer) drawOrderBuffer).capacity());
                        for (int j = 0; j < indexShortBuffer.capacity(); j++) {
                            indexShortBuffer.put((short) ((IntBuffer) drawOrderBuffer).get(j));
                        }
                        drawOrderBuffer = indexShortBuffer;
                    }
                }

                drawOrderBuffer.position(0);
                GLES20.glDrawElements(drawMode, drawOrderBuffer.capacity(), drawBufferType,
                        drawOrderBuffer);
                boolean error = GLUtil.checkGlError("glDrawElements");
                if (drawUsingUnsignedInt && error) {
                    drawUsingUnsignedInt = false;
                }

                if (id != flags.get(element)) {
                    Log.v("GLES20Renderer", "Rendering element " + i + " finished");
                    flags.put(element, this.id);
                }
            }
        } else {
            for (int i = 0; i < drawOrderBuffer.capacity(); i += drawSize) {
                drawOrderBuffer.position(i);
                GLES20.glDrawElements(drawMode, drawSize, drawBufferType, drawOrderBuffer);
                boolean error = GLUtil.checkGlError("glDrawElements");
                if (drawUsingUnsignedInt && error) {
                    drawUsingUnsignedInt = false;
                }
            }
        }
    }
    /**********************************************************************************************/
    private void drawPolygonsUsingIndex(Buffer drawOrderBuffer, int drawBufferType, List<int[]> polygonsList) {
        // Log.d(obj.getId(),"Drawing single polygons using elements...");
        for (int i = 0; i < polygonsList.size(); i++) {
            int[] drawPart = polygonsList.get(i);
            int drawModePolygon = drawPart[0];
            int vertexPos = drawPart[1];
            int drawSizePolygon = drawPart[2];
            drawOrderBuffer.position(vertexPos);
            GLES20.glDrawElements(drawModePolygon, drawSizePolygon, drawBufferType, drawOrderBuffer);
            boolean error = GLUtil.checkGlError("glDrawElements");
            if (drawUsingUnsignedInt && error) {
                drawUsingUnsignedInt = false;
            }
        }
    }
    /**********************************************************************************************/
    private void drawPolygonsUsingArrays(int drawMode, List<int[]> polygonsList) {
        for (int j = 0; j < polygonsList.size(); j++) {
            int[] polygon = polygonsList.get(j);
            int drawModePolygon = polygon[0];
            int vertexPos = polygon[1];
            int drawSizePolygon = polygon[2];
            if (drawMode == GLES20.GL_LINE_LOOP && polygon[2] > 3) {
                for (int i = 0; i < polygon[2] - 2; i++) {
                    GLES20.glDrawArrays(drawMode, polygon[1] + i, 3);
                    GLUtil.checkGlError("glDrawArrays");
                }
            } else {
                GLES20.glDrawArrays(drawMode, polygon[1], polygon[2]);
                GLUtil.checkGlError("glDrawArrays");
            }
        }
    }
    /**********************************************************************************************/
    @NonNull
    @Override
    public String toString() {
        return "GLES20Renderer{" +
                "id='" + id + '\'' +
                ", features=" + features +
                '}';
    }
}
