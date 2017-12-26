package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Earth;

public class taupModelMaker {

	public static void main(String[] args) throws IOException {
//		PolynomialStructure model = PolynomialStructure.PREM;
		PolynomialStructure model = new PolynomialStructure(
				Paths.get("/Users/Anselme/Dropbox/Kenji/FWICarib/specfem/Dpp+2per/model/model.poly"));
		Path root = Paths.get("/Users/Anselme/Dropbox/Kenji/JOINTMODELLING_Oba/VELFILES");
//		int nR = Integer.parseInt(args[0]);
		int nR = 5000;
		
//		Path outpath = root.resolve("PREM.vel");
//		outputSTD(PolynomialStructure.PREM, nR, outpath);
//		
//		outpath = root.resolve("IPREM.vel");
//		outputSTD(PolynomialStructure.ISO_PREM, nR, outpath);
//		
//		outpath = root.resolve("AK135.vel");
//		outputSTD(PolynomialStructure.AK135, nR, outpath);
		
//		Path outpath = root.resolve("MIASP91.vel");
		PolynomialStructure miasp91 = new PolynomialStructure(Paths.get("/Users/Anselme/Dropbox/Kenji/JOINTMODELLING_Oba/POLYFILES/miasp91.poly"));
//		outputSTD(miasp91, nR, outpath);
		
		Path outpath = root.resolve("TBL400_vs-Q.vel");
		PolynomialStructure tbl = new PolynomialStructure(Paths.get("/Users/Anselme/Dropbox/Kenji/JOINTMODELLING_Oba/POLYFILES/TBL400_3800_05_08_vs-Q.poly"));
		outputSTD(tbl, nR, outpath);
		
		double r = 6371.-2000.000000000001;
		double dvs = miasp91.getVshAt(r) - tbl.getVshAt(r);
		double dvp = miasp91.getVphAt(r) - tbl.getVphAt(r);
		double drho = miasp91.getRhoAt(r) - tbl.getRhoAt(r);
		System.out.println(dvp + " " + dvs + " " + drho);
		
		
//		System.out.println(miasp91.getRhoAt(6030.9));
//		System.out.println(miasp91.getRhoAt(5781.0));
//		System.out.println(-(6030.9*miasp91.getRhoAt(5781.0)-5781.*miasp91.getRhoAt(6030.9))/(5781-6030.9) + " " + (miasp91.getRhoAt(6030.9)-miasp91.getRhoAt(5781.0))/(6030.9-5781.0)*6371. );
		
		
		
//		for (int i = 0; i < 41; i++) {
//			double r = 6371 - 20.*i;
////			System.out.printf("            %.0f.  %.2f  %.2f  %.2f    %.1f      %.1f  %.2f  %.2f  %.5f%n"
////					,r * 1e3
////					,model.getRhoAt(r) * 1e3
////					,model.getVpvAt(r) * 1e3
////					,model.getVsvAt(r) * 1e3
////					,model.getQkappaAt(r)
////					,model.getQmuAt(r)
////					,model.getVphAt(r) * 1e3
////					,model.getVshAt(r) * 1e3
////					,model.getEtaAt(r));
//			System.out.println((6371.-r) + " " +model.computeMu(r));
//		}
//		System.exit(0);
		
//		outputTauP(model, nR);
//		outputAxiSEM(model, nR);
//		System.out.println(isDiscontinous(model, 6356.000, 6345.516) + " " + (Earth.EARTH_RADIUS -  6356) + " " + (Earth.EARTH_RADIUS - 6345) + " " + (Earth.EARTH_RADIUS - model.getRMinOf(model.getiZoneOf(6356))));
	}
	
	private static String TauPline(PolynomialStructure model, double r) {
		double depth = Earth.EARTH_RADIUS - r;
		return String.format("%.4f %.4f %.4f %.4f\n", depth, model.getVphAt(r), model.getVshAt(r), model.getRhoAt(r));
	}
	
	private static String stdline(PolynomialStructure model, double r) {
		double Qmu = model.getQmuAt(r) == Double.POSITIVE_INFINITY ? Double.POSITIVE_INFINITY : model.getQmuAt(r);
		return String.format("%.3f %.5f %.5f %.5f %.5f %.5f %.1f %.1f%n"
				,Earth.EARTH_RADIUS - r
				,model.getVphAt(r)
				,model.getVphAt(r)
				,model.getVshAt(r)
				,model.getVsvAt(r)
				,model.getRhoAt(r)
				,Qmu
				,model.getQkappaAt(r));
	}
	
	private static String AxiSEMline(PolynomialStructure model, double r) {
		double Qmu = model.getQmuAt(r) == Double.POSITIVE_INFINITY ? 0. : model.getQmuAt(r);
		return String.format("            %.0f.  %.2f  %.2f  %.2f    %.1f      %.1f  %.2f  %.2f  %.5f%n"
				,r * 1e3
				,model.getRhoAt(r) * 1e3
				,model.getVpvAt(r) * 1e3
				,model.getVsvAt(r) * 1e3
				,model.getQkappaAt(r)
				,Qmu
				,model.getVphAt(r) * 1e3
				,model.getVshAt(r) * 1e3
				,model.getEtaAt(r));
	}
	
	public static void outputTauP(PolynomialStructure model, int nR) {
		for (int i = 0; i <= nR; i++) {
			double dr = Earth.EARTH_RADIUS / nR;
			double r = Earth.EARTH_RADIUS - i * dr;
			double depth = Earth.EARTH_RADIUS - r;
			int izone = model.getiZoneOf(r);
			if (r > 0) {
				if (izone != model.getiZoneOf(r - dr)) {
					if (Math.abs(model.getVshAt(r) - model.getVshAt(r - dr)) > 0.05) {
						double rZone = model.getRMinOf(izone);
						System.out.print(TauPline(model, r));
						System.out.print(TauPline(model, rZone));
						System.out.print(TauPline(model, rZone - .00001));
					}
					else
						System.out.print(TauPline(model, r));
				}
				else 
					System.out.print(TauPline(model, r));
			}
			else
				System.out.print(TauPline(model, r));
		}
	}
	
	public static void outputSTD(PolynomialStructure model, int nR, Path outpath) throws IOException {
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			for (int i = 0; i <= nR; i++) {
				double dr = Earth.EARTH_RADIUS / nR;
				double r = Earth.EARTH_RADIUS - i * dr;
				double depth = Earth.EARTH_RADIUS - r;
				int izone = model.getiZoneOf(r);
				if (r > 0) {
					if (izone != model.getiZoneOf(r - dr)) {
						if (Math.abs(model.getVshAt(r) - model.getVshAt(r - dr)) > 0.05) {
							double rZone = model.getRMinOf(izone);
							pw.print(stdline(model, r));
							pw.print(stdline(model, rZone));
							pw.println("#DISCONTINUITY AT " + rZone + " km");
							pw.print(stdline(model, rZone - .00001));
						}
						else
							pw.print(stdline(model, r));
					}
					else 
						pw.print(stdline(model, r));
				}
				else
					pw.print(stdline(model, r));
			}
		}
	}
	
	public static void outputAxiSEM(PolynomialStructure model, int nR) {
		int iDisc = 1;
		System.out.printf("ANELASTIC       T%n"
				+ "ANISOTROPIC     T%n"
				+ "UNITS        m%n"
				+ "COLUMNS       radius      rho      vpv      vsv      qka      qmu      vph      vsh      eta%n");
		for (int i = 0; i <= nR; i++) {
			double dr = Earth.EARTH_RADIUS / nR;
			double r = Earth.EARTH_RADIUS - i * dr;
			int izone = model.getiZoneOf(r);
			if (r > 0) {
				if (izone != model.getiZoneOf(r - dr)) {
					if (isDiscontinous(model, r, r - dr)) {
						double rZone = model.getRMinOf(izone);
						System.out.print(AxiSEMline(model, r));
						System.out.print(AxiSEMline(model, rZone));
						System.out.printf("#          Discontinuity   %d, depth:      %.2f km%n", iDisc, Earth.EARTH_RADIUS-rZone);
						System.out.print(AxiSEMline(model, rZone - .00001));
						iDisc++;
					}
					else
						System.out.print(AxiSEMline(model, r));
				}
				else 
					System.out.print(AxiSEMline(model, r));
			}
			else
				System.out.print(AxiSEMline(model, r));
		}
	}
	
	private static boolean isDiscontinous(PolynomialStructure model, double rPlus, double rMinus) {
		boolean res = false;
		int n = (int) (rPlus - rMinus) * 10;
		for (int i = 0; i < n; i++) {
			double r1 = rPlus - i * (rPlus - rMinus) / n;
			double r2 = rPlus - (i+1) * (rPlus - rMinus) / n;
			res = Math.abs(model.getVshAt(r1) - model.getVshAt(r2)) > 0.05
					|| Math.abs(model.getVsvAt(r1) - model.getVsvAt(r2)) > 0.05
					|| Math.abs(model.getVphAt(r1) - model.getVphAt(r2)) > 0.05
					|| Math.abs(model.getVpvAt(r1) - model.getVpvAt(r2)) > 0.05
					|| Math.abs(model.getRhoAt(r1) - model.getRhoAt(r2)) > 0.05
					|| Math.abs(model.getQkappaAt(r1) - model.getQkappaAt(r2)) > 0.05
					|| Math.abs(model.getQmuAt(r1) - model.getQmuAt(r2)) > 0.05
					;
			if (res == true)
				break;
		}
		return res;
	}
}
