package org.teavm.graphhopper.hub;

/**
 *
 * @author Alexey Andreev
 */
public interface GraphHopperHubListener {
    void offlineStatusChanged();

    void mapDeleted(String mapId);

    void mapStatusChanged(String mapId);

    void mapDownlodError(String mapId);

    void updated();
}
