package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.addons.WeightingType;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Predicate;

import org.apache.commons.math3.linear.ArrayRealVector;


public class FiniteDifferencePartial {

	public static void main(String[] args) {
		Path idPath = Paths.get(args[0]);
		Path dataPath = Paths.get(args[1]);
		double dm = 1.;
		if (args.length == 3)
			dm = Double.parseDouble(args[2]);
			
		String dirCurves = "/work/anselme/TOPO/ETH/synthetics/timecurves";
		
		try {
			Path dir0 = Paths.get("finiteDifferencePartial");
			if (!Files.exists(dir0))
				Files.createDirectory(dir0);
			
			BasicID[] ids = BasicIDFile.read(idPath, dataPath);
			Predicate<BasicID> chooser = id -> true;
			Dvector dVector = new Dvector(ids, chooser, WeightingType.IDENTITY);
			
			BasicID[] obsIds = dVector.getObsIDs();
			BasicID[] synIds = dVector.getSynIDs();
			
			int daz = 45;
			int naz = 360 / daz;
			String[][] azimuthPlotString = new String[naz][3];
			GlobalCMTID event = new GlobalCMTID("200602021248A"); 
			for (int iaz = 0; iaz < naz; iaz++)
				for (int ic = 1; ic <= 3; ic++) {
					azimuthPlotString[iaz][ic-1] = "set terminal postscript enhanced color font \"Helvetica,12\"\n"
						+ "set output \"" + event + ".az" + (int) (iaz * daz) + "." + SACComponent.getComponent(ic) + ".ps\"\n"
						+ "set xrange [0:3545]\n"
						+ "set key t rm\n"
						+ "set xlabel 'Time aligned on S-wave arrival (s)'\n"
						+ "set ylabel 'Distance (deg)'\n"
						+ "set size 1,1\n"
						+ "p ";
				}
			
			for (int i = 0; i < obsIds.length; i++) {
				Phases phases = new Phases(obsIds[i].getPhases());
				Path dir1 = dir0.resolve(phases.toString());
				if (!Files.exists(dir1))
					Files.createDirectories(dir1);
				
				String endstring = obsIds[i].isConvolute() ? "sc" : "s";
				String outname = obsIds[i].getStation() + "." + obsIds[i].getGlobalCMTID() + "...par." + phases +"." + obsIds[i].getSacComponent() + endstring;
				String outfile = dir1 + "/" + outname;
				String outfile2 = dir1 + "/" + obsIds[i].getStation() + "." + obsIds[i].getGlobalCMTID() + "." + phases +"." + obsIds[i].getSacComponent() + endstring;
				String outfile3 = dir1 + "/" + obsIds[i].getStation() + "." + obsIds[i].getGlobalCMTID() + "." + phases +"." + obsIds[i].getSacComponent();
				
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outfile)));
				PrintWriter pw2 = new PrintWriter(new BufferedWriter(new FileWriter(outfile2)));
				PrintWriter pw3 = new PrintWriter(new BufferedWriter(new FileWriter(outfile3)));
				
				double[] obs = obsIds[i].getData();
				double[] syn = synIds[i].getData();
				double t0 = synIds[i].getStartTime();
				double dt = 1. / synIds[i].getSamplingHz();
				for (int k = 0; k < obs.length; k++) {
					pw.write(String.format("%.5f %.7e\n", t0+k*dt, (obs[k]-syn[k])/dm));
					pw2.write(String.format("%.5f %.7e\n", t0+k*dt, syn[k]));
					pw3.write(String.format("%.5f %.7e\n", t0+k*dt, obs[k]));
				}
				
				pw.close();
				pw2.close();
				pw3.close();
				
				int iaz = (int) (Math.toDegrees(obsIds[i].getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(obsIds[i].getStation().getPosition()))
						/ daz);
				int icomp = obsIds[i].getSacComponent().valueOf();
				double max = new ArrayRealVector(obs).subtract(new ArrayRealVector(syn)).getLInfNorm() * 0.167;
				double distance = Math.toDegrees(obsIds[i].getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(obsIds[i].getStation().getPosition()));
				azimuthPlotString[iaz][icomp-1] += String.format("'%s' u 1:($2/%.4e+%.3f) w l lc rgb 'black' lt 1 lw .5 noti,\\\n", outname, max, distance);
			}
			
			for (int iaz = 0; iaz < naz; iaz++)
				for (int ic = 1; ic <= 3; ic++) {
					Path outpath = dir0.resolve("plot_az" + (iaz*daz) + "_" + SACComponent.getComponent(ic) + ".plt");
					azimuthPlotString[iaz][ic-1] += traveltimeCurves(SACComponent.getComponent(ic), dirCurves);
					PrintWriter pw = new PrintWriter(outpath.toFile());
					pw.println(azimuthPlotString[iaz][ic-1]);
					pw.close();
				}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	private static String traveltimeCurves(SACComponent component, String dir) {
		String[] colors = new String[] {"magenta", "brown", "red", "orange", "gold", "green", "cyan", "blue", "purple"};
		String s = "";
		if (component.equals(SACComponent.T)) {
			String[] phases = new String[] {"ScS", "ScSScS", "ScSScSScS", "sScS", "sScSScS", "sScSScSScS", "Sdiff"};
			for (int i = 0; i < phases.length; i++)
				s += String.format("'%s/%s.txt' u 2:1 w l lt 1 lw 1 lc rgb '%s' ti '%s',\\\n", dir, phases[i], colors[i%colors.length], phases[i]);
		}
		if (component.equals(SACComponent.Z)) {
			String[] phases = new String[] {"PcP", "pPcP", "PcS", "ScP", "sScP", "PKP", "PKKP", "PKKKP", "PKKKKP", "Pdiff", "PcPPcP", "PcPPcPPcP", "PKPPKP", "PKiKP", "ScSScP", "sScSScP", "SKP"};
			for (int i = 0; i < phases.length; i++)
				s += String.format("'%s/%s.txt' u 2:1 w l lt 1 lw 1 lc rgb '%s' ti '%s',\\\n", dir, phases[i], colors[i%colors.length], phases[i]);
		}
		if (component.equals(SACComponent.R)) {
			String[] phases = new String[] {"ScS", "sScS", "SKS", "SKKS", "SKKKS", "sSKS", "sSKKS", "PcS", "Sdiff"};
			for (int i = 0; i < phases.length; i++)
				s += String.format("'%s/%s.txt' u 2:1 w l lt 1 lw 1 lc rgb '%s' ti '%s',\\\n", dir, phases[i], colors[i%colors.length], phases[i]);
		}
		return s;
	}
	
}
