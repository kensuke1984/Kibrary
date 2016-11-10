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
import org.jfree.data.xy.AbstractXYZDataset;
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
			pw.println("##double samplingHz (20)");
			pw.println("#samplingHz");
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
		if (!property.containsKey("samplingHz"))
			property.setProperty("samplingHz", "20");
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
		samplingHz = Double.parseDouble(property.getProperty("samplingHz"));
		tlen = Double.parseDouble(property.getProperty("tlen"));
		np = Integer.parseInt(property.getProperty("np"));
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
						
//							String network = obsname.readHeader().getSACString(SACHeaderEnum.KNETWK);
//							String stationString = obsname.getStationName() + "_" + network;
							String stationString = obsname.getStationName();
							GlobalCMTID id = obsname.getGlobalCMTID();
							SACComponent component = obsname.getComponent();
							String name = convolute
									? stationString + "." + id + "." + SACExtension.valueOfConvolutedSynthetic(component)
									: stationString + "." + id + "." + SACExtension.valueOfSynthetic(component);
							SACFileName synname = new SACFileName(synEventPath.resolve(name));
						
							double[][][] phaseEnvelope = computePhaseEnvelope(obsname, synname);
						
							String titleFig = stationString + "." + id;
							showPhaseMisfit(phaseEnvelope[0], titleFig);
					});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}, 10, TimeUnit.HOURS);
	}
	
	private double[][][] computePhaseEnvelope(SACFileName obsname, SACFileName synname) {
		double[][][] phaseEnvelope = new double[2][][];
		
		double endtime = 4000.;
		double margin = 750.;
		double starttime = 0;
		int n = (int) (endtime - starttime); // re-sampling at 1Hz
		int npow2 = Integer.highestOneBit(n) < n ? Integer.highestOneBit(n) * 2 : Integer.highestOneBit(n); 
		
		phaseEnvelope[0] = new double[np+1][];
		phaseEnvelope[1] = new double[np+1][];
		
		for (int i = 0; i < np+1; i++) {
			phaseEnvelope[0][i] = new double[n];
			phaseEnvelope[1][i] = new double[n];
		}
		
		try {
			if (endtime + margin > synname.readHeader().getValue(SACHeaderEnum.E))
				throw new IllegalArgumentException(synname + "end time smaller than the given end time: " + endtime);
			if (obsname.readHeader().getValue(SACHeaderEnum.B) > 0)
				throw new IllegalArgumentException(obsname + "start time > 0: " + obsname.readHeader().getValue(SACHeaderEnum.B));
			
			double[] tmpobsdata = obsname.read().createTrace().cutWindow(starttime, endtime + margin).getY();
			double[] tmpsyndata = synname.read().createTrace().cutWindow(starttime, endtime + margin).getY();
			
			double[] obsdata = new double[npow2];
			
			int sampling = (int) samplingHz;
			if (sampling != 20)
				System.err.println("Warning: sampling Hz != 20");
			
			for (int i = 0; i < n; i++) {
				obsdata[i] = tmpobsdata[i * sampling];
			}
			Arrays.fill(obsdata, n, npow2, 0.); // pad with 0 for the fft
			
			double dominantFrequency = new ArrayRealVector( Stream.of(fft.transform(obsdata, TransformType.FORWARD))
				.map(z -> z.abs()).toArray(Double[]::new) ).getSubVector(0, np + 1).getMaxIndex()
				* 1. / tlen;
			
			double sigma = 1. / dominantFrequency;
			System.out.println(sigma);
			sigma = 15;
			
			int nn = 10 * (int) sigma + 10 < n ? 10 * (int) sigma + 10 : n;
			int nnpow2 = Integer.highestOneBit(2*nn+1) < 2*nn+1 ? Integer.highestOneBit(2*nn+1) * 2 : Integer.highestOneBit(2*nn+1);
			nnpow2 = nnpow2 < 2048 ? 2048 : nnpow2;
			
			double[] gaborWindow = gaborWindow(sigma, 2*nn+1, 1.);
			obsdata = new double[nnpow2];
			double[] syndata = new double[nnpow2];
			
			for (int i = 0; i < n; i++) {
				int nlow = i - nn < 0 ? 0 : i - nn;
				int nnlow = i - nn < 0 ? nn - i : 0;
				int k = 0;
				for (int j = nlow; j <= i+nn; j++) {
					obsdata[nnlow + k] = tmpobsdata[j * sampling] * gaborWindow[2*nn - k];
					syndata[nnlow + k] = tmpsyndata[j * sampling] * gaborWindow[2*nn - k];
					k++;
				}
				Arrays.fill(obsdata, 2*nn+1, nnpow2, 0.);
				Arrays.fill(syndata, 2*nn+1, nnpow2, 0.);
				Arrays.fill(obsdata, 0, nnlow, 0.);
				Arrays.fill(syndata, 0, nnlow, 0.);
				
				Complex[] synSpc = fft.transform(syndata, TransformType.FORWARD);
				Complex[] obsSpc = fft.transform(obsdata, TransformType.FORWARD);
				
				for (int j = 0; j < np+1; j++) {
					if (synSpc[j].abs() == 0 || obsSpc[j].abs() == 0)
						phaseEnvelope[0][j][i] = Math.acos((obsSpc[j].getReal()*synSpc[j].getReal() + obsSpc[j].getImaginary()*obsSpc[j].getImaginary())
							/ (obsSpc[j].abs() * synSpc[j].abs())) * 180 / Math.PI;
					else
						phaseEnvelope[0][j][i] = Double.NaN;
					if (synSpc[j].abs() != 0)
						phaseEnvelope[1][j][i] = obsSpc[j].abs() / synSpc[j].abs();
					else
						phaseEnvelope[1][j][i] = Double.NaN;
				}
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		}
		
		return phaseEnvelope;
	}
	
	private static JFreeChart createChart(XYDataset dataset, int lowerbound, int upperbound, String title, double dx, double dy) {
		NumberAxis xAxis = new NumberAxis("Time from origin time (s)");
        NumberAxis yAxis = new NumberAxis("Frequency (Hz)");
        XYPlot plot = new XYPlot(dataset, xAxis, yAxis, null);
        XYBlockRenderer r = new XYBlockRenderer();
        SpectrumPaintScale ps = new SpectrumPaintScale(lowerbound, upperbound);
        r.setPaintScale(ps);
        r.setBlockHeight(dy);
        r.setBlockWidth(dx);
        plot.setRenderer(r);
        JFreeChart chart = new JFreeChart(title,
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
	
	private static XYZDataset createDataset(double[][] data, int xresample, int yresample, double dx, double dy) 
			throws IllegalArgumentException {
		if (Integer.highestOneBit(yresample) != yresample)
			throw new IllegalArgumentException("Error: y resampling factor should be a power of 2");
		double[][] dataResample = new double[data.length/yresample][];
		for (int j = 0; j < dataResample.length; j++) {
			dataResample[j] = new double[data[0].length/xresample];
			for (int i = 0; i < dataResample[0].length; i++) {
				dataResample[j][i] = data[j*yresample][i*xresample];
			}
		}
        XYZArrayDataset dataset = new XYZArrayDataset(dataResample, dx*xresample, dy*yresample);
        return dataset;
    }
	
	private void showPhaseMisfit(double[][] xyphase, String title) {
		show("phase misfit", title, xyphase, -90, 90);
	}
	
	private void showRatio(double[][] xyratio, String title) {
		show("amplitude ratio", title, xyratio, -4, 4);
	}
	
	private void show(String titleFrame, String titleFig, double[][] xydata, int lowerbound, int upperbound) {
		JFrame f = new JFrame(titleFrame);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        ChartPanel chartPanel = new ChartPanel(createChart(createDataset(xydata, 10, 4, 1., 1/tlen)
        		, lowerbound, upperbound, titleFig, 10., 4.*1./tlen)) {
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
		if (n % 2 == 1)
			return IntStream.range(-(n-1)/2, (n-1)/2+1).mapToDouble(i -> a*Math.exp(b * i * dt)).toArray();
		else
			return IntStream.range(-n/2-1, n/2).mapToDouble(i -> a*Math.exp(b * i * dt)).toArray();
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
	
	 private static class XYZArrayDataset extends AbstractXYZDataset {
	      double[][] data;
	      int rowCount = 0;
	      int columnCount = 0;
	      double dx;
	      double dy;
	      
	      XYZArrayDataset(double[][] data, double dx, double dy){
	         this.data = data;
	         rowCount = data.length;
	         columnCount = data[0].length;
	         this.dx = dx;
	         this.dy = dy;
	      }
	      public int getSeriesCount(){
	         return 1;
	      }
	      public Comparable getSeriesKey(int series){
	         return "serie";
	      }
	      public int getItemCount(int series){
	         return rowCount*columnCount;
	      }
	      public double getXValue(int series,int item){
	         return (int)(item/columnCount) * dx;
	      }
	      public double getYValue(int series,int item){
	         return (item % columnCount) * dy;
	      }
	      public double getZValue(int series,int item){
	         return data[(int)(item/columnCount)][item % columnCount];
	      }
	      public Number getX(int series,int item){
	         return new Double((int)(item/columnCount)) * dx;
	      }
	      public Number getY(int series,int item){
	         return new Double(item % columnCount) * dy;
	      }
	      public Number getZ(int series,int item){
	         return new Double(data[(int)(item/columnCount)][item % columnCount]);
	      }
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
//            float scaledValue = (float) (value / (getUpperBound() - getLowerBound()));
//            float scaledH = H1 + scaledValue * (H2 - H1);
            if (value < -75 && ! Double.isNaN(value))
            	return Color.BLUE;
            else if (value >= -75 && value < -60)
            	return Color.CYAN.darker();
            else if (value >= -60 && value < -45)
            	return Color.GREEN.darker();
            else if (value >= -45 && value < -30)
            	return Color.GREEN;
            else if (value >= -30 && value < -15)
            	return Color.lightGray;
            else if (value >= -15 && value <= 15)
            	return Color.WHITE;
            else if (value > 15 && value <= 30)
            	return Color.YELLOW;
            else if (value > 30 && value <= 45)
            	return Color.ORANGE.brighter();
            else if (value > 45 && value <= 60)
            	return Color.ORANGE;
            else if (value > 60 && value <= 75)
            	return Color.ORANGE.darker();
            else if (value > 75 && ! Double.isNaN(value))
            	return Color.RED;
            else if (Double.isNaN(value))
            	return Color.white;
            else {
            	throw new IllegalArgumentException(String.valueOf(value));
            }
        }
    }

}
