package org.teavm.graphhopper.hub;

/**
 *
 * @author Alexey Andreev
 */
public interface GraphHopperMapUploadListener {
    void progress(int bytes);

    void failed(Exception e);

    void complete();
}
