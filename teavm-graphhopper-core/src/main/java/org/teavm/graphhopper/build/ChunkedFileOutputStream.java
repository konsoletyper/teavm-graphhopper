package org.teavm.graphhopper.build;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 *
 * @author Alexey Andreev
 */
public class ChunkedFileOutputStream extends OutputStream {
    private File baseDir;
    private int chunkSize;
    private FileOutputStream currentFileOutput;
    private int chunkCount;
    private int position;
    private int totalBytes;

    public ChunkedFileOutputStream(File baseDir, int chunkSize) {
        this.baseDir = baseDir;
        this.chunkSize = chunkSize;
        position = chunkSize;
    }

    @Override
    public void write(int b) throws IOException {
        if (position == chunkSize) {
            nextFile();
        }
        currentFileOutput.write(b);
        ++position;
        ++totalBytes;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        if (currentFileOutput == null) {
            nextFile();
        }
        while (len > 0) {
            int sz = Math.min(chunkSize - position, len);
            currentFileOutput.write(b, off, sz);
            off += sz;
            len -= sz;
            position += sz;
            totalBytes += sz;
            if (len > 0) {
                nextFile();
            }
        }
    }

    @Override
    public void flush() throws IOException {
        if (currentFileOutput != null) {
            currentFileOutput.flush();
        }
        super.flush();
    }

    @Override
    public void close() throws IOException {
        if (currentFileOutput != null) {
            currentFileOutput.close();
        }
        super.close();
    }

    private void nextFile() throws IOException {
        if (currentFileOutput != null) {
            currentFileOutput.close();
        }
        currentFileOutput = new FileOutputStream(new File(baseDir, "chunk" + chunkCount++ + ".bin"));
        position = 0;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public int getTotalBytes() {
        return totalBytes;
    }
}
