package org.teavm.graphhopper.indexeddb;

import org.teavm.dom.indexeddb.EventHandler;
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

    private void openCursor(Range range, final AsyncCallback<Cursor> callback) {
        final IDBCursorRequest req = nativeIndex.openCursor(range.nativeRange);
        req.setOnSuccess(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.complete(new Cursor(req));
            }
        });
        req.setOnError(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.error(new IndexedDBException("Error creating cursor for store " + getName()));
            }
        });
    }

    @Async
    public native JSObject get(JSObject key);

    private void get(JSObject key, final AsyncCallback<JSObject> callback) {
        final IDBGetRequest req = nativeIndex.get(key);
        req.setOnSuccess(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.complete(req.getResult());
            }
        });
        req.setOnError(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.error(new IndexedDBException("Error retrieving value from store " + getName()));
            }
        });
    }

    public int count() {
        return countImpl();
    }

    @Async
    private native Integer countImpl();

    private void count(final AsyncCallback<Integer> callback) {
        final IDBCountRequest req = nativeIndex.count();
        req.setOnSuccess(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.complete(req.getResult());
            }
        });
        req.setOnError(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.error(new IndexedDBException("Error retrieving value from store " + getName()));
            }
        });
    }
}
