package org.andresoviedo.android_3d_model_engine.services.collada.entities;

import java.util.List;
/**************************************************************************************************/
public class SkinningData {
	/**********************************************************************************************/
	private final String id;
	/**********************************************************************************************/
	private final float[] bindShapeMatrix;
	/**********************************************************************************************/
	public final List<String> jointOrder;
	/**********************************************************************************************/
	public final List<VertexSkinData> verticesSkinData;
	/**********************************************************************************************/
	private final float[] inverseBindMatrix;

	/**********************************************************************************************/
	public SkinningData(String skinId, float[] bindShapeMatrix, List<String> jointOrder, List<VertexSkinData> verticesSkinData, float[] inverseBindMatrix) {
		this.id = skinId;
		this.bindShapeMatrix = bindShapeMatrix;
		this.jointOrder = jointOrder;
		this.verticesSkinData = verticesSkinData;
		this.inverseBindMatrix = inverseBindMatrix;
	}

	/**********************************************************************************************/
	public float[] getBindShapeMatrix() {
		return bindShapeMatrix;
	}

	/**********************************************************************************************/
	public float[] getInverseBindMatrix() {
		return inverseBindMatrix;
	}

	/**********************************************************************************************/
	public String getId() {
		return this.id;
	}
}
