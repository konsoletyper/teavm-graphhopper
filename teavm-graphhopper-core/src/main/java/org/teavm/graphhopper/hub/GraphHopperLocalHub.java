package org.teavm.graphhopper.hub;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.graphhopper.indexeddb.Cursor;
import org.teavm.graphhopper.indexeddb.Database;
import org.teavm.graphhopper.indexeddb.DatabaseUpdater;
import org.teavm.graphhopper.indexeddb.StoreParameters;
import org.teavm.graphhopper.indexeddb.Transaction;
import org.teavm.graphhopper.indexeddb.TransactionMode;
import org.teavm.graphhopper.util.IndexedDBFile;
import org.teavm.graphhopper.util.IndexedDBFileSystem;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperLocalHub {
    private IndexedDBFileSystem fs;
    private Map<String, GraphHopperLocalMap> maps = new HashMap<>();
    private Set<String> uploadingMaps = new HashSet<>();
    private Database database;

    public GraphHopperLocalHub() {
        this("graphhopper");
    }

    public GraphHopperLocalHub(String name) {
        fs = new IndexedDBFileSystem(name + "-fs");
        database = Database.open(name, 1, new DatabaseUpdater() {
            @Override
            public void update(Database database, int oldVersion, int newVersion) {
                createSchema(database);
            }
        });
        try (Transaction tx = database.begin("maps")) {
            for (Cursor cursor = tx.store("maps").openCursor(); cursor.hasNext(); cursor.next()) {
                GraphHopperLocalMap map = (GraphHopperLocalMap)cursor.getValue();
                maps.put(map.getId(), map);
            }
        }
    }

    private void createSchema(Database database) {
        database.createStore("maps", new StoreParameters().setKeyPath("id"));
    }

    public synchronized GraphHopperLocalMap[] getMaps() {
        return maps.values().toArray(new GraphHopperLocalMap[0]);
    }

    public synchronized GraphHopperLocalMap getMap(String id) {
        return maps.get(id);
    }

    public InputStream download(String id) throws IOException {
        synchronized (uploadingMaps) {
            if (uploadingMaps.contains(id)) {
                throw new IllegalStateException("Map is not uploaded yet");
            }
        }
        synchronized (this) {
            GraphHopperLocalMap map = maps.get(id);
            if (map == null) {
                return null;
            }
            return fs.file(id).read();
        }
    }

    public synchronized boolean hasMap(String id) {
        return maps.containsKey(id);
    }

    public boolean isUploading(String id) {
        synchronized (uploadingMaps) {
            return uploadingMaps.contains(id);
        }
    }

    public void upload(GraphHopperLocalMap map, GraphHopperMapReader reader) throws IOException {
        synchronized (uploadingMaps) {
            if (!uploadingMaps.add(map.getId())) {
                throw new IllegalStateException("Already uploading map " + map.getId());
            }
        }
        IndexedDBFile file;
        synchronized (this) {
            try (Transaction tx = database.begin(TransactionMode.READ_WRITE, "maps")) {
                tx.store("maps").put(map);
            }
            maps.put(map.getId(), map);
            file = fs.file(map.getId());
            file.clear();
        }
        while (true) {
            byte[] chunk = reader.next();
            if (chunk == null) {
                break;
            }
            synchronized (this) {
                if (!maps.containsKey(map.getId())) {
                    break;
                }
                file.append(chunk);
            }
        }
        synchronized (uploadingMaps) {
            uploadingMaps.remove(map.getId());
        }
    }
}
