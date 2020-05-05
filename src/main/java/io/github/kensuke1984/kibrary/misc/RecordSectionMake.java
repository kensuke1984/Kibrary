package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.butterworth.BandPassFilter;
import io.github.kensuke1984.kibrary.butterworth.ButterworthFilter;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * @version 0.0.1
 * @since 2020/04/28
 * @author Yuki Suzuki
 *
 */

public class RecordSectionMake {

	public static void main(String[] args) {

		double dt = 0.05;	// delta t (default is 0.05)
		double rf = 8.4; 	// ray parameter reduction (s/degree)	7. or 8.4 ?
		int int_convolve_shift = 0;

//		Path workDir = Paths.get("/Users/yuki/Dropbox/Research/TA/ensyu2020/working/manyStations/spcsac/201506231218A/6sec/test");
		Path workDir = Paths.get("/Users/yuki/Dropbox/Research/TA/ensyu2020/working/manyStations/spcsac/201506231218A/6sec");
		File travelTimeFileSH = new File("/Users/yuki/Dropbox/Research/TA/ensyu2020/working/ttData/ttData_MIASP_PSV_4rs_SH_S.inf");
		File travelTimeFileSV = new File("/Users/yuki/Dropbox/Research/TA/ensyu2020/working/ttData/ttData_MIASP_PSV_4rs_SV_S.inf");
		File travelTimeFileVertical = new File("/Users/yuki/Dropbox/Research/TA/ensyu2020/working/ttData/ttData_MIASP_PSV_4rs_SV_S.inf");

		double EdStart = 60.; // epicentral distance from which the data are treated (degree)
		double EdEnd = 110;

		//directory
		Path inDIR = workDir.resolve("");
		Path outDIR = inDIR;

		//filter
		boolean applyFilter = false;
		double upperFrequency = 20; // Hz
		double lowerFrequency = 0.001; // Hz
		RealVector stackedSH = new ArrayRealVector();
		RealVector stackedPSV = new ArrayRealVector();
		RealVector stackedVertical = new ArrayRealVector();
		RealVector timeStackedSH = new ArrayRealVector();
		RealVector timeStackedPSV = new ArrayRealVector();
		RealVector timeStackedVertical = new ArrayRealVector();

		// integration
		boolean integration = false;
		double amplitude;

		// time window
		boolean applyTimeWindow = true;
		double frontShift = 3000;// 60.; // s
		double rearShift = 1000;// 45.; // s

		// stack
		boolean stack = false;
		double edMinStack = 100.;
		double edMaxStack = 115.;
		boolean firststack = true;

		// noise
		boolean noise = false;

		// convolution
		String fm = "20.0Hz"; // peak frequency (Hz) in the ricker wavelet used
								// to convolve the sac data

		//convolution
		boolean convolution = false;
		double dtinf = rearShift*2;
		double dtsup = frontShift*2;

		// amplification
		boolean autoAmplify = false;
		double frontShiftAmply = 1800; //1.5
		double rearShiftAmply = 700;
		// double defaultPsvAmply = 5e-7;
		// double defaultSHAmply = 8e-7;
		double defaultPsvAmply = 8e-6;
		double defaultSHAmply = 2e-5;
		double defaultVerticalAmply = 3e-5;
		double defaultOneAmply = 1e-7;
		boolean inversePolarisationSV = true;
		boolean inversePolarisationSH = false;
		boolean inversePolarisationVertical = false;

		// peak amplitude
		boolean peakAmplitude = true;
		List<Double> distance = new ArrayList<>();
		List<Double> peakSH = new ArrayList<>();
		List<Double> peakSV = new ArrayList<>();
		List<Double> peakVertical = new ArrayList<>();

		// cross-correlation
		boolean correlation = false;
		int icorr = 1000;
		double tcorr = 10;
		double dtcorr = tcorr / icorr;
		double maxCorr = 0;
		double rearShiftCorr = 5.;
		double frontShiftCorr = 12;
		double[] except = new double[] { 78, 79, 80, 81, 82, 83, 84, 91, 92, 93 }; // iasp91
		boolean forceNegCorr = false;
		boolean forcePosCorr = false;


		// out
		String writeName = null;
		String writeNameSH = null;
		String writeNamePSV = null;
		String writeNameVertical = null;
		String writeNameCorr = null;
		String writeNameMaxCorr = null;
		String writeNameDiff = null;

		// get travel time information
		double[] degFromFileSH = null;
		double[] timeFromFileSH = null;
		double[] degFromFileSV = null;
		double[] timeFromFileSV = null;
		double[] degFromFileVertical = null;
		double[] timeFromFileVertical = null;

		try {
			FileInputStream fisSH = new FileInputStream(travelTimeFileSH);
			FileInputStream fisSV = new FileInputStream(travelTimeFileSV);
			FileInputStream fisVertical = new FileInputStream(travelTimeFileVertical);
			BufferedReader brSH = new BufferedReader(new InputStreamReader(fisSH));
			BufferedReader brSV = new BufferedReader(new InputStreamReader(fisSV));
			BufferedReader brVertical = new BufferedReader(new InputStreamReader(fisVertical));

			// count lines
			int nlinesSH, nlinesSV, nlinesVertical;
			byte[] c = new byte[1024];
			int count = 0;
			int readChars = 0;
			boolean empty = true;
			while ((readChars = fisSH.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}

			nlinesSH = (count == 0 && !empty) ? 1 : count;
//			System.out.println(nlinesSH);

			count = 0;
			readChars = 0;
			empty = true;
			while ((readChars = fisSV.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}

			nlinesSV = (count == 0 && !empty) ? 1 : count;

			count = 0;
			readChars = 0;
			empty = true;
			while ((readChars = fisVertical.read(c)) != -1) {
				empty = false;
				for (int i = 0; i < readChars; ++i) {
					if (c[i] == '\n') {
						++count;
					}
				}
			}

			nlinesVertical = (count == 0 && !empty) ? 1 : count;

			FileChannel fcSH = fisSH.getChannel();
			FileChannel fcSV = fisSV.getChannel();
			FileChannel fcVertical = fisVertical.getChannel();

			fcSH.position(0);
			fcSV.position(0);
			fcVertical.position(0);

			degFromFileSH = new double[nlinesSH];
			timeFromFileSH = new double[nlinesSH];
			degFromFileSV = new double[nlinesSV];
			timeFromFileSV = new double[nlinesSV];
			degFromFileVertical = new double[nlinesVertical];
			timeFromFileVertical = new double[nlinesVertical];

			String line = null;
			int k = 0;
			while ((line = brSH.readLine()) != null) {
//				System.out.println(k);
				String[] splitted = line.split(" ");
				degFromFileSH[k] = Double.valueOf(splitted[2]);
				timeFromFileSH[k] = Double.valueOf(splitted[3]);
//				System.out.println(splitted[0]+" "+splitted[1]);
				k++;
			}

			k = 0;
			while ((line = brSV.readLine()) != null) {
				String[] splitted = line.split(" ");
				degFromFileSV[k] = Double.valueOf(splitted[2]);
				timeFromFileSV[k] = Double.valueOf(splitted[3]);
				k++;
			}

			k = 0;
			while ((line = brVertical.readLine()) != null) {
				String[] splitted = line.split(" ");
				degFromFileVertical[k] = Double.valueOf(splitted[2]);
				timeFromFileVertical[k] = Double.valueOf(splitted[3]);
				k++;
			}

			fisSH.close();
			brSH.close();
			fisSV.close();
			brSV.close();
			fisVertical.close();
			brVertical.close();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		// apply modification and convert sac to txt
		int ip, is, iv, nsac;
		File folder = new File(inDIR.toString());
		File[] listOfFiles = folder.listFiles();
		File[] psvArray = new File[500];
		File[] shArray = new File[500];
		File[] vertical = new File[500];
//		System.out.println(listOfFiles.toString());
		ip = 0;
		is = 0;
		iv = 0;
		nsac = 0;
		for (int i = 0; i < listOfFiles.length; i++) {
			if (listOfFiles[i].isFile()) {
				File file = new File(listOfFiles[i].getAbsolutePath());
//				System.out.println(listOfFiles[i].getName());
				if (!file.exists()) {
					System.err.println("File not found");
					return;
				}
				if (integration) {
					if (file.getName().endsWith(".Rs-int")) {
						nsac++;
						psvArray[ip] = file;
						ip++;
					} else if (file.getName().endsWith(".Ts-int")) {
						nsac++;
						shArray[is] = file;
						is++;
					}
					else if (file.getName().endsWith(".Zs-int")){
						nsac++;
						vertical[is] = file;
						iv++;
					}
				} else {
					if (file.getName().endsWith(".Rsc")) { // "Rs_fm"+fm+".sac"
						nsac++;
						psvArray[ip] = file;
						ip++;
					} else if (file.getName().endsWith(".Tsc")) {
						nsac++;
						shArray[is] = file;
						is++;
					} else if (file.getName().endsWith(".Zsc")) {
						nsac++;
						vertical[iv] = file;
						iv++;
					}

				}
			}
		}

		if (nsac == 0) {
			System.out.println("No sac files in this directory");
			return;
		}
		else if (is > 0 && is == ip) {
			System.out.println("Converting sac files to txt...");

			if (applyFilter) {
				writeNameMaxCorr = outDIR + "/maxCorrelation_Rs" + String.valueOf(upperFrequency) + "Hz.txt";
				writeNameDiff = outDIR + "/tt_sh_sv_diff" + String.valueOf(upperFrequency) + "Hz.txt";
			} else {
				writeNameMaxCorr = outDIR + "/maxCorrelation_Rs.txt";
				writeNameDiff = outDIR + "/tt_sh_sv_diff.txt";
			}

			try {
				PrintWriter writerMaxCorr = new PrintWriter(writeNameMaxCorr);
				PrintWriter writerDiff = new PrintWriter(writeNameDiff);

				for (int i = 0; i < ip; i++) {

					// get the time series
					SACFileName sacSH = new SACFileName(Paths.get(shArray[i].toString()));
					SACFileName sacPSV = new SACFileName(Paths.get(psvArray[i].toString()));
					SACFileName sacVertical = new SACFileName(Paths.get(vertical[i].toString()));
					System.out.println(sacSH+" "+sacPSV+" "+sacVertical);

					// this is read part
					SACData shData = sacSH.read();
					SACData psvData = sacPSV.read();
					SACData verticalData = sacVertical.read();
					System.out.println(shData.getValue(SACHeaderEnum.GCARC)+" "+psvData.getValue(SACHeaderEnum.GCARC)
										+" "+verticalData.getValue(SACHeaderEnum.GCARC));

					// apply filter
					if (applyFilter) {
						ButterworthFilter filter = new BandPassFilter(upperFrequency, lowerFrequency, 4);
						filter.setBackward(false);
						shData = shData.applyButterworthFilter(filter);
						psvData = psvData.applyButterworthFilter(filter);
						verticalData = verticalData.applyButterworthFilter(filter);
					}

					// original double ed =
					// sacSH.getgetStation().getPosition().getEpicentralDistance(new
					// HorizontalPosition(0, 0))*180/Math.PI;
					double edsh = shData.getStation().getPosition().getEpicentralDistance(new HorizontalPosition(0, 0))
							* 180 / Math.PI;
					double edpsv = psvData.getStation().getPosition().getEpicentralDistance(new HorizontalPosition(0, 0))
							* 180 / Math.PI;
					double edvertical = verticalData.getStation().getPosition().getEpicentralDistance(new HorizontalPosition(0, 0))
							* 180 / Math.PI;
							// sorry ill add #getStation() now please use
							// Station#of(SACHeaderData)
					System.out.println(edsh+" "+edpsv+" "+edvertical);

					// if (ed < 0.) {
					// ed = 360. + ed;
					// }

					if ((edsh >= EdStart && edsh <= EdEnd)
							|| (edpsv >= EdStart && edpsv <= EdEnd)
							|| (edvertical >= EdStart && edvertical <= EdEnd)) {

						Trace waveformSH = shData.createTrace();
						Trace waveformPSV = psvData.createTrace();
						Trace waveformVertical = verticalData.createTrace();

						// realvector
						RealVector ampSH = new ArrayRealVector(waveformSH.getY());
						RealVector timeSH = new ArrayRealVector(waveformSH.getX());
						RealVector ampPSV = new ArrayRealVector(waveformPSV.getY());
						RealVector timePSV = new ArrayRealVector(waveformPSV.getX());
						RealVector ampVertical = new ArrayRealVector(waveformVertical.getY());
						RealVector timeVertical = new ArrayRealVector(waveformVertical.getX());

						double ttSH = 0;
						double ttSV = 0;
						double ttVertical = 0;

						if (applyTimeWindow || autoAmplify) {
							ttSH = extrapolate(edsh, timeFromFileSH, degFromFileSH);
							ttSV = extrapolate(edpsv, timeFromFileSV, degFromFileSV);
							ttVertical = extrapolate(edvertical, timeFromFileVertical, degFromFileVertical);
						}
//						System.out.print(shArray[i].getName() + " " + ed + " " + ttSH + " " + ttSV + " " + ttVertical + "\n");

						// convolve
						// if (convolution) {
						// double tinfSH = ttSH - dtinf;
						// double tsupSH = ttSH + dtsup;
						// double tinfSV = ttSV - dtinf;
						// double tsupSV = ttSV + dtsup;
						//
						// ampSH = convolveGaussian(ampSH, timeSH, 5, tinfSH,
						// tsupSH);
						// ampPSV = convolveGaussian(ampPSV, timePSV, 5, tinfSV,
						// tsupSV);
						// }

						// amplify data


						//startとendのindex
						int startSH = (int) ((ttSH - rearShiftAmply) / dt) + int_convolve_shift;
						int endSH = (int) ((ttSH + frontShiftAmply) / dt) + int_convolve_shift;
						int startSV = (int) ((ttSV - rearShiftAmply) / dt) + int_convolve_shift;
						int endSV = (int) ((ttSV + frontShiftAmply) / dt) + int_convolve_shift;
						int startVertical = (int) ((ttVertical - rearShiftAmply) / dt) + int_convolve_shift;
						int endVertical = (int) ((ttVertical + frontShiftAmply) / dt) + int_convolve_shift;

						if (startSH < 0)
							startSH = 0;
						if (startSV < 0)
							startSV = 0;
						if (startVertical < 0)
							startVertical = 0;
						if (endSH > ampSH.getDimension())
							endSH = ampSH.getDimension();
						if (endSV > ampPSV.getDimension())
							endSV = ampPSV.getDimension();
						if (endVertical > ampVertical.getDimension())
							endVertical = ampVertical.getDimension();

						RealVector tmp = ampSH.getSubVector(startSH, endSH - startSH);
						double maxSH = (Math.abs(tmp.getMaxValue()) >= Math.abs(tmp.getMinValue()))
								? Math.abs(tmp.getMaxValue()) : Math.abs(tmp.getMinValue());
						tmp = ampPSV.getSubVector(startSV, endSV - startSV);
						double maxPSV = (Math.abs(tmp.getMaxValue()) >= Math.abs(tmp.getMinValue()))
								? Math.abs(tmp.getMaxValue()) : Math.abs(tmp.getMinValue());
						tmp = ampVertical.getSubVector(startVertical, endVertical - startVertical);
						double maxVertical = (Math.abs(tmp.getMaxValue()) >= Math.abs(tmp.getMinValue()))
								? Math.abs(tmp.getMaxValue()) : Math.abs(tmp.getMinValue());
//								System.out.println(maxSH);

						if (autoAmplify) {
							ampSH = ampSH.mapDivide(maxSH);
							ampPSV = ampPSV.mapDivide(maxSH); //maxPSV
							ampVertical = ampVertical.mapDivide(maxSH);
						} else {
							ampSH = ampSH.mapDivide(defaultSHAmply);
							ampPSV = ampPSV.mapDivide(defaultPsvAmply);
							ampVertical = ampVertical.mapDivide(defaultVerticalAmply);
						}

						if (peakAmplitude) {
							distance.add(edsh);
							peakSH.add(maxSH);
							peakSV.add(maxPSV);
							peakVertical.add(maxVertical);
						}

						if (inversePolarisationSV) {
							ampPSV = ampPSV.mapMultiply(-1.);
						}
						if (inversePolarisationSH) {
							ampSH = ampSH.mapMultiply(-1);
						}
						if (inversePolarisationVertical) {
							ampVertical = ampVertical.mapMultiply(-1.);
						}

						// apply (shifted) time window and add ed to amplitude
						// data
						if (applyTimeWindow) {
							double shiftsh = edsh * rf;
							double shiftpsv = edpsv * rf;
							double shiftvertical = edvertical * rf;

							int start = (int) ((ttSH - rearShift) / dt);
							int end = (int) ((ttSV + frontShift) / dt);

							if (start < 0)
								start = 0;
							if(end > ampSH.getDimension())
								end = ampSH.getDimension();
							if(endSV > ampPSV.getDimension())
								endSV = ampPSV.getDimension();
							if(endVertical > ampVertical.getDimension())
								endVertical = ampVertical.getDimension();

							ampSH = ampSH.getSubVector(start + int_convolve_shift, end - start).mapAdd(edsh);
							timeSH = timeSH.getSubVector(start, end - start).mapAdd(-shiftsh);
							ampPSV = ampPSV.getSubVector(start + int_convolve_shift, end - start).mapAdd(edpsv);
							timePSV = timePSV.getSubVector(start, end - start).mapAdd(-shiftpsv);
							ampVertical = ampVertical.getSubVector(start + int_convolve_shift, end - start).mapAdd(edvertical);
							timeVertical = timeVertical.getSubVector(start, end - start).mapAdd(-shiftvertical);
						}
						else {
							double shiftsh = edsh * rf;
							double shiftpsv = edpsv * rf;
							double shiftvertical = edvertical * rf;
//							System.out.println(edsh+" "+edpsv+" "+edvertical);

							ampSH = ampSH.mapAdd(edsh);
							timeSH = timeSH.mapAdd(shiftsh);
							ampPSV = ampPSV.mapAdd(edsh);
							timePSV = timePSV.mapAdd(shiftpsv);
							ampVertical = ampVertical.mapAdd(edvertical);
							timeVertical = timeVertical.mapAdd(shiftvertical);
						}

						if (stack) {
							if (edsh >= edMinStack && edsh <= edMaxStack) {
								if (firststack) {
									stackedSH = new ArrayRealVector(ampSH.getDimension());
									stackedPSV = new ArrayRealVector(ampSH.getDimension());
									stackedSH = stackedSH.add(ampSH.mapAdd(-edsh));
									stackedPSV = stackedPSV.add(ampPSV.mapAdd(-edsh));
									stackedVertical = stackedVertical.add(ampVertical.mapAdd(-edsh));
									stackedVertical = stackedVertical.add(ampVertical.mapAdd(-edsh));
									firststack = false;
								}
								else {
								stackedSH = stackedSH.add(ampSH.mapAdd(-edsh));
								stackedPSV = stackedPSV.add(ampPSV.mapAdd(-edsh));
								}
							}
						}

						// cross-correlation
						RealVector shiftCorr = new ArrayRealVector(new double[icorr]);
						RealVector corr = new ArrayRealVector(new double[icorr]);

						if (correlation) {
							double start = ttSH - rearShiftCorr;
							double end = ttSH + frontShiftCorr;

							for (int j = 0; j < icorr; j++) {
								shiftCorr.setEntry(j, j * dtcorr - tcorr / 2);
//								System.out.println(waveformSH.getLength()+" "+waveformPSV.getLength());
								RealVector shV = waveformSH.cutWindow(start, end).getYVector();
								RealVector psvV = waveformPSV
										.cutWindow(start + shiftCorr.getEntry(j), end + shiftCorr.getEntry(j))
//										.cutWindow(start, end)
										.getYVector();
//								System.out.println(j+" "+shV.getDimension()+" "+psvV.getDimension());

								corr.setEntry(j, shV.dotProduct(psvV) / shV.getNorm() / psvV.getNorm());
							}

							int imax = Math.abs(corr.getMaxValue()) > Math.abs(corr.getMinValue()) ? corr.getMaxIndex()
									: corr.getMinIndex();
							if (forceNegCorr) {
								imax = corr.getMinIndex();
							}
							if (forcePosCorr) {
								imax = corr.getMaxIndex();
							}
							maxCorr = shiftCorr.getEntry(imax);
						}

						// write in txt file
						if (integration) {
							if (applyFilter) {
								writeNameSH = outDIR + "/" + shArray[i].getName().split("-")[0] + "_fm" + fm
										+ "_int.txt";
								writeNamePSV = outDIR + "/" + psvArray[i].getName().split("-")[0] + "_fm" + fm
										+ "_int.txt";
								writeNameVertical = outDIR + "/" + psvArray[i].getName().split("-")[0] + "_fm" + fm
										+ "_int.txt";
								writeNameCorr = outDIR + "/" + shArray[i].getName().split(".")[0] + "_correlation"
										+ "_fm" + fm + ".txt";
							} else {
								writeNameSH = outDIR + "/" + shArray[i].getName().split("-")[0] + "_int.txt";
								writeNamePSV = outDIR + "/" + psvArray[i].getName().split("-")[0] + "_int.txt";
								writeNameVertical = outDIR + "/" + psvArray[i].getName().split("-")[0] + "_int.txt";
								writeNameCorr = outDIR + "/" + shArray[i].getName().split(".")[0] + "_correlation.txt";
							}
						}
							else {
							if (applyFilter) {
								writeNameSH = outDIR + "/" + shArray[i].getName() + "_" + String.valueOf(upperFrequency)
										+ "Hz.txt";
								writeNamePSV = outDIR + "/" + psvArray[i].getName() + "_"
										+ String.valueOf(upperFrequency) + "Hz.txt";
								writeNameVertical = outDIR + "/" + vertical[i].getName() + "_"
										+ String.valueOf(upperFrequency) + "Hz.txt";
								writeNameCorr = outDIR + "/" + shArray[i].getName().split("\\.")[0] + "_correlation_"
										+ String.valueOf(upperFrequency) + "Hz.txt";
							} else {
								writeNameSH = outDIR + "/" + shArray[i].getName() + ".txt";
								writeNamePSV = outDIR + "/" + psvArray[i].getName() + ".txt";
								writeNameVertical = outDIR + "/" + vertical[i].getName() + ".txt";
								writeNameCorr = outDIR + "/" + shArray[i].getName().split("\\.")[0]
										+ "_correlation.txt";
							}
						}


						String writeStackNamePSV = outDIR + "/" + "stackPSV.txt";
						String writeStackNameSH = outDIR + "/" + "stackSH.txt";
						String writeStackNameVertical = outDIR + "/" + "stackVertical.txt";


						try {
							PrintWriter writerSH = new PrintWriter(writeNameSH);
							PrintWriter writerPSV = new PrintWriter(writeNamePSV);
							PrintWriter writerVertical = new PrintWriter(writeNameVertical);
							PrintWriter writerCorr = null;
							if (correlation) {
								writerCorr = new PrintWriter(writeNameCorr);
							}

							int nSH = ampSH.getDimension();
							int nPSV = ampPSV.getDimension();
							int nVertical = ampVertical.getDimension();

							int resample = 2;	//S:20 or 5 or 2 P:2

							for (int j = 0; j < nSH / resample; j++) {
								writerSH.printf("%f %f%n", timeSH.getEntry(j * resample), ampSH.getEntry(j * resample));
							}

							for (int j = 0; j < nPSV / resample; j++) {
								writerPSV.printf("%f %f%n", timePSV.getEntry(j * resample), ampPSV.getEntry(j * resample));
							}
							for (int j = 0; j < nVertical / resample; j++) {
								writerVertical.printf("%f %f%n", timeVertical.getEntry(j * resample), ampVertical.getEntry(j * resample));
							}

							if (correlation) {
								for (int j = 0; j < icorr; j++) {
									writerCorr.printf("%f %f%n", shiftCorr.getEntry(j), corr.getEntry(j));
								}
							}
							if (!isIn(except, edsh, 0.05)) {
								writerMaxCorr.printf("%f %f%n", edsh, maxCorr);
							}

							writerSH.close();
							writerPSV.close();
							writerVertical.close();
							if (correlation) {
								writerCorr.close();
							}
						} catch (FileNotFoundException e) {
							e.printStackTrace();
						}
					}
				}

				// compute difference in SV SH travel time curves
				for (int i = 0; i < 100; i++) {
					double ed = EdStart + (EdEnd - EdStart) / 99. * i;
					double diff = extrapolate(ed, timeFromFileSV, degFromFileSV)
							- extrapolate(ed, timeFromFileSH, degFromFileSH);

					writerDiff.printf("%f %f%n", ed, diff);
				}

				writerMaxCorr.close();
				writerDiff.close();
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		} else {
			System.err.println("Error: either empty directory or not same number of sh and psv records.");
		}

		String writeStackNamePSV = outDIR + "/" + "stackPSV.txt";
		String writeStackNameSH = outDIR + "/" + "stackSH.txt";
		String writeStackNameVertical = outDIR + "/" + "stackVertical.txt";
		try {
			PrintWriter writerStackSH = new PrintWriter(writeStackNameSH);
			PrintWriter writerStackPSV = new PrintWriter(writeStackNamePSV);
			PrintWriter writerStackVertical = new PrintWriter(writeStackNameVertical);

			for (int j = 0; j < stackedSH.getDimension(); j++) {
				writerStackSH.printf("%f %f%n", dt*j, stackedSH.getEntry(j));
			}
			for (int j = 0; j < stackedPSV.getDimension(); j++) {
				writerStackPSV.printf("%f %f%n", dt*j, stackedPSV.getEntry(j));
			}
			for (int j = 0; j < stackedVertical.getDimension(); j++) {
				writerStackVertical.printf("%f %f%n", dt*j, stackedVertical.getEntry(j));
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (peakAmplitude) {
			List<Pair> pairs = new ArrayList<>();
			IntStream.range(0, distance.size())
				.forEach(i -> pairs.add(new Pair(i, distance.get(i))));
			pairs.sort((a, b) -> a.compareTo(b));

			double[] maxSH = new double[] {0.};
			peakSH.stream().forEach(p -> {
				if (p > maxSH[0])
					maxSH[0] = p;
			});
			try {
				Files.write(Paths.get(outDIR.toString(), "peak.txt"), "".getBytes(), StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE);
				IntStream.range(0, distance.size()).forEach(i -> {
					try {
						Files.write(Paths.get(outDIR.toString(), "peak.txt"),
								(String.valueOf(pairs.get(i).value) + " " + String.valueOf(peakSH.get(pairs.get(i).index) / maxSH[0]) + " " + String.valueOf(peakSV.get(pairs.get(i).index) / maxSH[0]) + "\n").getBytes()
								, StandardOpenOption.APPEND);
					} catch (IOException e) {
						e.printStackTrace();
					}
				});
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		System.out.println("Done!");

	}


	private static RealVector convolveGaussian(RealVector amp, RealVector time, double s, double tinf, double tsup) {
		RealVector convolved = new ArrayRealVector(amp.getDimension());
		double c = 0;
		double h1, h2, h3;
		double tau;
		int pos;
		double tauInf = -(tsup - tinf), tauSup = (tsup - tinf);
		int jmax = (int) ((tauSup - tauInf) / 0.05);

		for (int i = 0; i < amp.getDimension(); i++) {
			c = 0;
			if (time.getEntry(i) >= tinf && time.getEntry(i) <= tsup) {
				for (int j = 0; j < jmax; j++) {
					tau = tauInf + j * 0.05;
					pos = (int) ((time.getEntry(i) - tau) / 0.05);
					if (pos < 0) {
						pos = 0;
					} else if (pos > amp.getDimension() - 2) {
						pos = amp.getDimension() - 2;
					}
					h1 = 1 / Math.sqrt(2 * Math.PI * s * s) * Math.exp(-tau * tau / (2 * s * s)) * amp.getEntry(pos);
					h2 = 1 / Math.sqrt(2 * Math.PI * s * s)
							* Math.exp(-0.5 * (2 * tau + 0.05) * (2 * tau + 0.05) / (2 * s * s)) * 0.5
							* (amp.getEntry(pos) + amp.getEntry(pos + 1));
					h3 = 1 / Math.sqrt(2 * Math.PI * s * s) * Math.exp(-(tau + 0.05) * (tau + 0.05) / (2 * s * s))
							* amp.getEntry(pos + 1);

					c += 0.05 / 6 * (h1 + 4 * h2 + h3);
				}
				convolved.setEntry(i, c);
			}
		}

		return convolved;
	}

	private static double extrapolate(double deg, double[] TT, double[] TTdeg) {
		double time = 0.;

		for (int i = 0; i < TT.length; i++) {
//			System.out.println(TT[i]);
			if (i != 0)
			if (deg >= TTdeg[i] && deg <= TTdeg[i - 1] && time == 0) {
				time = (TT[i] - TT[i - 1]) / (TTdeg[i] - TTdeg[i - 1]) * (deg - TTdeg[i]) + TT[i];
			}
			else
				time = TT[i];
		}
		return time;
	}

	private static boolean isIn(double[] arr, double ed, double eps) {
		boolean res = false;

		for (double x : arr) {
			if (x - eps < ed && x + eps > ed) {
				res = true;
			}
		}
		return res;
	}

	public static class Pair implements Comparable<Pair> {
		public final int index;
		public final double value;

		public Pair(int index, double value) {
			this.index = index;
			this.value = value;
		}

		@Override
		public int compareTo(Pair other) {
			return Double.valueOf(value).compareTo(Double.valueOf(other.value));
		}
	}

}
