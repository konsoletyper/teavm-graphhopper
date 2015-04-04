package org.teavm.graphhopper.hub;

import java.io.IOException;
import org.teavm.platform.Platform;
import org.teavm.platform.PlatformRunnable;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperMapController {
    private GraphHopperHubController hubController;
    private String id;
    private int bytesDownloaded;
    GraphHopperLocalMap localMap;
    GraphHopperRemoteMap remoteMap;

    GraphHopperMapController(String id, GraphHopperHubController hubController) {
        this.id = id;
        this.hubController = hubController;
    }

    public GraphHopperHubController getHubController() {
        return hubController;
    }

    public String getId() {
        return id;
    }

    public boolean isLocal() {
        return localMap != null;
    }

    public boolean isRemote() {
        return remoteMap != null;
    }

    public boolean isDownloading() {
        synchronized (this) {
            return hubController.hub.hasMap(id) && hubController.hub.isUploading(id);
        }
    }

    public int getBytesDownloaded() {
        return bytesDownloaded;
    }

    public void download() {
        final GraphHopperRemoteMap remote;
        synchronized (this) {
            if (isLocal() || isDownloading()) {
                throw new IllegalStateException("Map " + id + " is already downloaded");
            }
            if (isRemote()) {
                throw new IllegalStateException("Map " + id + " does not exist remotely");
            }
            remote = remoteMap;
            bytesDownloaded = 0;
        }
        new Thread() {
            @Override
            public void run() {
                synchronized (GraphHopperMapController.this) {
                    localMap = remote;
                }
                try {
                    final GraphHopperMapReader reader = remote.open();
                    hubController.hub.upload(remote, new GraphHopperMapReader() {
                        @Override
                        public byte[] next() throws IOException {
                            byte[] result = reader.next();
                            bytesDownloaded += result.length;
                            for (GraphHopperHubListener listener : hubController.listeners) {
                                listener.mapStatusChanged(id);
                            }
                            return result;
                        }
                    });
                } catch (IOException e) {
                    Platform.postpone(new PlatformRunnable() {
                        @Override
                        public void run() {
                            for (GraphHopperHubListener listener : hubController.listeners) {
                                listener.mapDownlodError(id);
                            }
                        }
                    });
                }
                Platform.postpone(new PlatformRunnable() {
                    @Override public void run() {
                        for (GraphHopperHubListener listener : hubController.listeners) {
                            listener.mapStatusChanged(id);
                        }
                    }
                });
            }
        }.start();
    }
}
