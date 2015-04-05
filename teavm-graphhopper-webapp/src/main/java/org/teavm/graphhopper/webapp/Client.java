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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.teavm.dom.browser.Window;
import org.teavm.dom.core.Node;
import org.teavm.dom.css.ElementCSSInlineStyle;
import org.teavm.dom.html.HTMLButtonElement;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLElement;
import org.teavm.graphhopper.hub.EventLoop;
import org.teavm.graphhopper.hub.GraphHopperHubController;
import org.teavm.graphhopper.hub.GraphHopperHubListener;
import org.teavm.graphhopper.hub.GraphHopperMapController;
import org.teavm.jso.JS;

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
    private HTMLElement hubView = document.getElementById("hub-view");
    private HTMLElement mapView = document.getElementById("map-view");
    private HTMLButtonElement refreshButton = (HTMLButtonElement)document.getElementById("refresh-button");
    private HTMLElement hubRows = document.getElementById("hub-rows");
    private HTMLElement initializingIndicator = document.getElementById("initializing-indicator");
    private GraphHopperUI ui;
    private GraphHopperHubController controller;
    private List<MapWrapper> maps = new ArrayList<>();

    public static void main(String[] args) {
        new Client().start(args.length > 0 ? args[0] : "maps");
    }

    public void start(String hubUrl) {
        ui = new GraphHopperUI(document.getElementById("map"));
        installHub(hubUrl);
        installControlPanel();
    }

    private void installHub(String url) {
        controller = new GraphHopperHubController(url);
        controller.addListener(new GraphHopperHubListener() {
            @Override public void updated() {
                updateRows();
            }
            @Override public void pendingStatusChanged() {
                refreshButton.setDisabled(!controller.isPendingChanges());
            }
            @Override public void offlineStatusChanged() {
                for (int i = 0; i < maps.size(); ++i) {
                    updateRow(i);
                }
            }
            @Override public void mapStatusChanged(String mapId) {
                for (int i = 0; i < maps.size(); ++i) {
                    if (maps.get(i).map.getId().equals(mapId)) {
                        updateRow(i);
                        break;
                    }
                }
            }
            @Override public void mapDownlodError(String mapId) {
                window.alert("Error downloading map " + mapId);
            }
            @Override public void mapDeleted(String mapId) {
            }
            @Override public void initialized() {
                hideElement(initializingIndicator);
            }
        });
        refreshButton.addEventListener("click", event -> EventLoop.submit(controller::refresh));
    }

    private void updateRows() {
        for (MapWrapper wrapper : maps) {
            wrapper.row.getParentNode().removeChild(wrapper.row);
        }
        maps.clear();
        for (GraphHopperMapController map : controller.getMapControllers()) {
            MapWrapper wrapper = new MapWrapper();
            wrapper.map = map;
            maps.add(wrapper);
        }
        Collections.sort(maps, (a, b) -> Integer.compare(a.index, b.index));
        for (int i = 0; i < maps.size(); ++i) {
            MapWrapper wrapper = maps.get(i);
            wrapper.index = i;
            wrapper.row = document.createElement("tr");
            hubRows.appendChild(wrapper.row);
            updateRow(i);
        }
    }

    private void updateRow(int index) {
        MapWrapper wrapper = maps.get(index);
        if (wrapper.name != null) {
            wrapper.row.removeChild(wrapper.name);
        }
        if (wrapper.status != null) {
            wrapper.row.removeChild(wrapper.status);
        }

        wrapper.name = document.createElement("td");
        wrapper.name.appendChild(document.createTextNode(wrapper.map.getName()));
        wrapper.row.appendChild(wrapper.name);

        wrapper.status = document.createElement("td");
        wrapper.row.appendChild(wrapper.status);

        if (wrapper.map.isDownloading()) {
            wrapper.status.appendChild(document.createTextNode("Downloading (" +
                    wrapper.map.getBytesDownloaded() + " of " + wrapper.map.getSizeInBytes()));
        } else if (wrapper.map.isRemote() && !wrapper.map.isLocal() && !controller.isOffline()) {
            HTMLButtonElement downloadButton = (HTMLButtonElement)document.createElement("button");
            downloadButton.addEventListener("click", e -> EventLoop.submit(wrapper.map::download));
            downloadButton.appendChild(document.createTextNode("Download"));
            wrapper.status.appendChild(downloadButton);
        } else if (wrapper.map.isLocal()) {
            HTMLButtonElement openButton = (HTMLButtonElement)document.createElement("button");
            openButton.addEventListener("click", e -> EventLoop.submit(() -> showMap(wrapper.map.getId())));
            openButton.appendChild(document.createTextNode("Open"));
            wrapper.status.appendChild(openButton);
        }
    }

    private void showMap(String id) {
        hideElement(hubView);
        showElement(mapView);
        GraphHopperMapController mapController = controller.getMapController(id);
        try (InputStream input = mapController.open()) {
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

    static class MapWrapper {
        GraphHopperMapController map;
        int index;
        HTMLElement row;
        HTMLElement name;
        HTMLElement status;
    }
}
