package org.andresoviedo.android_3d_model_engine.drawer;

import android.content.Context;
import androidx.annotation.NonNull;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.R;
import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.io.IOUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
/**************************************************************************************************/
public class RendererFactory {
    /**********************************************************************************************/
    private Map<String, String> shadersCode = new HashMap<>();
    /**********************************************************************************************/
    private Map<Shader, GLES20Renderer> drawers = new HashMap<>();

    /**********************************************************************************************/
    public RendererFactory(Context context) throws IllegalAccessException, IOException {
        Log.i("RendererFactory", "Discovering shaders...");
        Field[] fields = R.raw.class.getFields();
        for (int count = 0; count < fields.length; count++) {
            String shaderId = fields[count].getName();
            Log.v("RendererFactory", "Loading shader... " + shaderId);
            int shaderResId = fields[count].getInt(fields[count]);
            byte[] shaderBytes = IOUtils.read(context.getResources().openRawResource(shaderResId));
            String shaderCode = new String(shaderBytes);
            shadersCode.put(shaderId, shaderCode);
        }
        Log.i("RendererFactory", "Shaders loaded: " + shadersCode.size());
    }

    /**********************************************************************************************/
    public Renderer getDrawer(Object3DData obj, boolean usingSkyBox, boolean usingTextures, boolean usingLights, boolean usingAnimation, boolean drawColors) {
        boolean isAnimated = usingAnimation && obj instanceof AnimatedModel
                && ((AnimatedModel) obj).getAnimation() != null && (((AnimatedModel) obj).getAnimation()).isInitialized();
        boolean isUsingLights = usingLights && (obj.getNormalsBuffer() != null || obj.getNormalsBuffer() != null);
        boolean isTextured = usingTextures && obj.getTextureData() != null && obj.getTextureBuffer() != null;
        boolean isColoured = drawColors && obj != null && (obj.getColorsBuffer() != null || obj
                .getColorsBuffer() != null);

        final Shader shader = getShader(usingSkyBox, isAnimated, isUsingLights, isTextured, isColoured);

        GLES20Renderer drawer = drawers.get(shader);
        if (drawer != null) return drawer;

        String vertexShaderCode = shadersCode.get(shader.vertexShaderCode);
        String fragmentShaderCode = shadersCode.get(shader.fragmentShaderCode);
        if (vertexShaderCode == null || fragmentShaderCode == null) {
            Log.e("RendererFactory", "Shaders not found for " + shader.id);
            return null;
        }

        vertexShaderCode = vertexShaderCode.replace("void main(){", "void main(){\n\tgl_PointSize = 5.0;");

        vertexShaderCode = vertexShaderCode.replace("const int MAX_JOINTS = 60;", "const int MAX_JOINTS = gl_MaxVertexUniformVectors > 60 ? 60 : gl_MaxVertexUniformVectors;");

        Log.v("RendererFactory", "\n---------- Vertex shader ----------\n");
        Log.v("RendererFactory", vertexShaderCode);
        Log.v("RendererFactory", "---------- Fragment shader ----------\n");
        Log.v("RendererFactory", fragmentShaderCode);
        Log.v("RendererFactory", "-------------------------------------\n");
        drawer = GLES20Renderer.getInstance(shader.id, vertexShaderCode, fragmentShaderCode);

        drawers.put(shader, drawer);

        return drawer;
    }

    /**********************************************************************************************/
    @NonNull
    private Shader getShader(boolean isUsingSkyBox, boolean isAnimated, boolean isUsingLights, boolean isTextured, boolean
            isColoured) {

        if (isUsingSkyBox) {
            return Shader.SKYBOX;
        }

        Shader ret = null;
        if (isAnimated) {
            if (isUsingLights) {
                if (isTextured) {
                    if (isColoured) {
                        ret = Shader.ANIM_LIGHT_TEXTURE_COLORS;
                    } else {
                        ret = Shader.ANIM_LIGHT_TEXTURE;
                    }
                } else {
                    if (isColoured) {
                        ret = Shader.ANIM_LIGHT_COLORS;
                    } else {
                        ret = Shader.ANIM_LIGHT;
                    }
                }
            } else {
                if (isTextured) {
                    if (isColoured) {
                        ret = Shader.ANIM_TEXTURE_COLORS;
                    } else {
                        ret = Shader.ANIM_TEXTURE;
                    }
                } else {
                    if (isColoured) {
                        ret = Shader.ANIM_COLORS;
                    } else {
                        ret = Shader.ANIM;
                    }
                }
            }
        } else {
            if (isUsingLights) {
                if (isTextured) {
                    if (isColoured) {
                        ret = Shader.LIGHT_TEXTURE_COLORS;
                    } else {
                        ret = Shader.LIGHT_TEXTURE;
                    }
                } else {
                    if (isColoured) {
                        ret = Shader.LIGHT_COLORS;
                    } else {
                        ret = Shader.LIGHT;
                    }
                }
            } else {
                if (isTextured) {
                    if (isColoured) {
                        ret = Shader.TEXTURE_COLORS;
                    } else {
                        ret = Shader.TEXTURE;
                    }
                } else {
                    if (isColoured) {
                        ret = Shader.COLORS;
                    } else {
                        ret = Shader.SHADER;
                    }
                }
            }
        }
        return ret;
    }

    /**********************************************************************************************/
    public Renderer getBoundingBoxDrawer() {
        return getDrawer(null, false, false, false, false, false);
    }

    /**********************************************************************************************/
    public Renderer getFaceNormalsDrawer() {
        return getDrawer(null, false, false, false, false, false);
    }

    /**********************************************************************************************/
    public Renderer getBasicShader() {
        return getDrawer(null, false, false, false, false, false);
    }

    /**********************************************************************************************/
    public Renderer getSkyBoxDrawer() {
        return getDrawer(null, true, false, false, false, false);
    }
}
