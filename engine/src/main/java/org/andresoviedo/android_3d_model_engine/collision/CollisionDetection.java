package org.andresoviedo.android_3d_model_engine.collision;

import android.opengl.GLU;
import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.BoundingBox;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.math.Math3DUtils;

import java.util.Arrays;
import java.util.List;
/**************************************************************************************************/
public class CollisionDetection {
    /**********************************************************************************************/
    public static Object3DData getBoxIntersection(List<Object3DData> objects, int width, int height, float[] modelViewMatrix, float[] modelProjectionMatrix, float windowX, float windowY) {
        float[] nearHit = unProject(width, height, modelViewMatrix, modelProjectionMatrix, windowX, windowY, 0);
        float[] farHit = unProject(width, height, modelViewMatrix, modelProjectionMatrix, windowX, windowY, 1);
        float[] direction = Math3DUtils.substract(farHit, nearHit);
        Math3DUtils.normalize(direction);
        return getBoxIntersection(objects, nearHit, direction);
    }

    /**********************************************************************************************/
    private static Object3DData getBoxIntersection(List<Object3DData> objects, float[] p1, float[] direction) {
        float min = Float.MAX_VALUE;
        Object3DData ret = null;
        for (Object3DData obj : objects) {
            if ("Point".equals(obj.getId()) || "Line".equals(obj.getId())) {
                continue;
            }
            BoundingBox box = obj.getBoundingBox();
            float[] intersection = getBoxIntersection(p1, direction, box);
            if (intersection[0] > 0 && intersection[0] <= intersection[1] && intersection[0] < min) {
                min = intersection[0];
                ret = obj;
            }
        }
        if (ret != null) {
            Log.i("CollisionDetection", "Collision detected '" + ret.getId() + "' distance: " + min + ", dimensions: " + ret.getCurrentDimensions());
        }
        return ret;
    }

    /**********************************************************************************************/
    private static boolean isBoxIntersection(float[] origin, float[] dir, BoundingBox b) {
        float[] intersection = getBoxIntersection(origin, dir, b);
        return intersection[0] > 0 && intersection[0] < intersection[1];
    }

    /**********************************************************************************************/
    public static float[] getBoxIntersection(float[] origin, float[] dir, BoundingBox b) {
        float[] tMin = Math3DUtils.divide(Math3DUtils.substract(b.getMin(), origin), dir);
        float[] tMax = Math3DUtils.divide(Math3DUtils.substract(b.getMax(), origin), dir);
        float[] t1 = Math3DUtils.min(tMin, tMax);
        float[] t2 = Math3DUtils.max(tMin, tMax);
        float tNear = Math.max(Math.max(t1[0], t1[1]), t1[2]);
        float tFar = Math.min(Math.min(t2[0], t2[1]), t2[2]);
        return new float[]{tNear, tFar};
    }

    /**********************************************************************************************/
    public static float[] unProject(int width, int height, float[] modelViewMatrix, float[] modelProjectionMatrix,
                                    float rx, float ry, float rz) {
        float[] xyzw = {0, 0, 0, 0};
        ry = (float) height - ry;
        int[] viewport = {0, 0, width, height};
        GLU.gluUnProject(rx, ry, rz, modelViewMatrix, 0, modelProjectionMatrix, 0,
                viewport, 0, xyzw, 0);
        xyzw[0] /= xyzw[3];
        xyzw[1] /= xyzw[3];
        xyzw[2] /= xyzw[3];
        xyzw[3] = 1;
        return xyzw;
    }

    /**********************************************************************************************/
    public static float[] getTriangleIntersection(List<Object3DData> objects, int width, int height, float[] modelViewMatrix, float[] modelProjectionMatrix, float windowX, float windowY) {
        float[] nearHit = unProject(width, height, modelViewMatrix, modelProjectionMatrix, windowX, windowY, 0);
        float[] farHit = unProject(width, height, modelViewMatrix, modelProjectionMatrix, windowX, windowY, 1);
        float[] direction = Math3DUtils.substract(farHit, nearHit);
        Math3DUtils.normalize(direction);
        Object3DData intersected = getBoxIntersection(objects, nearHit, direction);
        if (intersected != null) {
            return getTriangleIntersection(intersected, nearHit, direction);
        }
        return null;
    }

    /**********************************************************************************************/
    public static float[] getTriangleIntersection(Object3DData hit, int width, int height, float[] viewMatrix, float[] projectionMatrix, float windowX, float windowY) {
        float[] nearHit = unProject(width, height, viewMatrix, projectionMatrix, windowX, windowY, 0);
        float[] farHit = unProject(width, height, viewMatrix, projectionMatrix, windowX, windowY, 1);
        float[] direction = Math3DUtils.substract(farHit, nearHit);
        Math3DUtils.normalize(direction);
        return getTriangleIntersection(hit, nearHit, direction);
    }

    /**********************************************************************************************/
    private static float[] getTriangleIntersection(final Object3DData hit, float[] nearHit, float[] direction) {
        Log.d("CollisionDetection", "Getting triangle intersection: " + hit.getId());
        Octree octree;
        synchronized (hit) {
            octree = hit.getOctree();
            if (octree == null) {
                octree = Octree.build(hit);
                hit.setOctree(octree);
            }
        }
        float intersection = getTriangleIntersectionForOctree(octree, nearHit, direction);
        if (intersection != -1) {
            float[] intersectionPoint = Math3DUtils.add(nearHit, Math3DUtils.multiply(direction, intersection));
            Log.d("CollisionDetection", "Interaction point: " + Arrays.toString(intersectionPoint));
            return intersectionPoint;
        } else {
            return null;
        }
    }

    /**********************************************************************************************/
    private static float getTriangleIntersectionForOctree(Octree octree, float[] rayOrigin, float[] rayDirection) {
        if (!isBoxIntersection(rayOrigin, rayDirection, octree.boundingBox)) {
            Log.d("CollisionDetection", "No octree intersection");
            return -1;
        }
        Octree selected = null;
        float min = Float.MAX_VALUE;
        for (Octree child : octree.getChildren()) {
            if (child == null) {
                continue;
            }
            float intersection = getTriangleIntersectionForOctree(child, rayOrigin, rayDirection);
            if (intersection != -1 && intersection < min) {
                Log.d("CollisionDetection", "Octree intersection: " + intersection);
                min = intersection;
                selected = child;
            }
        }
        float[] selectedTriangle = null;
        for (float[] triangle : octree.getTriangles()) {
            float[] vertex0 = new float[]{triangle[0], triangle[1], triangle[2]};
            float[] vertex1 = new float[]{triangle[4], triangle[5], triangle[6]};
            float[] vertex2 = new float[]{triangle[8], triangle[9], triangle[10]};
            float intersection = getTriangleIntersection(rayOrigin, rayDirection, vertex0, vertex1, vertex2);
            if (intersection != -1 && intersection < min) {
                min = intersection;
                selectedTriangle = triangle;
                selected = octree;

            }
        }
        if (min != Float.MAX_VALUE) {
            Log.d("CollisionDetection", "Intersection at distance: " + min);
            Log.d("CollisionDetection", "Intersection at triangle: " + Arrays.toString(selectedTriangle));
            Log.d("CollisionDetection", "Intersection at octree: " + selected);
            return min;
        }
        return -1;
    }

    /**********************************************************************************************/
    private static float getTriangleIntersection(float[] rayOrigin,
                                                 float[] rayVector,
                                                 float[] vertex0, float[] vertex1, float[] vertex2) {
        float EPSILON = 0.0000001f;
        float[] edge1, edge2, h, s, q;
        float a, f, u, v;
        edge1 = Math3DUtils.substract(vertex1, vertex0);
        edge2 = Math3DUtils.substract(vertex2, vertex0);
        h = Math3DUtils.crossProduct(rayVector, edge2);
        a = Math3DUtils.dotProduct(edge1, h);
        if (a > -EPSILON && a < EPSILON)
            return -1;
        f = 1 / a;
        s = Math3DUtils.substract(rayOrigin, vertex0);
        u = f * Math3DUtils.dotProduct(s, h);
        if (u < 0.0 || u > 1.0)
            return -1;
        q = Math3DUtils.crossProduct(s, edge1);
        v = f * Math3DUtils.dotProduct(rayVector, q);
        if (v < 0.0 || u + v > 1.0)
            return -1;
        float t = f * Math3DUtils.dotProduct(edge2, q);
        if (t > EPSILON) {
            Log.d("CollisionDetection", "Triangle intersection at: " + t);
            return t;
        } else
            return -1;
    }
}

