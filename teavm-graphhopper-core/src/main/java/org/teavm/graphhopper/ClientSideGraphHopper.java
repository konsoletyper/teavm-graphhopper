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

import java.util.Arrays;
import com.graphhopper.routing.AlgorithmOptions;
import com.graphhopper.routing.Path;
import com.graphhopper.routing.RoutingAlgorithm;
import com.graphhopper.routing.ch.PrepareContractionHierarchies;
import com.graphhopper.routing.util.CarFlagEncoder;
import com.graphhopper.routing.util.EncodingManager;
import com.graphhopper.routing.util.FastestWeighting;
import com.graphhopper.routing.util.FlagEncoder;
import com.graphhopper.routing.util.TraversalMode;
import com.graphhopper.routing.util.Weighting;
import com.graphhopper.storage.CHGraph;
import com.graphhopper.storage.Directory;
import com.graphhopper.storage.GraphExtension.NoOpExtension;
import com.graphhopper.storage.GraphHopperStorage;
import com.graphhopper.storage.index.LocationIndexTree;
import com.graphhopper.util.shapes.BBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alexey Andreev
 */
public class ClientSideGraphHopper {
    private static final Logger logger = LoggerFactory.getLogger(ClientSideGraphHopper.class);
    private GraphHopperStorage graph;
    private EncodingManager encodingManager;
    private LocationIndexTree locationIndex;
    private FlagEncoder encoder;
    private Weighting weighting;
    private PrepareContractionHierarchies prepare;

    public ClientSideGraphHopper(Directory directory) {
        if (logger.isInfoEnabled()) {
            logger.info("Initializing GraphHopper");
        }
        long start = System.currentTimeMillis();
        encoder = new CarFlagEncoder();
        encodingManager = new EncodingManager(encoder);
        weighting = new FastestWeighting(encoder);
        graph = new GraphHopperStorage(Arrays.asList(weighting), directory, encodingManager, true,
                new NoOpExtension());
        graph.loadExisting();

        locationIndex = new LocationIndexTree(graph, directory);
        locationIndex.loadExisting();

        prepare = new PrepareContractionHierarchies(directory, graph, graph.getGraph(CHGraph.class),
                encoder, weighting, TraversalMode.NODE_BASED);

        if (logger.isInfoEnabled()) {
            logger.info("GraphHopper initialized in {}ms", System.currentTimeMillis() - start);
        }
    }

    public BBox getBounds() {
        return graph.getBounds();
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
