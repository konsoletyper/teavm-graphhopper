package org.teavm.graphhopper.indexeddb;

/**
 *
 * @author Alexey Andreev
 */
public interface DatabaseUpdater {
    void update(Database database, int oldVersion, int newVersion);
}
