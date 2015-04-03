package org.teavm.graphhopper.storage;

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.util.BitUtil;
import org.teavm.dom.typedarrays.Int8Array;
import org.teavm.dom.typedarrays.TypedArrayFactory;
import org.teavm.graphhopper.indexeddb.Cursor;
import org.teavm.graphhopper.indexeddb.Range;
import org.teavm.graphhopper.indexeddb.Store;
import org.teavm.graphhopper.indexeddb.TransactionMode;
import org.teavm.jso.JS;

/**
 *
 * @author Alexey Andreev
 */
public class IndexedDBDataAccess implements DataAccess {
    private IndexedDBDirectory directory;
    protected static final int SEGMENT_SIZE_MIN = 1 << 7;
    private static final int SEGMENT_SIZE_DEFAULT = 1 << 20;
    private byte[][] segments = new byte[0][];
    private boolean[] dirtySegments;
    protected int header[] = new int[(HEADER_OFFSET - 20) / 4];
    protected static final int HEADER_OFFSET = 20 * 4 + 20;
    protected int segmentSizeInBytes = SEGMENT_SIZE_DEFAULT;
    protected transient int segmentSizePower;
    protected transient int indexDivisor;
    protected BitUtil bitUtil;
    private String name;

    @Override
    public boolean loadExisting() {
        return true;
    }

    @Override
    public void flush() {
        for (int i = 0; i < dirtySegments.length; ++i) {
            flushSegment(i);
        }
    }

    private void flushSegment(int id) {
        if (!dirtySegments[id]) {
            return;
        }
        dirtySegments[id] = false;
        storeSegment(id);
    }

    private void storeSegment(int id) {
        Int8Array data = ((TypedArrayFactory)JS.getGlobal()).createInt8Array(segments.length);
        byte[] bytes = segments[id];
        for (int i = 0; i < bytes.length; ++i) {
            data.set(i, bytes[i]);
        }
        IndexedDBSegment storedSegment = IndexedDBSegment.create();
        storedSegment.setId(id);
        storedSegment.setFileId(name);
        storedSegment.setData(data);
        directory.database
                .begin(TransactionMode.READ_WRITE, "segments")
                .store("segments")
                .put(storedSegment);
    }

    @Override
    public void close() {
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public long getCapacity() {
        return (long)getSegments() * segmentSizeInBytes;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void rename(String newName) {
        if (directory.dataAccessMap.containsKey(newName)) {
            throw new IllegalArgumentException("File " + newName + " already exists");
        }
        directory.dataAccessMap.remove(name);
        String oldName = name;
        name = newName;
        directory.dataAccessMap.put(newName, this);
        renameInDatabase(oldName, newName);
    }

    private void renameInDatabase(String oldName, String newName) {
        Store store = directory.database
                .begin(TransactionMode.READ_WRITE, "segments")
                .store("segments");

        Range oldRange = Range.only(JS.wrap(oldName));
        for (Cursor cursor = store.index("byStore").openCursor(oldRange); cursor.hasNext(); cursor.next()) {
            IndexedDBSegment segment = (IndexedDBSegment)cursor.getValue();
            segment.setFileId(newName);
            store.put(segment);
            cursor.delete();
        }
    }

    @Override
    public void setInt(long bytePos, int value) {
    }

    @Override
    public int getInt(long bytePos) {
        return 0;
    }

    @Override
    public void setShort(long bytePos, short value) {
    }

    @Override
    public short getShort(long bytePos) {
        return 0;
    }

    @Override
    public void setBytes(long bytePos, byte[] values, int length) {
    }

    @Override
    public void getBytes(long bytePos, byte[] values, int length) {
    }

    @Override
    public void setHeader(int bytePos, int value) {
    }

    @Override
    public int getHeader(int bytePos) {
        return 0;
    }

    @Override
    public DataAccess create(long bytes) {
        return null;
    }

    @Override
    public boolean ensureCapacity(long bytes) {
        return false;
    }

    @Override
    public void trimTo(long bytes) {
    }

    @Override
    public DataAccess copyTo(DataAccess da) {
        return null;
    }

    @Override
    public DataAccess setSegmentSize(int bytes) {
        return null;
    }

    @Override
    public int getSegmentSize() {
        return 0;
    }

    @Override
    public int getSegments() {
        return 0;
    }

    @Override
    public DAType getType() {
        return null;
    }
}
