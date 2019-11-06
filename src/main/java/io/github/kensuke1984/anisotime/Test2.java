package io.github.kensuke1984.anisotime;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.file.Paths;
import java.util.Arrays;

import org.apache.commons.cli.ParseException;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

public class Test2 {
	public static void main(String[] args) throws IOException {
		compareAnalytical();
//		showCatalog();
//		showActualCatalog();
//		compareToTaup();
	}
	
	public static void compareToTaup() {
		double eventDepth = 0;
		double eventR = 6371. - eventDepth;
		
//		int mindist = 70;
//		int maxdist = 100;
		int mindist = 85;
		int maxdist = 85;
		int ndist = (maxdist - mindist) + 1;
		
		double[] dist = new double[ndist];
		double[] rayparam = new double[ndist];
		double[] time = new double[ndist];
		
		double[] dist_taupRayp = new double[ndist];
		double[] time_taupRayp = new double[ndist];
		
		double[] dist_taup = new double[ndist];
		double[] rayparam_taup = new double[ndist];
		double[] time_taup = new double[ndist];
		
		try {
			TauP_Time timetool = new TauP_Time("/home/anselme/Dropbox/Kenji/anisoTimePaper/traveltime/taup_models/PREM_10000.taup");
			timetool.parsePhaseList("S");
			timetool.setSourceDepth(0);
			for (int i = 0; i < ndist; i++) {
				double d = mindist + i;
				timetool.calculate(d);
				dist_taup[i] = timetool.getArrival(0).getDistDeg();
				rayparam_taup[i] = timetool.getArrival(0).getRayParamDeg();
				time_taup[i] = timetool.getArrival(0).getTime();
			}
		} catch (TauModelException e) {
			e.printStackTrace();
		}
		
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			PrintStream ps = new PrintStream(baos);
			PrintStream old = System.out;
			System.setOut(ps);
		for (int i = 0; i < ndist; i++) {
			double d = mindist + i;
			ANISOtimeCLI.main(new String[] {"-mod", "iprem", "-ph", "S", "-deg", String.format("%.2f", d), "-h", "0", "-dec", "9"});
		}
			System.setOut(old);
			String[] lines = baos.toString().split("\\n");
			ps.close();
			
			for (int i = 0; i < lines.length; i++) {
				String[] ss = lines[i].split("\\s+");
				dist[i] = Double.parseDouble(ss[2]);
				rayparam[i] = Double.parseDouble(ss[1]);
				time[i] = Double.parseDouble(ss[3]);
			}
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.iprem());
		for (int i = 0; i < ndist; i++) {
			double raypdegree = rayparam_taup[i];
			double rayp = Math.toDegrees(raypdegree);
			Raypath raypath = new Raypath(rayp, VelocityStructure.iprem(), mesh);
			raypath.compute();
			dist_taupRayp[i] = Math.toDegrees(raypath.computeDelta(eventR, Phase.S));
			time_taupRayp[i] = raypath.computeT(eventR, Phase.S);
		}
		
		for (int i = 0; i < ndist; i++) {
			double dt = time[i] - time_taup[i];
			double dt_taupRayp = time_taupRayp[i] - time_taup[i];
			System.out.println(dist_taup[i] + " " + dist[i] + " " + dist_taupRayp[i] + " " + dt + " " + dt_taupRayp);
		}
	}
	
	public static void showActualCatalog() throws IOException {
//		VelocityStructure structure = 
//			new NamedDiscontinuityStructure(Paths.get("/home/anselme/Dropbox/Kenji/anisoTimePaper/traveltime/taup_models/PREM_1000.nd"));
		
		double eventR = 6371. - 0.;
		RaypathCatalog catalog = RaypathCatalog.ISO_PREM;
		Arrays.stream(catalog.getRaypaths())
			.forEach(p -> System.out.println(Math.toRadians(p.getRayParameter()) + " " + Math.toDegrees(p.computeDelta(eventR, Phase.S))));
	}
	
	public static void showCatalog() throws IOException {
//		VelocityStructure structure = 
//			new NamedDiscontinuityStructure(Paths.get("/home/anselme/Dropbox/Kenji/anisoTimePaper/traveltime/taup_models/PREM_1000.nd"));
		
		double eventR = 6371. - 0.;
//		RaypathCatalog catalog = RaypathCatalog.ISO_PREM;
//		Arrays.stream(catalog.getRaypaths())
//			.forEach(p -> System.out.println(Math.toRadians(p.getRayParameter()) + " " + Math.toDegrees(p.computeDelta(eventR, Phase.S))));
		
		double[] drs = new double[] {0.01, 0.1, 0.2, 0.5, 1};
		
		drs = new double[] {1};
		
		for (int ir = 0; ir < drs.length; ir++) {
			double dr = 0.1;
			dr = 1;
			dr = drs[ir];
//			ComputationalMesh mesh = new ComputationalMesh(VelocityStructure.iprem(), 1, 1, dr, 0.9);
			ComputationalMesh mesh = ComputationalMesh.simple(VelocityStructure.iprem());
//			RealVector meshV=mesh.getMesh(Partition.MANTLE);
//			for (int i = 0; i < meshV.getDimension(); i++) {
//		System.out.println(meshV.getEntry(i));
//		}
//			for (int i = 0; i < 600; i++) {
//				double rayParameter = Math.toDegrees(15.68 - i*0.00016);
////				double rayParameter = Math.toDegrees(8.74 - i *0.0001);
//				Raypath raypath = new Raypath(rayParameter, VelocityStructure.iprem(), mesh);
//				raypath.compute();
//				System.out.println(dr + " " + (rayParameter<Math.toDegrees(15.6605)) + " " + (rayParameter<Math.toDegrees(15.6377)) + " " + Math.toRadians(rayParameter) + " " + Math.toDegrees(raypath.computeDelta(eventR, Phase.S))+ " " + (raypath.getTurningR(PhasePart.SH))
//						+ " ");
//			}
			
			PrintWriter pw = new PrintWriter(Paths.get("/home/anselme/Dropbox/Kenji/anisoTimePaper/traveltime/iprem/v0.4.4.5/catalog/XofP_S.dat").toFile());
			for (int i = 0; i < 1700*5; i++) {
				double rayParameter = Math.toDegrees(24.7 - i*0.01/5.);
//				double rayParameter = Math.toDegrees(8.74 - i *0.0001);
				Raypath raypath = new Raypath(rayParameter, VelocityStructure.iprem(), mesh);
				raypath.compute();
				pw.println(dr + " " + (rayParameter<Math.toDegrees(15.6605)) + " " + (rayParameter<Math.toDegrees(15.6377)) + " " + Math.toRadians(rayParameter) + " " + Math.toDegrees(raypath.computeDelta(eventR, Phase.S))+ " " + (raypath.getTurningR(PhasePart.SH))
						+ " ");
			}
			pw.close();
		}
		
	}
	
	public static void compareAnalytical() throws IOException {
		double rayparameter = 10.;
		
		PrintWriter pwD = new PrintWriter(Paths.get("/home/anselme/Dropbox/Kenji/anisoTimePaper/homogeneousmedia/distance.dat").toFile());
		PrintWriter pwT = new PrintWriter(Paths.get("/home/anselme/Dropbox/Kenji/anisoTimePaper/homogeneousmedia/error_analytical.dat").toFile());
		
		pwT.println("# dr (km), err_scs_sh (%), err_scs_sv (%), err_pcp (%)");
		pwD.println("# dr (km), d_scs_sh (deg), d_scs_sv (deg), d_pcp (deg)");
		
		PolynomialStructure homogen = PolynomialStructure.HOMOGEN;
		double r0 = 5000.;
		double rho0 = homogen.getRho(r0);
		double N0 = homogen.getN(1);
		double L0 = homogen.getL(1);
		double A0 = homogen.getA(1);
		double C0 = homogen.getC(1);
		double F0 = homogen.getF(1);
		System.out.println(A0 + " " + C0 + " " + F0 + " " + L0 + " " + N0 + " " + rho0);
		
		//mesh
		for (int i = 0; i < 20; i++) {
			double dr = 1000*Math.exp(-i/4.);
			System.out.println(i + " " + dr);
			ComputationalMesh mesh = new ComputationalMesh(homogen, dr, dr, dr, 0.009);
			
			Raypath raypath = new Raypath(rayparameter, homogen, mesh);
			raypath.compute();
			double eventR = 6371.;
			
			double delta_scs_sh = Math.toDegrees(raypath.computeDelta(eventR, Phase.ScS));
			double t_scs_sh = raypath.computeT(eventR, Phase.ScS);
			
			double delta_scs_sv = Math.toDegrees(raypath.computeDelta(eventR, Phase.create("ScS", true)));
			double t_scs_sv = raypath.computeT(eventR, Phase.create("ScS", true));
			
			double delta_pcp = Math.toDegrees(raypath.computeDelta(eventR, Phase.PcP));
			double t_pcp = raypath.computeT(eventR, Phase.PcP);
			
			double tAnal_scs_sh = computeTanalyticalHomogenSH(rayparameter, 3480., 6371.) * 2;
			double tAnal_scs_sv = computeTanalyticalHomogenSV(rayparameter, 3480., 6371.) * 2;
			double tAnal_pcp = computeTanalyticalHomogenP(rayparameter, 3480., 6371.) * 2;
			
			System.out.println("ScS (SH): " + delta_scs_sh + " " + t_scs_sh + " " + tAnal_scs_sh);
			System.out.println("ScS (SV): " + delta_scs_sv + " " + t_scs_sv + " " + tAnal_scs_sv);
			System.out.println("PcP: " + delta_pcp + " " + t_pcp + " " + tAnal_pcp);
			
			double e_t_scs_sh = Math.abs(t_scs_sh - tAnal_scs_sh) / tAnal_scs_sh;
			double e_t_scs_sv = Math.abs(t_scs_sv - tAnal_scs_sv) / tAnal_scs_sv;
			double e_t_pcp = Math.abs(t_pcp - tAnal_pcp) / tAnal_pcp;
			
			
			pwD.println(dr + " " + delta_scs_sh + " " + delta_scs_sv + " " + delta_pcp);
			pwT.println(dr + " " + e_t_scs_sh + " " + e_t_scs_sv + " " + e_t_pcp);
			pwD.flush();
			pwT.flush();
		}
		pwT.close();
		pwD.close();
	}
	
	public static double computeTanalyticalHomogenSH(double rayparameter, double rmin, double rmax) {
		PolynomialStructure homogen = PolynomialStructure.HOMOGEN;
		double r0 = 5000.;
		double rho0 = homogen.getRho(r0);
		double L0 = homogen.getL(1);
		double N0 = homogen.getN(1);
		double qtau0 = Math.sqrt(rho0/L0 - N0/L0 * rayparameter * rayparameter);
		double qt0 = rho0 / (qtau0 * L0);
		
		return qt0 * Math.log(rmax / rmin);
	}
	
	public static double computeTanalyticalHomogenSV(double rayparameter, double rmin, double rmax) {
		PolynomialStructure homogen = PolynomialStructure.HOMOGEN;
		double r0 = 5000.;
		
		double rho0 = homogen.getRho(r0);
		double L0 = homogen.getL(1);
		double A0 = homogen.getA(1);
		double C0 = homogen.getC(1);
		double F0 = homogen.getF(1);
		
		double s10 = rho0 / 2. * (1./L0 + 1./C0);
		double s20 = rho0 / 2. * (1./L0 - 1./C0);
		double s30 = 1./(2*L0*C0) * (A0*C0 - F0*F0 - 2*L0*F0);
		double s40 = s30 * s30 - A0/C0;
		double s50 = rho0 /(2*C0) * (1+A0/L0) - s10 * s30;
		double R0 = Math.sqrt(s40 * Math.pow(rayparameter, 4) + 2*s50*Math.pow(rayparameter, 2) + s20 * s20);
		
		double qtau0 = Math.sqrt(s10 - s30 * rayparameter * rayparameter + R0);
		double qt0 = 1./qtau0 * (s10 + (s50 * rayparameter*rayparameter + s20 *s20) / R0);
		
		return qt0 * Math.log(rmax / rmin);
	}
	
	public static double computeTanalyticalHomogenP(double rayparameter, double rmin, double rmax) {
		PolynomialStructure homogen = PolynomialStructure.HOMOGEN;
		double r0 = 5000.;
		
		double rho0 = homogen.getRho(r0);
		double L0 = homogen.getL(1);
		double A0 = homogen.getA(1);
		double C0 = homogen.getC(1);
		double F0 = homogen.getF(1);
		
		double s10 = rho0 / 2. * (1./L0 + 1./C0);
		double s20 = rho0 / 2. * (1./L0 - 1./C0);
		double s30 = 1./(2*L0*C0) * (A0*C0 - F0*F0 - 2*L0*F0);
		double s40 = s30 * s30 - A0/C0;
		double s50 = rho0 /(2*C0) * (1+A0/L0) - s10 * s30;
		double R0 = Math.sqrt(s40 * Math.pow(rayparameter, 4) + 2*s50*Math.pow(rayparameter, 2) + s20 * s20);
		
		double qtau0 = Math.sqrt(s10 - s30 * rayparameter * rayparameter - R0);
		double qt0 = 1./qtau0 * (s10 - (s50 * rayparameter*rayparameter + s20 *s20) / R0);
		
		return qt0 * Math.log(rmax / rmin);
	}
}
