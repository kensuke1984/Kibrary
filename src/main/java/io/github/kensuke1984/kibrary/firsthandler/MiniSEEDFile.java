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
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

/**
 * Seed file utility rdseed must be in PATH.
 * 
 * @version 0.0.8.2
 * 
 * @author Yuki Suzuki
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/rdseed/>download</a>
 * @see <a href=https://ds.iris.edu/ds/nodes/dmc/manuals/rdseed/>manual</a>
 */
public class MiniSEEDFile {

	static {
		if (!ExternalProcess.isInPath("ms2sac"))
			throw new RuntimeException("ms2sac is not in PATH.");
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
			throw new RuntimeException("Usage: [mseed file name]");
		MiniSEEDFile mseed = new MiniSEEDFile(Paths.get(args[0]));
		GlobalCMTSearch sc = new GlobalCMTSearch(mseed.startingDate, mseed.endingDate);
		sc.search().forEach(System.out::println);
	}

	@Override
	public String toString() {
		return miniSeedPath.toString();
	}

	public Path getSeedPath() {
		return miniSeedPath;
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
	 * a miniSEED file
	 */
	private Path miniSeedPath;

	public MiniSEEDFile(Path seedPath) throws IOException {
		this.miniSeedPath = seedPath;
		// searchRdseed();
//		readVolumeHeader();
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
	 * ms2sac から読み込める情報を読み取る。
	 * 
	 * @throws IOException if any
	 */
	private void readVolumeHeader() throws IOException {
		if (!Files.exists(miniSeedPath))
			throw new NoSuchFileException(miniSeedPath.toString());

		String[] lines = readSeed("");
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
	 * ms2sac $option を行ったときの出力を返す if ms2sac does not exist, it returns empty
	 * array of String
	 * 
	 * @param option
	 *            -n 等
	 * @return output of rdseed$option. (this method never returns null)
	 * @throws IOException if any
	 */
	private String[] readSeed(String option) throws IOException {
		List<String> commands = new ArrayList<>();
		Path outSacPath = Paths.get(miniSeedPath.toString()+".SAC");
		commands.add("ms2sac");
		String[] parts = option.split("\\s+");
		commands.addAll(Arrays.asList(parts));
		commands.add(miniSeedPath.toString());
		commands.add(outSacPath.toString());
		ExternalProcess process = ExternalProcess.launch(commands);
		int exitStatus = process.waitFor();
		if (exitStatus == 1 || exitStatus == 255)
			throw new RuntimeException("ms2sac did not run correctly. " + exitStatus + " " + commands);
		return process.getStandardOutput().waitAndGetString();
	}

	/**
	 * Run ms2sac -q output -fRd $seedFile<br>
	 * 
	 * @param outputPath
	 *            rdseed の出力先 if this is relative path, then the root is the
	 *            directory of the seed file
	 * @throws IOException if any
	 */
	void extract(Path outputPath, GlobalCMTID id) throws IOException {
		if (Files.exists(outputPath))
			Files.createDirectories(outputPath);
		System.out.println("Extracting miniSeedFile: " + miniSeedPath);
		readSeed("-n "+ id.toString());
	}

	/**
	 * 移動後 {@link MiniSEEDFile#seedPath}も変更する
	 * 
	 * @param directory
	 *            if it does not exist, it will be created
	 * @return seedFile を directoryに持っていけたらtrue
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public boolean toDirectory(Path directory) throws IOException {
		Files.move(miniSeedPath, directory.resolve(miniSeedPath.getFileName()));
		miniSeedPath = directory.resolve(miniSeedPath.getFileName());
		return true;
	}

}
