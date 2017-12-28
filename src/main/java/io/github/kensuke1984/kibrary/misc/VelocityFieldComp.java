package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
//import java.util.Comparator;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Set;
import java.util.stream.IntStream;
//import java.util.stream.Stream; 

//import velocity.VelocityField;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.dsminformation.TransverselyIsotropicParameter;
//import manhattan.template.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

public class VelocityFieldComp {
	public static void main(String[] args) {
		for (int a=1; a<21; a++){
		Path homedir = Paths.get(args[0]);
		Path ansHome = homedir.resolve("CG"+a);
		Path answerPath = ansHome.resolve("CG"+a+".txt"); // 4のファイル
		Path parfilePath = homedir.resolve("unknownParameterOrder.inf"); // 偏微分ポイントのファイル （2、3、4列目に緯度経度半径）
		String outfileRatio = ansHome.toString()+"/velRatio"+a+".dat";
		String outfileVel = ansHome.toString()+"/velField"+a+".dat";
		String outfileRatioAverage = ansHome.toString()+"/velRatioAverage"+a+".dat";
		
//		Path ansPath = Paths.get(args[0]);
//		Path parFilePath = Paths.get(args[1]);
		System.out.println(outfileRatio.toString());
		double[] vs = toVelocity(readAnswer(answerPath), readPartialLocation(parfilePath));
		double[] ratio = Ratio(readAnswer(answerPath), readPartialLocation(parfilePath));
		Location[] loc = readPartialLocation(parfilePath);
		ArrayList<MuLocation> ratioAverage = RatioAverage(readAnswer(answerPath), readPartialLocation(parfilePath));
		
		System.out.println((Ratio(12., new Location(0, 0, 3480.))-1)*100);
		
		PrintWriter pw = null;
		try {
			FileWriter fw = new FileWriter(outfileVel);
			BufferedWriter bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			
			for(int i=0; i<vs.length;i++) {
				if (loc[i].getLongitude()<0) {
				pw.printf("%.1f %.1f %.1f %f\n", loc[i].getR(), loc[i].getLatitude(), loc[i].getLongitude()+360, vs[i]);
				} else
				pw.printf("%.1f %.1f %.1f %f\n", loc[i].getR(), loc[i].getLatitude(), loc[i].getLongitude(), vs[i]);
				}
			pw.close();
			bw.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			FileWriter fw = new FileWriter(outfileRatio);
			BufferedWriter bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			
			double max=0, min=100;
			for(int i=0; i<vs.length;i++) {
				if(loc[i].getLongitude()<0){
				pw.printf("%.1f %.1f %.1f %f %f\n", loc[i].getR(), loc[i].getLatitude(), loc[i].getLongitude()+360, 100*(ratio[i]-1), Math.log(ratio[i])); //ln(ratio) = dln(v)
				} else
				pw.printf("%.1f %.1f %.1f %f %f\n", loc[i].getR(), loc[i].getLatitude(), loc[i].getLongitude(), 100*(ratio[i]-1), Math.log(ratio[i]));
				if(max < ratio[i])
					max = ratio[i];
				if(min > ratio[i])
					min = ratio[i];
			}
			System.out.printf("%f %f%n", 100*(min-1), 100*(max-1));
			
			pw.close();
			bw.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		try {
			FileWriter fw = new FileWriter(outfileRatioAverage);
			BufferedWriter bw = new BufferedWriter(fw);
			pw = new PrintWriter(bw);
			
			for(int i=0; i<ratioAverage.size();i++) {
				if(ratioAverage.get(i).getLocation().getLongitude()<0){
				pw.printf("%.1f %.1f %.1f %f %f\n", ratioAverage.get(i).getLocation().getR(), ratioAverage.get(i).getLocation().getLatitude(), ratioAverage.get(i).getLocation().getLongitude()+360, 100*(ratioAverage.get(i).getMu()-1), Math.log(ratioAverage.get(i).getMu())); //ln(ratio) = dln(v)
				} else 
				pw.printf("%.1f %.1f %.1f %f %f\n", ratioAverage.get(i).getLocation().getR(), ratioAverage.get(i).getLocation().getLatitude(), ratioAverage.get(i).getLocation().getLongitude(), 100*(ratioAverage.get(i).getMu()-1), Math.log(ratioAverage.get(i).getMu()));
				}
			
			pw.close();
			bw.close();
			fw.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
		}	
	}	

	private static PolynomialStructure prem = PolynomialStructure.PREM;
//	private static data.PolynomialStructure tbl100 = data.PolynomialStructure.TBL100;

	private static double[] Ratio(double[] deltaMu, Location[] locations) {
		return IntStream.range(0, deltaMu.length)
				.mapToDouble(i -> Ratio(deltaMu[i], locations[i]))
				.toArray();
	}
	
	private static double Ratio(double deltaMu, Location loc){
		double radius = loc.getR();
		double v = prem.getVshAt(radius);
//		double vtbl100 = tbl100.getVs(radius);
//		System.out.println(prem.getMu(radius) + " " + deltaMu);
//		prem.getTransverselyIsotropicValue(TransverselyIsotropicParameter.L, radius);
		double vDash = Math.sqrt((prem.computeMu(radius) + deltaMu) / prem.getRhoAt(radius));
//		return (vDash/vtbl100);
		return (vDash/v);
	}
	
	private static ArrayList<MuLocation> RatioAverage(double[] deltaMu, Location[] locations) {
		ArrayList<MuLocation> muLocations = new ArrayList<>();
		ArrayList<MuLocation> muLocSorted = new ArrayList<>();
		ArrayList<MuLocation> muLocAverage = new ArrayList<>();
		double tmpAverage = 0;
		
		IntStream.range(0, deltaMu.length)
			.mapToObj(i -> new MuLocation(deltaMu[i], locations[i]))
			.forEach(muLocation -> {
				muLocations.add(muLocation);
			});
		
		muLocations.stream().sorted().forEach(muLocation -> {
			muLocSorted.add(muLocation);
		});
		
		for (int i=0; i < muLocSorted.size(); i++) {
			if (i % 8 == 0) {
				if (i != 0){
				muLocAverage.add(new MuLocation(tmpAverage/4, muLocSorted.get(i).getLocation()));
				tmpAverage = Ratio(muLocSorted.get(i).getMu(), muLocSorted.get(i).getLocation());
				} //else if (i == 0){
//					tmpAverage = Ratio(muLocSorted.get(i).getMu(), muLocSorted.get(i).getLocation());
//				}
			}
			else
				tmpAverage += Ratio(muLocSorted.get(i).getMu(), muLocSorted.get(i).getLocation());
		}
//		System.out.println(muLocAverage.size());		
		return muLocAverage;
		
	}
		
	private static double[] toVelocity(double[] deltaMu, Location[] locations) {
		return IntStream.range(0, deltaMu.length)
				.mapToDouble(i -> toVelocity(deltaMu[i], locations[i]))
				.toArray();
	}

	private static double toVelocity(double deltaMu, Location loc) {
		double r = loc.getR();
		return Math.sqrt((prem.computeMu(r) + deltaMu) / prem.getRhoAt(r));
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
		if (Double.parseDouble(parts[2]) > 180.0){
//			System.out.println(Double.parseDouble(parts[2]));
		return new Location(Double.parseDouble(parts[1]),
				Double.parseDouble(parts[2])-360.0, Double.parseDouble(parts[3]));
		} else 
//			System.out.println(Double.parseDouble(parts[2]));
			return new Location(Double.parseDouble(parts[1]),
					Double.parseDouble(parts[2]), Double.parseDouble(parts[3]));
	}

	/**
	 * @param parPath
	 *            unknown file (MU lat lon radius volume)
	 * @return
	 */
	private static Location[] readPartialLocation(Path parPath) {
//		System.out.println(parPath);
		try {
			return Files.readAllLines(parPath).stream()
					.map(VelocityFieldComp::toLocation)
					.toArray(n -> new Location[n]);
		} catch (Exception e) {
			throw new RuntimeException("par file has problems");
		}

	}
}

class MuLocation implements Comparable<MuLocation> {
	
	public MuLocation(double Mu, Location location) {
		this.Mu = Mu;
		this.location = location;
	}
	
	public double getMu() {
		return Mu;
	}
	
	public Location getLocation() {
		return location;
	}
	
	@Override
	public int compareTo(MuLocation other) {
		if (this.location.getLongitude() < other.location.getLongitude())
			return -1;
		else if (this.location.getLongitude() == other.location.getLongitude()) {
			if(this.location.getLatitude() < other.location.getLatitude() )
				return 1;
			else if (this.location.getLatitude() == other.location.getLatitude())
				return 0;
			else
				return -1;
		}
		else
			return 1;
	}
	
	private double Mu;
	private Location location;
}
