package org.teavm.graphhopper.indexeddb;

import org.teavm.dom.indexeddb.IDBKeyRange;
import org.teavm.jso.JSObject;

/**
 *
 * @author Alexey Andreev
 */
public class Range {
    IDBKeyRange nativeRange;

    Range(IDBKeyRange nativeRange) {
        this.nativeRange = nativeRange;
    }

    public static Range only(JSObject value) {
        return new Range(IDBKeyRange.only(value));
    }

    public static Range lowerBound(JSObject value, boolean open) {
        return new Range(IDBKeyRange.lowerBound(value, open));
    }

    public static Range lowerBound(JSObject value) {
        return lowerBound(value, false);
    }

    public static Range upperBound(JSObject value, boolean open) {
        return new Range(IDBKeyRange.upperBound(value, open));
    }

    public static Range upperBound(JSObject value) {
        return upperBound(value, false);
    }

    public static Range bound(JSObject lower, JSObject upper, boolean lowerOpen, boolean upperOpen) {
        return new Range(IDBKeyRange.bound(lower, upper, lowerOpen, upperOpen));
    }

    public static Range bound(JSObject lower, JSObject upper) {
        return bound(lower, upper, false, false);
    }
}
