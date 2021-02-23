package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionType;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.addons.VelocityField3D_deprec;
import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;

public class taupModelMaker {

	public static void main(String[] args) throws IOException {
//		PolynomialStructure model = PolynomialStructure.PREM;
//		PolynomialStructure model = new PolynomialStructure(Paths.get("/Users/Anselme/Dropbox/Kenji/UPPER_MANTLE/1D_REFERENCE_MODEL/POLYNOMIALS/tnasna.poly"));
//		PolynomialStructure model = new PolynomialStructure(Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster34/oneDPartial_cl4s0_it1/inversion/40km/mantleCorr/ampCorr/lmi_78_vs_cl4_az0_l007_g0_semucb/poly/cl4az0_it2.poly"));
//		PolynomialStructure model = new PolynomialStructure(Paths.get("/work/anselme/POLY/sw_it1.poly"));
		PolynomialStructure model = new PolynomialStructure(Paths.get("/work/anselme/POLY/cl4_high.poly"));
//		PolynomialStructure model = PolynomialStructure.AK135;
		
//		Path root = Paths.get("/work/anselme/CA_ANEL_NEW/oneDPartial_s0/inversion/alpha_03/lmi_s0_c06_cQ10/poly");
////		PolynomialStructure model = new PolynomialStructure(Paths.get(args[0]));
////		int nR = Integer.parseInt(args[0]);
		int nR = 5000;
		
		PolynomialStructure prem = PolynomialStructure.PREM;
		
//		model = PolynomialStructure.ISO_PREM;
		
		System.out.println(prem.getRhoAt(3780));
		System.exit(0);
		
		double f = 1.03;
		
//		System.out.println(f*(3480*prem.getVshAt(6345.) - 6345*prem.getVshAt(3480.))/(-6345 + 3480) + " " + (-prem.getVshAt(6345.) + prem.getVshAt(3480.))/(3480-6345)*6371);
//		System.out.println(f*(1221.5*prem.getVshAt(3479.9) - 3479.9*prem.getVshAt(1221.5))/(-3479.9 + 1221.5) + " " + (-prem.getVshAt(3479.9) + prem.getVshAt(1221.5))/(1221.5-3479.9)*6371);
//		System.out.println(f*(0*prem.getVshAt(1221.4) - 1221.4*prem.getVshAt(0.))/(-1221.4 + 0) + " " + (-prem.getVshAt(1221.4) + prem.getVshAt(0.))/(0-1221.4)*6371);
//		
//		System.out.println(f*(3480*prem.getVphAt(6345.) - 6345*prem.getVphAt(3480.))/(-6345 + 3480) + " " + (-prem.getVphAt(6345.) + prem.getVphAt(3480.))/(3480-6345)*6371);
//		System.out.println(f*(1221.5*prem.getVphAt(3479.9) - 3479.9*prem.getVphAt(1221.5))/(-3479.9 + 1221.5) + " " + (-prem.getVphAt(3479.9) + prem.getVphAt(1221.5))/(1221.5-3479.9)*6371);
//		System.out.println(f*(0*prem.getVphAt(1221.4) - 1221.4*prem.getVphAt(0.))/(-1221.4 + 0) + " " + (-prem.getVphAt(1221.4) + prem.getVphAt(0.))/(0-1221.4)*6371);
//		
////		System.out.println((PolynomialStructure.PREM.getVphAt(3630) - PolynomialStructure.AK135.getVphAt(3630))/ PolynomialStructure.PREM.getVphAt(3630) * 100);
//		System.exit(0);
		
//		System.out.println(prem.getVshAt(6371. - 2431.0));
//		System.exit(0);
		
//		double r0 = 6371-2550.;
//		double avg = 0;
////		for (int i = 0; i < 10000; i++) {
////			double r = r0 - i / 100.;
////			if (model.getVshAt(r) >= prem.getVshAt(r)) {
////				r0 = r;
////				break;
////			}
////		}
//		r0 = 3794.6;
//		double r00 = 6371. - 2400.;
//		System.out.println(r0);
//		int nr = (int) (r00 - 3480);
//		for (int i = 0; i < nr; i++) {
//			double r = r00 - i;
//			avg += (model.getVshAt(r) - prem.getVshAt(r)) / prem.getVshAt(r);
//		}
//		avg /= nr;
////		System.out.println(avg * 100 * (r00 - 3480) / (r0 - 3480));
//		System.out.println(avg * 100);
//		System.out.println((prem.getVshAt(r0-30) * 1.4314450941487382/100 + 11.1671) + " " + (prem.getVshAt(r0-30) * 1.4314450941487382/100 + 6.9254));
//		
//		double v1 = prem.getVshAt(r0 + 30.);
//		double v2 = prem.getVshAt(r0 - 30.) * (1. + 1.4314450941487382/100.);
//		double alpha = (v2 - v1) / 60. * 6371.;
//		double v0 = prem.getVshAt(r0+30) + alpha*(r0+30.) / 6371.;
//		double v22 = 14.081209539169757 - 11.460512512359322*(r0+30.) / 6371.;
//		System.out.println(v0 + " " + alpha);
//		System.exit(0);
		
//		System.out.println((PolynomialStructure.PREM.getVshAt(6371-2400)));
//		System.exit(0);
//		System.out.println((0.1*PolynomialStructure.PREM.getVshAt(3500)*PolynomialStructure.PREM.getVshAt(3500)*PolynomialStructure.PREM.getRhoAt(3500)));
//		System.out.println((model.getVshAt(3500)));
//		double[] rs = new double[] {3530., 3630., 3730.};
//		for (double r : rs) {
//			double vp = PolynomialStructure.PREM.getVphAt(r);
//			double vs = PolynomialStructure.PREM.getVshAt(r);
//			double vb = PolynomialStructure.PREM.getVbAt(r);
//			System.out.println(r+ " "+vp/vb + " " + vs/vb + " " + vs/vp + " " + 4./3.*vs*vs/vp/vp + " " + vp + " " + vs);
//		}
//		System.exit(0);
		
//		double[] rs = new double[] {3480., 3630., 3780., 3930.};
//		for (double r : rs) {
//			double vs = PolynomialStructure.PREM.getVshAt(r);
//			System.out.println(r+ " "+vs*0.01);
//		}
//		System.exit(0);
		
//		System.out.println((PolynomialStructure.PREM.getVshAt(6371-2401)));
//		System.out.println((PolynomialStructure.PREM.getVphAt(3480)));
//		System.out.println((PolynomialStructure.PREM.getVphAt(3690) - PolynomialStructure.PREM.getVphAt(3480)) / PolynomialStructure.PREM.getVphAt(3480));
//		System.out.println((PolynomialStructure.PREM.getVphAt(3690)));
//		System.out.println((PolynomialStructure.PREM.getVbAt(3480)));
//		System.out.println((PolynomialStructure.PREM.getVshAt(3480) + PolynomialStructure.PREM.getVshAt(3580))/2.*1.005);
//		System.exit(0);
		
//		double distance = Math.toDegrees(new HorizontalPosition(60,-153).getEpicentralDistance(new HorizontalPosition(45, -115)));
////		System.out.println(distance);
//		
//		PolynomialStructure ak135 = PolynomialStructure.AK135;
//		
//		System.out.println("dvs (%) = " + VelocityField3D.dvs(5867.25, 1., ak135)*100);
//		System.out.println("dvs (%) = " + VelocityField3D.dvs(5671., 1., ak135)*100);
//		
//		double r = 5581;
//		double dmu = ak135.computeMu(r) * 0.005;
//		System.out.println(dmu);
//		double vs = ak135.getVshAt(r);
//		double vsPrime = Math.sqrt(dmu / ak135.getRhoAt(r) + vs * vs);
//		double dvs = vsPrime - vs;
//		System.out.println(dvs + " " + dvs/vs*100 + " " + (dvs+12.213901));
//		
//		System.exit(0);
		
//		System.out.println(PolynomialStructure.PREM.getVshAt(6371-2500));
////		double vs = PolynomialStructure.PREM.getVshAt(6371-2500);
//		double vp = PolynomialStructure.PREM.getVphAt(6371-2500);
//		System.out.println(vp);
//		System.exit(0);
//		
//		System.out.println(ak135.getVphAt(5150)*0.001 + 24.138794);
//		System.out.println(ak135.getVphAt(5150)*ak135.getRhoAt(5150));
//		System.out.println( (2*ak135.getVphAt(5150)*ak135.getRhoAt(5150)*0.1) );
//		System.out.println(ak135.getVphAt(5150)/(2*ak135.getVshAt(5150))*0.1);
//		System.out.println(ak135.getVphAt(5150)/ak135.getVshAt(5150));
//		
//		
//		System.exit(0);
		
//		System.out.println(ak135.getVshAt(3480) + " " + PolynomialStructure.PREM.getVshAt(3480) + " " + PolynomialStructure.TNASNA.getVshAt(3480) + " " + PolynomialStructure.TBL50.getVshAt(3480));
//		System.exit(0);
		
//		double dv_mean = (ak135.getVshAt(5711)*1.015-ak135.getVshAt(5773.5)) / 2. - (ak135.getVshAt(5711)-ak135.getVshAt(5773.5)) / 2.;
//		System.out.println(dv_mean);
//		System.out.println(ak135.getVshAt(5711)*0.015);
//		System.out.println(dv_mean/ak135.getVshAt(5711+62.5/2.));
//		System.exit(0);
		
//		double r1=6161.0;
//		double r2=5961.0+1e-5;
//		double v1 = ak135.getVshAt(r1);
//		double v2 = ak135.getVshAt(r2)*0.985;
////		double r1=5961.0-1e-5;
////		double r2=5711.0+1e-5;
////		double v1 = ak135.getVshAt(r1)*0.985;
////		double v2 = ak135.getVshAt(r2)*1.015;
////		double r1=5711-1e-5;
////		double r2=5611.0;
////		double v1 = ak135.getVshAt(r1)*1.015;
////		double v2 = ak135.getVshAt(r2);
//		double a = (v1-v2)/(r1-r2)*6371.;
//		double b = -a*r1/6371. + v1;
//		System.out.printf("%.2f %.2f %.6f %.6f",r1, r2, b, a);
//		System.exit(0);
		
//		double mu = ak135.computeMu(5800.);
//		double dmu = mu * ((1.01)*(1.01)-1);
//		System.out.println(mu + " " + dmu);
//		System.exit(0);
//		
//		for (int i = 3482; i < 3882; i+=50) {
//			double vsh2 = ak135.getVshAt(i)*ak135.getVshAt(i);
//			double vsv2 = ak135.getVsvAt(i)*ak135.getVsvAt(i);
//			double vpv2 = ak135.getVpvAt(i)*ak135.getVpvAt(i);
//			System.out.println(vsh2 + " " + vsv2 + " " + vpv2);
//		}
//		
//		System.exit(0);
		
//		Path outpath = root.resolve("PREM.vel");
//		outputSTD(PolynomialStructure.PREM, nR, outpath);
//		
//		outpath = root.resolve("IPREM.vel");
//		outputSTD(PolynomialStructure.ISO_PREM, nR, outpath);
//		
//		outpath = root.resolve("AK135.vel");
//		outputSTD(PolynomialStructure.AK135, nR, outpath);
		
//		Path outpath = root.resolve("MIASP91.vel");
//		PolynomialStructure miasp91 = new PolynomialStructure(Paths.get("/Users/Anselme/Dropbox/Kenji/JOINTMODELLING_Oba/POLYFILES/miasp91.poly"));
//		outputSTD(miasp91, nR, outpath);
		
//		Path outpath = root.resolve("TBL400_vs-Q.vel");
//		PolynomialStructure tbl = new PolynomialStructure(Paths.get("/Users/Anselme/Dropbox/Kenji/JOINTMODELLING_Oba/POLYFILES/TBL400_3800_05_08_vs-Q.poly"));
//		outputSTD(tbl, nR, outpath);
//		
//		double r = 6371.-2000.000000000001;
//		double dvs = miasp91.getVshAt(r) - tbl.getVshAt(r);
//		double dvp = miasp91.getVphAt(r) - tbl.getVphAt(r);
//		double drho = miasp91.getRhoAt(r) - tbl.getRhoAt(r);
////		System.out.println(dvp + " " + dvs + " " + drho);
		
//		outputTauP(PolynomialStructure.TNASNA, nR);
		
//		outputSTD(PolynomialStructure.MAK135, nR, Paths.get("/Users/Anselme/Dropbox/Kenji/JOINTMODELLING_Oba/VELFILES/MAK135.vel"));
		
//		outputSTD(model, nR, Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/cluster34/oneDPartial_cl4s0_it1/inversion/40km/mantleCorr/ampCorr/lmi_78_vs_cl4_az0_l007_g0_semucb/poly/cl4az0_it2.vel"));
		outputSTD(model, nR, Paths.get("/work/anselme/POLY/cl4_high.vel"));
//		outputSTD(prem, nR, Paths.get("/work/anselme/POLY/PREM.vel"));
//		outputSTD(PolynomialStructure.ISO_PREM, 1000, Paths.get("/usr/local/share/TauP-2.4.5/StdModels/PREM_1000.vel"));
//		outputSTD(model, nR, Paths.get("/work/anselme/POLY/cbvspm1pQ212_it0.vel"));
		
//		outputTauP(model, nR);
		
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
		return String.format("%.3f %.8f %.8f %.8f %.8f %.8f %.1f %.1f%n"
				,Earth.EARTH_RADIUS - r
				,model.getVphAt(r)
				,model.getVpvAt(r)
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
			int izone = model.zoneOf(r);
			if (r > 0) {
				if (izone != model.zoneOf(r - dr)) {
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
				int izone = model.zoneOf(r);
				if (r > 0) {
					if (izone != model.zoneOf(r - dr)) {
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
	
//	public static void outputSTD(PolynomialStructure model, int nR, Path outpath, double rmin, double rmax) throws IOException {
//		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
//			for (int i = 0; i <= nR; i++) {
//				double dr = rEarth.EARTH_RADIUS / nR;
//				double r = Earth.EARTH_RADIUS - i * dr;
//				double depth = Earth.EARTH_RADIUS - r;
//				int izone = model.getiZoneOf(r);
//				if (r > 0) {
//					if (izone != model.getiZoneOf(r - dr)) {
//						if (Math.abs(model.getVshAt(r) - model.getVshAt(r - dr)) > 0.05) {
//							double rZone = model.getRMinOf(izone);
//							pw.print(stdline(model, r));
//							pw.print(stdline(model, rZone));
//							pw.println("#DISCONTINUITY AT " + rZone + " km");
//							pw.print(stdline(model, rZone - .00001));
//						}
//						else
//							pw.print(stdline(model, r));
//					}
//					else 
//						pw.print(stdline(model, r));
//				}
//				else
//					pw.print(stdline(model, r));
//			}
//		}
//	}
	
	public static void outputAxiSEM(PolynomialStructure model, int nR) {
		int iDisc = 1;
		System.out.printf("ANELASTIC       T%n"
				+ "ANISOTROPIC     T%n"
				+ "UNITS        m%n"
				+ "COLUMNS       radius      rho      vpv      vsv      qka      qmu      vph      vsh      eta%n");
		for (int i = 0; i <= nR; i++) {
			double dr = Earth.EARTH_RADIUS / nR;
			double r = Earth.EARTH_RADIUS - i * dr;
			int izone = model.zoneOf(r);
			if (r > 0) {
				if (izone != model.zoneOf(r - dr)) {
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
