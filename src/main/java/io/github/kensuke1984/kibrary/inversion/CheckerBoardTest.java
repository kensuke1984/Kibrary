package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.*;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;

/**
 * Checkerboard test
 * <p>
 * Creates born-waveforms for checkerboard tests
 *
 * @author Kensuke Konishi
 * @version 0.2.2
 */
public class CheckerBoardTest implements Operation {

    /**
     * Path of a {@link BasicIDFile} file (id part)
     */
    protected Path waveIDPath;
    /**
     * Path of a {@link BasicIDFile} file (data part)
     */
    protected Path waveformPath;
    /**
     * Path for the file ({@link UnknownParameterFile})
     */
    protected Path unknownParameterListPath;
    /**
     * Path of the partialID
     */
    protected Path partialIDPath;
    /**
     * Path of the partial data
     */
    protected Path partialWaveformPath;
    protected boolean iterate;
    protected boolean noise;
    protected double noisePower;
    /**
     * Path of a txt file containing psudoM
     */
    protected Path inputDataPath;
    private ObservationEquation eq;
    private Properties property;
    private Path workPath;
    private Set<Station> stationSet = new HashSet<>();
    private double[][] ranges;
    private Set<GlobalCMTID> idSet = new HashSet<>();

    public CheckerBoardTest(Properties property) throws IOException {
        this.property = (Properties) property.clone();
        set();
        read();
    }

    public CheckerBoardTest(ObservationEquation eq) {
        this.eq = eq;
    }

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
        }
        System.err.println(outPath + " is created.");
    }

    /**
     * @param args [a property file name]
     * @throws Exception if any
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

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("components")) property.setProperty("components", "Z R T");
        if (!property.containsKey("sourceTimeFunction")) property.setProperty("sourceTimeFunction", "0");
        if (!property.containsKey("timePartial")) property.setProperty("timePartial", "false");
        if (!property.containsKey("psvsh")) property.setProperty("psvsh", "0");
        if (!property.containsKey("modelName")) property.setProperty("modelName", "");
        if (!property.containsKey("noise")) property.setProperty("noise", "false");
        if (property.getProperty("noise").equals("true") && !property.containsKey("noisePower"))
            throw new RuntimeException("There is no information about 'noisePower'");
    }

    private void set() throws NoSuchFileException {
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));
        if (!Files.exists(workPath)) throw new NoSuchFileException(workPath + " (workPath)");
        waveIDPath = getPath("waveIDPath");
        waveformPath = getPath("waveformPath");
        partialIDPath = getPath("partialIDPath");
        partialWaveformPath = getPath("partialWaveformPath");
        unknownParameterListPath = getPath("unknownParameterListPath");
        inputDataPath = getPath("inputDataPath");
        if (!Files.exists(inputDataPath)) throw new NoSuchFileException(inputDataPath.toString());
        noise = Boolean.parseBoolean(property.getProperty("noise"));
        if (noise) noisePower = Double.parseDouble(property.getProperty("noisePower"));
        iterate = Boolean.parseBoolean(property.getProperty("iterate"));
    }

    private void readIDs() {
        List<double[]> ranges = new ArrayList<>();
        for (BasicID id : eq.getDVector().getObsIDs()) {
            stationSet.add(id.getStation());
            idSet.add(id.getGlobalCMTID());
            double[] range = new double[]{id.getMinPeriod(), id.getMaxPeriod()};
            if (ranges.isEmpty()) ranges.add(range);
            boolean exists = false;
            for (int i = 0; !exists && i < ranges.size(); i++)
                if (Arrays.equals(range, ranges.get(i))) exists = true;
            if (!exists) ranges.add(range);
        }
        this.ranges = ranges.toArray(new double[0][]);
    }

    private void read() throws IOException {
        BasicID[] ids = BasicIDFile.read(waveIDPath, waveformPath);
        Dvector dVector = new Dvector(ids);
        PartialID[] pids = PartialIDFile.read(partialIDPath, partialWaveformPath);
        List<UnknownParameter> parameterList = UnknownParameterFile.read(unknownParameterListPath);
        eq = new ObservationEquation(pids, parameterList, dVector);
    }

    /**
     * 読み込んだデータセットに対してボルン波形を観測波形として 理論波形を理論波形として書き込む（上書きではない）
     *
     * @param outIDPath   for write
     * @param outDataPath for write
     * @param bornVec     for write
     * @throws IOException if any
     */
    private void output4CheckerBoardTest(Path outIDPath, Path outDataPath, RealVector bornVec) throws IOException {
        // bornVec = dVector.getObsVectors();
        Objects.requireNonNull(bornVec);

        Dvector dVector = eq.getDVector();
        RealVector[] bornPart = dVector.separate(bornVec);
        System.err.println("outputting " + outIDPath + " " + outDataPath);
        try (WaveformDataWriter bdw = new WaveformDataWriter(outIDPath, outDataPath, stationSet, idSet, ranges)) {
            BasicID[] obsIDs = dVector.getObsIDs();
            BasicID[] synIDs = dVector.getSynIDs();
            for (int i = 0; i < dVector.getNTimeWindow(); i++) {
                double weighting = dVector.getWeighting(i);
                bdw.addBasicID(obsIDs[i].setData(bornPart[i].mapDivide(weighting).toArray()));
                bdw.addBasicID(synIDs[i].setData(dVector.getSynVectors()[i].mapDivide(weighting).toArray()));
            }
        }
    }

    /**
     * 読み込んだデータセットに対してボルン波形を理論波形として書き込む（上書きではない）
     *
     * @param outIDPath   {@link File} for write ID file
     * @param outDataPath {@link File} for write data file
     * @param bornVec     {@link RealVector} of born
     * @throws IOException if any
     */
    private void output4Iterate(Path outIDPath, Path outDataPath, RealVector bornVec) throws IOException {
        if (bornVec == null) {
            System.err.println("bornVec is not set");
            return;
        }
        Dvector dVector = eq.getDVector();
        RealVector[] bornPart = dVector.separate(bornVec);
        System.err.println("outputting " + outIDPath + " " + outDataPath);
        try (WaveformDataWriter bdw = new WaveformDataWriter(outIDPath, outDataPath, stationSet, idSet, ranges)) {
            BasicID[] obsIDs = dVector.getObsIDs();
            BasicID[] synIDs = dVector.getSynIDs();
            for (int i = 0; i < dVector.getNTimeWindow(); i++) {
                double weighting = dVector.getWeighting(i);
                bdw.addBasicID(obsIDs[i].setData(dVector.getObsVectors()[i].mapDivide(weighting).toArray()));
                bdw.addBasicID(synIDs[i].setData(bornPart[i].mapDivide(weighting).toArray()));
            }
        }
    }

    /**
     * Reads pseudoM
     */
    private RealVector readPseudoM() throws IOException {
        List<String> lines = Files.readAllLines(inputDataPath);
        if (lines.size() != eq.getMlength()) throw new RuntimeException("input model length is wrong");
        double[] pseudoM = lines.stream().mapToDouble(Double::parseDouble).toArray();
        return new ArrayRealVector(pseudoM, false);
    }

    /**
     * d = A m
     *
     * @param pseudoM &delta;m
     * @return d for the input pseudo M
     */
    public RealVector computePseudoD(RealVector pseudoM) {
        return eq.operate(pseudoM);
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
        readIDs();
        RealVector pseudoM = readPseudoM();
        RealVector pseudoD = computePseudoD(pseudoM);
        RealVector bornVec = pseudoD.add(getSynVector());
        String dateStr = Utilities.getTemporaryString();
        Path outIDPath = workPath.resolve("pseudoID" + dateStr + ".dat");
        Path outDataPath = workPath.resolve("pseudo" + dateStr + ".dat");

        if (noise) bornVec = bornVec.add(computeRandomNoise());
        if (iterate) output4Iterate(outIDPath, outDataPath, bornVec);
        else output4CheckerBoardTest(outIDPath, outDataPath, bornVec);

    }

    @Override
    public Path getWorkPath() {
        return workPath;
    }

}
