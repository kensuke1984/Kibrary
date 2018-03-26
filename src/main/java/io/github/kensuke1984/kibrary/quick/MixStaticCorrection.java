package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.util.Utilities;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

public class MixStaticCorrection {

	public static void main(String[] args) throws IOException {
		Path fujiStaticPath = Paths.get(args[0]);
		Path SEMFullStaticPath = Paths.get(args[1]);
		Path SEMTruncStaticPath = Paths.get(args[2]);
		
		Set<StaticCorrection> fujiCorrections = StaticCorrectionFile.read(fujiStaticPath);
		Set<StaticCorrection> semCorrections = StaticCorrectionFile.read(SEMFullStaticPath);
		Set<StaticCorrection> semTruncCorrections = StaticCorrectionFile.read(SEMTruncStaticPath);
		
		Set<StaticCorrection> mixed = new HashSet<>();
		
		for (StaticCorrection corr : fujiCorrections) {
			StaticCorrection semCorr = semCorrections.stream().filter(c -> corr.getGlobalCMTID().equals(c.getGlobalCMTID())
					&& corr.getStation().equals(c.getStation())
					&& corr.getComponent().equals(c.getComponent())
					&& corr.getSynStartTime() == c.getSynStartTime()).findFirst().get();
			StaticCorrection semTruncCorr = semTruncCorrections.stream().filter(c -> corr.getGlobalCMTID().equals(c.getGlobalCMTID())
					&& corr.getStation().equals(c.getStation())
					&& corr.getComponent().equals(c.getComponent())
					&& corr.getSynStartTime() == c.getSynStartTime()).findFirst().get();
			double difference = corr.getTimeshift() - semCorr.getTimeshift();
			StaticCorrection zeroCorr = new StaticCorrection(corr.getStation(), corr.getGlobalCMTID(), corr.getComponent()
					, corr.getSynStartTime(), 0., corr.getAmplitudeRatio(), corr.getPhases());
			StaticCorrection mix = Math.abs(difference) <= Math.abs(corr.getTimeshift()) ? semTruncCorr : zeroCorr;
			mixed.add(mix);
		}
		
		Path outmix = Paths.get("staticCorrection" + Utilities.getTemporaryString() + ".dat");
		System.out.println("Write mixed correction");
		StaticCorrectionFile.write(mixed, outmix);
	}
	
}
