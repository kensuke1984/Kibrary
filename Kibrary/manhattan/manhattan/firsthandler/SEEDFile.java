package manhattan.firsthandler;

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

import manhattan.external.ExternalProcess;
import manhattan.globalcmt.GlobalCMTSearch;

/**
 * Seedファイル
 * 
 * @since 2013/9/19
 * 
 * @version 0.0.1
 * @since 2013/9/19 rdseed 5.3 をベースにしている 今の段階では {@link FirstHandler}
 *        による使用だけを視野に入れている
 * 
 * @version 0.0.2
 * @since 2014/2/3 minor bugs and fixed {@link #readSeed(String)}
 * 
 * @version 0.0.3
 * @since 2014/10/2 rdseed MUST be in PATH.
 * 
 * 
 * @version 0.0.5
 * @since 2015/2/12 {@link java.util.Calendar} &rarr;
 *        {@link java.time.LocalDateTime} time should be of UT.
 * 
 * 
 * @version 0.0.6
 * @since 2015/8/19 {@link Path} base
 * 
 * @version 0.0.7
 * @since 2015/8/21 {@link #main(String[])} installed id findings gets smarter
 * 
 * @author kensuke
 * 
 */
public class SEEDFile {

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
		GlobalCMTSearch sc = new GlobalCMTSearch(seed.startingDate.toLocalDate());
		sc.setStartTime(seed.startingDate.toLocalTime());
		sc.setEndTime(seed.endingDate.toLocalTime());
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
	 * @throws IOException
	 */
	private void readVolumeHeader() throws IOException {
		// System.out.println("rdseed -cf " + seedFile);
		// System.out.println("Reading volume header of "+seedFile);
		if (!Files.exists(seedPath))
			throw new NoSuchFileException(seedPath.toString());

		String[] lines = readSeed("-cf");
		for (int i = 0; i < lines.length; i++) {
			// System.out.println(lines[i]);
			if (lines[i].contains("Starting date of this volume")) {
				Matcher m = datePattern.matcher(lines[i]);
				m.find();
				String dateString = m.group();
				startingDate = toLocalDateTime(dateString);
			} else if (lines[i].contains("Ending date of this volume")) {
				Matcher m = datePattern.matcher(lines[i]);
				m.find();
				String dateString = m.group();
				endingDate = toLocalDateTime(dateString);
			} else if (lines[i].contains("Creation Date of this volume")) {
				Matcher m = datePattern.matcher(lines[i]);
				m.find();
				String dateString = m.group();
				creationDate = toLocalDateTime(dateString);
			} else if (lines[i].contains("Originating Organization")) {
				String[] parts = lines[i].split("\\s+");
				if (parts.length == 2)
					continue;
				StringBuffer buffer = new StringBuffer();
				for (int j = 3; j < parts.length - 1; j++)
					buffer.append(parts[j]);
				buffer.append(parts[parts.length - 1]);
				originatingOrganization = buffer.toString();

			} else if (lines[i].contains("Volume Label")) {
				String[] parts = lines[i].split("\\s+");
				if (parts.length == 2)
					continue;
				StringBuffer buffer = new StringBuffer();
				for (int j = 3; j < parts.length - 1; j++)
					buffer.append(parts[j]);
				buffer.append(parts[parts.length - 1]);
				volumeLabel = buffer.toString();
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
	 * @throws IOException
	 */
	private String[] readSeed(String option) throws IOException {
		List<String> commands = new ArrayList<>();
		commands.add("rdseed");
		String[] parts = option.split("\\s+");
		commands.addAll(Arrays.asList(parts));
		commands.add(seedPath.toString());
		ExternalProcess process = ExternalProcess.launch(commands);
		if (process.waitFor() != 0)
			throw new RuntimeException("rdseed did not run correctly.");
		return process.getStandardOutput().waitAndGetString();
	}

	/**
	 * Run rdseed -q output -fRd $seedFile<br>
	 * 
	 * @param outputPath
	 *            rdseed の出力先 if this is relative path, then the root is the
	 *            directory of the seed file
	 * @throws IOException
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
