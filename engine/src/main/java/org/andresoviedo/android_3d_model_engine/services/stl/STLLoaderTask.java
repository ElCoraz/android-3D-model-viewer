package org.andresoviedo.android_3d_model_engine.services.stl;

import android.app.Activity;
import android.opengl.GLES20;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.android_3d_model_engine.services.LoadListener;
import org.andresoviedo.android_3d_model_engine.services.LoaderTask;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.MeshData;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
/**************************************************************************************************/
public final class STLLoaderTask extends LoaderTask {
    /**********************************************************************************************/
    private STLFileReader stlFileReader;

    /**********************************************************************************************/
    public STLLoaderTask(Activity parent, URI uri, LoadListener callback) {
        super(parent, uri, callback);
    }

    /**********************************************************************************************/
    @Override
    protected List<Object3DData> build() throws IOException {
        int counter = 0;

        try {
            Log.i("STLLoaderTask", "Parsing model...");

            super.publishProgress("Parsing model...");

            this.stlFileReader = new STLFileReader(new URL(uri.toString()));

            int totalFaces = stlFileReader.getNumOfFacets()[0];

            Log.i("STLLoaderTask", "Num of objects found: " + stlFileReader.getNumOfObjects());
            Log.i("STLLoaderTask", "Num facets found '" + totalFaces + "' facets");
            Log.i("STLLoaderTask", "Parsing messages: " + stlFileReader.getParsingMessages());

            final List<float[]> vertices = new ArrayList<>();
            final List<float[]> normals = new ArrayList<>();

            double[] normal = new double[3];
            double[][] triangle = new double[3][3];

            super.publishProgress("Loading facets...");

            while (stlFileReader.getNextFacet(normal, triangle) && counter++ < totalFaces) {

                normals.add(new float[]{(float) normal[0], (float) normal[1], (float) normal[2]});
                normals.add(new float[]{(float) normal[0], (float) normal[1], (float) normal[2]});
                normals.add(new float[]{(float) normal[0], (float) normal[1], (float) normal[2]});

                vertices.add(new float[]{(float) triangle[0][0], (float) triangle[0][1], (float) triangle[0][2]});
                vertices.add(new float[]{(float) triangle[1][0], (float) triangle[1][1], (float) triangle[1][2]});
                vertices.add(new float[]{(float) triangle[2][0], (float) triangle[2][1], (float) triangle[2][2]});
            }

            Log.i("STLLoaderTask", "Loaded model. Facets: " + counter + ", vertices:" + vertices.size() + ", normals: " + normals.size());

            final MeshData mesh = new MeshData.Builder().vertices(vertices).normals(normals).build();

            super.publishProgress("Validating data...");

            mesh.fixNormals();

            Object3DData data = new Object3DData(mesh.getVertexBuffer()).setNormalsBuffer(mesh.getNormalsBuffer());

            data.setMeshData(mesh);
            data.setDrawUsingArrays(true);
            data.setDrawMode(GLES20.GL_TRIANGLES);
            data.setId(uri.toString());

            super.onLoad(data);

            return Collections.singletonList(data);
        } catch (IOException e) {
            Log.e("STLLoaderTask", "Face '" + counter + "'" + e.getMessage(), e);
            throw e;
        } finally {
            try {
                stlFileReader.close();
            } catch (IOException e) {
                throw e;
            }
        }
    }

    /**********************************************************************************************/
    private static ByteBuffer createNativeByteBuffer(int length) {
        ByteBuffer bb = ByteBuffer.allocateDirect(length);
        bb.order(ByteOrder.nativeOrder());
        return bb;
    }
}
