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
import manhattan.external.TauPPhaseName;

/**
 * Information for {@link manhattan.timewindow.TimewindowMaker}
 * 
 * @since 2013/10/3
 * @version 0.0.2
 * 
 * @version 0.0.3
 * @since 2014/8/19 Constructors changed
 * 
 * @version 0.0.4
 * @since 2014/9/7 to Java 8
 * 
 * @version 0.0.5
 * @since 2014/9/11 {@link #checkElements()} installed.
 *
 * @version 0.0.6
 * @since 2014/10/13 outputList modified.
 * 
 * @version 0.0.7
 * @since 2015/1/23 output names are now automatically determined.
 * 
 * @version 0.0.8
 * @since 2015/2/23 No more taup path setting
 * 
 * @version 0.0.8.1
 * @since 2015/4/2
 * 
 * @version 0.0.8.2
 * @since 2015/8/6 {@link IOException}
 * 
 * @version 0.0.9
 * @since 2015/8/8 {@link Path} base
 * 
 * @version 0.0.9.1
 * @since 2015/8/13
 * 
 * @author Kensuke
 * 
 */
public class TimewindowMaker extends ParameterFile {

	protected TimewindowMaker(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	/**
	 * parameterのセット
	 */
	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		// outputFile = getFile("outputFile");
		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		str = reader.getStringArray("exPhases");

		if (str != null) // TODO
			exPhases = Arrays.stream(str).map(TauPPhaseName::valueOf).collect(Collectors.toSet());

		str = reader.getStringArray("usePhases");
		usePhases = Arrays.stream(str).map(TauPPhaseName::valueOf).collect(Collectors.toSet());

		frontShift = reader.getDouble("frontShift");
		rearShift = reader.getDouble("rearShift");

	}

	/**
	 * set of {@link SACComponent}
	 */
	protected Set<SACComponent> components;

	/**
	 * how many seconds it shifts the starting time [s] phase到着からどれだけずらすか if the
	 * value is 5(not -5), then basically, each timewindow starts 5 sec before
	 * each usePhase
	 */
	protected double frontShift;

	/**
	 * phase到着から後ろ何秒を取るか if the value is 10, basically, each timewindow ends 10
	 * secs after each usephase arrival
	 */
	protected double rearShift;

	/**
	 * 省きたいフェーズ
	 */
	protected Set<TauPPhaseName> exPhases;

	/**
	 * 使いたいフェーズ
	 */
	protected Set<TauPPhaseName> usePhases;

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException,InterruptedException {
		Path tmp = null;
		if (0 < args.length)
			tmp = Paths.get(args[0]).toAbsolutePath();
		else
			tmp = readFileName();

		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

	/**
	 * Output a default parameter list
	 * @param outPath Path of an output file
	 * @param options for output
	 * @throws IOException if an I/O error occurs
	 */
	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
			pw.println("#!java manhattan.timewindow.TimewindowMaker");
			pw.println("#SacComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a working folder (full path required)");
			pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			pw.println("#TauPPhase[] exPhases");
			pw.println("exPhases sSdiff sS");
			pw.println("#TauPPhase[] usePhases");
			pw.println("usePhases S ScS");
			pw.println("#double time before first phase. 10 then 10 s before arrival");
			pw.println("frontShift 10");
			pw.println("#double time after last phase. 60 then 60 s after arrival");
			pw.println("rearShift 60");

		}
		setExecutable(outPath);
	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("components");
		parameterSet.add("frontShift");
		parameterSet.add("rearShift");
		// parameterSet.add("exPhases");
		parameterSet.add("usePhases");
		// parameterSet.add("marking");
		return reader.containsAll(parameterSet);
	}

}
