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

import com.graphhopper.routing.Path;
import org.teavm.jso.JS;
import org.teavm.jso.JSArray;

/**
 *
 * @author Alexey Andreev
 */
public class Client {
    public static void main(String[] args) {
        JSArray<DataEntry> array = JS.createArray(100);

        ClientSideGraphHopper graphHopper = new ClientSideGraphHopper();
        graphHopper.load(array);

        double startLat = 37.418703;
        double startLon = 55.747844;
        double endLat = 37.70859;
        double endLon = 55.784829;

        int start = graphHopper.findNode(startLat, startLon);
        int end = graphHopper.findNode(endLat, endLon);

        Path path = graphHopper.route(start, end);
        System.out.println(path.getDistance());
    }
}
