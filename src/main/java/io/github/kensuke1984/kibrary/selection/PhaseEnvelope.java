package io.github.kensuke1984.kibrary.selection;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACExtension;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Paint;

import javax.swing.JFrame;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;

/**
 * 
 * Implements the phase Envelope as defined in Fichtner et al. (2008).
 * 
 * @author Anselme
 * 
 * @version 1.0
 * 
 */
public class PhaseEnvelope implements Operation {
	
	private Properties property;
	
	public PhaseEnvelope(Properties property) {
		this.property = (Properties) property.clone();
		set();
	}
	
	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths.get(PhaseEnvelope.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan PhaseEnvelope");
			pw.println("##Path of a working folder (.)");
			pw.println("#workPath");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Path of a root folder containing observed dataset (.)");
			pw.println("#obsPath");
			pw.println("##Path of a root folder containing synthetic dataset (.)");
			pw.println("#synPath");
			pw.println("##boolean convolute (true)");
			pw.println("#convolute");
			pw.println("##double ratio (2.5)");
			pw.println("#ratio");
			pw.println("##double phaseMisfit (degrees) (75)");
			pw.println("#phaseMisfit");
			pw.println("##int np (1024)");
			pw.println("#np 1024");
			pw.println("##double tlen (s) (6553.6)");
			pw.println("#tlen 6553.6");
		}
	}
	
	private void checkAndPutDefaults() {
		if (!property.containsKey("workPath"))
			property.setProperty("workPath", "");
		if (!property.containsKey("components"))
			property.setProperty("components", "Z R T");
		if (!property.containsKey("obsPath"))
			property.setProperty("obsPath", "");
		if (!property.containsKey("synPath"))
			property.setProperty("synPath", "");
		if (!property.containsKey("convolute"))
			property.setProperty("convolute", "true");
		if (!property.containsKey("ratio"))
			property.setProperty("ratio", "2");
		if (!property.containsKey("phaseMisfit"))
			property.setProperty("phaseMisfit", "75");
		if (!property.containsKey("np"))
			property.setProperty("np", "1024");
		if (!property.containsKey("tlen"))
			property.setProperty("tlen", "6553.6");
	}
	
	private Path workPath;
	
	private void set() {
		checkAndPutDefaults();
		workPath = Paths.get(property.getProperty("workPath"));

		if (!Files.exists(workPath))
			throw new RuntimeException("The workPath: " + workPath + " does not exist");
		String date = Utilities.getTemporaryString();
		outputPath = workPath.resolve("timewindow" + date + ".dat");
		timewindowSet = Collections.synchronizedSet(new HashSet<>());
		components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		obsPath = getPath("obsPath");
		synPath = getPath("synPath");
		convolute = Boolean.parseBoolean(property.getProperty("convolute"));
		ratio = Double.parseDouble(property.getProperty("ratio"));
		phaseMisfit = Double.parseDouble(property.getProperty("phaseMisfit"));

	}
	
	public static void main(String[] args) throws Exception {
		Properties property = new Properties();
		if (args.length == 0)
			property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1)
			property.load(Files.newBufferedReader(Paths.get(args[0])));
		else
			throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");

		PhaseEnvelope phaseEnvelope = new PhaseEnvelope(property);
		System.err.println(PhaseEnvelope.class.getName() + " is going.");
		long startT = System.nanoTime();
		phaseEnvelope.run();
		System.err.println(
				PhaseEnvelope.class.getName() + " finished in " + Utilities.toTimeString(System.nanoTime() - startT));
	}
	
	@Override
	public void run() throws Exception {
		Utilities.runEventProcess(obsPath, obsEventDir -> {
			try {
				obsEventDir.sacFileSet().stream().filter(sfn -> sfn.isOBS() && components.contains(sfn.getComponent()))
					.forEach(obsname -> {
						Path synEventPath = synPath.resolve(obsEventDir.getGlobalCMTID().toString());
						if (!Files.exists(synEventPath))
							throw new RuntimeException(synEventPath + " does not exist.");
						
							try {
								String network = obsname.readHeader().getSACString(SACHeaderEnum.KNETWK);
								
								String stationString = obsname.getStationName() + "_" + network;
								GlobalCMTID id = obsname.getGlobalCMTID();
								SACComponent component = obsname.getComponent();
								String name = convolute
										? stationString + "." + id + "." + SACExtension.valueOfConvolutedSynthetic(component)
										: stationString + "." + id + "." + SACExtension.valueOfSynthetic(component);
								SACFileName synname = new SACFileName(synEventPath.resolve(name));
							
								double[][][] phaseEnvelope = computePhaseEnvelope(obsname, synname);
							
								showPhaseMisfit(phaseEnvelope[0], tlen, np);
							} catch (IOException e) {
								e.printStackTrace();
							}
					});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 10, TimeUnit.HOURS);
	}
	
	private double[][][] computePhaseEnvelope(SACFileName obsname, SACFileName synname) {
		double[][][] phaseEnvelope = new double[2][][];
		
		double endtime = 4000.;
		double starttime = 0;
		int n = (int) (endtime - starttime); // re-sampling at 1Hz
		int lsmooth = Integer.highestOneBit(n) < n ? n : n * 2; 
		
		phaseEnvelope[0] = new double[n][];
		phaseEnvelope[1] = new double[n][];
		
		for (int i = 0; i < n; i++) {
			phaseEnvelope[0][i] = new double[np+1];
			phaseEnvelope[1][i] = new double[np+1];
		}
		
		try {
			if (endtime > synname.readHeader().getValue(SACHeaderEnum.E))
				throw new IllegalArgumentException(synname + "end time smaller than the given end time: " + endtime);
			if (obsname.readHeader().getValue(SACHeaderEnum.B) > 0)
				throw new IllegalArgumentException(obsname + "start time > 0: " + obsname.readHeader().getValue(SACHeaderEnum.B));
			
			double[] tmpobsdata = obsname.read().createTrace().cutWindow(starttime, endtime).getY();
			double[] tmpsyndata = synname.read().createTrace().cutWindow(starttime, endtime).getY();
			
			double[] obsdata = new double[n];
			
			int sampling = (int) samplingHz;
			if (sampling != 20)
				System.err.println("Warning: sampling Hz != 20");
			
			for (int i = 0; i < n; i++) {
				obsdata[i] = tmpobsdata[i * sampling];
			}
			Arrays.fill(obsdata, n, lsmooth, 0.); // pad with 0 for the fft
			
			double dominantFrequency = new ArrayRealVector( Stream.of(fft.transform(obsdata, TransformType.FORWARD))
				.map(z -> z.abs()).toArray(Double[]::new) ).getSubVector(0, np + 1).getMaxIndex()
				* np / tlen;
			
			double sigma = 1. / dominantFrequency;
			
			int nn = 10 * (int) sigma + 10 < n ? 10 * (int) sigma + 10 : n;
			int llsmooth = Integer.highestOneBit(nn) < n ? n : n * 2;
			
			double[] gaborWindow = gaborWindow(sigma, 2*llsmooth, 1.);
			obsdata = new double[2*llsmooth];
			double[] syndata = new double[2*llsmooth];
			
			for (int i = 0; i < n; i++) {
				int nlow = i - nn < 0 ? 0 : i - nn;
				int nnlow = i - nn < 0 ? nn - i : 0;
				int k = 0;
				for (int j = nlow; j <= i+nn; j++) {
					obsdata[nnlow + k] = tmpobsdata[j * sampling] * gaborWindow[n - j - 1];
					syndata[nnlow + k] = tmpsyndata[j * sampling] * gaborWindow[n - j - 1];
					k++;
				}
				Arrays.fill(obsdata, 2*nn+1, llsmooth, 0.);
				Arrays.fill(syndata, 2*nn+1, llsmooth, 0.);
				Arrays.fill(obsdata, 0, nnlow, 0.);
				Arrays.fill(syndata, 0, nnlow, 0.);
				
				Complex[] synSpc = fft.transform(syndata, TransformType.FORWARD);
				Complex[] obsSpc = fft.transform(obsdata, TransformType.FORWARD);
				
				for (int j = 0; j < np+1; j++) {
					phaseEnvelope[0][i][j] = obsSpc[j].getArgument() - synSpc[j].getArgument();
					if (synSpc[j].abs() != 0)
						phaseEnvelope[1][i][j] = obsSpc[j].abs() / synSpc[j].abs();
					else
						phaseEnvelope[1][i][j] = Double.NaN;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return phaseEnvelope;
	}
	
	
	private static JFreeChart createChart(XYDataset dataset, int iy, int ix) {
		NumberAxis xAxis = new NumberAxis("x Axis");
        NumberAxis yAxis = new NumberAxis("y Axis");
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
        XYBlockRenderer r = new XYBlockRenderer();
        SpectrumPaintScale ps = new SpectrumPaintScale(0, iy * ix);
        r.setPaintScale(ps);
        r.setBlockHeight(10.0f);
        r.setBlockWidth(10.0f);
        plot.setRenderer(r);
        JFreeChart chart = new JFreeChart("Title",
            JFreeChart.DEFAULT_TITLE_FONT, plot, false);
        NumberAxis scaleAxis = new NumberAxis("Scale");
        scaleAxis.setAxisLinePaint(Color.white);
        scaleAxis.setTickMarkPaint(Color.white);
        PaintScaleLegend legend = new PaintScaleLegend(ps, scaleAxis);
        legend.setSubdivisionCount(128);
        legend.setAxisLocation(AxisLocation.TOP_OR_RIGHT);
        legend.setPadding(new RectangleInsets(10, 10, 10, 10));
        legend.setStripWidth(20);
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setBackgroundPaint(Color.WHITE);
        chart.addSubtitle(legend);
        chart.setBackgroundPaint(Color.white);
        return chart;
	}
	
	private static XYZDataset createDataset(double[][] xyzarray, double tlen, int np) {
		int ix = xyzarray.length;
		int iy = np + 1;
        DefaultXYZDataset dataset = new DefaultXYZDataset();
        for (int i = 0; i < ix; i++) {
            double[][] data = new double[3][iy];
            for (int j = 0; j < iy; j++) {
                data[0][j] = i;
                data[1][j] = j * np / tlen;
                data[2][j] = xyzarray[i][j];
            }
            dataset.addSeries("Series" + i, data);
        }
        return dataset;
    }
	
	private void showPhaseMisfit(double[][] xyphase, double tlen, int np) {
		show("phase misfit", xyphase, tlen, np);
	}
	
	private void showRatio(double[][] xyratio, double tlen, int np) {
		show("amplitude ration", xyratio, tlen, np);
	}
	
	private void show(String title, double[][] xydata, double tlen, int np) {
		JFrame f = new JFrame(title);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ChartPanel chartPanel = new ChartPanel(createChart(createDataset(xydata, tlen, np), xydata.length, xydata[0].length)) {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(640, 480);
            }
        };
        chartPanel.setMouseZoomable(true, false);
        f.add(chartPanel);
        f.pack();
        f.setLocationRelativeTo(null);
        f.setVisible(true);
	}
	
	protected double[] gaborWindow(double s, int n, double dt) {
		double a = 1 / Math.pow(Math.PI * s, 0.25);
		double b = -1. / (2 * s * s);
		return IntStream.range(0, n).mapToDouble(i -> a*Math.exp(b * i * dt)).toArray();
	}
	
	private Set<SACComponent> components;
	
	private Path synPath;
	
	private Path obsPath;
	
	private Path outputPath;
	
	private double ratio;
	
	private double phaseMisfit;
	
	private double samplingHz;
	
	private double tlen;
	
	private int np;
	
	boolean convolute;
	
	private Set<TimewindowInformation> timewindowSet;
	
	protected final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
	
	@Override
	public Properties getProperties() {
		return (Properties) property.clone();
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}
	
	
	private static class SpectrumPaintScale implements PaintScale {

        private static final float H1 = 0f;
        private static final float H2 = 1f;
        private final double lowerBound;
        private final double upperBound;

        public SpectrumPaintScale(double lowerBound, double upperBound) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
        }

        @Override
        public double getLowerBound() {
            return lowerBound;
        }

        @Override
        public double getUpperBound() {
            return upperBound;
        }

        @Override
        public Paint getPaint(double value) {
            float scaledValue = (float) (value / (getUpperBound() - getLowerBound()));
            float scaledH = H1 + scaledValue * (H2 - H1);
            return Color.getHSBColor(scaledH, 1f, 1f);
        }
    }

}
