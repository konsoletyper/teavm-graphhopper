package org.teavm.graphhopper.indexeddb;

import org.teavm.dom.indexeddb.IDBCountRequest;
import org.teavm.dom.indexeddb.IDBCursorRequest;
import org.teavm.dom.indexeddb.IDBGetRequest;
import org.teavm.dom.indexeddb.IDBObjectStore;
import org.teavm.dom.indexeddb.IDBRequest;
import org.teavm.javascript.spi.Async;
import org.teavm.jso.JSObject;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class Store {
    private Database database;
    private IDBObjectStore nativeStore;

    Store(Database database, IDBObjectStore nativeStore) {
        this.database = database;
        this.nativeStore = nativeStore;
    }

    public String getName() {
        return nativeStore.getName();
    }

    public String[] getKeyPath() {
        return nativeStore.getKeyPath();
    }

    public String[] getIndexNames() {
        return nativeStore.getIndexNames();
    }

    public boolean isAutoIncrement() {
        return nativeStore.isAutoIncrement();
    }

    @Async
    public native void put(JSObject value);

    private void put(JSObject value, AsyncCallback<Void> callback) {
        IDBRequest req = nativeStore.put(value);
        req.setOnSuccess(() -> callback.complete(null));
        req.setOnError(() -> callback.error(new IndexedDBException("Error saving value in store " + getName())));
    }

    @Async
    public native void put(JSObject value, JSObject key);

    private void put(JSObject value, JSObject key, AsyncCallback<Void> callback) {
        IDBRequest req = nativeStore.put(value, key);
        req.setOnSuccess(() -> callback.complete(null));
        req.setOnError(() -> callback.error(new IndexedDBException("Error saving value in store " + getName())));
    }

    @Async
    public native void add(JSObject value);

    private void add(JSObject value, AsyncCallback<Void> callback) {
        IDBRequest req = nativeStore.add(value);
        req.setOnSuccess(() -> callback.complete(null));
        req.setOnError(() -> callback.error(new IndexedDBException("Error saving value in store " + getName())));
    }

    @Async
    public native void add(JSObject value, JSObject key);

    private void add(JSObject value, JSObject key, AsyncCallback<Void> callback) {
        IDBRequest req = nativeStore.add(value, key);
        req.setOnSuccess(() -> callback.complete(null));
        req.setOnError(() -> callback.error(new IndexedDBException("Error saving value in store " + getName())));
    }

    @Async
    public native void delete(JSObject key);

    private void delete(JSObject key, AsyncCallback<Void> callback) {
        IDBRequest req = nativeStore.delete(key);
        req.setOnSuccess(() -> callback.complete(null));
        req.setOnError(() -> callback.error(new IndexedDBException("Error removing value from store " + getName())));
    }

    @Async
    public native JSObject get(JSObject key);

    private void get(JSObject key, AsyncCallback<JSObject> callback) {
        IDBGetRequest req = nativeStore.get(key);
        req.setOnSuccess(() -> callback.complete(req.getResult()));
        req.setOnError(() -> callback.error(new IndexedDBException("Error retrieving value from store " + getName())));
    }

    @Async
    public native void clear();

    private void clear(AsyncCallback<Void> callback) {
        IDBRequest req = nativeStore.clear();
        req.setOnSuccess(() -> callback.complete(null));
        req.setOnError(() -> callback.error(new IndexedDBException("Error removing data from store " + getName())));
    }

    public Cursor openCursor() {
        return openCursor(null);
    }

    @Async
    public native Cursor openCursor(Range range);

    private void openCursor(Range range, AsyncCallback<Cursor> callback) {
        IDBCursorRequest req = range != null ? nativeStore.openCursor(range.nativeRange) : nativeStore.openCursor();
        req.setOnSuccess(() -> callback.complete(new Cursor(req)));
        req.setOnError(() -> callback.error(new IndexedDBException("Error creating cursor for store " + getName())));
    }

    public Index createIndex(String name, String... key) {
        return new Index(this, nativeStore.createIndex(name, key));
    }

    public Index index(String name) {
        return new Index(this, nativeStore.index(name));
    }

    public void deleteIndex(String name) {
        nativeStore.deleteIndex(name);
    }

    public int count(JSObject key) {
        return countImpl(key);
    }

    @Async
    private native Integer countImpl(JSObject key);

    private void count(JSObject key, AsyncCallback<Integer> callback) {
        IDBCountRequest req = nativeStore.count(key);
        req.setOnSuccess(() -> callback.complete(req.getResult()));
        req.setOnError(() -> callback.error(new IndexedDBException("Error retrieving value from store " + getName())));
    }

    public int count() {
        return countImpl();
    }

    @Async
    private native Integer countImpl();

    private void count(AsyncCallback<Integer> callback) {
        IDBCountRequest req = nativeStore.count();
        req.setOnSuccess(() -> callback.complete(req.getResult()));
        req.setOnError(() -> callback.error(new IndexedDBException("Error retrieving value from store " + getName())));
    }
}
