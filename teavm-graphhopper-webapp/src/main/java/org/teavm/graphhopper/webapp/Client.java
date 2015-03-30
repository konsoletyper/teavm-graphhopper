/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.graphhopper.webapp;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import com.graphhopper.routing.Path;
import org.teavm.dom.ajax.ReadyStateChangeHandler;
import org.teavm.dom.ajax.XMLHttpRequest;
import org.teavm.dom.browser.Window;
import org.teavm.graphhopper.ClientSideGraphHopper;
import org.teavm.javascript.spi.Async;
import org.teavm.jso.JS;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class Client {
    private static Window window = (Window)JS.getGlobal();
    private ClientSideGraphHopper graphHopper = new ClientSideGraphHopper();

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
        init();

        double startLat = 55.747844;
        double startLon = 37.418703;
        double endLat = 55.784829;
        double endLon = 37.70859;

        int start = graphHopper.findNode(startLat, startLon);
        int end = graphHopper.findNode(endLat, endLon);

        Path path = graphHopper.route(start, end);
        System.out.println(path.getDistance());
    }

    private void init() {
        byte[] data = loadData();
        try (ByteArrayInputStream input = new ByteArrayInputStream(data)) {
            graphHopper.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading data", e);
        }
    }

    @Async
    private static native byte[] loadData();

    private static void loadData(final AsyncCallback<byte[]> callback) {
        final XMLHttpRequest xhr = window.createXMLHttpRequest();
        xhr.overrideMimeType("text/plain; charset=x-user-defined");
        xhr.setOnReadyStateChange(new ReadyStateChangeHandler() {
            @Override
            public void stateChanged() {
                if (xhr.getReadyState() != XMLHttpRequest.DONE) {
                    return;
                }
                if (xhr.getStatus() != 200) {
                    callback.error(new RuntimeException("Status received from server: " + xhr.getStatus() + " " +
                            xhr.getStatusText()));
                    return;
                }
                String responseText = xhr.getResponseText();
                byte[] result = new byte[responseText.length()];
                for (int i = 0; i < result.length; ++i) {
                    result[i] = (byte)responseText.charAt(i);
                }
                callback.complete(result);
            }
        });
        xhr.open("get", "moscow-russia.gh");
        xhr.send();
    }
}
