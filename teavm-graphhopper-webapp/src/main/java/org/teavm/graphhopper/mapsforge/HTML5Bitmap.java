package org.teavm.graphhopper.mapsforge;

import java.io.IOException;
import java.io.OutputStream;
import org.mapsforge.core.graphics.Bitmap;
import org.teavm.dom.browser.Window;
import org.teavm.dom.canvas.CanvasImageSource;
import org.teavm.dom.canvas.CanvasRenderingContext2D;
import org.teavm.dom.html.HTMLCanvasElement;
import org.teavm.dom.html.HTMLElement;
import org.teavm.dom.html.HTMLImageElement;
import org.teavm.jso.JS;

/**
 *
 * @author Alexey Andreev
 */
public class HTML5Bitmap implements Bitmap {
    private static Window window = (Window)JS.getGlobal();
    public static final int TYPE_IMAGE = 0;
    public static final int TYPE_CANVAS = 1;
    private int refCount = 1;
    CanvasImageSource imageElem;
    int type;

    HTML5Bitmap(CanvasImageSource imageElem, int type) {
        this.imageElem = imageElem;
        this.type = type;
    }

    @Override
    public void compress(OutputStream outputStream) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void decrementRefCount() {
        if (--refCount == 0) {
            removeElement();
        }
    }

    private void removeElement() {
        HTMLElement elem = (HTMLElement)imageElem;
        elem.getParentNode().removeChild(elem);
    }

    @Override
    public int getHeight() {
        switch (type) {
            case TYPE_CANVAS:
                return ((HTMLCanvasElement)imageElem).getHeight();
            case TYPE_IMAGE:
                return ((HTMLImageElement)imageElem).getHeight();
            default:
                throw new AssertionError("Unexpected type " + type);
        }
    }

    @Override
    public int getWidth() {
        switch (type) {
            case TYPE_CANVAS:
                return ((HTMLCanvasElement)imageElem).getWidth();
            case TYPE_IMAGE:
                return ((HTMLImageElement)imageElem).getWidth();
            default:
                throw new AssertionError("Unexpected type " + type);
        }
    }

    @Override
    public void incrementRefCount() {
        ++refCount;
    }

    @Override
    public void scaleTo(int width, int height) {
        HTMLCanvasElement tmpCanvas = (HTMLCanvasElement)window.getDocument().createElement("canvas");
        tmpCanvas.setWidth(width);
        tmpCanvas.setHeight(height);
        tmpCanvas.getStyle().setProperty("width", "1px");
        tmpCanvas.getStyle().setProperty("height", "1px");
        window.getDocument().getBody().appendChild(tmpCanvas);
        CanvasRenderingContext2D tmpContext = (CanvasRenderingContext2D)tmpCanvas.getContext("2d");
        tmpContext.drawImage(imageElem, 0, 0, width, height);
        removeElement();
        imageElem = tmpCanvas;
        type = TYPE_CANVAS;
    }

    @Override
    public void setBackgroundColor(int color) {
    }
}
