/*
 *  Copyright 2015 Alexey Andreev.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.teavm.graphhopper;

import java.nio.ByteOrder;
import java.util.Arrays;
import org.slf4j.LoggerFactory;
import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.util.BitUtil;

/**
 *
 * @author Alexey Andreev
 */
public class InMemoryDataAccess implements DataAccess {
    private InMemoryDirectory directory;
    protected static final int SEGMENT_SIZE_MIN = 1 << 7;
    private static final int SEGMENT_SIZE_DEFAULT = 1 << 20;
    private byte[][] segments = new byte[0][];
    protected int header[] = new int[(HEADER_OFFSET - 20) / 4];
    protected static final int HEADER_OFFSET = 20 * 4 + 20;
    protected int segmentSizeInBytes = SEGMENT_SIZE_DEFAULT;
    protected transient int segmentSizePower;
    protected transient int indexDivisor;
    protected final BitUtil bitUtil;
    private String name;

    public InMemoryDataAccess(InMemoryDirectory directory, String name, ByteOrder order) {
        this.directory = directory;
        this.name = name;
        bitUtil = BitUtil.get(order);
    }

    @Override
    public DataAccess copyTo(DataAccess da) {
        copyHeader(da);
        da.ensureCapacity(getCapacity());
        long cap = getCapacity();
        // currently get/setBytes does not support copying more bytes than segmentSize
        int segSize = Math.min(da.getSegmentSize(), getSegmentSize());
        byte[] bytes = new byte[segSize];
        for (long bytePos = 0; bytePos < cap; bytePos += segSize) {
            getBytes(bytePos, bytes, segSize);
            da.setBytes(bytePos, bytes, segSize);
        }
        return da;
    }

    protected void copyHeader(DataAccess da) {
        for (int h = 0; h < header.length * 4; h += 4) {
            da.setHeader(h, getHeader(h));
        }
    }

    @Override
    public DataAccess create(long bytes) {
        if (segments.length > 0) {
            throw new IllegalStateException("already created");
        }

        // initialize transient values
        setSegmentSize(segmentSizeInBytes);
        ensureCapacity(Math.max(10 * 4, bytes));
        return this;
    }

    @Override
    public boolean isClosed() {
        return false;
    }

    @Override
    public boolean ensureCapacity(long bytes) {
        if (bytes < 0) {
            throw new IllegalArgumentException("new capacity has to be strictly positive");
        }

        long cap = getCapacity();
        long todoBytes = bytes - cap;
        if (todoBytes <= 0) {
            return false;
        }

        int segmentsToCreate = (int)(todoBytes / segmentSizeInBytes);
        if (todoBytes % segmentSizeInBytes != 0) {
            segmentsToCreate++;
        }

        byte[][] newSegs = Arrays.copyOf(segments, segments.length + segmentsToCreate);
        for (int i = segments.length; i < newSegs.length; i++) {
            newSegs[i] = new byte[1 << segmentSizePower];
        }
        segments = newSegs;
        return true;
    }

    @Override
    public void flush() {
        // Do nothing, as we always keep everything in memory
    }

    @Override
    public final void setInt(long bytePos, int value) {
        assert segmentSizePower > 0 : "call create or loadExisting before usage!";
        int bufferIndex = (int)(bytePos >>> segmentSizePower);
        int index = (int)(bytePos & indexDivisor);
        assert index + 4 <= segmentSizeInBytes : "integer cannot be distributed over two segments";
        bitUtil.fromInt(segments[bufferIndex], value, index);
    }

    @Override
    public final int getInt(long bytePos) {
        assert segmentSizePower > 0 : "call create or loadExisting before usage!";
        int bufferIndex = (int)(bytePos >>> segmentSizePower);
        int index = (int)(bytePos & indexDivisor);
        assert index + 4 <= segmentSizeInBytes : "integer cannot be distributed over two segments";
        if (bufferIndex > segments.length) {
            LoggerFactory.getLogger(getClass()).error(
                    getName() + ", segments:" + segments.length + ", bufIndex:" + bufferIndex + ", bytePos:" + bytePos +
                            ", segPower:" + segmentSizePower);
        }
        return bitUtil.toInt(segments[bufferIndex], index);
    }

    @Override
    public final void setShort(long bytePos, short value) {
        assert segmentSizePower > 0 : "call create or loadExisting before usage!";
        int bufferIndex = (int)(bytePos >>> segmentSizePower);
        int index = (int)(bytePos & indexDivisor);
        assert index + 2 <= segmentSizeInBytes : "integer cannot be distributed over two segments";
        bitUtil.fromShort(segments[bufferIndex], value, index);
    }

    @Override
    public final short getShort(long bytePos) {
        assert segmentSizePower > 0 : "call create or loadExisting before usage!";
        int bufferIndex = (int)(bytePos >>> segmentSizePower);
        int index = (int)(bytePos & indexDivisor);
        assert index + 2 <= segmentSizeInBytes : "integer cannot be distributed over two segments";
        return bitUtil.toShort(segments[bufferIndex], index);
    }

    @Override
    public void setBytes(long bytePos, byte[] values, int length) {
        assert segmentSizePower > 0 : "call create or loadExisting before usage!";
        int sourceIndex = 0;
        while (length > 0) {
            int index = (int)(bytePos & indexDivisor);
            int bufferIndex = (int)(bytePos >>> segmentSizePower);
            byte[] seg = segments[bufferIndex];
            int localLength = Math.min(length, segmentSizeInBytes - index);
            System.arraycopy(values, sourceIndex, seg, index, localLength);
            bytePos += localLength;
            sourceIndex += localLength;
            length -= localLength;
        }
    }

    @Override
    public void getBytes(long bytePos, byte[] values, int length) {
        assert segmentSizePower > 0 : "call create or loadExisting before usage!";
        int bufferIndex = (int)(bytePos >>> segmentSizePower);
        int targetIndex = 0;
        while (length > 0) {
            int index = (int)(bytePos & indexDivisor);
            byte[] seg = segments[bufferIndex];
            int localLength = Math.min(length, segmentSizeInBytes - index);
            System.arraycopy(seg, index, values, targetIndex, localLength);
            bytePos += localLength;
            targetIndex += localLength;
            length -= localLength;
        }
    }

    @Override
    public void close() {
        segments = new byte[0][];
    }

    @Override
    public long getCapacity() {
        return (long)getSegments() * segmentSizeInBytes;
    }

    @Override
    public int getSegments() {
        return segments.length;
    }

    @Override
    public void trimTo(long capacity) {
        if (capacity > getCapacity()) {
            throw new IllegalStateException("Cannot increase capacity (" + getCapacity() + ") to " + capacity +
                    " via trimTo. Use ensureCapacity instead. ");
        }

        if (capacity < segmentSizeInBytes) {
            capacity = segmentSizeInBytes;
        }

        int remainingSegments = (int)(capacity / segmentSizeInBytes);
        if (capacity % segmentSizeInBytes != 0) {
            remainingSegments++;
        }

        segments = Arrays.copyOf(segments, remainingSegments);
    }

    @Override
    public void rename(String newName) {
        if (directory.dataAccessMap.containsKey(newName)) {
            throw new IllegalArgumentException("File " + newName + " already exists");
        }
        directory.dataAccessMap.remove(name);
        name = newName;
        directory.dataAccessMap.put(newName, this);
    }

    @Override
    public DAType getType() {
        return DAType.RAM;
    }

    @Override
    public boolean loadExisting() {
        // TODO: implement loading from image, obtained from server
        return true;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setHeader(int bytePos, int value) {
        bytePos >>= 2;
        header[bytePos] = value;
    }

    @Override
    public int getHeader(int bytePos) {
        bytePos >>= 2;
        return header[bytePos];
    }

    @Override
    public DataAccess setSegmentSize(int bytes) {
        if (bytes > 0) {
            // segment size should be a power of 2
            int tmp = (int)(Math.log(bytes) / Math.log(2));
            segmentSizeInBytes = Math.max((int)Math.pow(2, tmp), SEGMENT_SIZE_MIN);
        }
        segmentSizePower = (int)(Math.log(segmentSizeInBytes) / Math.log(2));
        indexDivisor = segmentSizeInBytes - 1;
        return this;
    }

    @Override
    public int getSegmentSize() {
        return segmentSizeInBytes;
    }
}
