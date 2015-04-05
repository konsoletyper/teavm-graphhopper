package org.teavm.graphhopper.hub;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.teavm.graphhopper.indexeddb.Cursor;
import org.teavm.graphhopper.indexeddb.Database;
import org.teavm.graphhopper.indexeddb.StoreParameters;
import org.teavm.graphhopper.indexeddb.Transaction;
import org.teavm.graphhopper.indexeddb.TransactionMode;
import org.teavm.graphhopper.util.IndexedDBFile;
import org.teavm.graphhopper.util.IndexedDBFileSystem;
import static org.teavm.graphhopper.hub.EventLoop.requireEventLoop;
import static org.teavm.graphhopper.hub.EventLoop.submit;

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
        database = Database.open(name, 1, (database, oldVersion, newVersion) -> createSchema(database));
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

    public GraphHopperLocalMap[] getMaps() {
        requireEventLoop();
        return maps.values().toArray(new GraphHopperLocalMap[0]);
    }

    public GraphHopperLocalMap getMap(String id) {
        requireEventLoop();
        return maps.get(id);
    }

    public InputStream download(String id) throws IOException {
        requireEventLoop();
        if (!maps.containsKey(id)) {
            return null;
        }
        if (uploadingMaps.contains(id)) {
            throw new IllegalStateException("Map is not uploaded yet");
        }
        GraphHopperLocalMap map = maps.get(id);
        if (map == null) {
            return null;
        }
        return fs.file(id).read();
    }

    public boolean hasMap(String id) {
        requireEventLoop();
        return maps.containsKey(id);
    }

    public boolean isUploading(String id) {
        requireEventLoop();
        return uploadingMaps.contains(id);
    }

    public void deleteMap(String id) {
        requireEventLoop();
        if (maps.containsKey(id)) {
            fs.file(id).delete();
            maps.remove(id);
            uploadingMaps.remove(id);
        }
    }

    public void upload(final GraphHopperLocalMap map, final GraphHopperMapReader reader,
            GraphHopperMapUploadListener listener) {
        requireEventLoop();
        if (!uploadingMaps.add(map.getId())) {
            throw new IllegalStateException("Already uploading map " + map.getId());
        }
        maps.put(map.getId(), map);
        new Thread(() -> {
            IndexedDBFile file = null;
            try {
                file = fs.file(map.getId());
                file.clear();
                int pos = 0;
                while (true) {
                    byte[] chunk = reader.next();
                    if (chunk == null) {
                        break;
                    }
                    file.append(chunk);
                    pos += chunk.length;
                    int bytesUploaded = pos;
                    submit(() -> listener.progress(bytesUploaded));
                }
            } catch (Exception e) {
                try {
                    if (file != null) {
                        file.delete();
                    }
                } catch (Exception e2) {
                    // Ignore
                }
                submit(() -> {
                    if (uploadingMaps.remove(map.getId())) {
                        listener.failed(e);
                    } else {
                        listener.complete();
                    }
                });
                return;
            }
            try (Transaction tx = database.begin(TransactionMode.READ_WRITE, "maps")) {
                tx.store("maps").put(map);
            }
            submit(() -> {
                uploadingMaps.remove(map.getId());
                listener.complete();
            });
        }, "upload-" + map.getId()).start();
    }
}
