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

import com.graphhopper.storage.DAType;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.Directory;
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
}
