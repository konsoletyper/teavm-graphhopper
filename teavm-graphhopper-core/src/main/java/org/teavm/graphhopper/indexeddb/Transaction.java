package org.teavm.graphhopper.indexeddb;

import org.teavm.dom.indexeddb.IDBTransaction;
import org.teavm.javascript.spi.Async;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class Transaction implements AutoCloseable {
    private Database database;
    private TransactionMode mode;
    private IDBTransaction nativeTransaction;
    private boolean errorOccured;
    private AsyncCallback<Void> commitCallback;
    private AsyncCallback<Void> abortCallback;
    private boolean complete;
    private boolean abort;
    private boolean finished;

    Transaction(Database database, TransactionMode mode, IDBTransaction nativeTransaction) {
        this.database = database;
        this.mode = mode;
        this.nativeTransaction = nativeTransaction;
        nativeTransaction.setOnComplete(() -> {
            complete = true;
            if (commitCallback != null) {
                AsyncCallback<Void> callback = commitCallback;
                commitCallback = null;
                callback.complete(null);
            }
        });
        nativeTransaction.setOnAbort(() -> {
            complete = true;
            if (abortCallback != null) {
                AsyncCallback<Void> callback = abortCallback;
                abortCallback = null;
                callback.complete(null);
            }
        });
        nativeTransaction.setOnError(() -> {
            if (commitCallback != null) {
                commitCallback.error(new IndexedDBException("Error commiting transation: " +
                        nativeTransaction.getError().getName()));
                commitCallback = null;
            } else if (abortCallback != null) {
                abortCallback.error(new IndexedDBException("Error aborting transation: " +
                        nativeTransaction.getError().getName()));
                abortCallback = null;
            }
        });
    }

    public Database getDatabase() {
        return database;
    }

    public TransactionMode getMode() {
        return mode;
    }

    public void abort() {
        if (complete) {
            if (finished) {
                throw new IllegalStateException("Transaction is no longer active");
            }
            finished = true;
            if (nativeTransaction.getError() != null) {
                throw new RuntimeException("Error aborting transaction: " + nativeTransaction.getError().getName());
            }
            return;
        } else {
            finished = true;
            if (abortCallback != null || abortCallback != null) {
                throw new IllegalStateException("This method is called second time");
            }
            waitForAbort();
            nativeTransaction.abort();
        }
    }

    @Async
    private native void waitForAbort();

    private void waitForAbort(AsyncCallback<Void> callback) {
        abortCallback = callback;
    }

    public void commit() {
        if (complete) {
            if (finished) {
                throw new IllegalStateException("Transaction is no longer active");
            }
            finished = true;
            if (nativeTransaction.getError() != null) {
                throw new RuntimeException("Error commiting transaction: " + nativeTransaction.getError().getName());
            }
            return;
        } else {
            finished = true;
            if (commitCallback != null || abortCallback != null) {
                throw new IllegalStateException("This method is called second time");
            }
            waitForCommit();
        }
    }

    @Async
    private native void waitForCommit();

    private void waitForCommit(final AsyncCallback<Void> callback) {
        commitCallback = callback;
    }

    public Store store(String name) {
        return new Store(database, nativeTransaction.objectStore(name));
    }

    @Override
    public void close() {
        if (!complete) {
            commit();
        }
    }
}
