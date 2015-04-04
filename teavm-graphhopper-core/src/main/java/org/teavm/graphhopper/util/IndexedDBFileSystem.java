package org.teavm.graphhopper.util;

import org.teavm.graphhopper.indexeddb.Database;
import org.teavm.graphhopper.indexeddb.DatabaseUpdater;
import org.teavm.graphhopper.indexeddb.Store;
import org.teavm.graphhopper.indexeddb.StoreParameters;

/**
 *
 * @author Alexey Andreev
 */
public class IndexedDBFileSystem {
    Database database;

    public IndexedDBFileSystem(String name) {
        database = Database.open(name, 1, new DatabaseUpdater() {
            @Override
            public void update(Database database, int oldVersion, int newVersion) {
                Store chunks = database.createStore("chunks", new StoreParameters().setKeyPath("file", "index"));
                chunks.createIndex("byFile", "file");
                database.createStore("properties", new StoreParameters().setKeyPath("file", "name"));
            }
        });
    }

    public IndexedDBFile file(String name) {
        return new IndexedDBFile(this, name);
    }
}
