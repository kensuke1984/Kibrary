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
 * Information for {@link filehandling.spc.SpcSAC}
 * 
 * @version 0.1.3
 * 
 * @author kensuke
 * 
 */
public class SpcSAC extends ParameterFile {

	/**
	 * 計算するcomponents
	 */
	protected Set<SACComponent> components;

	/**
	 * bp, fp フォルダの下のどこにspcファイルがあるか 直下なら何も入れない（""）
	 */
	protected String modelName;

	/**
	 * サンプリングヘルツ 当面は２０Hz固定
	 */
	protected double samplingHz;

	/**
	 * source time function.-1:Users, 0: none, 1: boxcar, 2: triangle
	 */
	protected int sourceTimeFunction;

	/**
	 * psv と sh のカップリングをどうするか 0:both(default), 1:psv, 2:sh
	 */
	protected int psvsh;

	/**
	 * タイムパーシャルを計算するか？
	 */
	protected boolean timePartial;

	protected SpcSAC(Path parameterPath) throws IOException {
		super(parameterPath);
		if (!checkElements())
			throw new RuntimeException();
		set();
	}

	protected Path sourceTimeFunctionPath;

	/**
	 * parameterのセット
	 */
	private void set() {

		workPath = Paths.get(reader.getString("workPath"));
		// eventInfoFile = newFile(reader.getFirstValue("eventInfoFile"));
		String[] str = reader.getStringArray("components"); // SacComponent
		components = Arrays.stream(str).map(SACComponent::valueOf).collect(Collectors.toSet());

		try {
			sourceTimeFunction = Integer.parseInt(reader.getString("sourceTimeFunction"));
		} catch (Exception e) {
			sourceTimeFunction = -1;
			sourceTimeFunctionPath = getPath("sourceTimeFunction");
		}

		timePartial = Boolean.parseBoolean(reader.getString("timePartial"));
		psvsh = reader.getInt("psvsh");

		modelName = reader.getString("modelName");

		samplingHz = 20; // TODO
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
			pw.println("#!java filehandling.spc.SpcSAC");
			pw.println("#SACComponent[] components to be used");
			pw.println("components Z R T");
			pw.println("#Path of a working folder (full path is required)");
			if (outPath.getParent() != null)
				pw.println("workPath . " + outPath.getParent().toAbsolutePath());
			else
				throw new RuntimeException("Unexpected.");
			pw.println("#String if it is PREM spector file is in eventDir/PREM ");
			pw.println("#if it is blank, then automatically set as the name of a folder in eventDir");
			pw.println("#but the eventDirs can have only one folder inside.");
			pw.println("modelName ");
			pw.println("#Type source time function 0:none, 1:boxcar, 2:triangle.");
			pw.println("#or folder name containing *.stf if you want to your own GLOBALCMTID.stf ");
			pw.println("sourceTimeFunction 1");
			pw.println("#int psv+sh(0) psv(1) sh(2)");
			pw.println("psvsh 0");
			pw.println("#double");
			pw.println("#samplingHz 20 (cant change now)");
			pw.println("#timePartial");
			pw.println("timePartial false");

		}
		setExecutable(outPath);

	}

	@Override
	boolean checkElements() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("workPath");
		parameterSet.add("components");
		// parameterSet.add("modelName");
		parameterSet.add("timePartial");
		parameterSet.add("sourceTimeFunction");
		// parameterSet.add("samplingHz"); TODO
		parameterSet.add("psvsh");

		return reader.containsAll(parameterSet);

	}
}
