package parameter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 
 * Information for
 *        {@link manhattan.dsminformation.SshDSMInformationFileMaker}
 * @since 2013/7/3 
 * @version 0.0.1
 * 
 * @version 0.0.2
 * @since 2014/9/7 small fix
 * 
 * @version 0.0.3
 * @since 2014/10/16 modified
 * 
 * @version 0.0.3.1 {@link IOException}
 * 
 * @version 0.0.4
 * @since 2015/8/14 {@link Path} base
 * 
 * @author kensuke
 *
 */
public class SshDSMInformationFileMaker extends SyntheticDSMInformationFileMaker {

	protected double[] perturbationR;

	protected SshDSMInformationFileMaker(Path parameterPath) throws IOException {
		super(parameterPath);
		set();
	}

	private void set() {
		Set<String> parameterSet = new HashSet<>();
		parameterSet.add("perturbationR");
		if (!reader.containsAll(parameterSet))
			System.exit(9);

		String[] rStrs = reader.getStringArray("perturbationR");
		perturbationR = Arrays.stream(rStrs).mapToDouble(Double::parseDouble).toArray();

	}

	public static void outputList(Path outPath, OpenOption... options) throws IOException {
		SyntheticDSMInformationFileMaker.outputList(outPath, options);
		List<String> outLines = Files.readAllLines(outPath);
		outLines.set(0, "#!java manhattan.dsminformation.SshDSMInformationFileMaker");
		outLines.add("#perturbationR depths for computations");
		outLines.add("perturbationR 3490 3510 3530 3550 3570 3590 3610 3630 3650 3670"
				+ " 3690 3710 3730 3750 3770 3790 3810 3830 3850 3870");
		Files.write(outPath, outLines);

		setExecutable(outPath);
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Path tmp = null;
		if (0 < args.length)
			tmp = Paths.get(args[0]).toAbsolutePath();
		else
			tmp = readFileName();

		outputList(tmp, StandardOpenOption.CREATE_NEW);
	}

}
