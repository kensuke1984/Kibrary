package parameter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

/**
 * {@link manhattan.inversion.Answer}information
 * 
 * @author kensuke
 * @since 2014/08/19
 * @version 0.0.2
 * 
 * @version 0.0.3
 * @since 2014/9/7 to Java 8
 * 
 * @since 2014/9/11
 * @version 0.0.4 {@link #checkElements()} installed.
 * 
 * @version 0.0.4.1
 * @since 2015/8/7 {@link IOException}
 * 
 * @version 0.0.5
 * @since 2015/8/8 {@link Path} base
 * 
 * @version 0.0.5.1
 * @since 2015/8/13
 * 
 */
public class Answer extends ParameterFile {

	protected Answer(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		// this.parameterFile = parameterFile;
		// reader = new ParameterReader(this.parameterFile);
		set();
	}

	protected Path answerPath;
	protected Path unknownParameterSetPath;
	protected Path outPath;

	protected Path crossSectionListPath;
	protected Path gridPath;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if an 	I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Path tmp = null;
		String s = null;
		if (0< args.length ) {
			s = args[0];
			tmp = Paths.get(s).toAbsolutePath();
		} else
			tmp = readFileName();

		outputList(tmp);
	}

	public static void outputList(Path outPath) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("#!java manhattan.inversion.Answer");
			pw.println("#Path of a working folder (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#Path of a output folder");
			pw.println("outPath answerOut");
			pw.println("#Path of an answer file");
			pw.println("answerPath answer.dat");
			pw.println("#Path of an unknownset file");
			pw.println("unknownParameterSetPath unknown.inf");

			pw.println("#Path of a crossSectionList file");
			pw.println("crossSectionListPath crossSection.inf");
			pw.println("#Path of a grid file");
			pw.println("gridPath grid.inf");

		}

		setExecutable(outPath);
	}

	/**
	 * parameterのセット
	 */
	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		answerPath = getPath("answerPath");
		outPath = getPath("outPath");
		unknownParameterSetPath = getPath("unknownParameterSetPath");
		// String crossSection = reader.getString("crossSectionListFile");
		// String grid = reader.getString("gridFile");
		// if (crossSection != null)
		crossSectionListPath = getPath("crossSectionListPath");
		// if (grid != null)
		gridPath = getPath("gridPath");

	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("answerPath");
		parameterSet.add("unknownParameterSetPath");
		parameterSet.add("outPath");
		return reader.containsAll(parameterSet);
	}
}
