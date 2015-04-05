package org.teavm.graphhopper.hub;

import java.io.IOException;
import org.teavm.graphhopper.util.Ajax;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class GraphHopperRemoteMap extends GraphHopperLocalMap implements JSObject {
    @Override
    @JSProperty
    public abstract String getId();

    @Override
    @JSProperty
    public abstract String getName();

    @Override
    @JSProperty
    public abstract int getSizeInBytes();

    @Override
    @JSProperty
    public abstract String getLastModified();

    @JSProperty
    public abstract String getBaseUrl();

    @JSProperty
    public abstract int getChunkCount();

    public final GraphHopperMapReader open() {
        return new GraphHopperMapReader() {
            private int index = 0;
            @Override
            public byte[] next() throws IOException {
                if (index == getChunkCount()) {
                    return null;
                }
                return Ajax.getBinary(getBaseUrl() + "/chunk" + index++ + ".bin");
            }
        };
    }
}
