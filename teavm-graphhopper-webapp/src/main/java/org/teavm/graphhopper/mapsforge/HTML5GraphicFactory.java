package org.teavm.graphhopper.mapsforge;

import java.io.IOException;
import java.io.InputStream;
import org.mapsforge.core.graphics.Bitmap;
import org.mapsforge.core.graphics.Canvas;
import org.mapsforge.core.graphics.Color;
import org.mapsforge.core.graphics.Display;
import org.mapsforge.core.graphics.GraphicFactory;
import org.mapsforge.core.graphics.Matrix;
import org.mapsforge.core.graphics.Paint;
import org.mapsforge.core.graphics.Path;
import org.mapsforge.core.graphics.Position;
import org.mapsforge.core.graphics.ResourceBitmap;
import org.mapsforge.core.graphics.TileBitmap;
import org.mapsforge.core.mapelements.PointTextContainer;
import org.mapsforge.core.mapelements.SymbolContainer;
import org.mapsforge.core.model.Point;
import org.teavm.dom.browser.Window;
import org.teavm.dom.canvas.CanvasRenderingContext2D;
import org.teavm.dom.html.HTMLCanvasElement;
import org.teavm.jso.JS;

/**
 *
 * @author Alexey Andreev
 */
public class HTML5GraphicFactory implements GraphicFactory {
    private static Window window = (Window)JS.getGlobal();
    CanvasRenderingContext2D context;

    public HTML5GraphicFactory(CanvasRenderingContext2D context) {
        this.context = context;
    }

    @Override
    public Bitmap createBitmap(int width, int height) {
        HTMLCanvasElement elem = (HTMLCanvasElement)window.getDocument().createElement("canvas");
        return new HTML5Bitmap(elem, HTML5Bitmap.TYPE_CANVAS);
    }

    @Override
    public Bitmap createBitmap(int width, int height, boolean isTransparent) {
        return createBitmap(width, height);
    }

    @Override
    public Canvas createCanvas() {
        return null;
    }

    @Override
    public int createColor(Color color) {
        switch (color) {
            case BLACK:
                return 0x000000FF;
            case BLUE:
                return 0x0000FFFF;
            case GREEN:
                return 0x00FF00FF;
            case RED:
                return 0xFF0000FF;
            case TRANSPARENT:
                return 0x00000000;
            case WHITE:
                return 0xFFFFFFFF;
            default:
                return 0;
        }
    }

    @Override
    public int createColor(int alpha, int red, int green, int blue) {
        return ((red & 0xFF) << 24) | ((green & 0xFF) << 16) | ((blue & 0xFF) << 8) | (alpha & 0xFF);
    }

    @Override
    public Matrix createMatrix() {
        return null;
    }

    @Override
    public Paint createPaint() {
        return null;
    }

    @Override
    public Path createPath() {
        return null;
    }

    @Override
    public PointTextContainer createPointTextContainer(Point xy, Display display, int priority, String text,
            Paint paintFront, Paint paintBack, SymbolContainer symbolContainer, Position position, int maxTextWidth) {
        return null;
    }

    @Override
    public ResourceBitmap createResourceBitmap(InputStream inputStream, int hash) throws IOException {
        return null;
    }

    @Override
    public TileBitmap createTileBitmap(InputStream inputStream, int tileSize, boolean isTransparent) throws IOException {
        return null;
    }

    @Override
    public TileBitmap createTileBitmap(int tileSize, boolean isTransparent) {
        return null;
    }

    @Override
    public InputStream platformSpecificSources(String relativePathPrefix, String src) throws IOException {
        return null;
    }

    @Override
    public ResourceBitmap renderSvg(InputStream inputStream, float scaleFactor, int width, int height, int percent,
            int hash) throws IOException {
        return null;
    }
}
