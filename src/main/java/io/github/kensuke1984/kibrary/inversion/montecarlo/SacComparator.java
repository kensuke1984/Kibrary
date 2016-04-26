/**
 * 
 */
package io.github.kensuke1984.kibrary.inversion.montecarlo;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * SacCompare <b>Assume that there are no stations with the same name but
 * different networks</b>
 * 
 * @author Kensuke Konishi
 * 
 * @version 0.0.5
 * 
 * 
 */
class SacComparator {
	private static TraveltimeList sList;
	private static TraveltimeList scsList;
	private static Set<TimewindowInformation> twInfo;

	static void set(Path dir) throws IOException {
		Path sPath = dir.resolve("s.lst");
		Path scsPath = dir.resolve("scs.lst");
		Path twPath = dir.resolve("scsWindow.dat");
		if (!(Files.exists(sPath) && Files.exists(scsPath) && Files.exists(twPath)))
			throw new RuntimeException("No files for SacComparator");
		sList = new TraveltimeList(sPath);
		scsList = new TraveltimeList(scsPath);
		twInfo = TimewindowInformationFile.read(twPath);
	}

	private static final double sRange = 20;
	private static final double scsRange = 20;
	private static final double samplingHz = 20;

	private double taupS;
	private double taupScS;

	private double upSynSTime;
	private double downSynSTime;
	private double upObsSTime;
	private double downObsSTime;

	private double upSynScSTime;
	private double downSynScSTime;
	private double upObsScSTime;
	private double downObsScSTime;

	private double upSRatio;
	private double downSRatio;
	private double sRatio;

	private double upScSRatio;
	private double downScSRatio;
	private double scsRatio;

	// private TauPPhase scsPhase;
	private SACData obsSac;
	private SACData synSac;

	private double tstart;
	private double tend;

	private double variance;
	private double correlation;

	/**
	 * Peak-Peak
	 * 
	 * @return obs/syn
	 */
	double getSAmpRatio() {
		return sRatio;
	}

	/**
	 * Peak-Peak
	 * 
	 * @return obs/syn
	 */
	double getScSAmpRatio() {
		return scsRatio;
	}

	/**
	 * @return mean of the times of Peaks of S
	 */
	double getObsSTime() {
		return (upObsSTime + downObsSTime) / 2;
	}

	/**
	 * @return mean of the times of Peaks of S
	 */
	double getSynSTime() {
		return (upSynSTime + downSynSTime) / 2;
	}

	/**
	 * @return mean of the times of Peaks of S
	 */
	double getObsScSTime() {
		return (upObsScSTime + downObsScSTime) / 2;
	}

	/**
	 * @return mean of the times of Peaks of S
	 */
	double getSynScSTime() {
		return (upSynScSTime + downSynScSTime) / 2;
	}

	/**
	 * @param sac
	 *            to be cut
	 * @param startTime
	 *            start time
	 * @param npts
	 *            number of points to cut
	 * @return cut part of SAC file
	 */
	private static double[] cutDataSac(SACData sac, double startTime, int npts) {
		Trace trace = sac.createTrace();
		int startPoint = trace.getNearestXIndex(startTime);
		double[] waveData = trace.getY();
		return IntStream.range(0, npts).parallel().mapToDouble(i -> waveData[i + startPoint]).toArray();
	}

	private void findSpeaks() {
		int npts = (int) (samplingHz * 2 * sRange);
		double[] syn = cutDataSac(synSac, taupS - 5, npts);
		RealVector synVector = new ArrayRealVector(syn, false);
		int minSynIndex = synVector.getMinIndex();
		int maxSynIndex = synVector.getMaxIndex();
		upSynSTime = taupS - 5 + maxSynIndex / samplingHz;
		downSynSTime = taupS - 5 + minSynIndex / samplingHz;
		// System.out.println(upSynSTime + " " + downSynSTime);
		double[] obs = cutDataSac(obsSac, taupS - 5, npts);
		RealVector obsVector = new ArrayRealVector(obs, false);
		int minobsIndex = obsVector.getMinIndex();
		int maxobsIndex = obsVector.getMaxIndex();
		upObsSTime = taupS - 5 + maxobsIndex / samplingHz;
		downObsSTime = taupS - 5 + minobsIndex / samplingHz;

		upSRatio = obsVector.getMaxValue() / synVector.getMaxValue();
		downSRatio = obsVector.getMinValue() / synVector.getMinValue();
		sRatio = (obsVector.getMaxValue() - obsVector.getMinValue())
				/ (synVector.getMaxValue() - synVector.getMinValue());
		// System.out.println(upObsSTime + " " + downObsSTime+" "+sRatio);
	}

	private void findScSpeaks() {
		int npts = (int) (samplingHz * 2 * scsRange);
		double[] syn = cutDataSac(synSac, taupScS - 5, npts);
		RealVector synVector = new ArrayRealVector(syn,false);
		int minSynIndex = synVector.getMinIndex();
		int maxSynIndex = synVector.getMaxIndex();
		upSynScSTime = taupScS - 5 + maxSynIndex / samplingHz;
		downSynScSTime = taupScS - 5 + minSynIndex / samplingHz;
		// System.out.println(upSynScSTime + " " + downSynScSTime);
		double[] obs = cutDataSac(obsSac, taupScS - 5, npts);
		RealVector obsVector = new ArrayRealVector(obs,false);
		int minobsIndex = obsVector.getMinIndex();
		int maxobsIndex = obsVector.getMaxIndex();
		upObsScSTime = taupScS - 5 + maxobsIndex / samplingHz;
		downObsScSTime = taupScS - 5 + minobsIndex / samplingHz;
		upScSRatio = obsVector.getMaxValue() / synVector.getMaxValue();
		downScSRatio = obsVector.getMinValue() / synVector.getMinValue();
		scsRatio = (obsVector.getMaxValue() - obsVector.getMinValue())
				/ (synVector.getMaxValue() - synVector.getMinValue());
		// System.out.println(upObsScSTime + " " + downObsScSTime+" "+scsRatio);

	}

	SacComparator(SACData obsSac, SACData synSac) {
		if (twInfo == null)
			throw new RuntimeException("Still no info files set");
		this.obsSac = obsSac;
		this.synSac = synSac;
		String station = Station.of(obsSac).getStationName();

		GlobalCMTID id = new GlobalCMTID(obsSac.getSACString(SACHeaderEnum.KEVNM));
		TimewindowInformation window = twInfo.stream()
				.filter(info -> info.getStation().getStationName().equals(station))
				.filter(info -> info.getGlobalCMTID().equals(id)).filter(info -> info.getComponent() == SACComponent.T)
				.findAny().get();
		tstart = window.getStartTime();
		tend = window.getEndTime();
		taupS = sList.getTime(station, id);
		taupScS = scsList.getTime(station, id);
		compare();
	}

	private void compare() {
		findSpeaks();
		findScSpeaks();
		double shift = getObsSTime() - getSynSTime();
		double[] obsData = obsSac.createTrace().cutWindow(tstart, tend).getY();
		double[] obsDataShifted = obsSac.createTrace().cutWindow(tstart + shift, tend + shift + 1).getY();
		double[] synData = synSac.createTrace().cutWindow(tstart, tend).getY();
		RealVector obsV = new ArrayRealVector(obsData,false);
		RealVector obsVShifted = new ArrayRealVector(obsDataShifted, 0, obsData.length);
		RealVector synV = new ArrayRealVector(synData,false);
		variance = obsV.subtract(synV).getNorm() / obsV.getNorm();
		correlation = obsV.dotProduct(synV) / obsV.getNorm() / synV.getNorm();
		variancewTS = obsV.subtract(synV).getNorm() / obsVShifted.getNorm();
		correlationwTS = obsVShifted.dotProduct(synV) / obsVShifted.getNorm() / synV.getNorm();
	}

	double getVarianceTS() {
		return variancewTS;
	}

	double getCorrelationTS() {
		return correlationwTS;
	}

	private double variancewTS;
	private double correlationwTS;

	double getVariance() {
		return variance;
	}

	double getCorrelation() {
		return correlation;
	}

}
