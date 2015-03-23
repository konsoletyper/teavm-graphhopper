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

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.GHDirectory;
import java.io.*;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperFileBuilder {
    private String folderName = "gh-folder";

    public GraphHopperFileBuilder(String folderName) {
        this.folderName = folderName;
    }

    public void build(String osmFile, OutputStream output) throws IOException {
        build(osmFile, (DataOutput)new DataOutputStream(output));
    }

    public void build(String osmFile, DataOutput output) throws IOException {
        GraphHopper gh = new GraphHopper()
                .setGraphHopperLocation(folderName)
                .setOSMFile(osmFile)
                .setInMemory()
                .setEncodingManager(new EncodingManager(new CarFlagEncoder()));
        gh.importOrLoad();

        GHDirectory dir = (GHDirectory)gh.getGraph().getDirectory();
        output.writeShort(dir.getAll().size());
        for (DataAccess da : dir.getAll()) {
            output.writeUTF(da.getName());
            output.writeInt(da.getSegmentSize());

            long length = da.getSegmentSize() * (long)da.getSegments();
            output.writeLong(length);
            byte[] buffer = new byte[4096];
            for (long pos = 0; pos < length; pos += buffer.length) {
                int chunkSize = (int)Math.min(buffer.length, length - pos);
                da.getBytes(pos, buffer, chunkSize);
                output.write(buffer, 0, chunkSize);
            }


        }
    }
}
