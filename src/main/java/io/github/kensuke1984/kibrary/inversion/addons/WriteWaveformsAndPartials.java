package io.github.kensuke1984.kibrary.inversion.addons;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.ObservationEquation;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.commons.math3.linear.RealVector;
import org.apache.commons.math3.util.Precision;

public class WriteWaveformsAndPartials {

	public static void main(String[] args) throws IOException {
		System.out.println("waveformID waveform partialID partial unknowns");
		Path waveformIDPath = Paths.get(args[0]);
		Path waveformPath = Paths.get(args[1]);
		
		Path partialIDPath = Paths.get(args[2]);
		Path partialPath = Paths.get(args[3]);
		
		Path unknownsPath = Paths.get(args[4]);
		
		BasicID[] waveforms = BasicIDFile.read(waveformIDPath, waveformPath);
		PartialID[] partials = PartialIDFile.read(partialIDPath, partialPath);
		
		List<UnknownParameter> parameterList = UnknownParameterFile.read(unknownsPath);
		
		Dvector dVector = new Dvector(waveforms, id -> true, WeightingType.RECIPROCAL);
		ObservationEquation eq = new ObservationEquation(partials, parameterList, dVector, false, false, null, null, UnknownParameterWeightType.NO_WEIGHT, null);
		
		String tempString = Utilities.getTemporaryString();
		
		Path dir = Paths.get("output" + tempString);
		Files.createDirectories(dir);
		
		Path orderPath = dir;
		Path aPath = dir.resolve("partial");
		Path tracePath = dir.resolve("trace");
		
		dVector.outOrder(orderPath);
		System.out.println("Output traces...");
		outEachTrace(tracePath, dVector);
		System.out.println("Output partials...");
		eq.outputA(aPath);
		System.out.println("Done!");
	}
	
	/**
	 * outDirectory下にtraceフォルダを作りその下に理論波形と観測波形を書き込む
	 * 
	 * @param outPath
	 *            {@link Path} for output folder
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void outEachTrace(Path outPath, Dvector d) throws IOException {
		if (Files.exists(outPath))
			throw new FileAlreadyExistsException(outPath.toString());
		Files.createDirectories(outPath);

		Path eventPath = outPath.resolve("eventVariance.inf");
		Path stationPath = outPath.resolve("stationVariance.inf");
		try (PrintWriter pwEvent = new PrintWriter(Files.newBufferedWriter(eventPath));
				PrintWriter pwStation = new PrintWriter(Files.newBufferedWriter(stationPath))) {
			pwEvent.println("#id latitude longitude radius variance");
			d.getEventVariance().entrySet().forEach(entry -> {
				pwEvent.println(
						entry.getKey() + " " + entry.getKey().getEvent().getCmtLocation() + " " + entry.getValue());
			});
			pwStation.println("#name network latitude longitude variance");
			d.getStationVariance().entrySet().forEach(entry -> {
				pwStation.println(entry.getKey() + " " + entry.getKey().getNetwork() + " "
						+ entry.getKey().getPosition() + " " + entry.getValue());
			});

		}
		for (GlobalCMTID id : d.getUsedGlobalCMTIDset()) {
			Path eventFolder = outPath.resolve(id.toString());
			Files.createDirectories(eventFolder);
			Path obs = eventFolder.resolve("recordOBS.plt");
			Path syn = eventFolder.resolve("recordSYN.plt");
			Path w = eventFolder.resolve("recordW.plt");
			Path wa = eventFolder.resolve("recordWa.plt");
			try (PrintWriter plotO = new PrintWriter(
					Files.newBufferedWriter(obs, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotS = new PrintWriter(
							Files.newBufferedWriter(syn, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotW = new PrintWriter(
							Files.newBufferedWriter(w, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotWa = new PrintWriter(
							Files.newBufferedWriter(wa, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {

				plotW.println("set title\"" + id + "\"");
				plotW.print("p ");
				plotO.println("set title\"" + id + "\"");
				plotO.print("p ");
				plotS.println("set title\"" + id + "\"");
				plotS.print("p ");
				plotWa.println("set title\"" + id + "\"");
				plotWa.print("p ");
			}

		}

		BasicID[] obsIDs = d.getObsIDs();
		BasicID[] synIDs = d.getSynIDs();
		RealVector[] obsVec = d.getObsVec();
		RealVector[] synVec = d.getSynVec();
		RealVector[] delVec = d.getdVec();
		// each trace variance
		Path eachVariancePath = outPath.resolve("eachVariance.txt");
		try (PrintWriter pw1 = new PrintWriter(Files.newBufferedWriter(eachVariancePath))) {
			pw1.println("#i station network EventID variance correlation");
			for (int i = 0; i < d.getNTimeWindow(); i++) {
				double variance = delVec[i].dotProduct(delVec[i]) / obsVec[i].dotProduct(obsVec[i]);
				double correlation = obsVec[i].dotProduct(synVec[i]) / obsVec[i].getNorm() / synVec[i].getNorm();
				pw1.println(i + " " + obsIDs[i].getStation() + " " + obsIDs[i].getStation().getNetwork() + " "
						+ obsIDs[i].getGlobalCMTID() + " " + variance + " " + correlation);
			}
		}
		for (int i = 0; i < d.getNTimeWindow(); i++) {
			String name = obsIDs[i].getStation() + "." + obsIDs[i].getGlobalCMTID() + "." + obsIDs[i].getSacComponent()
					+ "." + i + ".txt";

			HorizontalPosition eventLoc = obsIDs[i].getGlobalCMTID().getEvent().getCmtLocation();
			HorizontalPosition stationPos = obsIDs[i].getStation().getPosition();
			double gcarc = Precision.round(Math.toDegrees(eventLoc.getEpicentralDistance(stationPos)), 2);
			double azimuth = Precision.round(Math.toDegrees(eventLoc.getAzimuth(stationPos)), 2);
			Path eventFolder = outPath.resolve(obsIDs[i].getGlobalCMTID().toString());
			// eventFolder.mkdir();
			Path plotPath = eventFolder.resolve("recordOBS.plt");
			Path plotPath2 = eventFolder.resolve("recordSYN.plt");
			Path plotPath3 = eventFolder.resolve("recordW.plt");
			Path plotPath4 = eventFolder.resolve("recordWa.plt");
			Path tracePath = eventFolder.resolve(name);
			try (PrintWriter pwTrace = new PrintWriter(Files.newBufferedWriter(tracePath));
					PrintWriter plotO = new PrintWriter(
							Files.newBufferedWriter(plotPath, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotS = new PrintWriter(
							Files.newBufferedWriter(plotPath2, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotW = new PrintWriter(
							Files.newBufferedWriter(plotPath3, StandardOpenOption.CREATE, StandardOpenOption.APPEND));
					PrintWriter plotWa = new PrintWriter(
							Files.newBufferedWriter(plotPath4, StandardOpenOption.CREATE, StandardOpenOption.APPEND))) {

				if (i < (d.getNTimeWindow() - 1)) {
					plotO.println("\"" + name + "\" u 1:($3+" + gcarc + ") ti\"" + obsIDs[i].getStation() + "\", \\");
					plotS.println("\"" + name + "\" u 2:($4+" + gcarc + ") ti\"" + obsIDs[i].getStation() + "\", \\");
					plotW.println("\"" + name + "\" u 2:($3+" + gcarc + ") lc rgb \"red\" noti ,  \"" + name
							+ "\" u 2:($4+" + gcarc + ") lc rgb \"blue\" ti\"" + obsIDs[i].getStation() + "\", \\");
					plotWa.println("\"" + name + "\" u 2:($3+" + azimuth + ") lc rgb \"red\" noti ,  \"" + name
							+ "\" u 2:($4+" + azimuth + ") lc rgb \"blue\" ti\"" + obsIDs[i].getStation() + "\", \\");
				} else {

					plotO.println("\"" + name + "\" u 1:($3+" + gcarc + ") ti\"" + obsIDs[i].getStation() + "\"");
					plotS.println("\"" + name + "\" u 2:($4+" + gcarc + ") ti\"" + obsIDs[i].getStation() + "\"");
					plotW.println("\"" + name + "\" u 2:($3+" + gcarc + ") lc rgb \"red\" noti ,  \"" + name
							+ "\" u 2:($4+" + gcarc + ") lc rgb \"blue\" ti\"" + obsIDs[i].getStation() + "\"");
					plotWa.println("\"" + name + "\" u 2:($3+" + azimuth + ") lc rgb \"red\" noti ,  \"" + name
							+ "\" u 2:($4+" + azimuth + ") lc rgb \"blue\" ti\"" + obsIDs[i].getStation() + "\"");
				}
//				double maxObs = obsVec[i].getLInfNorm();
				double obsStart = obsIDs[i].getStartTime();
				double synStart = synIDs[i].getStartTime();
				double samplingHz = obsIDs[i].getSamplingHz();
				pwTrace.println("#obstime syntime obs syn");
				for (int j = 0; j < obsIDs[i].getNpts(); j++)
					pwTrace.println((obsStart + j / samplingHz) + " " + (synStart + j / samplingHz) + " "
							+ obsVec[i].getEntry(j) + " " + synVec[i].getEntry(j));
			}
		}
	}
}
