package org.teavm.graphhopper.webapp;

import com.graphhopper.routing.Path;
import com.graphhopper.util.Instruction;
import com.graphhopper.util.InstructionList;
import com.graphhopper.util.PointList;
import com.graphhopper.util.shapes.BBox;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import org.teavm.dom.browser.Window;
import org.teavm.dom.html.HTMLDocument;
import org.teavm.dom.html.HTMLElement;
import org.teavm.graphhopper.ClientSideGraphHopper;
import org.teavm.graphhopper.storage.InMemoryDirectory;
import org.teavm.graphhopper.webapp.leaflet.LatLng;
import org.teavm.graphhopper.webapp.leaflet.LatLngBounds;
import org.teavm.graphhopper.webapp.leaflet.LeafletMap;
import org.teavm.graphhopper.webapp.leaflet.LeafletMapOptions;
import org.teavm.graphhopper.webapp.leaflet.LeafletMouseEvent;
import org.teavm.graphhopper.webapp.leaflet.Marker;
import org.teavm.graphhopper.webapp.leaflet.Polyline;
import org.teavm.graphhopper.webapp.leaflet.TileLayer;
import org.teavm.graphhopper.webapp.leaflet.TileLayerOptions;
import org.teavm.jso.JS;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperUI {
    private static Window window = (Window)JS.getGlobal();
    private static HTMLDocument document = window.getDocument();
    private HTMLElement element;
    private LeafletMap map;
    private ClientSideGraphHopper graphHopper;
    private Marker firstMarker;
    private Marker secondMarker;
    private Polyline pathDisplay;
    private Path path;
    private List<GraphHopperUIListener> listeners = new ArrayList<>();

    public GraphHopperUI() {
        this(document.createElement("div"));
        element.setAttribute("style", "width: 800px; height: 480px");
    }

    public GraphHopperUI(String elementId) {
        this(document.getElementById(elementId));
    }

    public GraphHopperUI(HTMLElement element) {
        this.element = element;
        map = LeafletMap.create(element, LeafletMapOptions.create());
        TileLayer.create("http://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png", TileLayerOptions.create()
                .attribution("&copy; <a href=\"http://www.openstreetmap.org/copyright\">OpenStreetMap</a> " +
                        "contributors"))
                .addTo(map);
        map.onClick((LeafletMouseEvent event) -> click(event.getLatlng()));
    }

    public void load(InputStream input) throws IOException {
        InMemoryDirectory directory = new InMemoryDirectory();
        directory.load(new DataInputStream(input));
        graphHopper  = new ClientSideGraphHopper(directory);
        BBox bounds = graphHopper.getBounds();
        LatLng southWest = LatLng.create(bounds.minLat, bounds.minLon);
        LatLng northEast = LatLng.create(bounds.maxLat, bounds.maxLon);
        LatLngBounds leafletBounds = LatLngBounds.create(southWest, northEast);
        map.setMaxBounds(leafletBounds);
        map.setView(leafletBounds.getCenter(), 10);
    }

    public HTMLElement getElement() {
        return element;
    }

    public LatLng getStart() {
        return firstMarker != null ? firstMarker.getLatLng() : null;
    }

    public LatLng getEnd() {
        return secondMarker != null ? secondMarker.getLatLng() : null;
    }

    public Path getPath() {
        return path;
    }

    private void click(LatLng latlng) {
        if (secondMarker != null) {
            map.removeLayer(firstMarker);
            map.removeLayer(secondMarker);
            if (pathDisplay != null) {
                map.removeLayer(pathDisplay);
            }
            path = null;
            secondMarker = null;
            notifyEndChanged();
            firstMarker = Marker.create(latlng).addTo(map);
            notifyStartChanged();
            pathDisplay = null;
        } else if (firstMarker == null) {
            firstMarker = Marker.create(latlng).addTo(map);
            notifyStartChanged();
        } else {
            secondMarker = Marker.create(latlng).addTo(map);
            LatLng first = firstMarker.getLatLng();
            LatLng second = secondMarker.getLatLng();
            int firstNode = graphHopper.findNode(first.getLat(), first.getLng());
            int secondNode = graphHopper.findNode(second.getLat(), second.getLng());
            if (firstNode < 0 || secondNode < 0) {
                pathDisplay = null;
                window.alert("One of the provided points is outside of the known region");
                return;
            }
            path = graphHopper.route(firstNode, secondNode);
            notifyEndChanged();
            InstructionList instructions = path.calcInstructions(new TeaVMTranslation());
            List<LatLng> array = new ArrayList<>();
            for (Instruction insn : instructions) {
                PointList points = insn.getPoints();
                for (int i = 0; i < points.size(); ++i) {
                    array.add(LatLng.create(points.getLat(i), points.getLon(i)));
                }
            }
            pathDisplay = Polyline.create(array).addTo(map);
        }
    }

    public void addListener(GraphHopperUIListener listener) {
        if (!listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removeListener(GraphHopperUIListener listener) {
        listeners.remove(listener);
    }

    private void notifyStartChanged() {
        for (GraphHopperUIListener listener : listeners) {
            listener.startChanged();
        }
    }

    private void notifyEndChanged() {
        for (GraphHopperUIListener listener : listeners) {
            listener.endChanged();
        }
    }
}
