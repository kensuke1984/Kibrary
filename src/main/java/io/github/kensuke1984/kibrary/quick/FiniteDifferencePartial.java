package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.WeightingType;
import io.github.kensuke1984.kibrary.util.Phases;
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


public class FiniteDifferencePartial {

	public static void main(String[] args) {
		Path idPath = Paths.get(args[0]);
		Path dataPath = Paths.get(args[1]);
		double dm = 1.;
		if (args.length == 3)
			dm = Double.parseDouble(args[2]);
			
		
		try {
			Path dir0 = Paths.get("finiteDifferencePartial");
			if (!Files.exists(dir0))
				Files.createDirectory(dir0);
			
			BasicID[] ids = BasicIDFile.readBasicIDandDataFile(idPath, dataPath);
			Predicate<BasicID> chooser = id -> true;
			Dvector dVector = new Dvector(ids, chooser, WeightingType.IDENTITY);
			
			BasicID[] obsIds = dVector.getObsIDs();
			BasicID[] synIds = dVector.getSynIDs();
			
			for (int i = 0; i < obsIds.length; i++) {
				Phases phases = new Phases(obsIds[i].getPhases());
				Path dir1 = dir0.resolve(phases.toString());
				if (!Files.exists(dir1))
					Files.createDirectories(dir1);
				
				String endstring = obsIds[i].isConvolute() ? "sc" : "s";
				String outfile = dir1 + "/" + obsIds[i].getStation() + "." + obsIds[i].getGlobalCMTID() + "...par." + phases +"." + obsIds[i].getSacComponent() + endstring;
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outfile)));
				
				double[] obs = obsIds[i].getData();
				double[] syn = synIds[i].getData();
				double t0 = synIds[i].getStartTime();
				double dt = 1. / synIds[i].getSamplingHz();
				for (int k = 0; k < obs.length; k++) {
					pw.write(String.format("%.5f %.7e\n", t0+k*dt, (obs[k]-syn[k])/dm));
				}
				
				pw.close();
			}
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
