package io.github.kensuke1984.kibrary.selection;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.math3.linear.ArrayRealVector;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.WaveformDataWriter;

public class SelectSpcAmp {

	public static void main(String[] args) {
		Path spcAmpIDPath = Paths.get(args[0]);
		Path spcAmpPath = Paths.get(args[1]);
		double varmax = Double.parseDouble(args[2]);
		System.out.println("Var max = " + varmax);
		
		Path outIDPath = Paths.get("spcAmpID" + ".dat");
		Path outPath = Paths.get("spcAmp" + ".dat");
		try {
			List<BasicID> ids = Arrays.stream(BasicIDFile.read(spcAmpIDPath, spcAmpPath)).collect(Collectors.toList());
			Dvector dVector = new Dvector(ids.toArray(new BasicID[ids.size()]));
			Set<GlobalCMTID> events = ids.stream().map(id -> id.getGlobalCMTID()).collect(Collectors.toSet());
			Set<Station> stations = ids.stream().map(id -> id.getStation()).collect(Collectors.toSet());
			double[][] periodRanges = new double[][] {{8, 200}};
			Phase[] phases = new Phase[] {Phase.ScS};
			WaveformDataWriter writer = new WaveformDataWriter(outIDPath, outPath, stations, events, periodRanges, phases);
			
			BasicID[] obsIDs = dVector.getObsIDs();
			BasicID[] synIDs = dVector.getSynIDs();
			
			for (int i = 0; i < dVector.getObsIDs().length; i++) {
				double var = new ArrayRealVector(obsIDs[i].getData()).subtract(new ArrayRealVector(synIDs[i].getData())).getNorm()
						/ new ArrayRealVector(obsIDs[i].getData()).getNorm();
				if (var > varmax)
					continue;
				writer.addBasicID(obsIDs[i]);
				writer.addBasicID(synIDs[i]);
			}
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
