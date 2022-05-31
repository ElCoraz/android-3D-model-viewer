package org.andresoviedo.android_3d_model_engine.animation;

import androidx.annotation.NonNull;

import java.util.Map;
/**************************************************************************************************/
public class KeyFrame {
	/**********************************************************************************************/
	private final float timeStamp;
	private final Map<String, JointTransform> pose;

	/**********************************************************************************************/
	public KeyFrame(float timeStamp, Map<String, JointTransform> jointKeyFrames) {
		this.timeStamp = timeStamp;
		this.pose = jointKeyFrames;
	}

	/**********************************************************************************************/
	protected float getTimeStamp() {
		return timeStamp;
	}

	/**********************************************************************************************/
	protected Map<String, JointTransform> getTransforms() {
		return pose;
	}

	/**********************************************************************************************/
	@NonNull
	@Override
	public String toString() {
		return "KeyFrame{" +
				"timeStamp=" + timeStamp +
				", pose=" + pose +
				'}';
	}
}
