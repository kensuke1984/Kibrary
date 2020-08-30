package io.github.kensuke1984.kibrary.firsthandler;

import io.github.kensuke1984.kibrary.external.ExternalProcess;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Seed file utility rdseed must be in PATH.
 *
 * @author Kensuke Konishi
 * @version 0.0.9
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/rdseed/>download</a>
 * @see <a href=https://ds.iris.edu/ds/nodes/dmc/manuals/rdseed/>manual</a>
 */
public class SEEDFile {

    /**
     * date format made by rdseed e.g. 1993,052,07:01:12.4000
     */
    private static final Pattern DATE_PATTERN =
            Pattern.compile("(\\d\\d\\d\\d),(\\d\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d).(\\d\\d\\d\\d)");
    private static final DateTimeFormatter HEADER_FORMAT = DateTimeFormatter.ofPattern("yyyy,DDD,HH:mm:ss.SSSS");

    static {
        if (!ExternalProcess.isInPath("rdseed")) throw new RuntimeException("No rdseed in PATH.");
    }

    /**
     * path of a SEED file
     */
    private final Path ORIGNAL_PATH;
    /**
     * B010F05 Starting date of this volume:
     */
    private LocalDateTime startingDate;
    /**
     * B010F06 Ending date of this volume:
     */
    private LocalDateTime endingDate;
    /**
     * B010F07 Creation date of this volume:
     */
    private LocalDateTime creationDate;
    /**
     * B010F08 Originating Organization:
     */
    private String originatingOrganization;
    /**
     * B010F09 Volume Label:
     */
    private String volumeLabel;

    /**
     * temporary symbolic link for shortening path
     */
    private final Path TEMP_LINK;


    public SEEDFile(Path seedPath) throws IOException {
        ORIGNAL_PATH = seedPath;
        TEMP_LINK = Files.createSymbolicLink(Files.createTempDirectory("seed").resolve(seedPath.getFileName()),
                seedPath.toAbsolutePath());
        // searchRdseed();
        readVolumeHeader();
    }

    /**
     * Displays Global CMT IDs which might be contained in the seedfile
     *
     * @param args [seed file name]
     * @throws IOException if an I/O error occurs
     */
    public static void main(String[] args) throws IOException {
        if (args.length != 1) throw new IllegalArgumentException("Usage: [seed file name]");
        SEEDFile seed = new SEEDFile(Paths.get(args[0]));
        GlobalCMTSearch sc = new GlobalCMTSearch(seed.startingDate, seed.endingDate);
        sc.search().forEach(System.out::println);
    }

    /**
     * @param dateString YYYY,DDD,HH:MM:
     * @return time for the dateString
     */
    private static LocalDateTime toLocalDateTime(String dateString) {
        return LocalDateTime.parse(dateString, HEADER_FORMAT);
    }

    @Override
    public String toString() {
        return ORIGNAL_PATH.toString();
    }

    /**
     * @return Path of the seed file.
     */
    public Path getSeedPath() {
        return ORIGNAL_PATH;
    }

    public LocalDateTime getStartingDate() {
        return startingDate;
    }

    public LocalDateTime getEndingDate() {
        return endingDate;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public String getOriginatingOrganization() {
        return originatingOrganization;
    }

    public String getVolumeLabel() {
        return volumeLabel;
    }

    /**
     * Read information by 'rdseed -cf seedFile'
     *
     * @throws IOException if any
     */
    private void readVolumeHeader() throws IOException {
        if (!Files.exists(ORIGNAL_PATH)) throw new NoSuchFileException(ORIGNAL_PATH.toString());
        String[] lines = readSeed("-cf");
        for (String line : lines) {
            if (line.contains("Starting date of this volume")) {
                Matcher m = DATE_PATTERN.matcher(line);
                m.find();
                String dateString = m.group();
                startingDate = toLocalDateTime(dateString);
            } else if (line.contains("Ending date of this volume")) {
                Matcher m = DATE_PATTERN.matcher(line);
                m.find();
                String dateString = m.group();
                endingDate = toLocalDateTime(dateString);
            } else if (line.contains("Creation Date of this volume")) {
                Matcher m = DATE_PATTERN.matcher(line);
                m.find();
                String dateString = m.group();
                creationDate = toLocalDateTime(dateString);
            } else if (line.contains("Originating Organization")) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) continue;
                StringBuilder builder = new StringBuilder();
                for (int j = 3; j < parts.length - 1; j++)
                    builder.append(parts[j]);
                builder.append(parts[parts.length - 1]);
                originatingOrganization = builder.toString();
            } else if (line.contains("Volume Label")) {
                String[] parts = line.split("\\s+");
                if (parts.length == 2) continue;
                StringBuilder builder = new StringBuilder();
                for (int j = 3; j < parts.length - 1; j++)
                    builder.append(parts[j]);
                builder.append(parts[parts.length - 1]);
                volumeLabel = builder.toString();
            }
        }
    }

    /**
     * rdseed $option を行ったときの出力を返す if rdseed does not exist, it returns empty
     * array of String
     *
     * @param option e.g. -cf
     * @return write of rdseed $option. (this method never returns null)
     * @throws IOException if any
     */
    private String[] readSeed(String option) throws IOException {
        List<String> commands = new ArrayList<>();
        commands.add("rdseed");
        String[] parts = option.split("\\s+");
        commands.addAll(Arrays.asList(parts));
        commands.add(TEMP_LINK.toString());
        ExternalProcess process = ExternalProcess.launch(commands);
        int exitStatus = process.waitFor();
        if (exitStatus == 1 || exitStatus == 255)
            throw new RuntimeException("rdseed did not run correctly. " + exitStatus + " " + commands);
        return process.getStandardOutput().waitAndGetString();
    }

    /**
     * Run rdseed -q write -fRd $seedFile<br>
     *
     * @param outputPath for rdseed
     * @throws IOException if any
     */
    void extract(Path outputPath) throws IOException {
        if (Files.exists(outputPath)) Files.createDirectories(outputPath);
        System.err.println("Extracting seedFile: " + ORIGNAL_PATH + " in " + outputPath);
        readSeed("-q " + outputPath + " -fRd");
    }

    /**
     * Creates a symbolic link (absolute path) to the seed file in the directory.
     *
     * @param directory if it does not exist, it will be created
     * @throws IOException if an I/O error occurs
     */
    void createLink(Path directory) throws IOException {
        Files.createSymbolicLink(directory.resolve(ORIGNAL_PATH.getFileName()), ORIGNAL_PATH.toAbsolutePath());
//        Files.move(seedPath, directory.resolve(seedPath.getFileName()));
    }

}
