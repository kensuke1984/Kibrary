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
		workpath = Paths.get("/Users/Anselme/Dropbox/Kenji/anisoTimePaper/waveforms/8192NP/sac/IPREM_elastic/filtered_lowpass_4s/200809031125A");
		
		double t0_0 = 500;
		double t1_0 = 2500.;
		double samplingHz = 20.;
		double finalSamplingHz = 20.;
		int samplingRatio = (int) (samplingHz / finalSamplingHz);
		
		double tbefore = 10;
		double tafter = 20;
		String normalize;
		
		double rvel = 0;
		if (args.length == 1) {
			normalize = args[0].trim().toUpperCase();
			if (normalize.equals("P"))
				rvel = 8.;
			else if (normalize.equals("S"))
				rvel = 15.;
		}
		else if (args.length == 2)
			rvel = Double.parseDouble(args[1]);
		
		normalize = "P";
		rvel = 4;
		
		System.out.println("Normalizing on " + normalize + " phase");
		System.out.println("Using rvel = " + rvel);
		
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
				if (timetool.getNumArrivals() == 0)
					System.err.println("No arrivals for phases P,p,Pdiff,S,s,Sdiff for " + sacname);
				else {
					double parrival = timetool.getArrival(0).getTime();
					double t0 = parrival - t0_0;
					double t1 = parrival + t1_0;
					
					if (t0 < 0)
						t0 = 0;
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
						double tstart = tarrival - tbefore >= 0 ? tarrival - tbefore : 0.;
						try {
							max = trace.cutWindow(tstart, tarrival + tafter).getYVector().getLInfNorm();
						} catch (RuntimeException e) {
							System.err.println(sacname);
							System.err.println(timetool.getArrival(0).getName());
							System.err.println(e.getMessage());
						}
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
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (TauModelException e) {
			e.printStackTrace();
		}
	}

}
