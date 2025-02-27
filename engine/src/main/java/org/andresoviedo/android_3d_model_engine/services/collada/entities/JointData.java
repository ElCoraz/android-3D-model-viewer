package org.andresoviedo.android_3d_model_engine.services.collada.entities;

import android.opengl.Matrix;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
/**************************************************************************************************/
public class JointData {
	/**********************************************************************************************/
	private final String id;
	private final String name;
	private final String sid;
	/**********************************************************************************************/
	private final String instance_geometry;
	/**********************************************************************************************/
	private final Map<String, String> materials;
	/**********************************************************************************************/
	private final float[] bindLocalMatrix;
	/**********************************************************************************************/
	private final Float[] bindLocalScale;
	private final Float[] bindLocalRotation;
	private final Float[] bindLocalLocation;
	/**********************************************************************************************/
	private final float[] bindLocalTransform;
	/**********************************************************************************************/
	private final float[] bindTransform;
	/**********************************************************************************************/
	private int index = -1;
	/**********************************************************************************************/
	private float[] inverseBindTransform;
	/**********************************************************************************************/
	public final List<JointData> children = new ArrayList<>();

	/**********************************************************************************************/
	public JointData(String id, String name, String sid,
					 float[] bindLocalMatrix, Float[] bindLocalScale, Float[] bindLocalRotation, Float[] bindLocalLocation,
					 final float[] bindLocalTransform, final float[] bindTransform,
					 String geometryId, Map<String, String> materials) {
		this.id = id;
		this.name = name;
		this.sid = sid;

		this.bindLocalMatrix = bindLocalMatrix;
		this.bindLocalScale = bindLocalScale;
		this.bindLocalRotation = bindLocalRotation;
		this.bindLocalLocation = bindLocalLocation;

		this.instance_geometry = geometryId;
		this.materials = materials;

		this.bindLocalTransform = bindLocalTransform;
		this.bindTransform = bindTransform;
	}

	/**********************************************************************************************/
	public JointData(String id) {
		this.index = -1;
		this.id = id;
		this.name = id;
		this.sid = id;

		this.bindLocalMatrix = new float[16];

		Matrix.setIdentityM(this.bindLocalMatrix, 0);

		this.bindLocalScale = new Float[]{1f, 1f, 1f};
		this.bindLocalRotation = new Float[3];
		this.bindLocalLocation = new Float[3];
		this.inverseBindTransform = new float[16];

		Matrix.setIdentityM(this.inverseBindTransform, 0);

		this.bindLocalTransform = new float[16];

		Matrix.setIdentityM(this.bindLocalTransform, 0);

		this.bindTransform = new float[16];

		Matrix.setIdentityM(this.bindTransform, 0);

		this.instance_geometry = id;
		this.materials = null;
	}

	/**********************************************************************************************/
	public String getId() {
		return id;
	}

	/**********************************************************************************************/
	public String getName() {
		return name;
	}

	/**********************************************************************************************/
	public String getSid() {
		return sid;
	}

	/**********************************************************************************************/
	public void setIndex(int index) {
		this.index = index;
	}

	/**********************************************************************************************/
	public int getIndex() {
		return index;
	}

	/**********************************************************************************************/
	public Float[] getBindLocalScale() {
		return bindLocalScale;
	}

	/**********************************************************************************************/
	public Float[] getBindLocalRotation() {
		return bindLocalRotation;
	}

	/**********************************************************************************************/
	public Float[] getBindLocalLocation() {
		return bindLocalLocation;
	}

	/**********************************************************************************************/
	public String getGeometryId() {
		return this.instance_geometry;
	}

	/**********************************************************************************************/
	public void setInverseBindTransform(float[] inverseBindTransform) {
		this.inverseBindTransform = inverseBindTransform;
	}

	/**********************************************************************************************/
	public float[] getInverseBindTransform() {
		return inverseBindTransform;
	}

	/**********************************************************************************************/
	public List<JointData> getChildren() {
		return children;
	}

	/**********************************************************************************************/
	public void addChild(JointData child) {
		children.add(child);
	}

	/**********************************************************************************************/
	@Deprecated
	public JointData find(String id) {
		if (id.equals(this.getId())) {
			return this;
		} else if (id.equals(this.getName())) {
			return this;
		} else if (id.equals(this.instance_geometry)) {
			return this;
		}

		for (JointData childJointData : this.children) {
			JointData candidate = childJointData.find(id);
			if (candidate != null) return candidate;
		}
		return null;
	}

	/**********************************************************************************************/
	public List<JointData> findAll(String id) {
		return findAllImpl(id, new ArrayList<>());
	}

	/**********************************************************************************************/
	private List<JointData> findAllImpl(String id, List<JointData> ret) {
		if (id.equals(this.getId())) {
			ret.add(this);
		} else if (id.equals(this.getName())) {
			ret.add(this);
		} else if (id.equals(this.instance_geometry)) {
			ret.add(this);
		}
		for (JointData childJointData : this.children) {
			ret.addAll(childJointData.findAll(id));
		}
		return ret;
	}

	/**********************************************************************************************/
	public boolean containsMaterial(String materialId) {
		return materials.containsKey(materialId);
	}

	/**********************************************************************************************/
	public String getMaterial(String materialId) {
		return materials.get(materialId);
	}

	/**********************************************************************************************/
	public float[] getBindTransform() {
		return bindTransform;
	}

	/**********************************************************************************************/
	public float[] getBindLocalTransform() {
		return bindLocalTransform;
	}

	/**********************************************************************************************/
	@NonNull
	@Override
	public String toString() {
		return "JointData{" +
				"index=" + getIndex() +
				", id='" + getId() + '\'' +
				", name='" + getName() + '\'' +
				'}';
	}
}
