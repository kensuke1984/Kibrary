package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

public class ComputeVelocityForIteration {

	public static void main(String[] args) throws IOException {
		Path workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/oneDPartialPREM_Q165/inversion/40km/20s/mantleCorr/noAmpCorr/iteration");
		workdir = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster34/oneDPartial_cl4s0/inversion/mantleCorr/noAmpCorr/iteration");
		Path perturbationPath = workdir.resolve("cl4az0_it1.txt");
		
		
		PolynomialStructure structure = PolynomialStructure.PREM;
		structure = new PolynomialStructure(Paths.get("/work/anselme/POLY/cl4az0.poly"));
		
		List<double[]> perturbations = Files.readAllLines(perturbationPath).stream().map(line -> {
			String[] ss = line.trim().split("\\s+");
			double depth = Double.parseDouble(ss[0]);
			double dvs = Double.parseDouble(ss[1]);
			return new double[] {depth, dvs};
		}).collect(Collectors.toList());
		
		Path outpath = workdir.resolve("vel_" + perturbationPath.getFileName());
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (double[] p : perturbations) {
			double vs = structure.getVshAt(6371. - p[0]) * (1+p[1]);
			pw.println(p[0] + " " + vs);
		}
		pw.close();
	}

}
