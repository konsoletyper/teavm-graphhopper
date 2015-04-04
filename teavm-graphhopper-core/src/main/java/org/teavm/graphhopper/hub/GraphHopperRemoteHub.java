package org.teavm.graphhopper.hub;

import java.io.IOException;
import org.teavm.graphhopper.util.Ajax;
import org.teavm.jso.JSArrayReader;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class GraphHopperRemoteHub implements JSObject {
    @JSBody(params = "text", script = "return JSON.parse(text);")
    public static native GraphHopperRemoteHub parse(String text);

    public static GraphHopperRemoteHub fetch(String url) throws IOException {
        return parse(Ajax.get(url));
    }

    @JSProperty
    public abstract JSArrayReader<GraphHopperRemoteMap> getMaps();
}
