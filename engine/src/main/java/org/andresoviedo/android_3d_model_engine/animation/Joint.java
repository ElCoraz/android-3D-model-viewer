package org.andresoviedo.android_3d_model_engine.animation;
/**************************************************************************************************/
import android.opengl.Matrix;

import org.andresoviedo.android_3d_model_engine.services.collada.entities.JointData;

import java.util.ArrayList;
import java.util.List;
/**************************************************************************************************/
public class Joint {
    /**********************************************************************************************/
    private final JointData data;
    /**********************************************************************************************/
    private final List<Joint> children = new ArrayList<>();
    /**********************************************************************************************/
    private final float[] animatedTransform = new float[16];

    /**********************************************************************************************/
    public Joint(JointData data) {
        this.data = data;
        Matrix.setIdentityM(animatedTransform, 0);
    }

    /**********************************************************************************************/
    public static Joint buildJoints(JointData rootJointData) {
        return buildJoint(rootJointData);
    }

    /**********************************************************************************************/
    private static Joint buildJoint(JointData data) {
        Joint ret = new Joint(data);
        for (JointData child : data.children) {
            ret.addChild(buildJoint(child));
        }
        return ret;
    }

    /**********************************************************************************************/
    public int getIndex() {
        return data.getIndex();
    }

    /**********************************************************************************************/
    public String getName() {
        return data.getId();
    }

    /**********************************************************************************************/
    public List<Joint> getChildren() {
        return children;
    }

    /**********************************************************************************************/
    public float[] getBindLocalTransform() {
        return data.getBindLocalTransform();
    }

    /**********************************************************************************************/
    public void addChild(Joint child) {
        this.children.add(child);
    }

    /**********************************************************************************************/
    public float[] getAnimatedTransform() {
        return animatedTransform;
    }

    /**********************************************************************************************/
    public float[] getInverseBindTransform() {
        return data.getInverseBindTransform();
    }

    /**********************************************************************************************/
    @Override
    public Joint clone() {
        final Joint ret = new Joint(data);
        for (final Joint child : this.children) {
            ret.addChild(child.clone());
        }
        return ret;
    }

    /**********************************************************************************************/
    @Override
    public String toString() {
        return data.toString();
    }

    /**********************************************************************************************/
    public JointData find(String id) {
        return data.find(id);
    }

    /**********************************************************************************************/
    public List<JointData> findAll(String id) {
        return data.findAll(id);
    }
    /**********************************************************************************************/
}
/**************************************************************************************************/
