package manhattan.dsminformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import manhattan.template.EventFolder;
import manhattan.template.Station;
import manhattan.template.Utilities;

/**
 * 作業フォルダ下のイベント群に対してDSM(tipsv, tish)のinformation fileを作る
 * 
 * @version 0.1.4
 * 
 * @author kensuke
 * 
 */
class SyntheticDSMInformationFileMaker extends parameter.SyntheticDSMInformationFileMaker {

	private SyntheticDSMInformationFileMaker(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	private static SyntheticDSMInformationFileMaker parse(String[] args) throws IOException {
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(parameterPath.toString());

			return new SyntheticDSMInformationFileMaker(parameterPath);
		}
		return new SyntheticDSMInformationFileMaker(null);
	}

	/**
	 * @param args
	 *            [parameter file name]
	 * @throws IOException if any
	 */
	public static void main(String[] args) throws IOException {
		SyntheticDSMInformationFileMaker sdif = parse(args);
		// System.exit(0);
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(sdif.workPath);

		PolynomialStructure ps = sdif.psPath == null ? PolynomialStructure.PREM : new PolynomialStructure(sdif.psPath);

		Path outPath = sdif.workPath.resolve("synthetic" + Utilities.getTemporaryString());
		Files.createDirectories(outPath);
		for (EventFolder eventDir : eventDirs) {
			try {
				Set<Station> stations = eventDir.sacFileSet(sfn -> !sfn.isOBS()).stream()
						.filter(name -> sdif.components.contains(name.getComponent())).map(name -> {
							try {
								return name.readHeader();
							} catch (Exception e2) {
								return null;
							}
						}).filter(Objects::nonNull).map(Station::of).collect(Collectors.toSet());
				if (stations.isEmpty())
					continue;
				int numberOfStation = (int) stations.stream().map(s -> s.getStationName()).count();
				if (numberOfStation != stations.size())
					System.err.println("!Caution there are stations with the same name and different positions in "
							+ eventDir.getGlobalCMTID());
				Path eventOut = outPath.resolve(eventDir.toString());
				SyntheticDSMInfo info = new SyntheticDSMInfo(ps, eventDir.getGlobalCMTID(), stations, sdif.header,
						sdif.tlen, sdif.np);
				Files.createDirectories(eventOut.resolve(sdif.header));
				info.writePSV(eventOut.resolve(sdif.header + "_PSV.inf"));
				info.writeSH(eventOut.resolve(sdif.header + "_SH.inf"));
			} catch (Exception e) {
				System.err.println("Error on " + eventDir);
				e.printStackTrace();
			}
		}

	}
}
