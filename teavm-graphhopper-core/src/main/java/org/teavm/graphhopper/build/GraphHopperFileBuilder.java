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
package org.teavm.graphhopper.build;

import com.graphhopper.GraphHopper;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.GHDirectory;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.util.shapes.BBox;
import java.io.BufferedOutputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperFileBuilder {
    private String folderName;
    private BBox bounds;

    public GraphHopperFileBuilder() {
        this("gh-folder");
    }

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
                .setElevation(true)
                .setEncodingManager(new EncodingManager(new CarFlagEncoder()));
        gh.importOrLoad();

        GraphHopperStorage graph = gh.getGraphHopperStorage();
        GHDirectory dir = (GHDirectory)graph.getDirectory();
        output.writeShort(dir.getAll().size());
        for (DataAccess da : dir.getAll()) {
            output.writeUTF(da.getName());
            output.writeInt(da.getSegmentSize());

            long length = da.getSegmentSize() * (long)da.getSegments();
            output.writeLong(length);
            for (long pos = 0; pos < length; pos += 4) {
                output.writeInt(da.getInt(pos));
            }

            output.writeInt(20);
            for (int i = 0; i < 20; ++i) {
                output.writeInt(da.getHeader(i * 4));
            }
        }

        bounds = graph.getBounds();
    }

    public BBox getBounds() {
        return bounds;
    }

    public static void main(String[] args) throws IOException {
        try (OutputStream output = new BufferedOutputStream(new FileOutputStream(args[1], false))) {
            new GraphHopperFileBuilder().build(args[0], output);
        }
    }
}
