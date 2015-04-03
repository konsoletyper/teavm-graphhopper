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
package org.teavm.graphhopper.storage;

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
import java.io.DataInput;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author Alexey Andreev
 */
public class InMemoryDirectory implements Directory {
    Map<String, InMemoryDataAccess> dataAccessMap = new HashMap<>();

    @Override
    public String getLocation() {
        return "memory";
    }

    @Override
    public ByteOrder getByteOrder() {
        return ByteOrder.LITTLE_ENDIAN;
    }

    @Override
    public DataAccess find(String name) {
        InMemoryDataAccess dataAccess = dataAccessMap.get(name);
        if (dataAccess == null) {
            dataAccess = new InMemoryDataAccess(this, name, getByteOrder());
            dataAccessMap.put(name, dataAccess);
        }
        return dataAccess;
    }

    @Override
    public DataAccess find(String name, DAType type) {
        return find(name);
    }

    @Override
    public void clear() {
        dataAccessMap.clear();
    }

    @Override
    public void remove(DataAccess da) {
        dataAccessMap.remove(da.getName());
    }

    @Override
    public DAType getDefaultType() {
        return DAType.RAM;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Collection<DataAccess> getAll() {
        return (Collection<DataAccess>)(Collection<?>)dataAccessMap.values();
    }

    public void load(DataInput input) throws IOException {
        short entryCount = input.readShort();
        for (int i = 0; i < entryCount; ++i) {
            String entryName = input.readUTF();
            DataAccess file = find(entryName);
            file.setSegmentSize(input.readInt());
            long length = input.readLong();
            file.create(length);
            byte[] buffer = new byte[(file.getSegmentSize() >> 2) << 2];
            for (long pos = 0; pos < length; pos += buffer.length) {
                int bytesToRead = (int)Math.min(length - pos, buffer.length);
                input.readFully(buffer, 0, bytesToRead);
                for (int j = 0; j < bytesToRead; j += 4) {
                    byte tmp = buffer[j];
                    buffer[j] = buffer[j + 3];
                    buffer[j + 3] = tmp;
                    tmp = buffer[j + 1];
                    buffer[j + 1] = buffer[j + 2];
                    buffer[j + 2] = tmp;
                }
                file.setBytes(pos, buffer, bytesToRead);
            }
            int headerLength = input.readInt();
            int[] header = new int[headerLength];
            for (int j = 0; j < header.length; ++j) {
                file.setHeader(j * 4, input.readInt());
            }
        }
    }
}
