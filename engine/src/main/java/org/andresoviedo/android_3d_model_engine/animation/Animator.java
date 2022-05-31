package org.andresoviedo.android_3d_model_engine.animation;
/**************************************************************************************************/
import android.opengl.Matrix;
import android.os.SystemClock;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.AnimatedModel;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.math.Math3DUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
/**************************************************************************************************/
public class Animator {
	/**********************************************************************************************/
	private float animationTime = 0;
	/**********************************************************************************************/
	private float speed = 1f;
	/**********************************************************************************************/
	private final Map<String, Object> cache = new HashMap<>();
	/**********************************************************************************************/
	private final Map<String, float[]> currentPose = new HashMap<>();
	;
	private KeyFrame[] previousAndNextKeyFrames = new KeyFrame[2];

	/**********************************************************************************************/
	public Animator() {
	}

	/**********************************************************************************************/
	public void update(Object3DData obj, boolean bindPoseOnly) {
		if (!(obj instanceof AnimatedModel)) {
			return;
		}

		AnimatedModel animatedModel = (AnimatedModel) obj;

		if (animatedModel.getAnimation() == null) return;

		initAnimation(animatedModel);

		increaseAnimationTime((AnimatedModel) obj);

		Map<String, float[]> currentPose = calculateCurrentAnimationPose(animatedModel);

		applyPoseToJoints(animatedModel, currentPose, animatedModel.getRootJoint(), Math3DUtils.IDENTITY_MATRIX,
				Integer.MAX_VALUE);
	}

	/**********************************************************************************************/
	private void initAnimation(AnimatedModel animatedModel) {
		if (animatedModel.getAnimation().isInitialized()) {
			return;
		}

		final KeyFrame[] keyFrames = animatedModel.getAnimation().getKeyFrames();
		Log.i("Animator", "Initializing " + animatedModel.getId() + ". " + keyFrames.length + " key frames...");

		final Set<String> allJointIds = new HashSet<>();
		for (int i = 0; i < keyFrames.length; i++) {
			allJointIds.addAll(keyFrames[i].getTransforms().keySet());
		}

		final Joint rootJoint = animatedModel.getRootJoint();
		for (int i = 0; i < keyFrames.length; i++) {

			final KeyFrame keyFrameCurrent = keyFrames[i];

			final Map<String, JointTransform> jointTransforms = keyFrameCurrent.getTransforms();

			for (String jointId : allJointIds) {

				final JointTransform currentTransform = jointTransforms.get(jointId);
				if (currentTransform != null && currentTransform.isComplete()) {
					continue;
				}

				if (currentTransform != null && i == 0) {
					currentTransform.complete(rootJoint.find(jointId));
					continue;
				}

				if (currentTransform == null && i == 0) {
					jointTransforms.put(jointId, JointTransform.ofNull());
					continue;
				}

				final KeyFrame keyFramePrevious = keyFrames[i - 1];
				final JointTransform previousTransform = keyFramePrevious.getTransforms().get(jointId);

				if (currentTransform == null && i == keyFrames.length - 1) {
					jointTransforms.put(jointId, previousTransform);
					continue;
				}

				boolean hasScaleX = currentTransform != null && currentTransform.hasScaleX();
				boolean hasScaleY = currentTransform != null && currentTransform.hasScaleY();
				boolean hasScaleZ = currentTransform != null && currentTransform.hasScaleZ();
				boolean hasRotationX = currentTransform != null && currentTransform.hasRotationX();
				boolean hasRotationY = currentTransform != null && currentTransform.hasRotationY();
				boolean hasRotationZ = currentTransform != null && currentTransform.hasRotationZ();
				boolean hasLocationX = currentTransform != null && currentTransform.hasLocationX();
				boolean hasLocationY = currentTransform != null && currentTransform.hasLocationY();
				boolean hasLocationZ = currentTransform != null && currentTransform.hasLocationZ();

				KeyFrame keyFrameNextScaleX = null;
				KeyFrame keyFrameNextScaleY = null;
				KeyFrame keyFrameNextScaleZ = null;
				KeyFrame keyFrameNextRotationX = null;
				KeyFrame keyFrameNextRotationY = null;
				KeyFrame keyFrameNextRotationZ = null;
				KeyFrame keyFrameNextLocationX = null;
				KeyFrame keyFrameNextLocationY = null;
				KeyFrame keyFrameNextLocationZ = null;

				for (int k = i + 1; k < keyFrames.length; k++) {
					JointTransform candidate = keyFrames[k].getTransforms().get(jointId);
					if (candidate == null) continue;
					if (candidate.getScale() != null) {
						if (!hasScaleX && keyFrameNextScaleX == null && candidate.getScale()[0] != null)
							keyFrameNextScaleX = keyFrames[k];
						if (!hasScaleY && keyFrameNextScaleY == null && candidate.getScale()[1] != null)
							keyFrameNextScaleY = keyFrames[k];
						if (!hasScaleZ && keyFrameNextScaleZ == null && candidate.getScale()[2] != null)
							keyFrameNextScaleZ = keyFrames[k];
					}
					if (candidate.getRotation() != null) {
						if (!hasRotationX && keyFrameNextRotationX == null && candidate.getRotation()[0] != null)
							keyFrameNextRotationX = keyFrames[k];
						if (!hasRotationY && keyFrameNextRotationY == null && candidate.getRotation()[1] != null)
							keyFrameNextRotationY = keyFrames[k];
						if (!hasRotationZ && keyFrameNextRotationZ == null && candidate.getRotation()[2] != null)
							keyFrameNextRotationZ = keyFrames[k];
					}
					if (candidate.getLocation() != null) {
						if (!hasLocationX && keyFrameNextLocationX == null && candidate.getLocation()[0] != null)
							keyFrameNextLocationX = keyFrames[k];
						if (!hasLocationY && keyFrameNextLocationY == null && candidate.getLocation()[1] != null)
							keyFrameNextLocationY = keyFrames[k];
						if (!hasLocationZ && keyFrameNextLocationZ == null && candidate.getLocation()[2] != null)
							keyFrameNextLocationZ = keyFrames[k];
					}
					if (keyFrameNextScaleX != null && keyFrameNextScaleY != null && keyFrameNextScaleZ != null
							&& keyFrameNextRotationX != null && keyFrameNextRotationY != null && keyFrameNextRotationZ != null
							&& keyFrameNextLocationX != null && keyFrameNextLocationY != null && keyFrameNextLocationZ != null) {
						break;
					}
				}

				if (keyFrameNextScaleX == null)
					keyFrameNextScaleX = hasScaleX ? keyFrameCurrent : keyFramePrevious;
				if (keyFrameNextScaleY == null)
					keyFrameNextScaleY = hasScaleY ? keyFrameCurrent : keyFramePrevious;
				if (keyFrameNextScaleZ == null)
					keyFrameNextScaleZ = hasScaleZ ? keyFrameCurrent : keyFramePrevious;
				if (keyFrameNextRotationX == null)
					keyFrameNextRotationX = hasRotationX ? keyFrameCurrent : keyFramePrevious;
				if (keyFrameNextRotationY == null)
					keyFrameNextRotationY = hasRotationY ? keyFrameCurrent : keyFramePrevious;
				if (keyFrameNextRotationZ == null)
					keyFrameNextRotationZ = hasRotationZ ? keyFrameCurrent : keyFramePrevious;
				if (keyFrameNextLocationX == null)
					keyFrameNextLocationX = hasLocationX ? keyFrameCurrent : keyFramePrevious;
				if (keyFrameNextLocationY == null)
					keyFrameNextLocationY = hasLocationY ? keyFrameCurrent : keyFramePrevious;
				if (keyFrameNextLocationZ == null)
					keyFrameNextLocationZ = hasLocationZ ? keyFrameCurrent : keyFramePrevious;

				final float elapsed = keyFrameCurrent.getTimeStamp() - keyFramePrevious.getTimeStamp();
				final float scaleProgressionX = keyFrameNextScaleX != keyFramePrevious ?
						elapsed / (keyFrameNextScaleX.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;
				final float scaleProgressionY = keyFrameNextScaleY != keyFramePrevious ?
						elapsed / (keyFrameNextScaleY.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;
				final float scaleProgressionZ = keyFrameNextScaleZ != keyFramePrevious ?
						elapsed / (keyFrameNextScaleZ.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;
				final float rotationProgressionX = keyFrameNextRotationX != keyFramePrevious ?
						elapsed / (keyFrameNextRotationX.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;
				final float rotationProgressionY = keyFrameNextRotationY != keyFramePrevious ?
						elapsed / (keyFrameNextRotationY.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;
				final float rotationProgressionZ = keyFrameNextRotationZ != keyFramePrevious ?
						elapsed / (keyFrameNextRotationZ.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;
				final float locationProgressionX = keyFrameNextLocationX != keyFramePrevious ?
						elapsed / (keyFrameNextLocationX.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;
				final float locationProgressionY = keyFrameNextLocationY != keyFramePrevious ?
						elapsed / (keyFrameNextLocationY.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;
				final float locationProgressionZ = keyFrameNextLocationZ != keyFramePrevious ?
						elapsed / (keyFrameNextLocationZ.getTimeStamp() - keyFramePrevious.getTimeStamp()) : 0;

				final JointTransform missingFrameTransform = JointTransform.ofInterpolation(
						previousTransform, keyFrameNextScaleX.getTransforms().get(jointId), scaleProgressionX,
						previousTransform, keyFrameNextScaleY.getTransforms().get(jointId), scaleProgressionY,
						previousTransform, keyFrameNextScaleZ.getTransforms().get(jointId), scaleProgressionZ,
						previousTransform, keyFrameNextRotationX.getTransforms().get(jointId), rotationProgressionX,
						previousTransform, keyFrameNextRotationY.getTransforms().get(jointId), rotationProgressionY,
						previousTransform, keyFrameNextRotationZ.getTransforms().get(jointId), rotationProgressionZ,
						previousTransform, keyFrameNextLocationX.getTransforms().get(jointId), locationProgressionX,
						previousTransform, keyFrameNextLocationY.getTransforms().get(jointId), locationProgressionY,
						previousTransform, keyFrameNextLocationZ.getTransforms().get(jointId), locationProgressionZ
				);

				jointTransforms.put(jointId, missingFrameTransform);
			}

			if (i < 10) {
				Log.d("Animator", "Completed Keyframe: " + keyFrameCurrent);
			} else if (i == 11) {
				Log.d("Animator", "Completed Keyframe... (omitted)");
			}
		}
		animatedModel.getAnimation().setInitialized(true);
		Log.i("Animator", "Initialized " + animatedModel.getId() + ". " + keyFrames.length + " key frames");
	}

	/**********************************************************************************************/
	private void increaseAnimationTime(AnimatedModel obj) {
		this.animationTime = SystemClock.uptimeMillis() / 1000f * speed;
		this.animationTime %= obj.getAnimation().getLength();
	}

	/**********************************************************************************************/
	private Map<String, float[]> calculateCurrentAnimationPose(AnimatedModel obj) {
		KeyFrame[] frames = getPreviousAndNextFrames(obj);
		float progression = calculateProgression(frames[0], frames[1]);
		return interpolatePoses(frames[0], frames[1], progression);
	}

	/**********************************************************************************************/
	private void applyPoseToJoints(AnimatedModel animatedModel, Map<String, float[]> currentPose, Joint joint, float[]
			parentTransform, int limit) {

		float[] currentTransform = (float[]) cache.get(joint.getName());
		if (currentTransform == null) {
			currentTransform = new float[16];
			cache.put(joint.getName(), currentTransform);
		}

		if (currentPose.get(joint.getName()) != null) {
			Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, currentPose.get(joint.getName()), 0);
		} else {
			Matrix.multiplyMM(currentTransform, 0, parentTransform, 0, joint.getBindLocalTransform(), 0);
		}

		if (limit >= 0) {
			if (joint.getInverseBindTransform() == null)
				Log.e("Animator", "joint with inverseBindTransform null: " + joint.getName() + ", index: " + joint.getIndex());
			Matrix.multiplyMM(joint.getAnimatedTransform(), 0, currentTransform, 0, joint.getInverseBindTransform(), 0);
		} else {
			System.arraycopy(Math3DUtils.IDENTITY_MATRIX, 0, joint.getAnimatedTransform(), 0, 16);
		}
		if (joint.getIndex() != -1) {
			animatedModel.updateAnimatedTransform(joint);
		}

		for (int i = 0; i < joint.getChildren().size(); i++) {
			Joint childJoint = joint.getChildren().get(i);
			applyPoseToJoints(animatedModel, currentPose, childJoint, currentTransform, limit - 1);
		}
	}

	/**********************************************************************************************/
	private KeyFrame[] getPreviousAndNextFrames(AnimatedModel obj) {
		KeyFrame[] allFrames = obj.getAnimation().getKeyFrames();
		KeyFrame previousFrame = allFrames[0];
		KeyFrame nextFrame = allFrames[0];
		for (int i = 1; i < allFrames.length; i++) {
			nextFrame = allFrames[i];
			if (nextFrame.getTimeStamp() > animationTime) {
				break;
			}
			previousFrame = allFrames[i];
		}
		previousAndNextKeyFrames[0] = previousFrame;
		previousAndNextKeyFrames[1] = nextFrame;
		return previousAndNextKeyFrames;
	}

	/**********************************************************************************************/
	private float calculateProgression(KeyFrame previousFrame, KeyFrame nextFrame) {
		float totalTime = nextFrame.getTimeStamp() - previousFrame.getTimeStamp();
		float currentTime = animationTime - previousFrame.getTimeStamp();

		return currentTime / totalTime * this.speed;
	}

	/**********************************************************************************************/
	private Map<String, float[]> interpolatePoses(KeyFrame previousFrame, KeyFrame nextFrame, float progression) {
		for (Map.Entry<String, JointTransform> entry : previousFrame.getTransforms().entrySet()) {

			final String jointName = entry.getKey();
			final JointTransform previousTransform = entry.getValue();

			if (Math.signum(progression) == 0) {
				currentPose.put(jointName, previousTransform.getMatrix());
				continue;
			}

			float[] tempMatrix1 = (float[]) cache.get(jointName);
			if (tempMatrix1 == null) {
				tempMatrix1 = new float[16];
				cache.put(jointName, tempMatrix1);
			}

			JointTransform nextTransform = nextFrame.getTransforms().get(jointName);

			JointTransform.interpolate(previousTransform, nextTransform, progression, tempMatrix1);

			currentPose.put(jointName, tempMatrix1);
		}
		return currentPose;
	}
	/**********************************************************************************************/
}
/**************************************************************************************************/
