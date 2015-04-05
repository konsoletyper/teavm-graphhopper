package org.teavm.graphhopper.indexeddb;

import org.teavm.dom.indexeddb.IDBCountRequest;
import org.teavm.dom.indexeddb.IDBCursorRequest;
import org.teavm.dom.indexeddb.IDBGetRequest;
import org.teavm.dom.indexeddb.IDBIndex;
import org.teavm.javascript.spi.Async;
import org.teavm.jso.JSObject;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class Index {
    private Store store;
    private IDBIndex nativeIndex;

    Index(Store store, IDBIndex nativeIndex) {
        this.store = store;
        this.nativeIndex = nativeIndex;
    }

    public Store getStore() {
        return store;
    }

    public String getName() {
        return nativeIndex.getName();
    }

    public String[] getKeyPath() {
        return nativeIndex.getKeyPath();
    }

    public boolean isUnique() {
        return nativeIndex.isUnique();
    }

    public Cursor openCursor() {
        return openCursor(null);
    }

    @Async
    public native Cursor openCursor(Range range);

    private void openCursor(Range range, AsyncCallback<Cursor> callback) {
        IDBCursorRequest req = range != null ? nativeIndex.openCursor(range.nativeRange) : nativeIndex.openCursor();
        req.setOnSuccess(() -> callback.complete(new Cursor(req)));
        req.setOnError(() -> callback.error(new IndexedDBException("Error creating cursor for store " + getName())));
    }

    @Async
    public native JSObject get(JSObject key);

    private void get(JSObject key, AsyncCallback<JSObject> callback) {
        IDBGetRequest req = nativeIndex.get(key);
        req.setOnSuccess(() -> callback.complete(req.getResult()));
        req.setOnError(() -> callback.error(new IndexedDBException("Error retrieving value from store " + getName())));
    }

    public int count() {
        return countImpl();
    }

    @Async
    private native Integer countImpl();

    private void count(AsyncCallback<Integer> callback) {
        IDBCountRequest req = nativeIndex.count();
        req.setOnSuccess(() -> callback.complete(req.getResult()));
        req.setOnError(() -> callback.error(new IndexedDBException("Error retrieving value from store " + getName())));
    }
}
