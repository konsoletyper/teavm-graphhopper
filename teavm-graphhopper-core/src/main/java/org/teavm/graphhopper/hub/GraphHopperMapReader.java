package org.teavm.graphhopper.hub;

import java.io.IOException;

/**
 *
 * @author Alexey Andreev
 */
public interface GraphHopperMapReader {
    byte[] next() throws IOException;
}
