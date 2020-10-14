package io.github.kensuke1984.kibrary.waveformdata.addons;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionType;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.util.addons.FrequencyRange;
import io.github.kensuke1984.kibrary.util.addons.Phases;

public class ResidualVarianceFile {
	
	public static void write(Path outpath, double[][][][] numerator, double[][][][] denominator
			, WeightingType[] weights, FrequencyRange[] frequencyRanges, Phases[] phases, StaticCorrectionType[] correctionTypes, int npts) throws IOException {
		PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outpath.toFile())));
		pw.println(npts);
		pw.println(weights.length);
		pw.println(frequencyRanges.length);
		pw.println(phases.length);
		pw.println(correctionTypes.length);
		for (int iweight = 0; iweight < weights.length; iweight++) {
			for (int ifreq = 0; ifreq < frequencyRanges.length; ifreq++) {
				for (int iphase = 0; iphase < phases.length; iphase++) {
					for (int icorr = 0; icorr < correctionTypes.length; icorr++) {
						double residual = numerator[iweight][ifreq][iphase][icorr] / denominator[iweight][ifreq][iphase][icorr];
						pw.println(weights[iweight] + " " + frequencyRanges[ifreq] + " " + phases[iphase] + " " + correctionTypes[icorr] 
								+ " " + String.format("%.8f", residual) + " " + String.format("%.8f", denominator[iweight][ifreq][iphase][icorr]));
					}
				}
			}
		}
		pw.close();
	}
	
	public static double[][][][] readVariance(Path path) throws IOException {
		BufferedReader br = Files.newBufferedReader(path);
		br.readLine();
		int nweight = Integer.parseInt(br.readLine());
		int nfreq = Integer.parseInt(br.readLine());
		int nphase = Integer.parseInt(br.readLine());
		int ncorr = Integer.parseInt(br.readLine());
		
		double[][][][] residuals = new double[nweight][][][];
		for (int iweight = 0; iweight < nweight; iweight++) {
			residuals[iweight] = new double[nfreq][][];
			for (int ifreq = 0; ifreq < nfreq; ifreq++) {
				residuals[iweight][ifreq] = new double[nphase][];
				for (int iphase = 0; iphase < nphase; iphase++) {
					residuals[iweight][ifreq][iphase] = new double[ncorr];
					for (int icorr = 0; icorr < ncorr; icorr++) {
						double residual = Double.parseDouble(br.readLine().split(" ")[4]);
						residuals[iweight][ifreq][iphase][icorr] = residual;
					}
				}
			}
		}
		br.close();
		
		return residuals;
	}
	
	public static double[][][][] readObsNorm(Path path) throws IOException {
		BufferedReader br = Files.newBufferedReader(path);
		br.readLine();
		int nweight = Integer.parseInt(br.readLine());
		int nfreq = Integer.parseInt(br.readLine());
		int nphase = Integer.parseInt(br.readLine());
		int ncorr = Integer.parseInt(br.readLine());
		
		double[][][][] residuals = new double[nweight][][][];
		for (int iweight = 0; iweight < nweight; iweight++) {
			residuals[iweight] = new double[nfreq][][];
			for (int ifreq = 0; ifreq < nfreq; ifreq++) {
				residuals[iweight][ifreq] = new double[nphase][];
				for (int iphase = 0; iphase < nphase; iphase++) {
					residuals[iweight][ifreq][iphase] = new double[ncorr];
					for (int icorr = 0; icorr < ncorr; icorr++) {
						double residual = Double.parseDouble(br.readLine().split(" ")[5]);
						residuals[iweight][ifreq][iphase][icorr] = residual;
					}
				}
			}
		}
		br.close();
		
		return residuals;
	}
	
	public static int readNPTS(Path path) throws IOException {
		BufferedReader br = Files.newBufferedReader(path);
		int npts = Integer.parseInt(br.readLine());
		br.close();
		
		return npts;
	}
	
}
