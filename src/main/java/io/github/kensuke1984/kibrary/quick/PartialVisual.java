package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Phases;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class PartialVisual {

	public static void main(String[] args) throws IOException {
		Path partialIDPath = Paths.get("partialID.dat");
		Path dataPath = Paths.get("partial.dat");
		PartialID[] partials = PartialIDFile.readPartialIDandDataFile(partialIDPath, dataPath);
		
		Set<double[]> periodRanges = Stream.of(partials).map(id -> new double[] {id.getMinPeriod(), id.getMaxPeriod()})
			.collect(Collectors.toSet());
		
		for (double[] range : periodRanges) {
			Path dir = Paths.get(String.format("%.1f-%.1f", range[0], range[1]));
			if (!Files.exists(dir))
				Files.createDirectory(dir);
		}
		
		for (PartialID partial : partials) {
			Location loc = partial.getPerturbationLocation();
//			Path outpath = Paths.get(String.format("%.1f-%.1f/%s.%s.%s.%.0f.%d.txt", partial.getMinPeriod(), partial.getMaxPeriod()
//					, partial.getStation().toString(), partial.getGlobalCMTID().toString(), partial.getSacComponent().toString(), loc.getR(), (int) partial.getStartTime()));
			
			Phases phases = new Phases(partial.getPhases());
			
			Path dir = Paths.get(String.format("%.1f-%.1f", partial.getMinPeriod(), partial.getMaxPeriod()));
			Path outpath =  dir.resolve(partial.getStation().getStationName() + "." 
					+ partial.getGlobalCMTID() + "." + partial.getSacComponent() + "."
					+ (int) loc.getLatitude() + "."
					+ (int) loc.getLongitude() + "." + (int) loc.getR() + "." + partial.getPartialType() + "."
					+ phases + ".txt");
			
			double t0 = partial.getStartTime();
			double dt = 1. / partial.getSamplingHz();
			int i = 0;
			Files.deleteIfExists(outpath);
			Files.createFile(outpath);
			for (double p : partial.getData()) {
				Files.write(outpath, String.format("%.6f %.16e\n", (t0 + i*dt), p).getBytes(), StandardOpenOption.APPEND);
				i++;
			}
		}
		
	}

}
