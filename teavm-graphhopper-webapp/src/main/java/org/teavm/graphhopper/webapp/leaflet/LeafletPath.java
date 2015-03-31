package org.teavm.graphhopper.webapp.leaflet;

/**
 *
 * @author Alexey Andreev
 */
public interface LeafletPath extends Layer {
    LeafletPath addTo(LeafletMap map);
}
