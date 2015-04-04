package org.teavm.graphhopper.indexeddb;

/**
 *
 * @author Alexey Andreev
 */
public class StoreParameters {
    private boolean autoIncrement;
    private String[] keyPath;

    public boolean isAutoIncrement() {
        return autoIncrement;
    }

    public StoreParameters setAutoIncrement(boolean autoIncrement) {
        this.autoIncrement = autoIncrement;
        return this;
    }

    public String[] getKeyPath() {
        return keyPath;
    }

    public StoreParameters setKeyPath(String... keyPath) {
        this.keyPath = keyPath;
        return this;
    }
}
