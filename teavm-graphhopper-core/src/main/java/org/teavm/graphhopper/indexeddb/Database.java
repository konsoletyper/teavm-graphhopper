package org.teavm.graphhopper.indexeddb;

import org.teavm.dom.indexeddb.IDBDatabase;
import org.teavm.dom.indexeddb.IDBFactory;
import org.teavm.dom.indexeddb.IDBObjectStore;
import org.teavm.dom.indexeddb.IDBObjectStoreParameters;
import org.teavm.dom.indexeddb.IDBOpenDBRequest;
import org.teavm.dom.indexeddb.IDBTransaction;
import org.teavm.javascript.spi.Async;
import org.teavm.platform.async.AsyncCallback;

/**
 *
 * @author Alexey Andreev
 */
public class Database {
    private IDBDatabase nativeDb;

    private Database() {
    }

    public static Database open(String name, int version, DatabaseUpdater updater) {
        Database database = new Database();
        final IDBOpenDBRequest req = IDBFactory.getInstance().open(name, version);
        init(database, req, updater);
        return database;
    }

    @Async
    private static native void init(Database database, IDBOpenDBRequest request, DatabaseUpdater updater);

    private static void init(Database database, IDBOpenDBRequest request, DatabaseUpdater updater,
            AsyncCallback<Void> callback) {
        request.setOnSuccess(() -> {
            database.nativeDb = request.getResult();
            callback.complete(null);
        });
        request.setOnError(() -> callback.error(new IndexedDBException("Error opening database: " + request + ": " +
                request.getError().getName())));
        request.setOnUpgradeNeeded(event -> {
            database.nativeDb = request.getResult();
            updater.update(database, event.getOldVersion(), event.getNewVersion());
        });
    }

    public String getName() {
        checkOpened();
        return nativeDb.getName();
    }

    public int getVersion() {
        checkOpened();
        return nativeDb.getVersion();
    }

    public String[] getStoreNames() {
        checkOpened();
        return nativeDb.getObjectStoreNames();
    }

    public Store createStore(String name) {
        checkOpened();
        return createStore(name, new StoreParameters());
    }

    public Store createStore(String name, StoreParameters params) {
        checkOpened();
        IDBObjectStore nativeStore = nativeDb.createObjectStore(name, IDBObjectStoreParameters.create()
                .autoIncrement(params.isAutoIncrement())
                .keyPath(params.getKeyPath()));
        return new Store(this, nativeStore);
    }

    public void deleteStore(String name) {
        nativeDb.deleteObjectStore(name);
    }

    public boolean isClosed() {
        return nativeDb == null;
    }

    private void checkOpened() {
        if (isClosed()) {
            throw new IllegalStateException("This database is already closed");
        }
    }

    public Transaction begin(TransactionMode mode, String... storeNames) {
        checkOpened();
        String nativeMode = "";
        switch (mode) {
            case READONLY:
                nativeMode = IDBDatabase.TRANSACTION_READONLY;
                break;
            case READ_WRITE:
                nativeMode = IDBDatabase.TRANSACTION_READWRITE;
                break;
            case UPDATE_VERSION:
                nativeMode = IDBDatabase.TRANSACTION_VERSIONCHANGE;
                break;
        }
        IDBTransaction nativeTx = nativeDb.transaction(storeNames, nativeMode);
        return new Transaction(this, mode, nativeTx);
    }

    public Transaction begin(String... storeNames) {
        return begin(TransactionMode.READONLY, storeNames);
    }

    public void close() {
        if (nativeDb != null) {
            nativeDb.close();
            nativeDb = null;
        }
    }
}
