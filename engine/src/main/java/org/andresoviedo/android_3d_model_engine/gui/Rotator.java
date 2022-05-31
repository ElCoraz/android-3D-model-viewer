package org.andresoviedo.android_3d_model_engine.gui;

import android.util.Log;

import org.andresoviedo.android_3d_model_engine.model.Dimensions;
import org.andresoviedo.android_3d_model_engine.model.Object3DData;
import org.andresoviedo.util.io.IOUtils;

import java.nio.FloatBuffer;
import java.util.EventObject;
/**************************************************************************************************/
public class Rotator extends Widget {
    /**********************************************************************************************/
    private final Widget widget;
    /**********************************************************************************************/
    private final Dimensions widget_dimensions;

    /**********************************************************************************************/
    private Rotator(Widget widget) {
        this.widget = widget;
        this.widget_dimensions = widget.getCurrentDimensions();
        try {
            final FloatBuffer vertexBuffer = IOUtils.createFloatBuffer(10 * 3);
            final FloatBuffer colorBuffer = IOUtils.createFloatBuffer(10 * 4);

            build(vertexBuffer, colorBuffer, getColor(), widget_dimensions);

            setVertexBuffer(vertexBuffer);
            setColorsBuffer(colorBuffer);

            widget.addListener(this);
        } catch (Exception e) {
            Log.e("Glyph", e.getMessage(), e);
        }
    }

    /**********************************************************************************************/
    public static Rotator build(Widget widget) {
        Rotator rotator = new Rotator(widget);
        rotator.setParent(widget);
        return rotator;
    }

    /**********************************************************************************************/
    @Override
    public boolean onEvent(EventObject event) {
        if (event.getSource() != this.widget) return super.onEvent(event);

        if (event instanceof ChangeEvent) {
            Object3DData source = (Object3DData) event.getSource();
            if (this.widget_dimensions != source.getCurrentDimensions()) {
                Log.d("Rotator", "[" + getId() + "] this dim: " + widget_dimensions);
                Log.d("Rotator", "[" + source.getId() + "] dimensions: " + source.getCurrentDimensions());
            }
            this.setLocation(source.getLocation());
            this.setScale(source.getScale());
            this.setVisible(source.isVisible());
        }
        return true;
    }

    /**********************************************************************************************/
    private static void build(FloatBuffer vertexBuffer, FloatBuffer colorBuffer, float[] color, Dimensions dimensions) {

        float[] min = dimensions.getMin();
        float[] max = dimensions.getMax();
        float[] center = dimensions.getCenter();
        float centerY = center[1];
        float maxZ = max[2];

        vertexBuffer.position(0);
        colorBuffer.position(0);

        Glyph.build(vertexBuffer, colorBuffer, Glyph.GLYPH_LESS_THAN_CODE,
                color, min[0] - 1f, centerY - 0.3f, maxZ);
        Glyph.build(vertexBuffer, colorBuffer, Glyph.GLYPH_GREATER_THAN_CODE,
                color, max[0] + 0.5f, centerY - 0.3f, maxZ);
    }
}
