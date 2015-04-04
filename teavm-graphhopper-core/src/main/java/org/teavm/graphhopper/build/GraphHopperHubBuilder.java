package org.teavm.graphhopper.build;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Alexey Andreev
 */
public class GraphHopperHubBuilder {
    private static final Logger logger = LoggerFactory.getLogger(GraphHopperHubBuilder.class);
    private File baseDir = new File(".");
    private String urlPrefix = "http://localhost:8080/maps";
    private int chunkSize = 65536;
    private ObjectMapper mapper = new ObjectMapper();

    public File getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(File baseDir) {
        this.baseDir = baseDir;
    }

    public String getUrlPrefix() {
        return urlPrefix;
    }

    public void setUrlPrefix(String urlPrefix) {
        this.urlPrefix = urlPrefix;
    }

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public void build(GraphHopperInputHub inputHub, File buildLocation) throws IOException {
        GraphHopperHub outputHub = new GraphHopperHub();
        int index = 0;
        for (GraphHopperInputMap map : inputHub.getMaps()) {
            if (logger.isInfoEnabled()) {
                logger.info("Building map {} ({} of {})", map.getId(), ++index, inputHub.getMaps().size());
            }
            GraphHopperMap outputMap = new GraphHopperMap(map.getId());
            File ghLocation = new File(baseDir, "tmp-gh/" + map.getId());
            GraphHopperFileBuilder fileBuilder = new GraphHopperFileBuilder(ghLocation.getAbsolutePath());
            File relativeOsmFile = new File(map.getOsmFileName());
            File osmFile = relativeOsmFile.isAbsolute() ? relativeOsmFile : new File(baseDir, map.getOsmFileName());
            File mapDir = new File(buildLocation, map.getId());
            mapDir.mkdirs();
            try (ChunkedFileOutputStream chunkedOut = new ChunkedFileOutputStream(mapDir, chunkSize);
                    OutputStream output = new BufferedOutputStream(chunkedOut)) {
                fileBuilder.build(osmFile.getAbsolutePath(), output);
                outputMap.setChunkCount(chunkedOut.getChunkCount());
                outputMap.setSizeInBytes(chunkedOut.getTotalBytes());
            }
            StringBuilder url = new StringBuilder(urlPrefix);
            if (!urlPrefix.endsWith("/")) {
                url.append('/');
            }
            url.append(map.getId());
            outputMap.setBaseUrl(url.toString());
            outputMap.setName(map.getName());
            outputMap.setLastModified(new Date(osmFile.lastModified()));
            outputMap.setWest(fileBuilder.getBounds().minLon);
            outputMap.setEast(fileBuilder.getBounds().maxLon);
            outputMap.setNorth(fileBuilder.getBounds().maxLat);
            outputMap.setSouth(fileBuilder.getBounds().minLat);
            outputHub.getMaps().add(outputMap);
        }

        mapper.writer().writeValue(new File(buildLocation, "graphhopper.json"), outputHub);
    }

    @SuppressWarnings("static-access")
    public static void main(String[] args) {
        Options options = new Options();
        options.addOption(OptionBuilder
                .hasArg()
                .withArgName("directory")
                .withLongOpt("base-dir")
                .withDescription("directory where GH build should get OSM files")
                .create('b'));
        options.addOption(OptionBuilder
                .hasArg()
                .withArgName("directory")
                .withLongOpt("output-dir")
                .create('o'));
        options.addOption(OptionBuilder
                .hasArg()
                .withArgName("url")
                .withLongOpt("url-prefix")
                .withDescription("directory where GH build should put generated files")
                .create('u'));
        options.addOption(OptionBuilder
                .hasArg()
                .withArgName("number")
                .withLongOpt("chunk-size")
                .withDescription("size of one data chunk (65536 by default)")
                .create());

        PosixParser parser = new PosixParser();
        CommandLine cmdline;
        try {
            cmdline = parser.parse(options, args);
        } catch (ParseException e) {
            printUsage(options);
            System.exit(1);
            return;
        }

        args = cmdline.getArgs();
        if (args.length != 1) {
            System.err.println("input JSON file was not specified");
            printUsage(options);
            System.exit(1);
            return;
        }

        GraphHopperHubBuilder builder = new GraphHopperHubBuilder();
        if (cmdline.hasOption('b')) {
            builder.setBaseDir(new File(cmdline.getOptionValue('b')));
        }
        File outputDir = new File(".");
        if (cmdline.hasOption('o')) {
            outputDir = new File(cmdline.getOptionValue('o'));
        }
        if (cmdline.hasOption('u')) {
            builder.setUrlPrefix(cmdline.getOptionValue('u'));
        }
        if (cmdline.hasOption("chunk-size")) {
            try {
                builder.setChunkSize(Integer.parseInt(cmdline.getOptionValue("chunk-size")));
            } catch (NumberFormatException e) {
                System.err.println("Chunk size must be integer number");
                printUsage(options);
                System.exit(1);
                return;
            }
        }

        File inputFile = new File(args[0]);
        if (!inputFile.isAbsolute()) {
            inputFile = new File(builder.getBaseDir(), inputFile.getPath());
        }
        if (!inputFile.exists()) {
            System.err.println("File not found: " + inputFile.getAbsolutePath());
            System.exit(1);
            return;
        }

        ObjectMapper mapper = new ObjectMapper();
        GraphHopperInputHub hub;
        try {
            hub = mapper.readValue(inputFile, GraphHopperInputHub.class);
        } catch (JsonParseException e) {
            System.err.println("Error parsing input file: " + inputFile.getAbsolutePath());
            e.printStackTrace();
            System.exit(1);
            return;
        } catch (JsonMappingException e) {
            System.err.println("Error mapping input file: " + inputFile.getAbsolutePath());
            e.printStackTrace();
            System.exit(1);
            return;
        } catch (IOException e) {
            System.err.println("IO exception occured reading input file: " + inputFile.getAbsolutePath());
            e.printStackTrace();
            System.exit(1);
            return;
        }

        long start = System.currentTimeMillis();
        try {
            builder.build(hub, outputDir);
        } catch (IOException e) {
            System.err.println("IO exception occured writing output files");
            e.printStackTrace();
            System.exit(1);
            return;
        }

        System.out.println("Build complete in " + (System.currentTimeMillis() - start) / 1000.0 + " seconds");
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java org.teavm.graphhopper.build.GraphHopperHubBuilder [options] input-file", options);
    }
}
