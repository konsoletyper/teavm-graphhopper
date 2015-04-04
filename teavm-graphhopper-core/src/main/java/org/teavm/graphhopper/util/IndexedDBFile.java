package org.teavm.graphhopper.util;

import java.io.IOException;
import java.io.InputStream;
import org.teavm.dom.typedarrays.Int8Array;
import org.teavm.graphhopper.indexeddb.Cursor;
import org.teavm.graphhopper.indexeddb.Database;
import org.teavm.graphhopper.indexeddb.Index;
import org.teavm.graphhopper.indexeddb.IndexedDBException;
import org.teavm.graphhopper.indexeddb.Range;
import org.teavm.graphhopper.indexeddb.Store;
import org.teavm.graphhopper.indexeddb.Transaction;
import org.teavm.graphhopper.indexeddb.TransactionMode;
import org.teavm.jso.JS;
import org.teavm.jso.JSArray;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public class IndexedDBFile implements AutoCloseable {
    private Database database;
    private String name;
    private int clusterSize = 4096;

    IndexedDBFile(IndexedDBFileSystem fs, String name) {
        database = fs.database;
        this.name = name;
    }

    public InputStream read() throws IOException {
        return new IndexedDBInputStream(name, database);
    }

    public void clear() {
        try (Transaction tx = database.begin(TransactionMode.READ_WRITE, "chunks", "properties")) {
            Store properties = tx.store("properties");
            properties.put(Property.create(name, "size", JS.wrap(0)));
            deleteChunks(tx);
        }
    }

    public void delete() {
        try (Transaction tx = database.begin(TransactionMode.READ_WRITE, "chunks", "properties")) {
            tx.store("properties").delete(propertyKey(name, "size"));
            deleteChunks(tx);
        }
    }

    private void deleteChunks(Transaction tx) {
        Index chunks = tx.store("chunks").index("byFile");
        Range range = Range.only(JS.wrap(new String[] { name }));
        for (Cursor cursor = chunks.openCursor(range); cursor.hasNext(); cursor.next()) {
            cursor.delete();
        }
    }

    public void append(byte[] data) throws IOException {
        try (Transaction tx = database.begin(TransactionMode.READ_WRITE, "chunks", "properties")) {
            int size = size(tx);
            Store properties = tx.store("properties");
            properties.put(Property.create(name, "size", JS.wrap(size + data.length)));
            Store chunks = tx.store("chunks");
            chunks.clear();
            int index = size / clusterSize;
            int pos = size % clusterSize;
            for (int i = 0; i < data.length;) {
                int chunkSize = Math.min(data.length - i, clusterSize - pos);
                Chunk chunk = Chunk.create(name, index, chunkSize);
                Int8Array chunkData = chunk.getData();
                if (pos > 0) {
                    Chunk oldChunk = (Chunk)tx.store("chunks").get(chunkKey(name, index));
                    Int8Array oldData = oldChunk.getData();
                    for (int j = 0; j < oldData.getLength(); ++j) {
                        chunkData.set(j, oldData.get(j));
                    }
                }
                for (int j = pos; j < chunkSize; ++j) {
                    chunkData.set(j, data[i++]);
                }
                chunks.put(chunk);
                ++index;
                pos = 0;
            }
        }
    }

    public int size() {
        try (Transaction tx = database.begin("properties")) {
            return size(tx);
        } catch (IOException e) {
            throw new IllegalStateException("File does not exist");
        }
    }

    private int size(Transaction tx) throws IOException {
        Property sizeProperty = (Property)tx.store("properties").get(propertyKey(name, "size"));
        if (JS.isUndefined(sizeProperty)) {
            throw new IOException("File does not exist");
        }
        return JS.unwrapInt(sizeProperty.getValue());
    }

    public boolean exists() {
        try (Transaction tx = database.begin("properties")) {
            return exists(tx);
        }
    }

    private boolean exists(Transaction tx) {
        return !JS.isUndefined(tx.store("properties").get(propertyKey(name, "size")));
    }

    @Override
    public void close() {
        database = null;
    }

    static abstract class Property implements JSObject {
        @JSBody(params = { "name", "value" }, script = "return { 'file' : file, 'name' : name, 'value' : value };")
        public static native Property create(String file, String name, JSObject value);

        @JSProperty
        public abstract String getName();

        @JSProperty
        public abstract JSObject getValue();
    }

    static abstract class Chunk implements JSObject {
        @JSBody(params = { "file", "index", "size" },
                script = "return { 'file' : file, 'index' : index, 'data' : new Int8Array(size) };")
        public static native Chunk create(String file, int index, int size);

        @JSProperty
        public abstract int getIndex();

        @JSProperty
        public abstract Int8Array getData();
    }

    static JSArray<JSObject> chunkKey(String file, int index) {
        return JS.wrap(new JSObject[] { JS.wrap(file), JS.wrap(index) });
    }

    static JSObject propertyKey(String file, String property) {
        return JS.wrap(new String[] { file, property });
    }

    static class IndexedDBInputStream extends InputStream {
        Transaction tx;
        Chunk currentChunk;
        String file;
        int available;
        int pos;
        int chunkIndex = -1;

        public IndexedDBInputStream(String file, Database database) throws IOException {
            this.file = file;
            try {
                tx = database.begin("properties", "chunks");
                Property sizeProperty = (Property)tx.store("properties").get(propertyKey(file, "size"));
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
            currentChunk = (Chunk)tx.store("chunks").get(chunkKey(file, chunkIndex));
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
