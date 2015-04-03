package org.teavm.graphhopper.storage;

import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.teavm.graphhopper.indexeddb.Database;
import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;

/**
 *
 * @author Alexey Andreev
 */
public class IndexedDBDirectory implements Directory {
    Database database;
    Map<String, IndexedDBDataAccess> dataAccessMap = new HashMap<>();

    @Override
    public String getLocation() {
        return null;
    }

    @Override
    public ByteOrder getByteOrder() {
        return null;
    }

    @Override
    public DataAccess find(String name) {
        return null;
    }

    @Override
    public DataAccess find(String name, DAType type) {
        return null;
    }

    @Override
    public void remove(DataAccess da) {
    }

    @Override
    public DAType getDefaultType() {
        return null;
    }

    @Override
    public void clear() {
    }

    @Override
    public Collection<DataAccess> getAll() {
        return null;
    }
}
