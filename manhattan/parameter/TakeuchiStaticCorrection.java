package parameter;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import filehandling.sac.SACComponent;

/**
 * Parameter file for {@link manhattan.datacorrection.TakeuchiStaticCorrection}
 * 
 * @author kensuke
 * @since 2015/08/07
 * @version 0.0.1
 * 
 * @version 0.0.2
 * @since 2015/8/14 {@link IOException} {@link Path}base
 * 
 * 
 */
public class TakeuchiStaticCorrection extends ParameterFile {

	/**
	 * components for computation
	 */
	protected Set<SACComponent> components;

	/**
	 * コンボリューションされている波形かそうでないか （両方は無理）
	 */
	protected boolean isConvolved;

	/**
	 * {@link Path} for a root directory containing observed data
	 */
	protected Path obsPath;

	/**
	 * sampling Hz [Hz] in sac files
	 */
	protected double sacSamplingHz;
	/**
	 * {@link Path} for a root directory containing synthetic data
	 */
	protected Path synPath;
	protected Path timeWindowInformationPath;

	protected TakeuchiStaticCorrection(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Path tmp = null;
		if (0 < args.length)
			tmp = Paths.get(args[0]).toAbsolutePath();
		else
			tmp = readFileName();

		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

	public static void outputList(Path outPath, OpenOption... options) throws IOException {

		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.datacorrection.TakeuchiStaticCorrection");
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a working directory (full path required!)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#Path of a root directory containing observed data");
			pw.println("obsPath . obs");
			pw.println("#Path of a root directory containing synthetic data");
			pw.println("synPath . syn");
			pw.println("#Path of a file for timeWindow information");
			pw.println("timeWindowInformationPath timewindow.dat");
			pw.println("#boolean isConvolved");
			pw.println("isConvolved true");
			pw.println("#double sacSamplingHz");
			pw.println("#sacSamplingHz cant change now");

		}
		setExecutable(outPath);
	}

	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		synPath = getPath("synPath");
		obsPath = getPath("obsPath");
		timeWindowInformationPath = getPath("timeWindowInformationPath");

		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		isConvolved = Boolean.parseBoolean(reader.getString("isConvolved"));
		// sacSamplingHz
		// =Double.parseDouble(reader.getFirstValue("sacSamplingHz")); TODO
		sacSamplingHz = 20;
	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("components");
		parameterSet.add("isConvolved");
		parameterSet.add("timeWindowInformationPath");
		parameterSet.add("synPath");
		parameterSet.add("obsPath");
		// parameterSet.add("sacSamplingHz"); //TODO
		return reader.containsAll(parameterSet);

	}
}
