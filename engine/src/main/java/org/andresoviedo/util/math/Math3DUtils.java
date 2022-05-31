package org.andresoviedo.util.math;

import android.opengl.Matrix;

import org.andresoviedo.android_3d_model_engine.animation.JointTransform;

import java.math.BigDecimal;
import java.util.List;
import java.util.Locale;
/**************************************************************************************************/
public class Math3DUtils {
    /**********************************************************************************************/
    public static final float IDENTITY_MATRIX[] = new float[16];

    /**********************************************************************************************/
    static {
        Matrix.setIdentityM(IDENTITY_MATRIX, 0);
    }

    /**********************************************************************************************/
    public static float[] initMatrixIfNull(float[] matrix) {
        if (matrix != null)  {
            return matrix;
        }

        matrix = new float[16];

        Matrix.setIdentityM(matrix, 0);

        return matrix;
    }

    /**********************************************************************************************/
    public static float[] calculateNormal(float[] v0, float[] v1, float[] v2) {
        double[] va = new double[]{v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2]};
        double[] vb = new double[]{v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2]};

        float[] vn = {(float) (va[1] * vb[2] - va[2] * vb[1]), (float) (va[2] * vb[0] - va[0] * vb[2]), (float) (va[0] * vb[1] - va[1] * vb[0])};

        if (length(vn) != 0) {
            return vn;
        } else {
            return calculateNormal_highPrecision(v0, v1, v2);
        }
    }

    /**********************************************************************************************/
    public static float[] calculateNormal_highPrecision(float[] v0, float[] v1, float[] v2) {
        final float[] u = substract(v1, v0);
        final float[] v = substract(v2, v0);

        final BigDecimal[] u_ = new BigDecimal[]{
                new BigDecimal(Float.toString(u[0])),
                new BigDecimal(Float.toString(u[1])),
                new BigDecimal(Float.toString(u[2]))
        };

        final BigDecimal[] v_ = new BigDecimal[]{
                new BigDecimal(Float.toString(v[0])),
                new BigDecimal(Float.toString(v[1])),
                new BigDecimal(Float.toString(v[2]))
        };

        final BigDecimal[] n_ = new BigDecimal[]{
                u_[1].multiply(v_[2]).subtract(u_[2].multiply(v_[1])),
                u_[2].multiply(v_[0]).subtract(u_[0].multiply(v_[2])),
                u_[0].multiply(v_[1]).subtract(u_[1].multiply(v_[0]))
        };

        return new float[]{
                n_[0].floatValue(),
                n_[1].floatValue(),
                n_[2].floatValue()
        };
    }

    /**********************************************************************************************/
    public static float[][] calculateNormalLine(float[] v0, float[] v1, float[] v2, boolean scale) {
        float[] va = new float[]{v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2]};
        float[] vb = new float[]{v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2]};
        float[] n = new float[]{va[1] * vb[2] - va[2] * vb[1], va[2] * vb[0] - va[0] * vb[2], va[0] * vb[1] - va[1] * vb[0]};

        float modul = Matrix.length(n[0], n[1], n[2]);

        float[] vn = new float[]{n[0] / modul, n[1] / modul, n[2] / modul};

        return getNormalLine(v0, v1, v2, vn, scale, 1);
    }

    /**********************************************************************************************/
    public static float[][] getNormalLine(float[] v0, float[] v1, float[] v2, float[] normal, boolean scale, float rescale) {
        final float[] faceCenter = calculateFaceCenter(v0, v1, v2);

        final float[] va = new float[]{v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2]};
        final float[] vb = new float[]{v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2]};
        final float[] vc = new float[]{v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2]};

        final float scaleFactor = scale ? (length(va[0], va[1], va[2]) + length(vb[0], vb[1], vb[2]) + length(vc[0], vc[1], vc[2])) / 3 : 1;

        float[] vn2 = new float[]{faceCenter[0] + normal[0] * scaleFactor * rescale, faceCenter[1] + normal[1] * scaleFactor * rescale, faceCenter[2] + normal[2] * scaleFactor * rescale};

        return new float[][]{faceCenter, vn2};
    }

    /**********************************************************************************************/
    public static float[][] getNormalLines(float[] v0, float[] v1, float[] v2, float[] normal, boolean scale, float rescale) {
        final float[] va = new float[]{v1[0] - v0[0], v1[1] - v0[1], v1[2] - v0[2]};
        final float[] vb = new float[]{v2[0] - v0[0], v2[1] - v0[1], v2[2] - v0[2]};
        final float[] vc = new float[]{v2[0] - v1[0], v2[1] - v1[1], v2[2] - v1[2]};

        final float scaleFactor = scale ? (length(va[0], va[1], va[2]) + length(vb[0], vb[1], vb[2]) + length(vc[0], vc[1], vc[2])) / 3 : 1;

        float[] vn0 = new float[]{v0[0] + normal[0] * scaleFactor * rescale, v0[1] + normal[1] * scaleFactor * rescale, v0[2] + normal[2] * scaleFactor * rescale};
        float[] vn1 = new float[]{v1[0] + normal[0] * scaleFactor * rescale, v1[1] + normal[1] * scaleFactor * rescale, v1[2] + normal[2] * scaleFactor * rescale};
        float[] vn2 = new float[]{v2[0] + normal[0] * scaleFactor * rescale, v2[1] + normal[1] * scaleFactor * rescale, v2[2] + normal[2] * scaleFactor * rescale};

        return new float[][]{v0, vn0, v1, vn1, v2, vn2};
    }

    /**********************************************************************************************/
    public static float[] calculateFaceCenter(float[] v0, float[] v1, float[] v2) {
        return new float[]{(v0[0] + v1[0] + v2[0]) / 3, (v0[1] + v1[1] + v2[1]) / 3, (v0[2] + v1[2] + v2[2]) / 3};
    }

    /**********************************************************************************************/
    public static float calculateDistanceOfIntersection(float[] rayPoint1, float[] rayPoint2, float[] target, float precision) {
        float raySteps = 100f;
        float objHalfWidth = precision / 2;

        float length = Matrix.length(rayPoint2[0] - rayPoint1[0], rayPoint2[1] - rayPoint1[1], rayPoint2[2] - rayPoint1[2]);

        float lengthDiff = length / raySteps;

        float xDif = (rayPoint2[0] - rayPoint1[0]) / raySteps;
        float yDif = (rayPoint2[1] - rayPoint1[1]) / raySteps;
        float zDif = (rayPoint2[2] - rayPoint1[2]) / raySteps;

        for (int i = 0; i < raySteps; i++) {

            if ((rayPoint1[0] + (xDif * i)) > target[0] - objHalfWidth
                    && (rayPoint1[0] + (xDif * i)) < target[0] + objHalfWidth
                    && (rayPoint1[1] + (yDif * i)) > target[1] - objHalfWidth
                    && (rayPoint1[1] + (yDif * i)) < target[1] + objHalfWidth
                    && (rayPoint1[2] + (zDif * i)) > target[2] - objHalfWidth
                    && (rayPoint1[2] + (zDif * i)) < target[2] + objHalfWidth) {

                return i * lengthDiff;
            }
        }
        return -1;
    }

    /**********************************************************************************************/
    public static float[] substract(float[] a, float[] b) {
        return new float[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    /**********************************************************************************************/
    public static float[] divide(float[] a, float[] b) {
        return new float[]{a[0] / b[0], a[1] / b[1], a[2] / b[2]};
    }

    /**********************************************************************************************/
    public static float[] divide(float[] a, float b) {
        return new float[]{a[0] / b, a[1] / b, a[2] / b};
    }

    /**********************************************************************************************/
    public static float[] min(float[] a, float[] b) {
        return new float[]{Math.min(a[0], b[0]), Math.min(a[1], b[1]), Math.min(a[2], b[2])};
    }

    /**********************************************************************************************/
    public static float[] max(float[] a, float[] b) {
        return new float[]{Math.max(a[0], b[0]), Math.max(a[1], b[1]), Math.max(a[2], b[2])};
    }

    /**********************************************************************************************/
    public static void normalize(float[] a) {
        float length = length(a);

        if (length == 0) {
            throw new IllegalArgumentException("vector length is zero");
        }

        a[0] = a[0] / length;
        a[1] = a[1] / length;
        a[2] = a[2] / length;
    }

    /**********************************************************************************************/
    public static float[] crossProduct(float[] a, float[] b) {
        float x = a[1] * b[2] - a[2] * b[1];
        float y = a[2] * b[0] - a[0] * b[2];
        float z = a[0] * b[1] - a[1] * b[0];

        return new float[]{x, y, z};
    }

    /**********************************************************************************************/
    public static float[] crossProduct(float ax, float ay, float az, float bx, float by, float bz) {
        float x = ay * bz - az * by;
        float y = az * bx - ax * bz;
        float z = ax * by - ay * bx;

        return new float[]{x, y, z};
    }

    /**********************************************************************************************/
    public static float dotProduct(float[] a, float[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /**********************************************************************************************/
    public static float[] multiply(float[] a, float t) {
        return new float[]{a[0] * t, a[1] * t, a[2] * t};
    }

    /**********************************************************************************************/
    public static float[] add(float[] a, float[] b) {
        return new float[]{a[0] + b[0], a[1] + b[1], a[2] + b[2]};
    }

    /**********************************************************************************************/
    public static float[] mean(List<float[]> normals) {
        float[] normal_mean = normals.get(0);

        for (int i = 1; i < normals.size(); i++) {
            float[] normal_next = normals.get(i);
            normal_mean = mean(normal_mean, normal_next);
        }

        return normal_mean;
    }

    /**********************************************************************************************/
    public static float[] mean(float[] a, float[] b) {
        float[] add = add(a, b);

        add[0] /= 2;
        add[1] /= 2;
        add[2] /= 2;

        return add;
    }

    /**********************************************************************************************/
    public static String toString(float[] matrix, int indent) {
        StringBuilder ret = new StringBuilder();
        for (int i = 0; i < 4; i++) {
            ret.append("\n");
            for (int k = 0; k < indent; k++) {
                ret.append(" ");
            }
            for (int j = 0; j < 4; j++) {
                if (matrix[j * 4 + i] >= 0) {
                    ret.append("+");
                }
                ret.append(String.format(Locale.getDefault(), "%+.3f", matrix[j * 4 + i]));
                ret.append("  ");
            }
        }
        return ret.toString();
    }

    /**********************************************************************************************/
    public static String toString(float[] array) {
        StringBuilder ret = new StringBuilder("[");
        for (int i = 0; i < array.length; i++) {
            if (i > 0) ret.append(" ");
            ret.append(String.format(Locale.getDefault(), "%+.4f", array[i]));
        }
        ret.append("]");
        return ret.toString();
    }

    /**********************************************************************************************/
    public static float[] parseFloat(String[] rawData) {
        float[] matrixData = new float[rawData.length];
        for (int i = 0; i < rawData.length; i++) {
            matrixData[i] = Float.parseFloat(rawData[i]);
        }
        return matrixData;
    }

    /**********************************************************************************************/
    public static void setRotateM(float[] rm, int rmOffset, float a, float x, float y, float z) {
        rm[rmOffset + 3] = 0;
        rm[rmOffset + 7] = 0;
        rm[rmOffset + 11] = 0;
        rm[rmOffset + 12] = 0;
        rm[rmOffset + 13] = 0;
        rm[rmOffset + 14] = 0;
        rm[rmOffset + 15] = 1;

        a *= (float) (Math.PI / 180.0f);

        float s = (float) Math.sin(a);
        float c = (float) Math.cos(a);

        if (1.0f == x && 0.0f == y && 0.0f == z) {
            rm[rmOffset + 5] = c;
            rm[rmOffset + 10] = c;
            rm[rmOffset + 6] = s;
            rm[rmOffset + 9] = -s;
            rm[rmOffset + 1] = 0;
            rm[rmOffset + 2] = 0;
            rm[rmOffset + 4] = 0;
            rm[rmOffset + 8] = 0;
            rm[rmOffset + 0] = 1;
        } else if (0.0f == x && 1.0f == y && 0.0f == z) {
            rm[rmOffset + 0] = c;
            rm[rmOffset + 10] = c;
            rm[rmOffset + 8] = s;
            rm[rmOffset + 2] = -s;
            rm[rmOffset + 1] = 0;
            rm[rmOffset + 4] = 0;
            rm[rmOffset + 6] = 0;
            rm[rmOffset + 9] = 0;
            rm[rmOffset + 5] = 1;
        } else if (0.0f == x && 0.0f == y && 1.0f == z) {
            rm[rmOffset + 0] = c;
            rm[rmOffset + 5] = c;
            rm[rmOffset + 1] = s;
            rm[rmOffset + 4] = -s;
            rm[rmOffset + 2] = 0;
            rm[rmOffset + 6] = 0;
            rm[rmOffset + 8] = 0;
            rm[rmOffset + 9] = 0;
            rm[rmOffset + 10] = 1;
        } else {
            float len = length(x, y, z);

            if (1.0f != len) {
                float recipLen = 1.0f / len;

                x *= recipLen;
                y *= recipLen;
                z *= recipLen;
            }

            float nc = 1.0f - c;
            float xy = x * y;
            float yz = y * z;
            float zx = z * x;
            float xs = x * s;
            float ys = y * s;
            float zs = z * s;

            rm[rmOffset + 0] = x * x * nc + c;
            rm[rmOffset + 4] = xy * nc - zs;
            rm[rmOffset + 8] = zx * nc + ys;
            rm[rmOffset + 1] = xy * nc + zs;
            rm[rmOffset + 5] = y * y * nc + c;
            rm[rmOffset + 9] = yz * nc - xs;
            rm[rmOffset + 2] = zx * nc - ys;
            rm[rmOffset + 6] = yz * nc + xs;
            rm[rmOffset + 10] = z * z * nc + c;
        }
    }

    /**********************************************************************************************/
    public static float length(float[] vector) {
        return length(vector[0], vector[1], vector[2]);
    }

    /**********************************************************************************************/
    public static float length(float x, float y, float z) {
        return (float) Math.sqrt(x * x + y * y + z * z);
    }

    /**********************************************************************************************/
    public static void interpolate(JointTransform result, JointTransform start, JointTransform end, float progression) {
        interpolate(result.getScale(), start.getScale(), end.getScale(), progression);
        interpolate(result.getLocation(), start.getLocation(), end.getLocation(), progression);
        interpolate(result.getRotation1(), start.getRotation1(), end.getRotation1(), progression);
        interpolate(result.getRotation2(), start.getRotation2(), end.getRotation2(), progression);
        interpolate(result.getRotation2Location(), start.getRotation2Location(), end.getRotation2Location(), progression);
        Quaternion.interpolate(result.getQRotation(), start.getQRotation(), end.getQRotation(), progression);
    }

    /**********************************************************************************************/
    public static void interpolate(Float result[], Float[] start, Float[] end, float progression) {
        for (int i = 0; i < result.length; i++) {
            result[i] = start[i] + (end[i] - start[i]) * progression;
        }
    }

    /**********************************************************************************************/
    public static float[] negate(float[] vector) {
        float[] ret = new float[vector.length];
        for (int i = 0; i < vector.length; i++) ret[i] = -vector[i];
        return ret;
    }

    /**********************************************************************************************/
    public static float[] mult(float[] vector1, float[] vector2) {
        float[] ret = new float[vector1.length];
        for (int i = 0; i < vector1.length; i++) ret[i] = vector1[i] * vector2[i];
        return ret;
    }

    /**********************************************************************************************/
    public static Float[] scaleFromMatrix(float[] matrix) {
        Float[] ret = new Float[3];

        ret[0] = (float) Math.sqrt(Math.pow(matrix[0], 2) + Math.pow(matrix[1], 2) + Math.pow(matrix[2], 2));
        ret[1] = (float) Math.sqrt(Math.pow(matrix[4], 2) + Math.pow(matrix[5], 2) + Math.pow(matrix[6], 2));
        ret[2] = (float) Math.sqrt(Math.pow(matrix[8], 2) + Math.pow(matrix[9], 2) + Math.pow(matrix[10], 2));

        if (determinant(matrix) < 0) {
            ret[1] = -ret[1];
        }

        return ret;
    }

    /**********************************************************************************************/
    public static float[] scaleFromMatrixf(float[] matrix) {
        float[] ret = new float[3];

        ret[0] = (float) Math.sqrt(Math.pow(matrix[0], 2) + Math.pow(matrix[1], 2) + Math.pow(matrix[2], 2));
        ret[1] = (float) Math.sqrt(Math.pow(matrix[4], 2) + Math.pow(matrix[5], 2) + Math.pow(matrix[6], 2));
        ret[2] = (float) Math.sqrt(Math.pow(matrix[8], 2) + Math.pow(matrix[9], 2) + Math.pow(matrix[10], 2));

        if (determinant(matrix) < 0) {
            ret[1] = -ret[1];
        }

        return ret;
    }

    /**********************************************************************************************/
    public static float determinant(float[] matrix) {
        float ret = 0;

        ret += matrix[0] * (matrix[5] * (matrix[10] * matrix[15] - matrix[11] * matrix[14]));
        ret -= matrix[1] * (matrix[6] * (matrix[11] * matrix[12] - matrix[8] * matrix[15]));
        ret += matrix[2] * (matrix[7] * (matrix[8] * matrix[13] - matrix[9] * matrix[12]));
        ret -= matrix[3] * (matrix[4] * (matrix[9] * matrix[14] - matrix[10] * matrix[13]));

        return ret;
    }

    /**********************************************************************************************/
    public static float[] createRotationMatrixAroundVector(float angle, float x, float y, float z) {

        final float[] matrix = new float[16];
        final int offset = 0;

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

        return matrix;
    }
}

