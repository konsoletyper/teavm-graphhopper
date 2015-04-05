package org.teavm.graphhopper.util;

import java.io.IOException;
import org.teavm.dom.ajax.XMLHttpRequest;
import org.teavm.dom.browser.Window;
import org.teavm.javascript.spi.Async;
import org.teavm.jso.JS;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class Ajax {
    private static final Window window = (Window)JS.getGlobal();

    @Async
    public static native String get(String url) throws IOException;

    private static void get(final String url, final AsyncCallback<String> callback) {
        final XMLHttpRequest xhr = window.createXMLHttpRequest();
        xhr.overrideMimeType("text/plain; charset=x-user-defined");
        xhr.setOnReadyStateChange(() -> {
            if (xhr.getReadyState() != XMLHttpRequest.DONE) {
                return;
            }
            if (xhr.getStatus() != 200) {
                callback.error(new IOException("Error loading remote resource " + url + ". Status: " +
                        xhr.getStatus() + " " + xhr.getStatusText()));
                return;
            }
            callback.complete(xhr.getResponseText());
        });
        xhr.open("get", url);
        xhr.send();
    }

    @Async
    public static native byte[] getBinary(String url) throws IOException;

    private static void getBinary(final String url, final AsyncCallback<byte[]> callback) {
        final XMLHttpRequest xhr = window.createXMLHttpRequest();
        xhr.overrideMimeType("text/plain; charset=x-user-defined");
        xhr.setOnReadyStateChange(() -> {
            if (xhr.getReadyState() != XMLHttpRequest.DONE) {
                return;
            }
            if (xhr.getStatus() != 200) {
                callback.error(new IOException("Error loading remote resource " + url + ". Status: " +
                        xhr.getStatus() + " " + xhr.getStatusText()));
                return;
            }
            String responseText = xhr.getResponseText();
            byte[] result = new byte[responseText.length()];
            for (int i = 0; i < result.length; ++i) {
                result[i] = (byte)responseText.charAt(i);
            }
            callback.complete(result);
        });
        xhr.open("get", url);
        xhr.send();
    }
}
