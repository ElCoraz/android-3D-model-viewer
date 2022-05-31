package org.andresoviedo.util.math;

import androidx.annotation.NonNull;

/**************************************************************************************************/
public class Quaternion {
    /**********************************************************************************************/
    private float x, y, z, w;

    /**********************************************************************************************/
    public Quaternion() {
        this(0, 0, 0, 1);
    }

    /**********************************************************************************************/
    public Quaternion(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    /**********************************************************************************************/
    public void normalize() {
        float mag = (float) Math.sqrt(w * w + x * x + y * y + z * z);

        w /= mag;
        x /= mag;
        y /= mag;
        z /= mag;
    }

    /**********************************************************************************************/
    public float[] toRotationMatrix(float[] matrix) {
        final float xy = x * y;
        final float xz = x * z;
        final float xw = x * w;
        final float yz = y * z;
        final float yw = y * w;
        final float zw = z * w;

        final float xSquared = x * x;
        final float ySquared = y * y;
        final float zSquared = z * z;

        matrix[0] = 1 - 2 * (ySquared + zSquared);
        matrix[1] = 2 * (xy - zw);
        matrix[2] = 2 * (xz + yw);
        matrix[3] = 0;
        matrix[4] = 2 * (xy + zw);
        matrix[5] = 1 - 2 * (xSquared + zSquared);
        matrix[6] = 2 * (yz - xw);
        matrix[7] = 0;
        matrix[8] = 2 * (xz - yw);
        matrix[9] = 2 * (yz + xw);
        matrix[10] = 1 - 2 * (xSquared + ySquared);
        matrix[11] = 0;
        matrix[12] = 0;
        matrix[13] = 0;
        matrix[14] = 0;
        matrix[15] = 1;

        return matrix;
    }

    /**********************************************************************************************/
    public static Quaternion fromMatrix(float[] matrix) {
        float w, x, y, z;

        float diagonal = matrix[0] + matrix[5] + matrix[10];

        if (diagonal > 0) {
            float w4 = (float) (Math.sqrt(diagonal + 1f) * 2f);

            w = w4 / 4f;
            x = (matrix[9] - matrix[6]) / w4;
            y = (matrix[2] - matrix[8]) / w4;
            z = (matrix[4] - matrix[1]) / w4;
        } else if ((matrix[0] > matrix[5]) && (matrix[0] > matrix[10])) {
            float x4 = (float) (Math.sqrt(1f + matrix[0] - matrix[5] - matrix[10]) * 2f);

            w = (matrix[9] - matrix[6]) / x4;
            x = x4 / 4f;
            y = (matrix[1] + matrix[4]) / x4;
            z = (matrix[2] + matrix[8]) / x4;
        } else if (matrix[5] > matrix[10]) {
            float y4 = (float) (Math.sqrt(1f + matrix[5] - matrix[0] - matrix[10]) * 2f);

            w = (matrix[2] - matrix[8]) / y4;
            x = (matrix[1] + matrix[4]) / y4;
            y = y4 / 4f;
            z = (matrix[6] + matrix[9]) / y4;
        } else {
            float z4 = (float) (Math.sqrt(1f + matrix[10] - matrix[0] - matrix[5]) * 2f);
            w = (matrix[4] - matrix[1]) / z4;
            x = (matrix[2] + matrix[8]) / z4;
            y = (matrix[6] + matrix[9]) / z4;
            z = z4 / 4f;
        }
        return new Quaternion(x, y, z, w);
    }

    /**********************************************************************************************/
    public static void interpolate(Quaternion result, Quaternion a, Quaternion b, float blend) {
        float dot = a.w * b.w + a.x * b.x + a.y * b.y + a.z * b.z;

        float blendI = 1f - blend;

        if (dot < 0) {
            result.w = blendI * a.w + blend * -b.w;
            result.x = blendI * a.x + blend * -b.x;
            result.y = blendI * a.y + blend * -b.y;
            result.z = blendI * a.z + blend * -b.z;
        } else {
            result.w = blendI * a.w + blend * b.w;
            result.x = blendI * a.x + blend * b.x;
            result.y = blendI * a.y + blend * b.y;
            result.z = blendI * a.z + blend * b.z;
        }
        result.normalize();
    }

    /**********************************************************************************************/
    @NonNull
    @Override
    public String toString() {
        return "Quaternion{" + "x=" + x + ", y=" + y + ", z=" + z + ", w=" + w + '}';
    }

    /**********************************************************************************************/
    public float[] toEuler() {
        Quaternion q = this;

        double sinr_cosp = 2 * (q.w * q.x + q.y * q.z);
        double cosr_cosp = 1 - 2 * (q.x * q.x + q.y * q.y);

        float roll = (float) Math.atan2(sinr_cosp, cosr_cosp);

        double sinp = 2 * (q.w * q.y - q.z * q.x);

        final float pitch;

        if (Math.abs(sinp) >= 1)
            pitch = (float) Math.copySign(Math.PI / 2, sinp);
        else
            pitch = (float) Math.asin(sinp);

        double siny_cosp = 2 * (q.w * q.z + q.x * q.y);
        double cosy_cosp = 1 - 2 * (q.y * q.y + q.z * q.z);

        float yaw = (float) Math.atan2(siny_cosp, cosy_cosp);

        return new float[]{roll, pitch, yaw, 1};
    }
}
