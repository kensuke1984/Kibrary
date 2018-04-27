package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import org.apache.commons.math3.exception.DimensionMismatchException;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

public class Orthogonality {
	
	public Orthogonality(Path parIDPath, Path parPath, Path parRefIDPath, Path parRefPath, Set<Station> stations, Set<GlobalCMTID> events, Path outPath) throws IOException {
		this.dotProduct = new HashMap<>();
		this.norm = new HashMap<>();
		
		PartialID[] tmpPar = PartialIDFile.readPartialIDandDataFile(parIDPath, parPath);
		List<PartialID> pars = new ArrayList<>();
		List<PartialID> parRefs = new ArrayList<>(); 
		
		AtomicInteger i = new AtomicInteger(0);
		
		try {
			Files.write(outPath, "#radius of the perturbation points in upper mantle 5921 6021 6121 6221 6321\n".getBytes(), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		Stream.of(PartialIDFile.readPartialIDandDataFile(parRefIDPath, parRefPath))
			.filter(par -> stations.stream().filter(sta -> sta.equals(par.getStation())).count() > 0 
					&& events.contains(par.getGlobalCMTID()))
			.forEach(parRef -> { 
			parRefs.add(parRef);
			});
		Stream.of(tmpPar)
			.filter(par -> stations.stream().filter(sta -> sta.equals(par.getStation())).count() > 0 
				&& events.contains(par.getGlobalCMTID()))
			.forEach(par -> pars.add(par));
		
		i.set(0);

		this.refLocs = new HashSet<>();
		parRefs.forEach(par -> refLocs.add(par.getPerturbationLocation()));

		Map<Location, Double> parNorm = new HashMap<>();
		for (PartialID tmp : pars) {
			if (parNorm.containsKey(tmp.getPerturbationLocation()))
				parNorm.put(tmp.getPerturbationLocation(), 
						parNorm.get(tmp.getPerturbationLocation())
						+ (new ArrayRealVector(tmp.getData())).dotProduct(new ArrayRealVector(tmp.getData())));
			else
				parNorm.put(tmp.getPerturbationLocation(), 
						(new ArrayRealVector(tmp.getData())).dotProduct(new ArrayRealVector(tmp.getData())));
		}
		
		i.set(0);
		refLocs.stream().forEach(refLoc -> {
			parRefs.stream().filter(par -> par.getPerturbationLocation().equals(refLoc))
				.forEach(parRef -> {
					pars.stream()
						.filter(par -> parRef.getStation().equals(par.getStation()) 
								&& parRef.getGlobalCMTID().equals(par.getGlobalCMTID())
								&& parRef.getStartTime() == par.getStartTime())
						.forEach(par -> {
						
							RealVector parData = new ArrayRealVector(par.getData());
							RealVector parRefData = new ArrayRealVector(parRef.getData());
							if (! dotProduct.containsKey(par.getPerturbationLocation())) {
								dotProduct.put(par.getPerturbationLocation()
									, new double[] {
										parData.dotProduct(parRefData), 0, 0, 0, 0
										});
								norm.put(par.getPerturbationLocation()
										, new double[] {
										parNorm.get(par.getPerturbationLocation()) * parRefData.dotProduct(parRefData), 0, 0, 0, 0
											});
							}
							else {
								double[] tmp = dotProduct.get(par.getPerturbationLocation());
								try {
									tmp[i.get()] += parData.dotProduct(parRefData);
								} catch (DimensionMismatchException e) {
									e.printStackTrace();
									System.out.println(par.getStation() + " " + parRef.getStation()
									+ " " + par.getGlobalCMTID().toString() + " " + parRef.getGlobalCMTID().toString()
									+ " " + par.getNpts() + " " + parRef.getNpts()
									+ " " + par.getPerturbationLocation().toString() + " " + parRef.getPerturbationLocation().toString());
								}
								dotProduct.replace(par.getPerturbationLocation()
										, tmp);
								
								tmp = norm.get(par.getPerturbationLocation());
								tmp[i.get()] += parNorm.get(par.getPerturbationLocation()) * parRefData.dotProduct(parRefData);
								norm.replace(par.getPerturbationLocation()
										, tmp);
							}
						});
				});
			i.incrementAndGet();
		});
		Location[] keys = new Location[dotProduct.size()];
		AtomicInteger k = new AtomicInteger(0);
		dotProduct.keySet().stream().forEach(loc -> keys[k.getAndIncrement()] = loc);
		int nLocRef = dotProduct.get(keys[0]).length;
		for (Location loc : keys) {
			String s = "";
			for (int l=0; l < nLocRef; l++) {
				s += String.valueOf(dotProduct.get(loc)[l] / Math.sqrt(norm.get(loc)[l])) + " ";
			}
			try {
			Files.write(outPath, (s + "\n").getBytes(), StandardOpenOption.APPEND);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static void main(String[] args) {
		try {
		Path parIDPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/partialID.dat");
		Path parPath =  Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/partial.dat");
		Path parRefIDPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/orthogonality/partialID.dat");
		Path parRefPath = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/orthogonality/partial.dat");
		
		Path outPath11 = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/orthogonality/cos/201101122132A_C28A.txt"); //200705251747A_G11A.txt
		Path outPath1all = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/orthogonality/cos/201101122132A.txt");
		Path outPathall1 = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/orthogonality/cos/C28.txt");
		Path outPathallall = Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/orthogonality/cos/all.txt");
		
		InversionResult ir = new InversionResult(Paths.get("/mnt/doremi/suzuki/ALASKA/DIVIDE/PREM/ALL/threeDPartial/Inv"));
		
		Set<Station> stations = new HashSet<>();
		Set<GlobalCMTID> events = new HashSet<>();
		
		stations.add(ir.stationSet().stream().filter(sta -> sta.getName().equals("C28A")).findAny().get()); //200512232147A WHY
		events.add(new GlobalCMTID("201101122132A"));
		Orthogonality ortho = new Orthogonality(parIDPath, parPath, parRefIDPath, parRefPath, stations, events, outPath11);
		
//		stations = ir.stationSet();
//		events = ir.idSet();
//		Orthogonality ortho = new Orthogonality(parIDPath, parPath, parRefIDPath, parRefPath, stations, events, outPathallall);
		
//		stations = ir.stationSet();
//		events.add(new GlobalCMTID("201101122132A"));
//		Orthogonality ortho = new Orthogonality(parIDPath, parPath, parRefIDPath, parRefPath, stations, events, outPath1all);
		
//		stations.add(ir.stationSet().stream().filter(sta -> sta.getStationName().equals("C28A")).findAny().get());
//		events = ir.idSet();
//		Orthogonality ortho = new Orthogonality(parIDPath, parPath, parRefIDPath, parRefPath, stations, events, outPathall1);
		
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Map<Location, double[]> dotProduct;
	public Map<Location, double[]> norm;
	Set<Location> refLocs;

}
