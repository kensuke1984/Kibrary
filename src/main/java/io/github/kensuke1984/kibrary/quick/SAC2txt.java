package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import edu.sc.seis.TauP.SeismicPhase;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

public class SAC2txt {

	public static void main(String[] args) {
		Path workpath = Paths.get(".");
		
		double t0 = 0.;
		double t1 = 1000.;
		double samplingHz = 20.;
		double finalSamplingHz = 10.;
		int samplingRatio = (int) (samplingHz / finalSamplingHz);
		
//		String normalize = "P"; // "S" or "P"
		String normalize = args[0].trim().toUpperCase();
		double tbefore = 10;
		double tafter = 20;
		
		double rvel = 0;
		if (normalize.equals("P"))
			rvel = 8.;
		else if (normalize.equals("S"))
			rvel = 15.;
		
		try {
			Set<SACFileName> sacnames = Files.list(workpath).filter(SACFileName::isSacFileName).map(SACFileName::new).collect(Collectors.toSet());
			
			TauP_Time timetool = new TauP_Time("prem");
			timetool.parsePhaseList("P,p,Pdiff,S,s,Sdiff");
			
			for (SACFileName sacname : sacnames) {
				SACData sacdata = sacname.read();
				
				double distance = sacdata.getValue(SACHeaderEnum.GCARC);
				double depth = sacdata.getValue(SACHeaderEnum.EVDP);
				
				timetool.setSourceDepth(depth);
				timetool.calculate(distance);
				double parrival = timetool.getArrival(0).getTime();
				t0 = parrival - 100;
				t1 = parrival + 1000;
				
				Trace trace = sacdata.createTrace().cutWindow(t0, t1);
				
				double max = 1.;
				if (normalize.equals("S")) {
					double tarrival = 0.;
					for (int i = 0; i < timetool.getNumArrivals(); i++) {
						if (timetool.getArrival(i).getName().equals("S") || timetool.getArrival(i).getName().equals("Sdiff")
								|| timetool.getArrival(i).getName().equals("s")) {
							tarrival = timetool.getArrival(i).getTime();
							break;
						}
					}
					max = trace.cutWindow(tarrival - tbefore, tarrival + tafter).getYVector().getLInfNorm();
				}
				else if (normalize.equals("P")) {
					double tarrival = 0.;
					for (int i = 0; i < timetool.getNumArrivals(); i++) {
						if (timetool.getArrival(i).getName().equals("P") || timetool.getArrival(i).getName().equals("Pdiff")
								|| timetool.getArrival(i).getName().equals("p")) {
							tarrival = timetool.getArrival(i).getTime();
							break;
						}
					}
					max = trace.cutWindow(tarrival - tbefore, tarrival + tafter).getYVector().getLInfNorm();
				}
				
				Path outpath = workpath.resolve(sacname.getName() + ".txt");
				
				PrintWriter pw = new PrintWriter(outpath.toFile());
				pw.println("# rvel = " + rvel);
				for (int i = 0; i < trace.getLength() / samplingRatio; i++) {
					double t = trace.getXAt(i * samplingRatio) - rvel * distance;
					double y = trace.getYAt(i * samplingRatio) / max + distance;
					pw.println(t + " " + y);
				}
				pw.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TauModelException e) {
			e.printStackTrace();
		}
	}

}
