package io.github.kensuke1984.kibrary.selection.addons;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.xml.transform.stream.StreamSource;

import org.apache.commons.math3.linear.ArrayRealVector;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;


public class NoiseInformation {

	public static void main(String[] args) throws IOException {
		Path spcAmpIDPath = Paths.get(args[0]);
		Path spcAmpPath = Paths.get(args[1]);
		Path spcNoiseIDPath = Paths.get(args[2]);
		Path spcNoisePath = Paths.get(args[3]);
		Path outpath = Paths.get("signalNoise.dat");
		
		
		BasicID[] waveforms = BasicIDFile.read(spcAmpIDPath, spcAmpPath);
		List<BasicID> noises = Stream.of(BasicIDFile.read(spcNoiseIDPath, spcNoisePath))
				.filter(noise -> noise.getWaveformType().equals(WaveformType.OBS)).collect(Collectors.toList());
		
		Set<StaticCorrection> snratios = new HashSet<>();
		for (BasicID waveform : waveforms) {
			if (waveform.getWaveformType().equals(WaveformType.OBS)) {
				BasicID noise = noises.parallelStream().filter(n ->
					n.getGlobalCMTID().equals(waveform.getGlobalCMTID())
					&& n.getStation().equals(waveform.getStation())
					&& n.getSacComponent().equals(waveform.getSacComponent()))
				.findFirst().get();
				
				double snratio = signalNoiseRatio(noise, waveform);
				StaticCorrection ratiodata = new StaticCorrection(
						waveform.getStation(), waveform.getGlobalCMTID(),
						waveform.getSacComponent(), 0, 0, snratio, waveform.getPhases());
				snratios.add(ratiodata);
			}
		}
		
		StaticCorrectionFile.write(snratios, outpath);
				
	}
	
	
	static double signalNoiseRatioSpc(BasicID spcNoise, BasicID spc) {
		double mean_noise = new ArrayRealVector(spcNoise.getData()).map(Math::exp).getL1Norm() / spcNoise.getNpts();
		double mean_data = new ArrayRealVector(spc.getData()).map(Math::exp).getL1Norm() / spc.getNpts();
		return mean_data / mean_noise;
	}
	
	static double signalNoiseRatio(BasicID noise, BasicID data) {
		double mean_noise = new ArrayRealVector(noise.getData()).getNorm() / noise.getNpts();
		double mean_data = new ArrayRealVector(data.getData()).getNorm() / data.getNpts();
		return mean_data / mean_noise;
	}

}
