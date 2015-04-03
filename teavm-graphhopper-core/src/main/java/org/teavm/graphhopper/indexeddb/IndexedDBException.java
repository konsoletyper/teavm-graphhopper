package org.teavm.graphhopper.indexeddb;

/**
 *
 * @author Alexey Andreev
 */
public class IndexedDBException extends RuntimeException {
    private static final long serialVersionUID = -813947949970667058L;

    public IndexedDBException() {
        super();
    }

    public IndexedDBException(String message, Throwable cause) {
        super(message, cause);
    }

    public IndexedDBException(String message) {
        super(message);
    }

    public IndexedDBException(Throwable cause) {
        super(cause);
    }
}
