package org.teavm.graphhopper.hub;

import static org.teavm.graphhopper.hub.EventLoop.requireEventLoop;
import static org.teavm.graphhopper.hub.EventLoop.submit;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.teavm.jso.JS;


/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperHubController {
    private String remoteUrl;
    GraphHopperLocalHub hub;
    private GraphHopperRemoteHub remoteHub;
    List<GraphHopperHubListener> listeners = new ArrayList<>();
    private Map<String, GraphHopperMapController> mapControllers = new HashMap<>();
    private boolean remoteConnection;
    private Thread thread;
    private final Object threadMonitor = new Object();
    private boolean initialized;
    private boolean pendingChanges;

    public GraphHopperHubController(String remoteUrl) {
        this(remoteUrl, "graphhopper");
    }

    public GraphHopperHubController(String remoteUrl, String localName) {
        this.remoteUrl = remoteUrl;
        hub = new GraphHopperLocalHub(localName);
        new Thread(() -> {
            fetchOnce();
            submit(() -> {
                pendingChanges = false;
                initialized = true;
                for (GraphHopperHubListener listener : listeners) {
                    listener.initialized();
                }
                refresh();
            });
        }).start();
    }

    public void setRemoteConnection(boolean connection) {
        requireEventLoop();
        if (remoteConnection == connection) {
            return;
        }
        remoteConnection = connection;
        if (connection) {
            thread = new Thread(this::fetchLoop, "hub-fetch");
            thread.start();
        } else {
            thread = null;
            threadMonitor.notifyAll();
        }
    }

    public boolean isInitialized() {
        requireEventLoop();
        return initialized;
    }

    public boolean isPendingChanges() {
        requireEventLoop();
        return pendingChanges;
    }

    public void refresh() {
        requireEventLoop();
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

    public GraphHopperMapController getMapController(String id) {
        requireEventLoop();
        return mapControllers.get(id);
    }

    public GraphHopperMapController[] getMapControllers() {
        requireEventLoop();
        return mapControllers.values().toArray(new GraphHopperMapController[0]);
    }

    public boolean isOffline() {
        requireEventLoop();
        return remoteHub == null;
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

        GraphHopperRemoteHub newHub = hub;
        submit(() -> putRemoteHub(newHub));
    }

    private void putRemoteHub(GraphHopperRemoteHub newHub) {
        boolean oldOffline = remoteHub == null;
        remoteHub = newHub;
        if (oldOffline != isOffline()) {
            for (GraphHopperHubListener listener : listeners) {
                listener.offlineStatusChanged();
            }
        }
        if (remoteHub == null) {
            return;
        }

        boolean oldPendingChanges = pendingChanges;
        Set<String> newRemoteIds = new HashSet<>();
        for (GraphHopperRemoteMap remoteMap : JS.iterate(remoteHub.getMaps())) {
            GraphHopperMapController mapController = mapControllers.get(remoteMap.getId());
            if (mapController == null) {
                pendingChanges = true;
                break;
            }
            if (mapController.remoteMap.getLastModified().compareTo(remoteMap.getLastModified()) < 0) {
                pendingChanges = true;
                break;
            }
            newRemoteIds.add(remoteMap.getId());
        }
        if (!pendingChanges) {
            for (GraphHopperMapController mapController : mapControllers.values()) {
                if (mapController.remoteMap != null && !newRemoteIds.contains(mapController.getId())) {
                    pendingChanges = true;
                    break;
                }
            }
        }
        if (oldPendingChanges != pendingChanges) {
            for (GraphHopperHubListener listener : listeners) {
                listener.pendingStatusChanged();
            }
        }
    }

    public void addListener(GraphHopperHubListener listener) {
        requireEventLoop();
        listeners.add(listener);
    }
}
