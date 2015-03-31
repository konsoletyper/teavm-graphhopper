package org.teavm.graphhopper.webapp.leaflet;

import org.teavm.jso.JSFunctor;

/**
 *
 * @author Alexey Andreev
 */
@JSFunctor
public interface LeafletEventListener<T extends LeafletEvent> {
    void occur(T event);
}
