package org.andresoviedo.android_3d_model_engine.model;

import android.opengl.Matrix;
import android.util.Log;

import org.andresoviedo.util.math.Math3DUtils;
/**************************************************************************************************/
public class Camera {
    /**********************************************************************************************/
    private static final float ROOM_CENTER_SIZE = 0.5f;
    private static final float ROOM_SIZE = 1000;
    /**********************************************************************************************/
    private final BoundingBox centerBox = new BoundingBox("scene", -ROOM_CENTER_SIZE, ROOM_CENTER_SIZE,
            -ROOM_CENTER_SIZE, ROOM_CENTER_SIZE, -ROOM_CENTER_SIZE, ROOM_CENTER_SIZE);
    private final BoundingBox roomBox = new BoundingBox("scene", -ROOM_SIZE, ROOM_SIZE,
            -ROOM_SIZE, ROOM_SIZE, -ROOM_SIZE, ROOM_SIZE);
    /**********************************************************************************************/
    private float[] buffer = new float[12 + 12 + 16 + 16];
    /**********************************************************************************************/
    private long animationCounter;
    private Object[] lastAction;
    private boolean changed = false;
    /**********************************************************************************************/
    private float[] coordinates = new float[16];
    /**********************************************************************************************/
    private float[] distanceToCenter = null;
    /**********************************************************************************************/
    private float[] pos = new float[]{0, 0, 0, 1};
    private float[] view = new float[]{0, 0, 0, 1};
    private float[] up = new float[]{0, 0, 0, 1};

    /**********************************************************************************************/
    public Camera(float distance) {
        this(0, 0, distance, 0, 0, 0, 0, 1, 0);
    }

    /**********************************************************************************************/
    public Camera(float xPos, float yPos, float zPos, float xView, float yView, float zView, float xUp, float yUp,
                  float zUp) {
        this.pos[0] = xPos;
        this.pos[1] = yPos;
        this.pos[2] = zPos;

        this.view[0] = xView;
        this.view[1] = yView;
        this.view[2] = zView;

        this.up[0] = xUp;
        this.up[1] = yUp;
        this.up[2] = zUp;
    }

    /**********************************************************************************************/
    public synchronized void animate() {
        if (lastAction == null || animationCounter == 0) {
            lastAction = null;
            animationCounter = 100;
            return;
        }
        String method = (String) lastAction[0];
        if (method.equals("translate")) {
            float dX = (Float) lastAction[1];
            float dY = (Float) lastAction[2];
            translateCameraImpl(dX * animationCounter / 100, dY * animationCounter / 100);
        } else if (method.equals("rotate")) {
            float rotZ = (Float) lastAction[1];
            RotateImpl(rotZ / 100 * animationCounter);
        }
        animationCounter--;
    }

    /**********************************************************************************************/
    public synchronized void MoveCameraZ(float direction) {
        if (direction == 0) return;
        MoveCameraZImpl(direction);
        lastAction = new Object[]{"zoom", direction};
    }

    /**********************************************************************************************/
    private void MoveCameraZImpl(float direction) {
        float xLookDirection, yLookDirection, zLookDirection;

        xLookDirection = getxView() - pos[0];
        yLookDirection = getyView() - pos[1];
        zLookDirection = view[2] - pos[2];

        float dp = Matrix.length(xLookDirection, yLookDirection, zLookDirection);

        xLookDirection /= dp;
        yLookDirection /= dp;
        zLookDirection /= dp;

        float x = pos[0] + xLookDirection * direction;
        float y = pos[1] + yLookDirection * direction;
        float z = pos[2] + zLookDirection * direction;

        if (isOutOfBounds(x, y, z)) return;

        pos[0] = x;
        pos[1] = y;
        pos[2] = z;

        setChanged(true);
    }

    /**********************************************************************************************/
    private boolean isOutOfBounds(float x, float y, float z) {
        if (roomBox.outOfBound(x, y, z)) {
            Log.v("Camera", "Out of room walls. " + x + "," + y + "," + z);
            return true;
        }
        if (!centerBox.outOfBound(x, y, z)) {
            Log.v("Camera", "Inside absolute center");
            return true;
        }
        return false;
    }

    /**********************************************************************************************/
    public synchronized void translateCamera(float dX, float dY) {
        if (dX == 0 && dY == 0) return;
        translateCameraImpl(dX, dY);
        lastAction = new Object[]{"translate", dX, dY};
    }

    /**********************************************************************************************/
    private void translateCameraImpl(float dX, float dY) {
        float vlen;
        float xLook, yLook, zLook;

        xLook = getxView() - pos[0];
        yLook = getyView() - pos[1];
        zLook = view[2] - pos[2];

        vlen = Matrix.length(xLook, yLook, zLook);

        xLook /= vlen;
        yLook /= vlen;
        zLook /= vlen;

        float xArriba, yArriba, zArriba;

        xArriba = getxUp() - pos[0];
        yArriba = getyUp() - pos[1];
        zArriba = getzUp() - pos[2];

        vlen = Matrix.length(xArriba, yArriba, zArriba);

        xArriba /= vlen;
        yArriba /= vlen;
        zArriba /= vlen;

        float xRight, yRight, zRight;

        xRight = (yLook * zArriba) - (zLook * yArriba);
        yRight = (zLook * xArriba) - (xLook * zArriba);
        zRight = (xLook * yArriba) - (yLook * xArriba);

        vlen = Matrix.length(xRight, yRight, zRight);

        xRight /= vlen;
        yRight /= vlen;
        zRight /= vlen;

        xArriba = (yRight * zLook) - (zRight * yLook);
        yArriba = (zRight * xLook) - (xRight * zLook);
        zArriba = (xRight * yLook) - (yRight * xLook);

        vlen = Matrix.length(xArriba, yArriba, zArriba);

        xArriba /= vlen;
        yArriba /= vlen;
        zArriba /= vlen;

        coordinates[0] = pos[0];
        coordinates[1] = pos[1];
        coordinates[2] = pos[2];
        coordinates[3] = 1;
        coordinates[4] = getxView();
        coordinates[5] = getyView();
        coordinates[6] = view[2];
        coordinates[7] = 1;
        coordinates[8] = getxUp();
        coordinates[9] = getyUp();
        coordinates[10] = getzUp();
        coordinates[11] = 1;

        if (dX != 0 && dY != 0) {

            xRight *= dY;
            yRight *= dY;
            zRight *= dY;

            xArriba *= dX;
            yArriba *= dX;
            zArriba *= dX;

            float rotX, rotY, rotZ;

            rotX = xRight + xArriba;
            rotY = yRight + yArriba;
            rotZ = zRight + zArriba;

            vlen = Matrix.length(rotX, rotY, rotZ);

            rotX /= vlen;
            rotY /= vlen;
            rotZ /= vlen;

            createRotationMatrixAroundVector(buffer, 24, vlen, rotX, rotY, rotZ);
        } else if (dX != 0) {
            createRotationMatrixAroundVector(buffer, 24, dX, xArriba, yArriba, zArriba);
        } else {
            createRotationMatrixAroundVector(buffer, 24, dY, xRight, yRight, zRight);
        }
        multiplyMMV(buffer, 0, buffer, 24, coordinates, 0);

        if (isOutOfBounds(buffer[0], buffer[1], buffer[2])) return;

        pos[0] = buffer[0] / buffer[3];
        pos[1] = buffer[1] / buffer[3];
        pos[2] = buffer[2] / buffer[3];

        view[0] = buffer[4] / buffer[4 + 3];
        view[1] = buffer[4 + 1] / buffer[4 + 3];
        view[2] = buffer[4 + 2] / buffer[4 + 3];

        up[0] = buffer[8] / buffer[8 + 3];
        up[1] = buffer[8 + 1] / buffer[8 + 3];
        up[2] = buffer[8 + 2] / buffer[8 + 3];

        setChanged(true);
    }

    /**********************************************************************************************/
    private static void createRotationMatrixAroundVector(float[] matrix, int offset, float angle, float x, float y,
                                                         float z) {
        float cos = (float) Math.cos(angle);
        float sin = (float) Math.sin(angle);
        float cos_1 = 1 - cos;

        matrix[offset] = cos_1 * x * x + cos;
        matrix[offset + 1] = cos_1 * x * y - z * sin;
        matrix[offset + 2] = cos_1 * z * x + y * sin;
        matrix[offset + 3] = 0;
        matrix[offset + 4] = cos_1 * x * y + z * sin;
        matrix[offset + 5] = cos_1 * y * y + cos;
        matrix[offset + 6] = cos_1 * y * z - x * sin;
        matrix[offset + 7] = 0;
        matrix[offset + 8] = cos_1 * z * x - y * sin;
        matrix[offset + 9] = cos_1 * y * z + x * sin;
        matrix[offset + 10] = cos_1 * z * z + cos;
        matrix[offset + 11] = 0;
        matrix[offset + 12] = 0;
        matrix[offset + 13] = 0;
        matrix[offset + 14] = 0;
        matrix[offset + 15] = 1;
    }

    /**********************************************************************************************/
    private static void multiplyMMV(float[] result, int retOffset, float[] matrix, int matOffet, float[] vector4Matrix,
                                    int vecOffset) {
        for (int i = 0; i < vector4Matrix.length / 4; i++) {
            Matrix.multiplyMV(result, retOffset + (i * 4), matrix, matOffet, vector4Matrix, vecOffset + (i * 4));
        }
    }

    /**********************************************************************************************/
    public boolean hasChanged() {
        return changed;
    }

    /**********************************************************************************************/
    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    /**********************************************************************************************/
    @Override
    public String toString() {
        return "Camera [xPos=" + pos[0] + ", yPos=" + pos[1] + ", zPos=" + pos[2] + ", xView=" + getxView() + ", yView=" + getyView()
                + ", zView=" + view[2] + ", xUp=" + getxUp() + ", yUp=" + getyUp() + ", zUp=" + getzUp() + "]";
    }

    /**********************************************************************************************/
    public synchronized void Rotate(float rotViewerZ) {
        if (rotViewerZ == 0) return;
        RotateImpl(rotViewerZ);
        lastAction = new Object[]{"rotate", rotViewerZ};
    }

    /**********************************************************************************************/
    private void RotateImpl(float rotViewerZ) {
        if (Float.isNaN(rotViewerZ)) {
            Log.w("Rot", "NaN");
            return;
        }
        float xLook = getxView() - pos[0];
        float yLook = getyView() - pos[1];
        float zLook = view[2] - pos[2];

        float vlen = Matrix.length(xLook, yLook, zLook);

        xLook /= vlen;
        yLook /= vlen;
        zLook /= vlen;

        createRotationMatrixAroundVector(buffer, 24, rotViewerZ, xLook, yLook, zLook);

        coordinates[0] = pos[0];
        coordinates[1] = pos[1];
        coordinates[2] = pos[2];
        coordinates[3] = 1;
        coordinates[4] = getxView();
        coordinates[5] = getyView();
        coordinates[6] = view[2];
        coordinates[7] = 1;
        coordinates[8] = getxUp();
        coordinates[9] = getyUp();
        coordinates[10] = getzUp();
        coordinates[11] = 1;

        multiplyMMV(buffer, 0, buffer, 24, coordinates, 0);

        pos[0] = buffer[0];
        pos[1] = buffer[1];
        pos[2] = buffer[2];

        view[0] = buffer[4];
        view[1] = buffer[4 + 1];
        view[2] = buffer[4 + 2];

        up[0] = buffer[8];
        up[1] = buffer[8 + 1];
        up[2] = buffer[8 + 2];

        setChanged(true);
    }

    /**********************************************************************************************/
    public Camera[] toStereo(float eyeSeparation) {
        float xLook = getxView() - pos[0];
        float yLook = getyView() - pos[1];
        float zLook = view[2] - pos[2];

        float[] crossRight = Math3DUtils.crossProduct(xLook, yLook, zLook, getxUp(), getyUp(), getzUp());

        Math3DUtils.normalize(crossRight);

        float xPosLeft = pos[0] - crossRight[0] * eyeSeparation / 2;
        float yPosLeft = pos[1] - crossRight[1] * eyeSeparation / 2;
        float zPosLeft = pos[2] - crossRight[2] * eyeSeparation / 2;

        float xViewLeft = getxView() - crossRight[0] * eyeSeparation / 2;
        float yViewLeft = getyView() - crossRight[1] * eyeSeparation / 2;
        float zViewLeft = view[2] - crossRight[2] * eyeSeparation / 2;

        float xPosRight = pos[0] + crossRight[0] * eyeSeparation / 2;
        float yPosRight = pos[1] + crossRight[1] * eyeSeparation / 2;
        float zPosRight = pos[2] + crossRight[2] * eyeSeparation / 2;

        float xViewRight = getxView() + crossRight[0] * eyeSeparation / 2;
        float yViewRight = getyView() + crossRight[1] * eyeSeparation / 2;
        float zViewRight = view[2] + crossRight[2] * eyeSeparation / 2;

        xViewLeft = getxView();
        yViewLeft = getyView();
        zViewLeft = view[2];

        xViewRight = getxView();
        yViewRight = getyView();
        zViewRight = view[2];

        Camera left = new Camera(xPosLeft, yPosLeft, zPosLeft, xViewLeft, yViewLeft, zViewLeft, getxUp(), getyUp(), getzUp());
        Camera right = new Camera(xPosRight, yPosRight, zPosRight, xViewRight, yViewRight, zViewRight, getxUp(), getyUp(), getzUp());

        return new Camera[]{left, right};
    }

    /**********************************************************************************************/
    public float[] getDistanceToCenterVector() {
        if (distanceToCenter != null) return distanceToCenter;

        distanceToCenter = new float[4];

        distanceToCenter[0] = -pos[0];
        distanceToCenter[1] = -pos[1];
        distanceToCenter[2] = -pos[2];
        distanceToCenter[3] = 1;

        return distanceToCenter;
    }

    /**********************************************************************************************/
    public void rotate(float degrees, float x, float y, float z) {
        Matrix.setIdentityM(buffer, 24); // first matrix
        Matrix.rotateM(buffer, 40, buffer, 24, degrees, x, y, z); // 2nd matrix
        Matrix.multiplyMV(buffer, 0, buffer, 40, pos, 0);

        pos[0] = buffer[0];
        pos[1] = buffer[1];
        pos[2] = buffer[2];

        Matrix.multiplyMV(buffer, 0, buffer, 40, view, 0);

        view[0] = buffer[0];
        view[1] = buffer[1];
        view[2] = buffer[2];

        Matrix.multiplyMV(buffer, 0, buffer, 40, up, 0);

        up[0] = buffer[0];
        up[1] = buffer[1];
        up[2] = buffer[2];

        setChanged(true);
    }

    /**********************************************************************************************/
    public float getxView() {
        return view[0];
    }

    /**********************************************************************************************/
    public float getyView() {
        return view[1];
    }

    /**********************************************************************************************/
    public float getzView() {
        return view[2];
    }

    /**********************************************************************************************/
    public float getxUp() {
        return up[0];
    }

    /**********************************************************************************************/
    public float getyUp() {
        return up[1];
    }

    /**********************************************************************************************/
    public float getzUp() {
        return up[2];
    }

    /**********************************************************************************************/
    public float getxPos() {
        return pos[0];
    }

    /**********************************************************************************************/
    public float getyPos() {
        return pos[1];
    }

    /**********************************************************************************************/
    public float getzPos() {
        return pos[2];
    }
}
