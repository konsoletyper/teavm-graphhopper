package org.teavm.graphhopper.mapsforge;

import org.mapsforge.core.model.BoundingBox;
import org.mapsforge.core.model.LatLong;
import org.mapsforge.core.model.Tile;
import org.mapsforge.map.reader.MapDataStore;
import org.mapsforge.map.reader.MapReadResult;
import org.teavm.graphhopper.util.IndexedDBFile;

/**
 *
 * @author Alexey Andreev
 */
public class IndexedDBDataStore implements MapDataStore {
    private IndexedDBFile file;

    public IndexedDBDataStore(IndexedDBFile file) {
        this.file = file;
    }

    @Override
    public BoundingBox boundingBox() {
        return null;
    }

    @Override
    public void close() {
    }

    @Override
    public long getDataTimestamp(Tile tile) {
        return 0;
    }

    @Override
    public LatLong startPosition() {
        return null;
    }

    @Override
    public Byte startZoomLevel() {
        return null;
    }

    @Override
    public MapReadResult readMapData(Tile tile) {
        return null;
    }

    @Override
    public boolean supportsTile(Tile tile) {
        return false;
    }
}
