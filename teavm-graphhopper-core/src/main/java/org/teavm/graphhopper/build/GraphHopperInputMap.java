package org.teavm.graphhopper.build;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperInputMap {
    private String id;
    private String name;
    private String osmFileName;

    GraphHopperInputMap() {
    }

    public GraphHopperInputMap(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOsmFileName() {
        return osmFileName;
    }

    public void setOsmFileName(String osmFileName) {
        this.osmFileName = osmFileName;
    }

    public String getId() {
        return id;
    }
}
