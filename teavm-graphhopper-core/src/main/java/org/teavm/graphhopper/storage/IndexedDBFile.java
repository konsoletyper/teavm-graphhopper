package org.teavm.graphhopper.storage;

import java.io.IOException;
import java.io.InputStream;
import org.teavm.dom.typedarrays.Int8Array;
import org.teavm.graphhopper.indexeddb.Database;
import org.teavm.graphhopper.indexeddb.DatabaseUpdater;
import org.teavm.graphhopper.indexeddb.IndexedDBException;
import org.teavm.graphhopper.indexeddb.Store;
import org.teavm.graphhopper.indexeddb.StoreParameters;
import org.teavm.graphhopper.indexeddb.Transaction;
import org.teavm.graphhopper.indexeddb.TransactionMode;
import org.teavm.jso.JS;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public class IndexedDBFile implements AutoCloseable {
    private Database database;
    private int clusterSize = 4096;

    public IndexedDBFile(String name) {
        database = Database.open(name, 1, new DatabaseUpdater() {
            @Override
            public void update(Database database, int oldVersion, int newVersion) {
                database.createStore("chunks", new StoreParameters().setKeyPath("index"));
                database.createStore("properties", new StoreParameters().setKeyPath("name"));
            }
        });
    }

    public InputStream read() throws IOException {
        return new IndexedDBInputStream(database);
    }

    public void write(byte[] data) {
        Transaction tx = database.begin(TransactionMode.READ_WRITE, "chunks", "properties");
        Store properties = tx.store("properties");
        properties.put(Property.create("size", JS.wrap(data.length)));
        Store chunks = tx.store("chunks");
        chunks.clear();
        int index = 0;
        for (int i = 0; i < data.length;) {
            int chunkSize = Math.min(data.length - i, clusterSize);
            Chunk chunk = Chunk.create(index++, chunkSize);
            Int8Array chunkData = chunk.getData();
            for (int j = 0; j < chunkSize; ++j) {
                chunkData.set(j, data[i++]);
            }
            chunks.put(chunk);
        }
        tx.commit();
    }

    public boolean exists() {
        return !JS.isUndefined(database.begin("properties").store("properties")
                .get(JS.wrap(new String[] { "size" })));
    }

    @Override
    public void close() {
        database.close();
    }

    static abstract class Property implements JSObject {
        @JSBody(params = { "name", "value" }, script = "return { 'name' : name, 'value' : value };")
        public static native Property create(String name, JSObject value);

        @JSProperty
        public abstract String getName();

        @JSProperty
        public abstract JSObject getValue();
    }

    static abstract class Chunk implements JSObject {
        @JSBody(params = { "index", "size" }, script = "return { 'index' : index, 'data' : new Int8Array(size) };")
        public static native Chunk create(int index, int size);

        @JSProperty
        public abstract int getIndex();

        @JSProperty
        public abstract Int8Array getData();
    }

    static class IndexedDBInputStream extends InputStream {
        Transaction tx;
        Chunk currentChunk;
        int available;
        int pos;
        int chunkIndex = -1;

        public IndexedDBInputStream(Database database) throws IOException {
            try {
                tx = database.begin("properties", "chunks");
                Property sizeProperty = (Property)tx.store("properties").get(JS.wrap(new String[] { "size" }));
                if (JS.isUndefined(sizeProperty)) {
                    throw new IOException("File does not exist");
                }
                available = JS.unwrapInt(sizeProperty.getValue());
                nextChunk();
            } catch (IndexedDBException e) {
                throw new IOException("Error opening file", e);
            }
        }

        @Override
        public int read() throws IOException {
            if (pos >= currentChunk.getData().getLength()) {
                available -= currentChunk.getData().getLength();
                if (available == 0) {
                    return -1;
                }
                nextChunk();
                pos = 0;
            }
            return currentChunk.getData().get(pos++);
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            if (pos >= currentChunk.getData().getLength()) {
                available -= currentChunk.getData().getLength();
                if (available == 0) {
                    return -1;
                }
                nextChunk();
                pos = 0;
            }
            int sz = Math.min(currentChunk.getData().getLength() - pos, len);
            for (int i = 0; i < sz; ++i) {
                b[off++] = currentChunk.getData().get(pos++);
            }
            return sz;
        }

        @Override
        public long skip(long n) throws IOException {
            int sz = n >= Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)n;
            sz = Math.min(sz, available());
            int remaining = sz;
            while (remaining > currentChunk.getData().getLength() - pos) {
                available -= currentChunk.getData().getLength();
                remaining -= currentChunk.getData().getLength() - pos;
                nextChunk();
                pos = 0;
            }
            pos = remaining;
            return sz;
        }

        private void nextChunk() throws IOException {
            currentChunk = (Chunk)tx.store("chunks").get(JS.wrap(new int[] { ++chunkIndex }));
            if (JS.isUndefined(currentChunk)) {
                throw new IOException("File broken");
            }
        }

        @Override
        public int available() throws IOException {
            return available - pos;
        }

        @Override
        public void close() throws IOException {
            if (tx != null) {
                tx.commit();
                tx = null;
            }
        }
    }
}
