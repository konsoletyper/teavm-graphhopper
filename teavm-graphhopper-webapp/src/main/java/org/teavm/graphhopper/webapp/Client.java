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

import java.io.IOException;
import java.io.InputStream;
import org.teavm.dom.ajax.ReadyStateChangeHandler;
import org.teavm.dom.ajax.XMLHttpRequest;
import org.teavm.dom.browser.Window;
import org.teavm.dom.core.Node;
import org.teavm.dom.css.ElementCSSInlineStyle;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLElement;
import org.teavm.graphhopper.util.IndexedDBFile;
import org.teavm.javascript.spi.Async;
import org.teavm.jso.JS;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class Client {
    private Window window = (Window)JS.getGlobal();
    private HTMLDocument document = window.getDocument();
    private HTMLElement startElement = document.getElementById("start-panel");
    private HTMLElement startLatElement = document.getElementById("start-lat");
    private HTMLElement startLonElement = document.getElementById("start-lon");
    private HTMLElement endElement = document.getElementById("end-panel");
    private HTMLElement endLatElement = document.getElementById("end-lat");
    private HTMLElement endLonElement = document.getElementById("end-lon");
    private HTMLElement distanceElement = document.getElementById("distance-panel");
    private HTMLElement distanceValueElement = document.getElementById("distance-value");
    private GraphHopperUI ui;

    public static void main(String[] args) {
        new Client().start();
    }

    public void start() {
        ui = new GraphHopperUI(document.getElementById("map"));
        installControlPanel();
        try (InputStream input = openFile()) {
            ui.load(input);
        } catch (IOException e) {
            throw new RuntimeException("Error loading data", e);
        }
    }

    private void installControlPanel() {
        hideElement(startElement);
        hideElement(endElement);
        hideElement(distanceElement);
        ui.addListener(new GraphHopperUIListener() {
            @Override public void startChanged() {
                updateControlPanel();
            }
            @Override public void endChanged() {
                updateControlPanel();
            }
        });
    }

    private void updateControlPanel() {
        if (ui.getStart() != null) {
            showElement(startElement);
            removeTextContent(startLatElement);
            startLatElement.appendChild(document.createTextNode(String.valueOf(ui.getStart().getLat())));
            removeTextContent(startLonElement);
            startLonElement.appendChild(document.createTextNode(String.valueOf(ui.getStart().getLng())));
        } else {
            hideElement(startElement);
        }

        if (ui.getEnd() != null) {
            showElement(endElement);
            removeTextContent(endLatElement);
            endLatElement.appendChild(document.createTextNode(String.valueOf(ui.getEnd().getLat())));
            removeTextContent(endLonElement);
            endLonElement.appendChild(document.createTextNode(String.valueOf(ui.getEnd().getLng())));
        } else {
            hideElement(endElement);
        }

        if (ui.getPath() != null) {
            showElement(distanceElement);
            removeTextContent(distanceValueElement);
            distanceValueElement.appendChild(document.createTextNode(String.valueOf(
                    ui.getPath().getDistance() / 1000)));
        } else {
            hideElement(distanceElement);
        }
    }

    private void removeTextContent(Node node) {
        node = node.getFirstChild();
        while (node != null) {
            Node next = node.getNextSibling();
            if (node.getNodeType() == Node.TEXT_NODE || node.getNodeType() == Node.ENTITY_REFERENCE_NODE ||
                    node.getNodeType() == Node.CDATA_SECTION_NODE) {
                node.getParentNode().removeChild(node);
            }
            node = next;
        }
    }

    private void hideElement(ElementCSSInlineStyle element) {
        element.getStyle().setProperty("display", "none");
    }

    private void showElement(ElementCSSInlineStyle element) {
        element.getStyle().removeProperty("display");
    }

    private InputStream openFile() throws IOException {
        @SuppressWarnings("resource")
        IndexedDBFile file = new IndexedDBFile("moscow");
        if (!file.exists()) {
            file.write(loadData());
        }
        return file.read();
    }

    @Async
    private native byte[] loadData();

    private void loadData(final AsyncCallback<byte[]> callback) {
        final XMLHttpRequest xhr = window.createXMLHttpRequest();
        xhr.overrideMimeType("text/plain; charset=x-user-defined");
        xhr.setOnReadyStateChange(new ReadyStateChangeHandler() {
            @Override
            public void stateChanged() {
                if (xhr.getReadyState() != XMLHttpRequest.DONE) {
                    return;
                }
                if (xhr.getStatus() != 200) {
                    callback.error(new IOException("Status received from server: " + xhr.getStatus() + " " +
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
