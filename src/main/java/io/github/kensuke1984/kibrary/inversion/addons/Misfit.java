package io.github.kensuke1984.kibrary.inversion.addons;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

public class Misfit {

	public static void main(String[] args) throws IOException {
		Path waveformIDPath = Paths.get(args[0]);
		Path waveformPath = Paths.get(args[1]);
		BasicID[] ids = BasicIDFile.read(waveformIDPath, waveformPath);
		
		Dvector dVector = new Dvector(ids, id -> true, WeightingType.IDENTITY);
		RealVector obs = dVector.getObs();
		RealVector syn = dVector.getSyn();
		
		double l2 = l2Misfit(obs, syn);
		double cc = crossCorrelationMisfit(obs, syn);
		double chenji = chenjiMisfit(obs, syn);
		
		System.out.printf("l2 = %.4f\ncc = %.4f\nchenji = %.4f\n", l2, cc, chenji);
	}
	
	public static double l2Misfit(RealVector obs, RealVector syn) {
		return obs.subtract(syn).dotProduct(obs.subtract(syn)) / obs.dotProduct(obs);
	}
	
	public static double crossCorrelationMisfit(RealVector obs, RealVector syn) {
		return 1. - obs.dotProduct(syn) / Math.sqrt(obs.dotProduct(obs) * syn.dotProduct(syn));
	}
	
	public static double chenjiMisfit(RealVector obs, RealVector syn) {
		return 4 * obs.subtract(syn).dotProduct(obs.subtract(syn)) / (obs.add(syn).dotProduct(obs.add(syn)));
	}
}
