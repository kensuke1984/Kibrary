package io.github.kensuke1984.kibrary.util.spc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import edu.sc.seis.TauP.Arrival;
import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

public class ReadLLNL3DEARTHoutput {

	public static void main(String[] args) throws IOException, TauModelException {
		Path llnl3Dearth_outputPath = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_stf_12.5-200s/llnl3d_tmp.dat.TT");
		Path outpath = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_stf_12.5-200s/mantleCorr_llnl3d_S-ScS.dat");
		
		TauP_Time timetool = new TauP_Time("prem");
		timetool.parsePhaseList("S, ScS");
		
		Map<Integer, StaticCorrection> answerMap = new HashMap<>();
		
		AtomicInteger keyMax = new AtomicInteger();
		Files.readAllLines(llnl3Dearth_outputPath).stream().forEach(line -> {
			if (line.startsWith("Earth3D"))
				return;
			String[] ss = line.trim().split("\\s+");
			int n = ss.length;
			Integer key = Integer.parseInt(ss[n - 1]);
			double evtLat = Double.parseDouble(ss[0]);
			double evtLon = Double.parseDouble(ss[1]);
			double evtR = 6371. - Double.parseDouble(ss[2]);
			double staLat = Double.parseDouble(ss[3]);
			double staLon = Double.parseDouble(ss[4]);
			GlobalCMTID id = new GlobalCMTID(ss[7].trim());
			Station station = new Station(ss[8].trim(), new HorizontalPosition(staLat, staLon), ss[9].trim());
			double traveltime = Double.parseDouble(ss[n-7]);
			Phase phase = Phase.create(ss[6].trim());
			
			StaticCorrection entry = new StaticCorrection(station, id, SACComponent.T, 0.
					, traveltime, 1., new Phase[] {phase});
			answerMap.put(key, entry);
			if (key > keyMax.intValue())
				keyMax.set(key);
		});
		
		Set<StaticCorrection> corrections = new HashSet<>();
		for (int i = 0; i < keyMax.get(); i+=2) {
			StaticCorrection corrS = answerMap.get(i);
			StaticCorrection corrScS = answerMap.get(i + 1);
			if (!corrS.getPhases()[0].equals(Phase.S)) {
				corrS = answerMap.get(i + 1);
				corrScS = answerMap.get(i);
			}
			
			Location evtLoc = corrS.getGlobalCMTID().getEvent().getCmtLocation();
			double distance = Math.toDegrees(evtLoc.getEpicentralDistance(corrS.getStation().getPosition()));
			timetool.setSourceDepth(6371. - evtLoc.getR());
			timetool.calculate(distance);
			if (timetool.getNumArrivals() != 2) {
				System.err.println("Problems computing travel time " + corrS);
				continue;
			}
			Map<String, Arrival> arrivalMap = new HashMap<>();
			timetool.getArrivals().stream().forEach(a -> arrivalMap.put(a.getPhase().getName(), a));
			
			double shift = (corrS.getTimeshift() - arrivalMap.get("S").getTime()) 
					- (corrScS.getTimeshift() - arrivalMap.get("ScS").getTime());
			StaticCorrection corr = new StaticCorrection(corrS.getStation(), corrS.getGlobalCMTID(),
					corrS.getComponent(), corrS.getSynStartTime(), shift, 1., new Phase[] {Phase.S, Phase.ScS});
			corrections.add(corr);
		}
		
		StaticCorrectionFile.write(corrections, outpath);
	}

}
