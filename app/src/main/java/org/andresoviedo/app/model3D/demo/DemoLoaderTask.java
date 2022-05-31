package org.andresoviedo.app.model3D.demo;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.objects.Cube;
import org.andresoviedo.android_3d_model_engine.services.LoadListener;
import org.andresoviedo.android_3d_model_engine.services.LoadListenerAdapter;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.SceneLoader;
import org.andresoviedo.android_3d_model_engine.services.collada.ColladaLoader;
import org.andresoviedo.android_3d_model_engine.services.wavefront.WavefrontLoader;
import org.andresoviedo.android_3d_model_engine.util.Exploder;
import org.andresoviedo.android_3d_model_engine.util.Rescaler;
import org.andresoviedo.util.android.ContentUtils;
import org.andresoviedo.util.io.IOUtils;

import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
/**************************************************************************************************/
public class DemoLoaderTask extends LoaderTask {
    /**********************************************************************************************/
    public DemoLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
        ContentUtils.provideAssets(parent);
    }

    /**********************************************************************************************/
    @Override
    protected List<Object3DData> build() throws Exception {
        super.publishProgress("Loading demo...");

        final List<Exception> errors = new ArrayList<>();

        try {
            Object3DData obj10 = Cube.buildCubeV1();
            obj10.setColor(new float[]{1f, 0f, 0f, 0.5f});
            obj10.setLocation(new float[]{-2f, 2f, 0f});
            obj10.setScale(0.5f, 0.5f, 0.5f);
            super.onLoad(obj10);

            Object3DData obj11 = Cube.buildCubeV1();
            obj11.setColor(new float[]{1f, 1f, 0f, 0.5f});
            obj11.setLocation(new float[]{0f, 2f, 0f});
            Exploder.centerAndScaleAndExplode(obj11, 2.0f, 1.5f);
            obj11.setId(obj11.getId() + "_exploded");
            obj11.setScale(0.5f, 0.5f, 0.5f);
            super.onLoad(obj11);

            Object3DData obj12 = Cube.buildCubeV1_with_normals();
            obj12.setColor(new float[]{1f, 0f, 1f, 1f});
            obj12.setLocation(new float[]{0f, 0f, -2f});
            obj12.setScale(0.5f, 0.5f, 0.5f);
            super.onLoad(obj12);

            Object3DData obj20 = Cube.buildCubeV2();
            obj20.setColor(new float[]{0f, 1f, 0, 0.25f});
            obj20.setLocation(new float[]{2f, 2f, 0f});
            obj20.setScale(0.5f, 0.5f, 0.5f);
            super.onLoad(obj20);

            try {
                InputStream open = ContentUtils.getInputStream("penguin.bmp");
                Object3DData obj3 = Cube.buildCubeV3(IOUtils.read(open));
                open.close();
                obj3.setColor(new float[]{1f, 1f, 1f, 1f});
                obj3.setLocation(new float[]{-2f, -2f, 0f});
                obj3.setScale(0.5f, 0.5f, 0.5f);
                super.onLoad(obj3);
            } catch (Exception ex) {
                errors.add(ex);
            }

            try {
                InputStream open = ContentUtils.getInputStream("cube.bmp");
                Object3DData obj4 = Cube.buildCubeV4(IOUtils.read(open));
                open.close();
                obj4.setColor(new float[]{1f, 1f, 1f, 1f});
                obj4.setLocation(new float[]{0f, -2f, 0f});
                obj4.setScale(0.5f, 0.5f, 0.5f);
                super.onLoad(obj4);
            } catch (Exception ex) {
                errors.add(ex);
            }

            try {
                Object3DData obj51 = new WavefrontLoader(GLES20.GL_TRIANGLE_FAN, new LoadListenerAdapter() {
                    @Override
                    public void onLoad(Object3DData obj53) {
                        obj53.setLocation(new float[]{-2f, 0f, 0f});
                        obj53.setColor(new float[]{1.0f, 1.0f, 0f, 1.0f});
                        Rescaler.rescale(obj53, 2f);
                        DemoLoaderTask.this.onLoad(obj53);
                    }
                }).load(new URI("android://org.andresoviedo.dddmodel2/assets/models/teapot.obj")).get(0);
            } catch (Exception ex) {
                errors.add(ex);
            }

            try {
                Object3DData obj52 = new WavefrontLoader(GLES20.GL_TRIANGLE_FAN, new LoadListenerAdapter() {
                    @Override
                    public void onLoad(Object3DData obj53) {
                        obj53.setLocation(new float[]{1.5f, -2.5f, -0.5f});
                        obj53.setColor(new float[]{0.0f, 1.0f, 1f, 1.0f});
                        DemoLoaderTask.this.onLoad(obj53);
                    }
                }).load(new URI("android://org.andresoviedo.dddmodel2/assets/models/cube.obj")).get(0);
            } catch (Exception ex) {
                errors.add(ex);
            }

            try {
                Object3DData obj53 = new WavefrontLoader(GLES20.GL_TRIANGLE_FAN, new LoadListenerAdapter() {
                    @Override
                    public void onLoad(Object3DData obj53) {
                        obj53.setLocation(new float[]{2f, 0f, 0f});
                        obj53.setColor(new float[]{1.0f, 1.0f, 1f, 1.0f});
                        Rescaler.rescale(obj53, 2f);
                        DemoLoaderTask.this.onLoad(obj53);
                    }
                }).load(new URI("android://org.andresoviedo.dddmodel2/assets/models/ToyPlane.obj")).get(0);
            } catch (Exception ex) {
                errors.add(ex);
            }

            try {
                Object3DData obj53 = new ColladaLoader().load(new URI("android://org.andresoviedo.dddmodel2/assets/models/cowboy.dae"), new LoadListenerAdapter() {
                    @Override
                    public void onLoad(Object3DData obj53) {
                        obj53.setLocation(new float[]{0f, -1f, 1f});
                        obj53.setColor(new float[]{1.0f, 1.0f, 1f, 1.0f});
                        obj53.setRotation(new float[]{-90, 0, 0});
                        Rescaler.rescale(obj53, 2f);
                        DemoLoaderTask.this.onLoad(obj53);
                    }
                }).get(0);
            } catch (Exception ex) {
                errors.add(ex);
            }

            {
                Object3DData obj111 = Cube.buildCubeV1();
                obj111.setColor(new float[]{1f, 0f, 0f, 0.25f});
                obj111.setLocation(new float[]{-1f, -2f, -1f});
                obj111.setScale(0.5f, 0.5f, 0.5f);
                super.onLoad(obj111);

                Object3DData obj112 = Cube.buildCubeV1();
                obj112.setColor(new float[]{1f, 0f, 1f, 0.25f});
                obj112.setLocation(new float[]{1f, -2f, -1f});
                obj112.setScale(0.5f, 0.5f, 0.5f);
                super.onLoad(obj112);

            }
            {
                Object3DData obj111 = Cube.buildCubeV1();
                obj111.setColor(new float[]{1f, 1f, 0f, 0.25f});
                obj111.setLocation(new float[]{-1f, -2f, 1f});
                obj111.setScale(0.5f, 0.5f, 0.5f);
                super.onLoad(obj111);

                Object3DData obj112 = Cube.buildCubeV1();
                obj112.setColor(new float[]{0f, 1f, 1f, 0.25f});
                obj112.setLocation(new float[]{1f, -2f, 1f});
                obj112.setScale(0.5f, 0.5f, 0.5f);
                super.onLoad(obj112);

            }

        } catch (Exception ex) {
            errors.add(ex);
            if (!errors.isEmpty()) {
                StringBuilder msg = new StringBuilder("There was a problem loading the data");
                for (Exception error : errors) {
                    Log.e("Example", error.getMessage(), error);
                    msg.append("\n").append(error.getMessage());
                }
                throw new Exception(msg.toString());
            }
        }
        return null;
    }

    /**********************************************************************************************/
    @Override
    public void onProgress(String progress) {
        super.publishProgress(progress);
    }
}
