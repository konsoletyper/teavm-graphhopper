package org.teavm.graphhopper.webapp.leaflet;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public interface LeafletEvent extends JSObject {
    @JSProperty
    LatLng getLatlng();
}
