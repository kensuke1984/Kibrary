package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.IntStream;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Location;

public class CheckerboardTestInputMaker {
	public static void main(String[] args) {
		String ansFile = args[0];
		String parFile = args[1];
		String outfile = args[2];
		Path answerPath = Paths.get(ansFile);
		Path parfilePath = Paths.get(parFile);
//		double[] deltaMu = RatioToDeltaMu(readAnswer(answerPath), readPartialLocation(parfilePath));
		double[] deltaMu = VdashToDeltaMu(readAnswer(answerPath), readPartialLocation(parfilePath));
		try{
			PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outfile, true)));
			for(double d:deltaMu){
				pw.println(d);
				System.out.println(d);
			} pw.close();
		} catch (IOException e) {
			e.getStackTrace();
//			System.out.println("ファイルを作成できません");
		}
	}

	private static PolynomialStructure prem = PolynomialStructure.PREM;
/**
	private static double[] toVelocity(double[] deltaMu, Location[] locations) {
		return IntStream.range(0, deltaMu.length)
				.mapToDouble(i -> toVelocity(deltaMu[i], locations[i]))
				.toArray();
	}

	private static double toVelocity(double deltaMu, Location loc) {
		double r = loc.getR();
		return Math.sqrt((prem.getMu(r) + deltaMu) / prem.getRho(r));
	}
*/	
	private static double[] RatioToDeltaMu(double[] vRatio, Location[] locations) {
		return IntStream.range(0, vRatio.length)
				.mapToDouble(i -> RatioToDeltaMu(vRatio[i], locations[i]))
				.toArray();
	}
	
	private static double[] VdashToDeltaMu(double[] deltaVs, Location[] locations) {
		return IntStream.range(0, deltaVs.length)
				.mapToDouble(i -> VdashToDeltaMu(deltaVs[i], locations[i]))
				.toArray();
	}
	
	private static double RatioToDeltaMu(double vRatio, Location loc){
		double radius = loc.getR();
		double v = prem.getVshAt(radius); 
		double rho = prem.getRhoAt(radius);
		double vDash = ((vRatio + 100) / 100 ) * v;
		double deltaMu = (rho * vDash * vDash) - prem.computeMu(radius);
		return deltaMu;
	}
	
	private static double VdashToDeltaMu(double deltaVs, Location loc){
		double radius = loc.getR();
		double v = prem.getVshAt(radius); 
		double rho = prem.getRhoAt(radius);
		double vDash = (1 + deltaVs * 0.01) * v;
		double deltaMu = (rho * vDash * vDash) - prem.computeMu(radius);
		return deltaMu;
	}

	private static double[] readAnswer(Path answerPath) {
		try {
			return Files.readAllLines(answerPath).stream()
					.mapToDouble(Double::parseDouble).toArray();
		} catch (Exception e) {
			throw new RuntimeException("answer file has problems");
		}
	}

	private static Location toLocation(String line) {
		String[] parts = line.split("\\s+");
		return new Location(Double.parseDouble(parts[1]),
				Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
	}

	/**
	 * @param parPath
	 *            unknown file (MU lat lon radius volume)
	 * @return
	 */
	private static Location[] readPartialLocation(Path parPath) {
		try {
			return Files.readAllLines(parPath).stream()
					.map(CheckerboardTestInputMaker::toLocation)
					.toArray(n -> new Location[n]);
		} catch (Exception e) {
			throw new RuntimeException("par file has problems");
		}

	}
}
