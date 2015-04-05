package org.teavm.graphhopper.hub;

import java.io.IOException;
import java.io.InputStream;

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

    public String getName() {
        EventLoop.requireEventLoop();
        return localMap != null ? localMap.getName() : remoteMap.getName();
    }

    public int getSizeInBytes() {
        EventLoop.requireEventLoop();
        return remoteMap != null ? remoteMap.getSizeInBytes() : localMap.getSizeInBytes();
    }

    public boolean isLocal() {
        EventLoop.requireEventLoop();
        return localMap != null;
    }

    public boolean isRemote() {
        EventLoop.requireEventLoop();
        return remoteMap != null;
    }

    public boolean isDownloading() {
        EventLoop.requireEventLoop();
        return hubController.hub.hasMap(id) && hubController.hub.isUploading(id);
    }

    public int getBytesDownloaded() {
        EventLoop.requireEventLoop();
        return bytesDownloaded;
    }

    public void download() {
        EventLoop.requireEventLoop();
        final GraphHopperRemoteMap remote;
        if (isLocal() || isDownloading()) {
            throw new IllegalStateException("Map " + id + " is already downloaded");
        }
        if (!isRemote()) {
            throw new IllegalStateException("Map " + id + " does not exist remotely");
        }
        remote = remoteMap;
        bytesDownloaded = 0;
        hubController.hub.upload(remote, remote.open(), new GraphHopperMapUploadListener() {
            @Override public void progress(int bytes) {
                bytesDownloaded = bytes;
                for (GraphHopperHubListener listener : hubController.listeners) {
                    listener.mapStatusChanged(id);
                }
            }
            @Override public void failed(Exception e) {
                for (GraphHopperHubListener listener : hubController.listeners) {
                    listener.mapDownlodError(id);
                }
                for (GraphHopperHubListener listener : hubController.listeners) {
                    listener.mapStatusChanged(id);
                }
            }
            @Override public void complete() {
                localMap = hubController.hub.getMap(id);
                for (GraphHopperHubListener listener : hubController.listeners) {
                    listener.mapStatusChanged(id);
                }
            }
        });
    }

    public InputStream open() throws IOException {
        EventLoop.requireEventLoop();
        if (localMap == null) {
            throw new IllegalStateException("Map " + id + " does not exist locally");
        }
        return hubController.hub.download(id);
    }
}
