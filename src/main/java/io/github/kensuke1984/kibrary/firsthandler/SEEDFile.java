package io.github.kensuke1984.kibrary.firsthandler;

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

import io.github.kensuke1984.kibrary.external.ExternalProcess;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

/**
 * Seed file utility rdseed must be in PATH.
 * 
 * @version 0.0.8.2
 * 
 * @author Kensuke Konishi
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/rdseed/>download</a>
 * @see <a href=https://ds.iris.edu/ds/nodes/dmc/manuals/rdseed/>manual</a>
 */
public class SEEDFile {

	static {
		if (!ExternalProcess.isInPath("rdseed"))
			throw new RuntimeException("rdseed is not in PATH.");
	}

	/**
	 * Displays Global CMT IDs which might be contained in the seedfile
	 * 
	 * @param args
	 *            [seed file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		if (args.length != 1)
			throw new RuntimeException("Usage: [seed file name]");
		SEEDFile seed = new SEEDFile(Paths.get(args[0]));
		GlobalCMTSearch sc = new GlobalCMTSearch(seed.startingDate, seed.endingDate);
		sc.search().forEach(System.out::println);
	}

	@Override
	public String toString() {
		return seedPath.toString();
	}

	public Path getSeedPath() {
		return seedPath;
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
	 * a SEED file
	 */
	private Path seedPath;

	public SEEDFile(Path seedPath) throws IOException {
		this.seedPath = seedPath;
		// searchRdseed();
		readVolumeHeader();
	}

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
	 * rdseedから出力される 日付のフォーマット 例） 1993,052,07:01:12.4000
	 * 
	 */
	private static final Pattern datePattern = Pattern
			.compile("(\\d\\d\\d\\d),(\\d\\d\\d),(\\d\\d):(\\d\\d):(\\d\\d).(\\d\\d\\d\\d)");

	/**
	 * rdseed -cf seedFile から読み込める情報を読み取る。
	 * 
	 * @throws IOException if any
	 */
	private void readVolumeHeader() throws IOException {
		if (!Files.exists(seedPath))
			throw new NoSuchFileException(seedPath.toString());

		String[] lines = readSeed("-cf");
		for (String line : lines) {
			if (line.contains("Starting date of this volume")) {
				Matcher m = datePattern.matcher(line);
				m.find();
				String dateString = m.group();
				startingDate = toLocalDateTime(dateString);
			} else if (line.contains("Ending date of this volume")) {
				Matcher m = datePattern.matcher(line);
				m.find();
				String dateString = m.group();
				endingDate = toLocalDateTime(dateString);
			} else if (line.contains("Creation Date of this volume")) {
				Matcher m = datePattern.matcher(line);
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

	private static DateTimeFormatter headerFormat = DateTimeFormatter.ofPattern("yyyy,DDD,HH:mm:ss.SSSS");

	/**
	 * 
	 * @param dateString
	 *            YYYY,DDD,HH:MM:
	 * @return time for the dateString
	 */
	private static LocalDateTime toLocalDateTime(String dateString) {
		return LocalDateTime.parse(dateString, headerFormat);
	}

	/**
	 * rdseed $option を行ったときの出力を返す if rdseed does not exist, it returns empty
	 * array of String
	 * 
	 * @param option
	 *            -cf 等
	 * @return output of rdseed$option. (this method never returns null)
	 * @throws IOException if any
	 */
	private String[] readSeed(String option) throws IOException {
		List<String> commands = new ArrayList<>();
		commands.add("rdseed");
		String[] parts = option.split("\\s+");
		commands.addAll(Arrays.asList(parts));
		commands.add(seedPath.toString());
		ExternalProcess process = ExternalProcess.launch(commands);
		int exitStatus = process.waitFor();
		if (exitStatus == 1 || exitStatus == 255)
			throw new RuntimeException("rdseed did not run correctly. " + exitStatus + " " + commands);
		return process.getStandardOutput().waitAndGetString();
	}

	/**
	 * Run rdseed -q output -fRd $seedFile<br>
	 * 
	 * @param outputPath
	 *            rdseed の出力先 if this is relative path, then the root is the
	 *            directory of the seed file
	 * @throws IOException if any
	 */
	void extract(Path outputPath) throws IOException {
		if (Files.exists(outputPath))
			Files.createDirectories(outputPath);
		System.out.println("Extracting seedFile: " + seedPath);
		readSeed("-q " + outputPath + " -fRd");
	}

	/**
	 * 移動後 {@link SEEDFile#seedPath}も変更する
	 * 
	 * @param directory
	 *            if it does not exist, it will be created
	 * @return seedFile を directoryに持っていけたらtrue
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public boolean toDirectory(Path directory) throws IOException {
		Files.move(seedPath, directory.resolve(seedPath.getFileName()));
		seedPath = directory.resolve(seedPath.getFileName());
		return true;
	}

}
