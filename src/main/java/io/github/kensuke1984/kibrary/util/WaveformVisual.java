package io.github.kensuke1984.kibrary.util;

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
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.linear.ArrayRealVector;

public class WaveformVisual {

	public static void main(String[] args) throws IOException {
		Path waveformPath = Paths.get(args[0]);
		Path waveformIDPath = Paths.get(args[1]);
		
		BasicID[] ids = BasicIDFile.readBasicIDandDataFile(waveformIDPath, waveformPath);
		
		Set<GlobalCMTID> events = Stream.of(ids).map(id -> id.getGlobalCMTID()).collect(Collectors.toSet());
		
		String tmpString = Utilities.getTemporaryString();
		
		Path stackDir = Paths.get("stack" + tmpString);
		Path profileDir = Paths.get("profile" + tmpString);
		Files.createDirectory(stackDir);
		Files.createDirectory(profileDir);
		
		SACComponent[] components = new SACComponent[] {SACComponent.T, SACComponent.R, SACComponent.Z};
		
		double dt = 1./ ids[0].getSamplingHz();
		
		for (GlobalCMTID event : events) {
			Path eventDir = stackDir.resolve(event.toString());
			Files.createDirectory(eventDir);
			
			Path profileEventDir = profileDir.resolve(event.toString());
			Files.createDirectory(profileEventDir);
		
			for (SACComponent component : components) {
				double[][] obsStack = new double[120][0];
				double[][] synStack = new double[120][0];
				
				
				Path plotProfilePath = profileEventDir.resolve(Paths.get("plot_" + component + ".plt"));
				PrintWriter pwPlot = new PrintWriter(plotProfilePath.toFile());
				pwPlot.println("set terminal postscript enhanced color font \"Helvetica,12\"");
				pwPlot.println("set output \"" + event + "." + component + ".ps\"");
				pwPlot.println("unset key");
				pwPlot.println("set yrange [63:102]"); 
				pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
				pwPlot.println("set ylabel 'Distance (deg)'");
				pwPlot.println("set size .5,1");
				pwPlot.print("p ");
				
				for (BasicID id : ids) {
					if (!id.getGlobalCMTID().equals(event) || id.getSacComponent() != component)
						continue;
					
					double distance = Math.toDegrees(id.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(id.getStation().getPosition()));
					int k = (int) distance;
					
					if (id.getWaveformType().equals(WaveformType.OBS))
						obsStack[k] = add(obsStack[k], id.getData());
					if (id.getWaveformType().equals(WaveformType.SYN))
						synStack[k] = add(synStack[k], id.getData());
					
					String filename = id.getStation() + "." + event.toString() + "." + component + "." + id.getWaveformType() + ".txt";
					Path outpath = profileEventDir.resolve(filename);
					PrintWriter pw = new PrintWriter(outpath.toFile());
					Trace trace = id.getTrace();
					for (int i = 0; i < trace.getLength(); i++)
						pw.println(trace.getXAt(i) + " " + trace.getYAt(i));
					pw.close();
					
					double max = trace.getYVector().getLInfNorm();
					if (id.getWaveformType().equals(WaveformType.SYN))
						pwPlot.println("'" + filename + "' u 0:($2/" + max + "+" + distance + ") w l lw .5 lc rgb 'red',\\");
					if (id.getWaveformType().equals(WaveformType.OBS))
						pwPlot.println("'" + filename + "' u 0:($2/" + max + "+" + distance + ") w l lw .5 lc rgb 'black',\\");
				}
				pwPlot.close();
				
				
				Path plotPath = eventDir.resolve(Paths.get("plot_" + component + ".plt"));
				pwPlot = new PrintWriter(plotPath.toFile());
				pwPlot.println("set terminal postscript enhanced color font \"Helvetica,12\"");
				pwPlot.println("set output \"" + event + "." + component + ".ps\"");
				pwPlot.println("unset key");
				pwPlot.println("set yrange [63:102]"); 
				pwPlot.println("set xlabel 'Time aligned on S-wave arrival (s)'");
				pwPlot.println("set ylabel 'Distance (deg)'");
				pwPlot.println("set size .5,1");
				pwPlot.print("p ");
				
				for (int i = 0; i < obsStack.length; i++) {
					if (obsStack[i].length == 0)
						continue;
					
					double max = new ArrayRealVector(obsStack[i]).getLInfNorm();
					
					String filename = i + "." + event.toString() + "." + component + ".txt";
					Path outpath = eventDir.resolve(filename);
					PrintWriter pw = new PrintWriter(outpath.toFile());
					for (int j = 0; j < obsStack[i].length; j++)
						pw.println((j * dt) + " " + (synStack[i][j] / max) + " " + (obsStack[i][j] / max));
					pw.close();
					
					pwPlot.println("'" + filename + "' u 1:($2+" + i + ") w l lc rgb 'red',\\");
					pwPlot.println("'" + filename + "' u 1:($3+" + i + ") w l lc rgb 'black',\\");
				}
				
				pwPlot.close();
			}
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
