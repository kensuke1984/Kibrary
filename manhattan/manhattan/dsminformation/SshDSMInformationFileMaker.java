package manhattan.dsminformation;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import filehandling.sac.SACComponent;
import manhattan.template.EventFolder;
import manhattan.template.Station;
import manhattan.template.Utilities;

/**
 * Information file for SSHSH
 * @version 0.0.6
 * 
 * @author kensuke
 *
 */
class SshDSMInformationFileMaker extends parameter.SshDSMInformationFileMaker {

	private SshDSMInformationFileMaker(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	/**
	 * @param args
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		SshDSMInformationFileMaker sdif = null;
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			sdif = new SshDSMInformationFileMaker(parameterPath);
		} else
			sdif = new SshDSMInformationFileMaker(null);

		Set<EventFolder> eventDirs = Utilities.eventFolderSet(sdif.workPath);
		PolynomialStructure ps = sdif.psPath == null ? PolynomialStructure.PREM : new PolynomialStructure(sdif.psPath);
		String temporaryString = Utilities.getTemporaryString();
		Path output = sdif.workPath.resolve("oneDPartial" + temporaryString);
		Files.createDirectories(output);
		Set<SACComponent> useComponents = sdif.components;
		for (EventFolder eventDir : eventDirs) {
			// Event e = eventinfo.getEvent(eventDir.getEventName());
			Set<Station> stations = eventDir.sacFileSet(sfn -> !sfn.isOBS()).stream()
					.filter(name -> useComponents.contains(name.getComponent())).map(name -> {
						try {
							return name.readHeader();
						} catch (Exception e2) {
							e2.printStackTrace();
							return null;
						}
					}).filter(Objects::nonNull).map(Station::of).collect(Collectors.toSet());
			if (stations.isEmpty())
				continue;
			int numberOfStation = (int) stations.stream().map(s -> s.getStationName()).count();
			if (numberOfStation != stations.size())
				System.err.println("!Caution there are stations with the same name and different positions in "
						+ eventDir.getGlobalCMTID());
			SshDSMinfo info = new SshDSMinfo(ps, eventDir.getGlobalCMTID(), stations, sdif.header, sdif.perturbationR,
					sdif.tlen, sdif.np);
			Path outEvent = output.resolve(eventDir.getGlobalCMTID().toString());
			Path modelPath = outEvent.resolve(sdif.header);
			Files.createDirectories(outEvent);
			Files.createDirectories(modelPath);
			info.outPSV(outEvent.resolve("par5_" + sdif.header + "_PSV.inf"));
			info.outSH(outEvent.resolve("par5_" + sdif.header + "_SH.inf"));
			info.outPSVi(outEvent.resolve("par2_" + sdif.header + "_PSV.inf"));
			info.outSHi(outEvent.resolve("par2_" + sdif.header + "_SH.inf"));
		}

	}

}
