package org.teavm.graphhopper.indexeddb;

import org.teavm.dom.indexeddb.EventHandler;
import org.teavm.dom.indexeddb.IDBCursor;
import org.teavm.dom.indexeddb.IDBCursorRequest;
import org.teavm.dom.indexeddb.IDBRequest;
import org.teavm.javascript.spi.Async;
import org.teavm.jso.JSObject;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class Cursor {
    private IDBCursorRequest req;
    private IDBCursor nativeCursor;
    private AsyncCallback<Void> nextCallback;

    Cursor(IDBCursorRequest req) {
        this.req = req;
        this.nativeCursor = req.getResult();
        req.setOnSuccess(new EventHandler() {
            @Override
            public void handleEvent() {
                nativeCursor = Cursor.this.req.getResult();
                nextCallback.complete(null);
            }
        });
        req.setOnError(new EventHandler() {
            @Override
            public void handleEvent() {
                nextCallback.error(new IndexedDBException("Error moving further through cursor"));
            }
        });
    }

    public JSObject getKey() {
        return nativeCursor.getKey();
    }

    public JSObject getValue() {
        return nativeCursor.getValue();
    }

    public JSObject getPrimaryKey() {
        return nativeCursor.getPrimaryKey();
    }

    @Async
    public native void update(JSObject obj);

    private void update(JSObject obj, final AsyncCallback<Void> callback) {
        final IDBRequest req = nativeCursor.update(obj);
        req.setOnSuccess(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.complete(null);
            }
        });
        req.setOnError(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.error(new IndexedDBException("Error updating cursor value"));
            }
        });
    }

    public boolean hasNext() {
        return nativeCursor != null;
    }

    @Async
    public void next() {
        if (nativeCursor == null) {
            throw new IllegalStateException("Cursor has reached its end");
        }
        nextImpl();
    }

    @Async
    private native void nextImpl();

    private void nextImpl(AsyncCallback<Void> callback) {
        nextCallback = callback;
        nativeCursor.doContinue();
    }

    @Async
    public native void delete();

    private void delete(final AsyncCallback<Void> callback) {
        final IDBRequest req = nativeCursor.delete();
        req.setOnSuccess(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.complete(null);
            }
        });
        req.setOnError(new EventHandler() {
            @Override
            public void handleEvent() {
                callback.error(new IndexedDBException("Error deleting item from cursor"));
            }
        });
    }
}
