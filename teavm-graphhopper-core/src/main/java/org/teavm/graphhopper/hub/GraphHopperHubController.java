package org.teavm.graphhopper.hub;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.teavm.jso.JS;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperHubController {
    private String remoteUrl;
    GraphHopperLocalHub hub;
    private GraphHopperRemoteHub remoteHub;
    private final Object remoteHubMonitor = new Object();
    List<GraphHopperHubListener> listeners = new ArrayList<>();
    private Map<String, GraphHopperMapController> mapControllers = new HashMap<>();
    private boolean remoteConnection;
    private Thread thread;
    private Object threadMonitor = new Object();

    public GraphHopperHubController(String remoteUrl) {
        this(remoteUrl, "graphhopper");
    }

    public GraphHopperHubController(String remoteUrl, String localName) {
        this.remoteUrl = remoteUrl;
        hub = new GraphHopperLocalHub(localName);
        fetchOnce();
        refresh();
    }

    public void setRemoteConnection(boolean connection) {
        if (remoteConnection == connection) {
            return;
        }
        remoteConnection = connection;
        if (connection) {
            synchronized (threadMonitor) {
                thread = new Thread() {
                    @Override
                    public void run() {
                        fetchLoop();
                    }
                };
                thread.start();
            }
        } else {
            synchronized (threadMonitor) {
                thread = null;
                threadMonitor.notifyAll();
            }
        }
    }

    public void refresh() {
        synchronized (mapControllers) {
            mapControllers.clear();
            for (GraphHopperLocalMap map : hub.getMaps()) {
                GraphHopperMapController mapController = new GraphHopperMapController(map.getId(), this);
                mapControllers.put(map.getId(), mapController);
                mapController.localMap = map;
            }
            if (remoteHub != null) {
                for (GraphHopperRemoteMap map : JS.iterate(remoteHub.getMaps())) {
                    GraphHopperMapController mapController = mapControllers.get(map.getId());
                    if (mapController == null) {
                        mapController = new GraphHopperMapController(map.getId(), this);
                        mapController.remoteMap = map;
                    }
                }
            }
            for (GraphHopperHubListener listener : listeners) {
                listener.updated();
            }
        }
    }

    public GraphHopperMapController getMapController(String id) {
        synchronized (mapControllers) {
            return mapControllers.get(id);
        }
    }

    public GraphHopperMapController[] getMapControllers() {
        synchronized (mapControllers) {
            return mapControllers.values().toArray(new GraphHopperMapController[0]);
        }
    }

    public boolean isOffline() {
        synchronized (remoteHubMonitor) {
            return remoteHub == null;
        }
    }

    private void fetchLoop() {
        while (true) {
            fetchOnce();
            synchronized (threadMonitor) {
                if (thread == null) {
                    break;
                }
                try {
                    threadMonitor.wait(30 * 60000);
                } catch (InterruptedException e) {
                    break;
                }
            }
        }
    }

    private void fetchOnce() {
        GraphHopperRemoteHub hub;
        try {
            hub = GraphHopperRemoteHub.fetch(remoteUrl);
        } catch (IOException e) {
            hub = null;
        }
        synchronized (remoteHubMonitor) {
            remoteHub = hub;
            boolean oldOffline = remoteHub == null;
            if (oldOffline != isOffline()) {
                for (GraphHopperHubListener listener : listeners) {
                    listener.offlineStatusChanged();
                }
            }
        }
    }
}
