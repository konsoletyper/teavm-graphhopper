package org.teavm.graphhopper.mapsforge;

import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.model.Dimension;
import org.teavm.dom.canvas.CanvasRenderingContext2D;

/**
 *
 * @author Alexey Andreev
 */
public class HTML5Canvas implements Canvas {
    private CanvasRenderingContext2D context;

    public HTML5Canvas(CanvasRenderingContext2D context) {
        this.context = context;
    }

    @Override
    public void drawBitmap(Bitmap bitmap, int left, int top) {
        context.drawImage(null, left, top);
    }

    @Override
    public void drawBitmap(Bitmap bitmap, Matrix matrix) {
    }

    @Override
    public void drawCircle(int x, int y, int radius, Paint paint) {
    }

    @Override
    public void drawLine(int x1, int y1, int x2, int y2, Paint paint) {
    }

    @Override
    public void drawPath(Path path, Paint paint) {
    }

    @Override
    public void drawText(String text, int x, int y, Paint paint) {
    }

    @Override
    public void drawTextRotated(String text, int x1, int y1, int x2, int y2, Paint paint) {
    }

    @Override
    public void fillColor(Color color) {
    }

    @Override
    public void fillColor(int color) {
    }

    @Override
    public void resetClip() {
    }

    @Override
    public void setClip(int left, int top, int width, int height) {
    }

    @Override
    public void setClipDifference(int left, int top, int width, int height) {
    }

    @Override
    public void destroy() {
    }

    @Override
    public Dimension getDimension() {
        return null;
    }

    @Override
    public int getHeight() {
        return 0;
    }

    @Override
    public int getWidth() {
        return 0;
    }

    @Override
    public void setBitmap(Bitmap bitmap) {
    }
}
