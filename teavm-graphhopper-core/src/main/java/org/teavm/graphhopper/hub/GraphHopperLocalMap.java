package org.teavm.graphhopper.hub;

import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class GraphHopperLocalMap implements JSObject {
    @JSProperty
    public abstract String getId();

    @JSProperty
    public abstract String getName();

    @JSProperty
    public abstract int getSizeInBytes();

    @JSProperty
    public abstract String getLastModified();
}
