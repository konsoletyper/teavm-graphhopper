package org.teavm.graphhopper.storage;

import org.teavm.dom.typedarrays.Int8Array;
import org.teavm.jso.JSBody;
import org.teavm.jso.JSObject;
import org.teavm.jso.JSProperty;

/**
 *
 * @author Alexey Andreev
 */
public abstract class IndexedDBSegment implements JSObject {
    @JSBody(params = {}, script = "return { id : null, fileId : null, directoryId : null, data : null };")
    public static native IndexedDBSegment create();

    @JSProperty
    public abstract int getId();

    @JSProperty
    public abstract void setId(int id);

    @JSProperty
    public abstract String getFileId();

    @JSProperty
    public abstract void setFileId(String fileId);

    @JSProperty
    public abstract Int8Array getData();

    @JSProperty
    public abstract void setData(Int8Array data);
}
