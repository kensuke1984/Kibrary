package io.github.kensuke1984.kibrary.inversion.addons;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.math.geometry.XYZ;
import io.github.kensuke1984.kibrary.util.Location;

public class Complementation {
	public static void main(String[] args) {
		Path answerPath  = Paths.get(args[0]); // 解のファイル(vRatio)。水平方向5°おき、鉛直方向50kmおきの解
		Path cmpfilePath = Paths.get(args[1]); // 1°おきの緯度経度半径のファイル （1、2、3列目に緯度経度半径）
		Path ppfilePath  = Paths.get(args[2]); // 水平方向5°おき、鉛直方向50kmおきのperturbation pointファイル
		
		Complementation c = new Complementation();
		Location[] locations = c.readPoint(ppfilePath);  //既知の場所(5°おき)を読み込む	順番が大事
		double [] values = c.readValue(answerPath);  //既知の値を読み込む 順番が大事 場所と値の順番は同じね
		Map<Location, Double> valueOfLocation = IntStream.range(0, locations.length)
				                                         .mapToObj(i -> i)
				                                         .collect(Collectors.toMap(i -> locations[i], i -> values[i]));
//		Location target = c.toPoint(cmpfilePath);  // 内挿したい点
		Location[] targetList = c.readPoint(cmpfilePath);  //内挿したい点の配列
//		Location[] nears = c.getNearest4(locations, target); //内挿したい点からの近接４点 locationsの中からtargetに近い4点
//		double[] nearpointsValue = c.getValue(nears, valueOfLocation);
//		double[] nearpointsValue = Arrays.stream(nears).mapToDouble(loc -> valueOfLocation.get(loc)).toArray(); // これは順番が同じというところからさがす
//		double valueForTarget = c.complement(nears, nearpointsValue, target);
//		double[] interpo = new double[targetList.length];
		double tmp = 0;
//		System.out.println(targetList.length);
		for(int i=0;i<targetList.length;i++){
			Location[] near = c.getNearestHorizontal4(locations, targetList[i]);
//			double[] nearpointValue = Arrays.stream(near).mapToDouble(loc -> valueOfLocation.get(loc)).toArray();
			double[] nearpointsValue = new double[near.length];
			  for(int n=0;n<near.length;n++){
			      double nearpointValue = valueOfLocation.get(near[n]);
			      nearpointsValue[n] = nearpointValue;
			  }
			tmp = c.complement(near, nearpointsValue, targetList[i]);
//			interpo[i] = tmp;
			System.out.println(tmp);
			 }
//		System.out.println(interpo);
//		System.out.println(valueForTarget);
	}
	
//	private double[] getValue(Location[] nears, Map<Location,Double> valueOfLocation){
//		for(int i=0;i<nears.length;i++){
//			double tmp = valueOfLocation.get(nears[i]);
//			nearValue[i] = tmp;
//			System.out.println(tmp);
//		}
//		return nearValue;
//	}
	
	/**
	 * ここでコンプリメントする
	 * @param nearPoints
	 * @param nearpointsValue
	 * @param target
	 * @return
	 */
	public double complement(Location[] nearPoints, double[] nearpointsValue, Location target){
		double wsum = 0;
		double wzsum = 0;
		  for(int i=0;i<nearPoints.length;i++){	
			  if (nearPoints[i].equals(target))
				  return nearpointsValue[i];
			  double weight = Complementation.weight(nearPoints[i], target);
			  wsum += weight;
			  double wz = weight * nearpointsValue[i];
			  wzsum += wz;
		  }
		return wzsum/wsum;
//		return value;
	}
	
	public double complementEnhanceHorizontal(Location[] nearPoints, double[] nearpointsValue, Location target){
		double wsum = 0;
		double wzsum = 0;
		  for(int i=0;i<nearPoints.length;i++){	
		  double weight = Complementation.weightHorizontalEnhanced(nearPoints[i], target);
		  wsum += weight;
		  double wz = weight * nearpointsValue[i];
		  wzsum += wz;
		  }
		return wzsum/wsum;
//		return value;
	}
	
	// TODO
	public double trilinearInterpolation(double[][] xyz, double[] cellValues, Location target) {
		double v = 0;
		double xd = (target.getLatitude() - xyz[0][0]) / (xyz[0][1] - xyz[0][0]);
		double yd = (target.getLongitude() - xyz[1][0]) / (xyz[1][1] - xyz[1][0]);
		double zd = (target.getR() - xyz[2][0]) / (xyz[2][1] - xyz[2][0]);
		
		double c00 = xyz[0][0] * (1 - xd) + xyz[0][1];
		double c01 = xyz[2][0] * (1 - xd) + xyz[0][1];
		double c10 = xyz[0][0] * (1 - xd) + xyz[0][1];
		double c11 = xyz[0][0] * (1 - xd) + xyz[0][1];
		
		return v;
	}
	
	/**
	 * 重み関数を計算
	 * @param kichi
	 * @param michi
	 * @return
	 */
	private static double weight(Location kichi, Location michi){
//		double kLat = kichi.getLatitude();
//		double kLon = kichi.getLongitude();
//		double mLat = michi.getLatitude();
//		double mLon = michi.getLongitude();
		double distance = michi.getDistance(kichi);
		return 1/distance;
//		double weight = 1/distance;
//		return weight;
	}
	
	private static double weightHorizontalEnhanced(Location loc1, Location loc2) {
		double arcDistance = loc1.getEpicentralDistance(loc2) * (loc1.getR() + loc2.getR()) / 2.;
		double dR = Math.abs(loc1.getR() - loc2.getR());
		arcDistance /= 4.;
		
		return Math.sqrt(arcDistance * arcDistance + dR * dR);
	}
	
	/**
	 * @param locations この中から
	 * @param location この点に
	 * @return 近い点を4つかえす
	 */
	public Location[] getNearest4(Location[] locations, Location location){
//		double[] distance = distance(locations, location);
//		Arrays.sort(distance); 
		Location[] sortLoc = location.getNearestLocation(locations);
		Location[] near4 = new Location[4];
		for(int i=0;i<4;i++){
			near4[i] = sortLoc[i];
		}
	    return near4;
	}
	
	public Location[] getNearest4(Location[] locations, Location location, double maxSearchRange){
//		double[] distance = distance(locations, location);
//		Arrays.sort(distance); 
		Location[] sortLoc = location.getNearestLocation(locations, maxSearchRange);
		Location[] near4 = new Location[4];
		for(int i=0;i<4;i++){
			near4[i] = sortLoc[i];
		}
	    return near4;
	}
	
	public Location[] getNearest(Location[] locations, Location location){
		double minDistance = Double.MAX_VALUE;
		Location[] nearest = new Location[1];
		for (Location loc : locations) {
			double distance = loc.getDistance(location);
			if (distance < minDistance) {
				minDistance = distance;
				nearest[0] = loc;
			}
		}
		return nearest;
	}
	
	public Location[] get8CellNodes(Location[] locations, Location location, double dR, double dL) {
		Location[] nodes = new Location[8];
		Location[] sortLoc = location.getNearestLocation(locations);
		Location nearest = sortLoc[0];
		double lat = nearest.getLatitude();
		double lon = nearest.getLongitude();
		double r = nearest.getR();
		
//		RealVector diffVector = nearest.toXYZ().toRealVector().subtract(location.toXYZ().toRealVector());
//		double x = diffVector.getEntry(0);
//		double y = diffVector.getEntry(1);
//		double z = diffVector.getEntry(2);
		double x = nearest.getLatitude() - location.getLatitude();
		double y = nearest.getLongitude() - location.getLongitude();
		double z = nearest.getR() - location.getR();
//		System.out.println("DEBUG0: "  + x + " " + y + " " + z);
		double dLon = 0;
		double dLat = 0;
		double dRad = 0;
		dLat = x == 0 ? 0. : (x > 0 ? dL : -dL);
		dLon = y == 0 ? 0. : (y > 0 ? dL : -dL);
		dRad = z == 0 ? 0. : (z > 0 ? -dR : dR);
		
		
		nodes[0] = nearest;
		nodes[1] = new Location(lat + dLat, lon, r);
		nodes[2] = new Location(lat, lon + dLon, r);
		nodes[3] = new Location(lat + dLat, lon + dLon, r);
		nodes[4] = new Location(lat, lon, r - dRad);
		nodes[5] = new Location(lat + dLat, lon, r - dRad);
		nodes[6] = new Location(lat, lon + dLon, r - dRad);
		nodes[7] = new Location(lat + dLat, lon + dLon, r - dRad);
		
		Set<Location> fixedNodes = new HashSet<>();
		fixedNodes.add(nodes[0]);
		for (int i = 1; i < 8; i++) {
			Location[] nearests = nodes[i].getNearestLocation(locations);
			if (nearests.length > 0)
				fixedNodes.add(nodes[i].getNearestLocation(locations)[0]);
		}
		
		return fixedNodes.toArray(new Location[0]);
	}
	
	public double[][] getLocationsForTrilinear(Location[] locations, Location location, double dR, double dL) {
		double[][] nodes = new double[3][];
		for (int i = 0; i < 3; i++)
			nodes[i] = new double[2];
		Location[] sortLoc = location.getNearestLocation(locations);
		Location nearest = sortLoc[0];
		double lat = nearest.getLatitude();
		double lon = nearest.getLongitude();
		double r = nearest.getR();
		
		RealVector diffVector = nearest.toXYZ().toRealVector().subtract(location.toXYZ().toRealVector());
		double x = diffVector.getEntry(0);
		double y = diffVector.getEntry(1);
		double z = diffVector.getEntry(2);
		double dLon = 0;
		double dLat = 0;
		double dRad = 0;
		dLon = x >= 0 ? dL : -dL;
		dLat = y >= 0 ? dL : -dL;
		dRad = z >= 0 ? -dR : dR;
		
		nodes[0][0] = nearest.getLatitude();
		nodes[0][1] = nodes[0][0] + dLat;
		nodes[1][0] = nearest.getLongitude();
		nodes[1][1] = nodes[1][0] + dLon;
		nodes[2][0] = nearest.getR();
		nodes[2][1] = nodes[2][0] + dRad;
		
		return nodes;
	}
	
	/**
	 * @param locations
	 * @param location
	 * @return nearest location with the same radius as this
	 */
	public Location[] getNearestHorizontal4(Location[] locations, Location location){
		Location[] sortLoc = location.getNearestHorizontalLocation(locations);
		Location[] near4 = new Location[4];
		for(int i = 0; i < 4;i++)
			near4[i] = sortLoc[i];
	    return near4;
	}
	
	/**
	 * locationとlocationsの距離を配列にして返す
	 * @param locations
	 * @param location
	 * @return
	 */
	private static double[] distance(Location[] locations, Location location){
		 return IntStream.range(0, locations.length)
				 .mapToDouble(i -> locations[i].getDistance(location))
				 .toArray();
	}
	
	/**
	 * ファイルパスに対して１行づつ読み込み、Locationを返す
	 * @param cmp
	 * @return Location
	 */
	private Location toPoint(Path cmp) {
		String path = cmp.toString();
		String line = Complementation.fileRead(path);
		String[] parts = line.split("\\s+");
		return new Location (Double.parseDouble(parts[0]), 
				Double.parseDouble(parts[1]), Double.parseDouble(parts[2]) );
	 }
	
	private static Location toLocation(String line) {
		String[] parts = line.split("\\s+");
		return new Location(Double.parseDouble(parts[0]),
		Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
	}
	
	private Location[] readPoint(Path ppPath){
		try {
			return Files.readAllLines(ppPath).stream()
					.map(Complementation::toLocation)
					.toArray(n -> new Location[n]);
		}catch (Exception e) {
			throw new RuntimeException("complement file has problems");
		}
	}
	
/**	
	private Location[] newPoint(Path cmpPath) {
		try {
			return Files.readAllLines(cmpPath).stream()
					.map(Complementation::toLocation)
					.toArray(m -> new Location[m]);
		}catch (Exception e) {
			throw  new RuntimeException("perturbation point file has problems");
		}
     }
*/
	
	/**
	 * 既知の(5°おきの)δVを返す
	 * @param valuePath
	 * @return
	 */
	private double[] readValue(Path valuePath){
		try {
			return Files.readAllLines(valuePath).stream()
					.mapToDouble(Double::parseDouble).toArray();
		}catch (Exception e) {
			throw new RuntimeException("answer file has problems");
		}
	}
	
	/**
	 * ファイルパス(string)に対して1行づつ返す
	 * @param filePath
	 * @return line
	 */
	public static String fileRead(String filePath) {
	    FileReader fr = null;
	    BufferedReader br = null;
	    String line;
	    line = null;
	    String raw;
	    raw = null;
	    try {
	        fr = new FileReader(filePath);
	        br = new BufferedReader(fr);
	        while ((line = br.readLine()) != null) {
//	            System.out.println(line);
	        	raw = line;
	        	return line;
	        }
	    } catch (FileNotFoundException e) {
	        e.printStackTrace();
	    } catch (IOException e) {
	        e.printStackTrace();
	    } finally {
	        try {
	            br.close();
	            fr.close();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
           } 
	    return raw;
	}
	
}
