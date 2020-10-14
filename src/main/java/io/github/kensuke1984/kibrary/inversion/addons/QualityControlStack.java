package io.github.kensuke1984.kibrary.inversion.addons;


import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import org.apache.commons.math3.linear.RealVector;


public class QualityControlStack {

	public static void main(String[] args) {
		Path root = Paths.get(".");
		GlobalCMTID idref = new GlobalCMTID(args[0]);
		
		RealVector[] obsStacks = new RealVector[40];
		RealVector[] synStacks = new RealVector[40];
		RealVector[] bornStacks = new RealVector[40];
		
		Path outputFolder = root.resolve("qcStack");
		
		try {
			if (!Files.exists(outputFolder))
				Files.createDirectories(outputFolder);
			
			InversionResult ir = new InversionResult(root);
			
			ir.getBasicIDList().stream()
				.filter(bid -> bid.getGlobalCMTID().equals(idref))
				.forEach(bid -> {
					double distance = bid.getGlobalCMTID().getEvent().getCmtLocation()
							.getEpicentralDistance(bid.getStation().getPosition()) * 180 / Math.PI;
					
					if (distance >= 20 && distance < 40) {
						try {
							RealVector obs = ir.observedOf(bid).getYVector();
							RealVector syn = ir.syntheticOf(bid).getYVector();
							RealVector born = ir.bornOf(bid, InverseMethodEnum.CONJUGATE_GRADIENT, 9).getYVector();
							
							int i = (int) ((distance - 20) * 2);
							
							if (obsStacks[i] == null) {
								obsStacks[i] = obs;
								synStacks[i] = syn;
								bornStacks[i] = born;
							}
							else {
								obsStacks[i] = add(obsStacks[i], obs);
								synStacks[i] = add(synStacks[i], syn);
								bornStacks[i] = add(bornStacks[i], born);
							}
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				});
			
			Path gnuplot = outputFolder.resolve("stack_" + idref.toString() + ".plt");
			Files.deleteIfExists(gnuplot);
			BufferedWriter writerPlot = Files.newBufferedWriter(gnuplot);
			writerPlot.write("set terminal postscript enhanced color\nset output 'stack_" + idref.toString() + ".ps'\nunset key\nset size .5,1\np ");
			
			for (int i=0; i < 40; i++) {
				Path outObs = outputFolder.resolve(String.format("obsStack_%s_%.1f-%.1f.txt", idref.toString(), 20 + .5*i, 20.5 + .5*i));
				Path outSyn = outputFolder.resolve(String.format("synStack_%s_%.1f-%.1f.txt", idref.toString(), 20 + .5*i, 20.5 + .5*i));
				Path outBorn = outputFolder.resolve(String.format("bornStack_%s_%.1f-%.1f.txt", idref.toString(), 20 + .5*i, 20.5 + .5*i));
				
				if (obsStacks[i] != null) {
					write(obsStacks[i], outObs);
					write(synStacks[i], outSyn);
					write(bornStacks[i], outBorn);
					
					writerPlot.write("'" + outObs.getFileName() + "' u 1:($2 + " + String.valueOf(20.5 + .5*i) + ") w lines lc rgb 'black',\\\n"
							+ "'" + outSyn.getFileName() + "' u 1:($2 + " + String.valueOf(20.5 + .5*i) + ") w lines lc rgb 'green',\\\n"
							+ "'" + outBorn.getFileName() + "' u 1:($2 + " + String.valueOf(20.5 + .5*i) + ") w lines lc rgb 'purple',\\\n");
				}
			}
			writerPlot.newLine();
			writerPlot.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static RealVector add(RealVector v1, RealVector v2) {
		RealVector res = null;
		
		if (v1.getDimension() == 0)
			res = v2;
		else if (v2.getDimension() == 0)
			res = v1;
		else
			res = v1.getDimension() > v2.getDimension() ? v2.add(v1.getSubVector(0, v2.getDimension())) 
					: v1.add(v2.getSubVector(0, v1.getDimension()));
		
		return res;
	}
	
	private static void write(RealVector v, Path outPath) throws IOException {
		Files.deleteIfExists(outPath);
		Files.createFile(outPath);
		
		double dt = 1;
		
		double max = v.getMaxValue() > -v.getMinValue() ? v.getMaxValue() : -v.getMinValue();
		
		for (int i=0; i < v.getDimension(); i++)
			Files.write(outPath, String.format("%.2f %.5f\n", i * dt, v.getEntry(i) / max / 2.).getBytes(), StandardOpenOption.APPEND);
	}
}
