package manhattan.inversion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import manhattan.butterworth.BandPassFilter;
import manhattan.butterworth.ButterworthFilter;
import manhattan.globalcmt.GlobalCMTID;
import manhattan.template.Station;
import manhattan.template.Utilities;
import manhattan.waveformdata.BasicID;
import manhattan.waveformdata.BasicIDFile;
import manhattan.waveformdata.PartialID;
import manhattan.waveformdata.PartialIDFile;
import manhattan.waveformdata.WaveformDataWriter;

/**
 * Checkerboard test
 * 
 * Creates born-waveforms for checkerboard tests
 * 
 * @version 0.1.2
 * 
 * @author Kensuke
 * 
 */
public class CheckerBoardTest extends parameter.CheckerBoardTest {

	private ObservationEquation eq;

	private CheckerBoardTest(Path parameterPath) throws IOException {
		super(parameterPath);
		// System.out.println("hi");
		set();
		readIDs();
	}

	public CheckerBoardTest(ObservationEquation equation) {
		this.eq = equation;
		readIDs();
	}

	private Set<Station> stationSet;
	private double[][] ranges;
	private Set<GlobalCMTID> idSet;

	private void readIDs() {
		List<double[]> ranges = new ArrayList<>();
		for (BasicID id : eq.getDVector().getObsIDs()) {
			stationSet.add(id.getStation());
			idSet.add(id.getGlobalCMTID());
			double[] range = new double[] { id.getMinPeriod(), id.getMaxPeriod() };
			if (ranges.size() == 0)
				ranges.add(range);
			boolean exists = false;
			for (int i = 0; !exists && i < ranges.size(); i++)
				if (Arrays.equals(range, ranges.get(i)))
					exists = true;
			if(!exists)
				ranges.add(range);
		}
		this.ranges = ranges.toArray(new double[ranges.size()][]);
	}

	private void set() throws IOException {
		BasicID[] ids = BasicIDFile.readBasicIDandDataFile(waveIDPath, waveformPath);
		// basicDataFile.read();
		// System.out.println("s");
		Dvector dVector = new Dvector(ids);
		PartialID[] pids = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialWaveformPath);
		// System.out.println("v");
		List<UnknownParameter> parameterList = UnknownParameterFile.read(unknownParameterListPath);
		eq = new ObservationEquation(pids, parameterList, dVector);
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
		System.out.println("outputting " + outIDPath + " " + outDataPath);
		try (WaveformDataWriter bdw = new WaveformDataWriter(outIDPath, outDataPath, stationSet, idSet, ranges)) {
			BasicID[] obsIDs = dVector.getObsIDs();
			BasicID[] synIDs = dVector.getSynIDs();
			// RealVector[] addVec = dVector.addSyn(pseudoD);
			for (int i = 0; i < dVector.getNTimeWindow(); i++) {
				bdw.addBasicID(obsIDs[i].setData(bornPart[i].mapDivide(dVector.getWeighting()[i]).toArray()));
				bdw.addBasicID(
						synIDs[i].setData(dVector.getSynVec()[i].mapDivide(dVector.getWeighting()[i]).toArray()));
			}
		}
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
		// bornVec = dVector.getObsVec();
		if (bornVec == null) {
			System.out.println("bornVec is not set");
			return;
		}
		Dvector dVector = eq.getDVector();
		RealVector[] bornPart = dVector.separate(bornVec);
		System.out.println("outputting " + outIDPath + " " + outDataPath);
		try (WaveformDataWriter bdw = new WaveformDataWriter(outIDPath, outDataPath, stationSet, idSet, ranges)) {
			BasicID[] obsIDs = dVector.getObsIDs();
			BasicID[] synIDs = dVector.getSynIDs();
			// RealVector[] addVec = dVector.addSyn(pseudoD);
			for (int i = 0; i < dVector.getNTimeWindow(); i++) {
				bdw.addBasicID(
						obsIDs[i].setData(dVector.getObsVec()[i].mapDivide(dVector.getWeighting()[i]).toArray()));
				bdw.addBasicID(synIDs[i].setData(bornPart[i].mapDivide(dVector.getWeighting()[i]).toArray()));
			}
		}
	}

	/**
	 * pseudoM を読み込む
	 */
	private RealVector readPseudoM() throws IOException {
		List<String> lines = Files.readAllLines(inputDataPath);
		if (lines.size() != eq.getMlength())
			throw new RuntimeException("input model length is wrong");
		double[] pseudoM = new double[eq.getMlength()];
		Arrays.setAll(pseudoM, i -> Double.parseDouble(lines.get(i)));
		return new ArrayRealVector(pseudoM);
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
	 *            [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		CheckerBoardTest cbt = null;
		if (0 < args.length) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			// System.out.println("hi");
			cbt = new CheckerBoardTest(parameterPath);
		} else
			cbt = new CheckerBoardTest((Path) null);
		// System.exit(0);
		RealVector pseudoM = cbt.readPseudoM();
		RealVector pseudoD = cbt.computePseudoD(pseudoM);
		RealVector bornVec = pseudoD.add(cbt.getSynVector());

		String dateStr = Utilities.getTemporaryString();
		Path outIDPath = cbt.workPath.resolve("pseudoID" + dateStr + ".dat");
		Path outDataPath = cbt.workPath.resolve("pseudo" + dateStr + ".dat");

		if (cbt.noise)
			bornVec = bornVec.add(cbt.computeRandomNoise());
		if (cbt.iterate)
			cbt.output4Iterate(outIDPath, outDataPath, bornVec);
		else
			cbt.output4ChekeBoardTest(outIDPath, outDataPath, bornVec);
		//

	}

	public RealVector getSynVector() {
		return eq.getDVector().combine(eq.getDVector().getSynVec());
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

}
