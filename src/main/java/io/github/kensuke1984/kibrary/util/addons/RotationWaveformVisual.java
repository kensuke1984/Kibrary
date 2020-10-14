package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

public class RotationWaveformVisual {
	
	final double eps = 0.1;
	
	public static void main(String[] args) throws IOException, TauModelException {
		Path waveformPath = Paths.get(args[0]);
		Path waveformIDPath = Paths.get(args[1]);
		
		BasicID[] ids = BasicIDFile.read(waveformIDPath, waveformPath);
		
		Set<GlobalCMTID> events = Stream.of(ids).map(id -> id.getGlobalCMTID()).collect(Collectors.toSet());
		
		Path stackDir = Paths.get("stack" + Utilities.getTemporaryString());
		Files.createDirectory(stackDir);
		
		double dt = 1./ ids[0].getSamplingHz();
		
		List<BasicID> ids_Z = Stream.of(ids).filter(id -> id.getSacComponent().equals(SACComponent.Z)).collect(Collectors.toList());
		List<BasicID> tmp_R = Stream.of(ids).filter(id -> id.getSacComponent().equals(SACComponent.R)).collect(Collectors.toList());
		List<BasicID> ids_R = new ArrayList<>();
		for (BasicID idZ : ids_Z) {
			BasicID idR = tmp_R.stream().filter(id -> id.getGlobalCMTID().equals(idZ.getGlobalCMTID()) && id.getStation().equals(idZ.getStation())
					&& id.getWaveformType().equals(idZ.getWaveformType()) && Utilities.equalWithinEpsilon(id.getStartTime(), idZ.getStartTime(), 0.1))
					.findFirst().get();
			ids_R.add(idR);
		}
		
		
		TauP_Time timeTool = new TauP_Time("prem");
		timeTool.parsePhaseList("P");
		
		for (GlobalCMTID event : events) {
			Path eventDir = stackDir.resolve(event.toString());
			Files.createDirectory(eventDir);
			
			List<BasicID> ids_Z_event = ids_Z.stream().filter(id -> id.getGlobalCMTID().equals(event)).collect(Collectors.toList());
			List<BasicID> ids_R_event = ids_R.stream().filter(id -> id.getGlobalCMTID().equals(event)).collect(Collectors.toList());
			
			double[][] obsStack = new double[240][0];
			double[][] synStack = new double[240][0];
			
			timeTool.setSourceDepth(6371. - event.getEvent().getCmtLocation().getR());
			
			for (int i = 0; i < ids_Z_event.size(); i++) {
				BasicID idZ = ids_Z_event.get(i);
				BasicID idR = ids_R_event.get(i);
				
				double distance = Math.toDegrees(idZ.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(idZ.getStation().getPosition()));
				int k = (int) (distance * 2);
				
				timeTool.calcTime(distance);
				double p = timeTool.getArrival(0).getRayParamDeg();
				double theta = p * 6371. * PolynomialStructure.PREM.getVphAt(6371.);
				System.out.println(theta);
				theta = Math.asin(Math.toRadians(theta));
				
				Rotator rotator = new Rotator(idR.getTrace(), idZ.getTrace());
				Trace trace = rotator.rotate(theta);
				
				if (idZ.getWaveformType().equals(WaveformType.OBS))
					obsStack[k] = add(obsStack[k], trace.getY());
				if (idZ.getWaveformType().equals(WaveformType.SYN))
					synStack[k] = add(synStack[k], trace.getX());
			}
			
			
			Path plotPath = eventDir.resolve(Paths.get("plot_" + "L" + ".plt"));
			PrintWriter pwPlot = new PrintWriter(plotPath.toFile());
			pwPlot.println("set terminal postscript enhanced color font \"Helvetica,12\"");
			pwPlot.println("set output \"" + event + "." + "L" + ".ps\"");
			pwPlot.println("unset key");
			pwPlot.println("set yrange [68:102]"); 
			pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
			pwPlot.println("set ylabel 'Distance (deg)'");
			pwPlot.println("set size .5,1");
			pwPlot.print("p ");
			
			for (int i = 0; i < obsStack.length; i++) {
				if (obsStack[i].length == 0)
					continue;
				
				double max = new ArrayRealVector(obsStack[i]).getLInfNorm();
				
				String filename = i + "." + event.toString() + "." + "L" + ".txt";
				Path outpath = eventDir.resolve(filename);
				PrintWriter pw = new PrintWriter(outpath.toFile());
				for (int j = 0; j < obsStack[i].length; j++)
					pw.println((j * dt) + " " + (synStack[i][j] / max) + " " + (obsStack[i][j] / max));
				pw.close();
				
				pwPlot.println("'" + filename + "' u 1:($2+" + (i / 2.) + ") w l lc rgb 'red',\\");
				pwPlot.println("'" + filename + "' u 1:($3+" + (i / 2.) + ") w l lc rgb 'black',\\");
			}
			
			pwPlot.close();
		}
	}
	
	
	private static double[] add(double[] y1, double[] y2) {
		if (y1.length == 0)
			return y2;
		else if (y2.length == 0)
			return y1;
		else {
			int n = Math.min(y1.length, y2.length);
			double[] tmp = new double[n];
			for (int i = 0; i < n; i++)
				tmp[i] = y1[i] + y2[i];
			return tmp;
		}
	}
}
