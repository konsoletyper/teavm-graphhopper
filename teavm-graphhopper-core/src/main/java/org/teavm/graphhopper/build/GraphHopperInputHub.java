package org.teavm.graphhopper.build;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperInputHub {
    private List<GraphHopperInputMap> maps = new ArrayList<>();

    public List<GraphHopperInputMap> getMaps() {
        return maps;
    }
}
