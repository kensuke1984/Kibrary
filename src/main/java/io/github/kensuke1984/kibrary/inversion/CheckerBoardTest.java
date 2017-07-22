package io.github.kensuke1984.kibrary.inversion;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.datacorrection.FujiStaticCorrection;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

/**
 * Checkerboard test
 * 
 * Creates born-waveforms for checkerboard tests
 * 
 * @version 0.2.0.9
 * 
 * @author Kensuke Konishi
 * 
 */
public class CheckerBoardTest implements Operation {

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(CheckerBoardTest.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan CheckerBoardTest");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##Path of a waveID file, must be defined");
			pw.println("#waveIDPath id.dat");
			pw.println("##Path of a waveform file, must be defined");
			pw.println("#waveformPath waveform.dat");
			pw.println("##Path of a partial id file, must be defined");
			pw.println("#partialIDPath partialID.dat");
			pw.println("##Path of a partial waveform file, must be defined");
			pw.println("#partialWaveformPath partial.dat");
			pw.println("##Path of an unknown parameter list file, must be defined");
			pw.println("#unknownParameterListPath unknowns.inf");
			pw.println("##Path of an input data list file, must be defined");
			pw.println("#inputDataPath input.inf");
			pw.println("##boolean If this is for Iterate (false)");
			pw.println("#iterate");
			pw.println("##boolean if it adds noise (false)");
			pw.println("#noise");
			pw.println("##noise power (1000)");
			pw.println("#noisePower");
			pw.println("##apply time shift? (false)");
			pw.println("#timeShift ");
		}
		System.err.println(outPath + " is created.");
	}

	private ObservationEquation eq;
	private Properties property;

	public CheckerBoardTest(Properties property) throws IOException {
		this.property = (Properties) property.clone();
		set();
		read();
		readIDs();
	}

	public CheckerBoardTest(ObservationEquation eq) {
		this.eq = eq;
		readIDs();
	}

	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("sourceTimeFunction"))
			property.setProperty("sourceTimeFunction", "0");
		if (!property.containsKey("timePartial"))
			property.setProperty("timePartial", "false");
		if (!property.containsKey("psvsh"))
			property.setProperty("psvsh", "0");
		if (!property.containsKey("modelName"))
			property.setProperty("modelName", "");
		if (!property.containsKey("noize"))
			property.setProperty("noize", "false");
		if (property.getProperty("noize").equals("true") && property.containsKey("noizePower"))
			throw new RuntimeException("There is no information about 'noizePower'");
		if (!property.containsKey("timeShift"))
			property.setProperty("timeShift", "false");
		
	}

	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));
		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		waveIDPath = getPath("waveIDPath");
		waveformPath = getPath("waveformPath");
		partialIDPath = getPath("partialIDPath");
		partialWaveformPath = getPath("partialWaveformPath");
		unknownParameterListPath = getPath("unknownParameterListPath");
		inputDataPath = getPath("inputDataPath");
		noise = Boolean.parseBoolean(property.getProperty("noise"));
		if (noise)
			noisePower = Double.parseDouble(property.getProperty("noisePower"));
		iterate = Boolean.parseBoolean(property.getProperty("iterate"));
		timeShift = Boolean.parseBoolean(property.getProperty("timeShift"));
	}

	private Path workPath;
	
	private boolean timeShift;

	/**
	 * 観測波形、理論波形の入ったファイル (BINARY)
	 */
	protected Path waveformPath;

	/**
	 * 求めたい未知数を羅列したファイル (ASCII)
	 */
	protected Path unknownParameterListPath;

	/**
	 * partialIDの入ったファイル
	 */
	protected Path partialIDPath;
	/**
	 * partial波形の入ったファイル
	 */
	protected Path partialWaveformPath;

	protected boolean iterate;

	protected boolean noise;

	/**
	 * 観測、理論波形のID情報
	 */
	protected Path waveIDPath;

	protected double noisePower;

	/**
	 * pseudoMの元になるファイル
	 */
	protected Path inputDataPath;
	private Set<Station> stationSet = new HashSet<>();
	private double[][] ranges;
	private Phase[] phases;
	private Set<GlobalCMTID> idSet = new HashSet<>();

	private void readIDs() {
		List<double[]> ranges = new ArrayList<>();
		Set<Phase> tmpPhases = new HashSet<>();
		for (BasicID id : eq.getDVector().getObsIDs()) {
			stationSet.add(id.getStation());
			idSet.add(id.getGlobalCMTID());
			for (Phase phase : id.getPhases())
				tmpPhases.add(phase);
			double[] range = new double[] { id.getMinPeriod(), id.getMaxPeriod() };
			if (ranges.size() == 0)
				ranges.add(range);
			boolean exists = false;
			for (int i = 0; !exists && i < ranges.size(); i++)
				if (Arrays.equals(range, ranges.get(i)))
					exists = true;
			if (!exists)
				ranges.add(range);
		}
		this.ranges = ranges.toArray(new double[0][]);
		phases = new Phase[tmpPhases.size()];
		phases = tmpPhases.toArray(phases); 
	}

	private void read() throws IOException {
		BasicID[] ids = BasicIDFile.readBasicIDandDataFile(waveIDPath, waveformPath);
		Dvector dVector = new Dvector(ids);
		PartialID[] pids = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialWaveformPath);
		List<UnknownParameter> parameterList = UnknownParameterFile.read(unknownParameterListPath);
		eq = new ObservationEquation(pids, parameterList, dVector, false, false, null, null);
	}

	/**
	 * 読み込んだデータセットに対してボルン波形を観測波形として 理論波形を理論波形として書き込む（上書きではない）
	 * 
	 * @param outIDPath
	 *            for output
	 * @param outDataPath
	 *            for output
	 * @param bornVec
	 *            for output
	 */
	public void output4ChekeBoardTest(Path outIDPath, Path outDataPath, RealVector bornVec) throws IOException {
		// bornVec = dVector.getObsVec();
		Objects.requireNonNull(bornVec);

		Dvector dVector = eq.getDVector();
		RealVector[] bornPart = dVector.separate(bornVec);
		System.err.println("outputting " + outIDPath + " " + outDataPath);
		try (WaveformDataWriter bdw = new WaveformDataWriter(outIDPath, outDataPath, stationSet, idSet, ranges, phases)) {
			BasicID[] obsIDs = dVector.getObsIDs();
			BasicID[] synIDs = dVector.getSynIDs();
			for (int i = 0; i < dVector.getNTimeWindow(); i++) {
				bdw.addBasicID(obsIDs[i].setData(bornPart[i].mapDivide(dVector.getWeighting(i)).toArray()));
				bdw.addBasicID(synIDs[i].setData(dVector.getSynVec()[i].mapDivide(dVector.getWeighting(i)).toArray()));
			}
		}
	}
	
	public void output4ChekeBoardTestTimeshifted(Path outIDPath, Path outDataPath, RealVector bornVec) throws IOException {
		// bornVec = dVector.getObsVec();
		Objects.requireNonNull(bornVec);

		Dvector dVector = eq.getDVector();
		RealVector[] bornPart = dVector.separate(bornVec);
		System.err.println("outputting " + outIDPath + " " + outDataPath);
		try (WaveformDataWriter bdw = new WaveformDataWriter(outIDPath, outDataPath, stationSet, idSet, ranges, phases)) {
			BasicID[] obsIDs = dVector.getObsIDs();
			BasicID[] synIDs = dVector.getSynIDs();
			for (int i = 0; i < dVector.getNTimeWindow(); i++) {
				RealVector synVec_i = dVector.getSynVec()[i];
				RealVector bornVec_i = new ArrayRealVector(shiftPseudo(bornPart[i].toArray(), synVec_i.toArray()));
				bdw.addBasicID(obsIDs[i].setData(bornVec_i.mapDivide(dVector.getWeighting(i)).toArray()));
				bdw.addBasicID(synIDs[i].setData(synVec_i.mapDivide(dVector.getWeighting(i)).toArray()));
			}
		}
	}
	
	private double[] shiftPseudo(double[] born, double[] syn) {
		double[] shifted = new double[born.length];
		FujiStaticCorrection fsc = new FujiStaticCorrection(1., 0.2, 15.);
		double shift = fsc.computeTimeshiftForBestCorrelation(born, syn);
		System.out.println(shift);
		if (shift < 0) { // born arrives earlier
			int pointShift = -(int) shift;
			for (int i = 0; i < born.length - pointShift; i++)
				shifted[i + pointShift] = born[i];
		}
		else if (shift > 0) {
			int pointShift = (int) shift;
			for (int i = pointShift; i < born.length; i++)
				shifted[i - pointShift] = born[i];
		}
		else
			shifted = born;
		return shifted;
	}

	/**
	 * 読み込んだデータセットに対してボルン波形を理論波形として書き込む（上書きではない）
	 * 
	 * @param outIDPath
	 *            {@link File} for output ID file
	 * @param outDataPath
	 *            {@link File} for output data file
	 * @param bornVec
	 *            {@link RealVector} of born
	 */
	public void output4Iterate(Path outIDPath, Path outDataPath, RealVector bornVec) throws IOException {
		if (bornVec == null) {
			System.err.println("bornVec is not set");
			return;
		}
		Dvector dVector = eq.getDVector();
		RealVector[] bornPart = dVector.separate(bornVec);
		System.err.println("outputting " + outIDPath + " " + outDataPath);
		try (WaveformDataWriter bdw = new WaveformDataWriter(outIDPath, outDataPath, stationSet, idSet, ranges, phases)) {
			BasicID[] obsIDs = dVector.getObsIDs();
			BasicID[] synIDs = dVector.getSynIDs();
			for (int i = 0; i < dVector.getNTimeWindow(); i++) {
				double weighting = dVector.getWeighting(i);
				bdw.addBasicID(obsIDs[i].setData(dVector.getObsVec()[i].mapDivide(weighting).toArray()));
				bdw.addBasicID(synIDs[i].setData(bornPart[i].mapDivide(weighting).toArray()));
			}
		}
	}

	/**
	 * Reads pseudoM
	 */
	private RealVector readPseudoM() throws IOException {
		List<String> lines = Files.readAllLines(inputDataPath);
		if (lines.size() != eq.getMlength())
			throw new RuntimeException("input model length is wrong");
		double[] pseudoM = lines.stream().mapToDouble(Double::parseDouble).toArray();
		return new ArrayRealVector(pseudoM, false);
	}

	/**
	 * d = A m
	 * 
	 * @param pseudoM
	 *            &delta;m
	 * @return d for the input pseudo M
	 */
	public RealVector computePseudoD(RealVector pseudoM) {
		return eq.operate(pseudoM);
	}

	/**
	 * @param args
	 *            [a property file name]
	 * @throws Exception
	 *             if any
	 */
	public static void main(String[] args) throws Exception {
		Properties property = Property.parse(args);
		CheckerBoardTest cbt = new CheckerBoardTest(property);
		long time = System.nanoTime();
		System.err.println(CheckerBoardTest.class.getName() + " is going.");
		cbt.run();
		System.err.println(
				CheckerBoardTest.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - time));
	}

	public RealVector getSynVector() {
		return eq.getDVector().getSyn();
	}

	public RealVector computeRandomNoise() {

		Dvector dVector = eq.getDVector();
		RealVector[] noiseV = new RealVector[dVector.getNTimeWindow()];
		int[] pts = dVector.getLengths();
		ButterworthFilter bpf = new BandPassFilter(2 * Math.PI * 0.05 * 0.08, 2 * Math.PI * 0.05 * 0.005, 4);
		for (int i = 0; i < dVector.getNTimeWindow(); i++) {
			// System.out.println(i);
			double[] u = RandomNoiseMaker.create(noisePower, 20, 3276.8, 1024).getY();
			u = bpf.applyFilter(u);
			int startT = (int) dVector.getObsIDs()[i].getStartTime() * 20; // 6*4=20
			noiseV[i] = new ArrayRealVector(pts[i]);
			for (int j = 0; j < pts[i]; j++)
				noiseV[i].setEntry(j, u[j * 20 + startT]);
		}
		// pseudoD = pseudoD.add(randomD);
		return dVector.combine(noiseV);
	}

	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public void run() throws Exception {
		RealVector pseudoM = readPseudoM();
		RealVector pseudoD = computePseudoD(pseudoM);
		RealVector bornVec = pseudoD.add(getSynVector());

		String dateStr = Utilities.getTemporaryString();
		Path outIDPath = workPath.resolve("pseudoID" + dateStr + ".dat");
		Path outDataPath = workPath.resolve("pseudo" + dateStr + ".dat");

		if (noise)
			bornVec = bornVec.add(computeRandomNoise());
		if (iterate)
			output4Iterate(outIDPath, outDataPath, bornVec);
		else {
			if (timeShift) {
				System.out.println("Using time shift");
				output4ChekeBoardTestTimeshifted(outIDPath, outDataPath, bornVec);
			}
			else
				output4ChekeBoardTest(outIDPath, outDataPath, bornVec);
		}

	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}

}
