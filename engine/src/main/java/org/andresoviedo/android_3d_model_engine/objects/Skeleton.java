package org.andresoviedo.android_3d_model_engine.objects;

import android.opengl.GLES20;
import android.opengl.Matrix;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.services.collada.entities.JointData;
import org.andresoviedo.util.io.IOUtils;
import org.andresoviedo.util.math.Math3DUtils;

import java.nio.FloatBuffer;
/**************************************************************************************************/
public final class Skeleton {
    /**********************************************************************************************/
    public static AnimatedModel build(AnimatedModel animatedModel) {
        AnimatedModel skeleton = animatedModel.clone();

        skeleton.setVertexBuffer(IOUtils.createFloatBuffer(animatedModel.getJointCount() * 9));   // 3 floats (xyz) x 3 vertex = 9 floats / joint
        skeleton.setNormalsBuffer(IOUtils.createFloatBuffer(animatedModel.getJointCount() * 9));

        skeleton.setJointIds(IOUtils.createFloatBuffer(animatedModel.getJointCount() * 9));
        skeleton.setVertexWeights(IOUtils.createFloatBuffer(animatedModel.getJointCount() * 9));

        FloatBuffer colorBuffer = IOUtils.createFloatBuffer(animatedModel.getJointCount() * 12);  // 4 floats (rgba) x 3 vertex = 12 floats / joint

        skeleton.setColorsBuffer(colorBuffer);

        skeleton.setId(animatedModel.getId() + "-skeleton");
        skeleton.setDrawMode(GLES20.GL_TRIANGLES);
        skeleton.setDrawUsingArrays(true);

        Log.i("Skeleton", "Building skeleton... joints: " + skeleton.getJointCount());

        float[] parentPoint = {0, 0, 0};

        JointData headJoint = skeleton.getJointsData().getHeadJoint();
        buildBones(skeleton, skeleton.getJointCount(), headJoint, parentPoint, -1, colorBuffer);

        return skeleton;
    }

    /**********************************************************************************************/
    private static void buildBones(AnimatedModel animatedModel, int jointCount, JointData joint,
                                   float[] parentPoint, int parentJoinIndex, FloatBuffer colorBuffer) {

        float[] point = new float[4];

        float[] inverted = new float[16];

        Matrix.invertM(inverted, 0, joint.getInverseBindTransform(), 0);
        Matrix.multiplyMV(point, 0, inverted, 0, new float[]{0, 0, 0, 1}, 0);

        float[] v = Math3DUtils.substract(point, parentPoint);

        float[] point1 = new float[]{point[0], point[1], point[2] - Matrix.length(v[0], v[1], v[2]) * 0.05f};
        float[] point2 = new float[]{point[0], point[1], point[2] + Matrix.length(v[0], v[1], v[2]) * 0.05f};

        float[] normal = Math3DUtils.calculateNormal(parentPoint, point1, point2);

        if (Math3DUtils.length(normal) == 0) {
            normal = new float[]{0, 1, 0};
        } else {
            Math3DUtils.normalize(normal);
        }

        animatedModel.getVertexBuffer().put(parentPoint[0]);
        animatedModel.getVertexBuffer().put(parentPoint[1]);
        animatedModel.getVertexBuffer().put(parentPoint[2]);
        animatedModel.getVertexBuffer().put(point1[0]);
        animatedModel.getVertexBuffer().put(point1[1]);
        animatedModel.getVertexBuffer().put(point1[2]);
        animatedModel.getVertexBuffer().put(point2[0]);
        animatedModel.getVertexBuffer().put(point2[1]);
        animatedModel.getVertexBuffer().put(point2[2]);

        animatedModel.getNormalsBuffer().put(normal);
        animatedModel.getNormalsBuffer().put(normal);
        animatedModel.getNormalsBuffer().put(normal);

        animatedModel.getJointIds().put(parentJoinIndex);
        animatedModel.getJointIds().put(0);
        animatedModel.getJointIds().put(0);
        animatedModel.getVertexWeights().put(parentJoinIndex >= 0 ? 1 : 0);
        animatedModel.getVertexWeights().put(0);
        animatedModel.getVertexWeights().put(0);

        for (int i = 3; i < 9; i += 3) {
            animatedModel.getJointIds().put(joint.getIndex());
            animatedModel.getJointIds().put(0);
            animatedModel.getJointIds().put(0);
        }
        for (int i = 3; i < 9; i += 3) {
            animatedModel.getVertexWeights().put(joint.getIndex() >= 0 ? 1 : 0);
            animatedModel.getVertexWeights().put(0);
            animatedModel.getVertexWeights().put(0);
        }

        if (joint.getIndex() < 0) {
            final float color = 0.75f;

            colorBuffer.put(new float[]{color, color, color, 0.5f});
            colorBuffer.put(new float[]{color, color, color, 0.5f});
            colorBuffer.put(new float[]{color, color, color, 0.5f});
        } else {
            final float color = 1;//-(float)joint.getIndex()/(float)jointCount;

            colorBuffer.put(new float[]{color, 0, color, 1});
            colorBuffer.put(new float[]{color, 0, color, 1});
            colorBuffer.put(new float[]{color, 0, color, 1});
        }

        for (JointData child : joint.getChildren()) {
            buildBones(animatedModel, jointCount, child, point, joint.getIndex(), colorBuffer);
        }
    }
}
