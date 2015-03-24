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

import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.routing.util.*;
import com.graphhopper.storage.DataAccess;
import com.graphhopper.storage.LevelGraphStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.shapes.BBox;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alexey Andreev
 */
public class ClientSideGraphHopper {
    private static final Logger logger = LoggerFactory.getLogger(ClientSideGraphHopper.class);
    private InMemoryDirectory directory = new InMemoryDirectory();
    private LevelGraphStorage graph;
    private EncodingManager encodingManager;
    private LocationIndexTree locationIndex;
    private FlagEncoder encoder;
    private Weighting weighting;
    private PrepareContractionHierarchies prepare;

    public void load(InputStream input) throws IOException {
        if (logger.isInfoEnabled()) {
            logger.info("Loading GraphGopper directory");
        }
        long start = System.currentTimeMillis();
        loadStorage(new DataInputStream(input));
        if (logger.isInfoEnabled()) {
            logger.info("GraphHopper directory loaded in {}ms", System.currentTimeMillis() - start);
        }

        if (logger.isInfoEnabled()) {
            logger.info("Initializing GraphHopper");
        }
        start = System.currentTimeMillis();
        encodingManager = new EncodingManager(new CarFlagEncoder());
        graph = new LevelGraphStorage(directory, encodingManager, true);
        encoder = new CarFlagEncoder();
        graph.loadExisting();

        locationIndex = new LocationIndexTree(graph, directory);
        locationIndex.loadExisting();

        weighting = new FastestWeighting(encoder);
        prepare = new PrepareContractionHierarchies(directory, graph, encoder, weighting,
                TraversalMode.EDGE_BASED_2DIR);

        if (logger.isInfoEnabled()) {
            logger.info("GraphHopper initialized in {}ms", System.currentTimeMillis() - start);
        }
    }

    public BBox getBounds() {
        return graph.getBounds();
    }

    private void loadStorage(DataInput input) throws IOException {
        short entryCount = input.readShort();
        for (int i = 0; i < entryCount; ++i) {
            String entryName = input.readUTF();
            DataAccess file = directory.find(entryName);
            file.setSegmentSize(input.readInt());
            long length = input.readLong();
            file.create(length);
            byte[] buffer = new byte[4096];
            for (long pos = 0; pos < length; pos += buffer.length) {
                int chunkSize = (int)Math.min(buffer.length, length - pos);
                input.readFully(buffer, 0, chunkSize);
                file.setBytes(pos, buffer, chunkSize);
            }
            int headerLength = input.readInt();
            int[] header = new int[headerLength];
            for (int j = 0; j < header.length; ++j) {
                file.setHeader(j, input.readInt());
            }
        }
    }

    public int findNode(double lat, double lng) {
        long start = System.currentTimeMillis();
        int result = locationIndex.findID(lat, lng);
        if (logger.isInfoEnabled()) {
            logger.info("Node {} at ({};{}) found in {} ms", result, lat, lng, System.currentTimeMillis() - start);
        }
        return result;
    }

    public Path route(int from, int to) {
        long start = System.currentTimeMillis();
        AlgorithmOptions algOptions = new AlgorithmOptions(AlgorithmOptions.ASTAR_BI, encoder, weighting);
        RoutingAlgorithm algo = prepare.createAlgo(graph, algOptions);
        Path path = algo.calcPath(from, to);
        if (logger.isInfoEnabled()) {
            logger.info("Path from {} to {} found in {} ms. Distance is {}", from, to,
                    System.currentTimeMillis() - start, path.getDistance());
        }
        return path;
    }
}
