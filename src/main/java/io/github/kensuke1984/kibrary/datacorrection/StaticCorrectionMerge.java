package io.github.kensuke1984.kibrary.datacorrection;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;

public class StaticCorrectionMerge {

	public static void main(String[] args) {
		if(args.length != 3 || args[0] == "--help") {
			System.err.println("usage: correction file src1(T), correction file src2(R), correction file output.");
			return;
		}	
		Path correctionPathRef = Paths.get(args[0]);
		Path correctionPath = Paths.get(args[1]);
		Path outPath = Paths.get(args[2]);
		Set<StaticCorrection> correctionOuts = new HashSet<>();
		try {
			Set<StaticCorrection> correctionRefs = StaticCorrectionFile.read(correctionPathRef);
			Set<StaticCorrection> corrections = StaticCorrectionFile.read(correctionPath);
			corrections.stream().forEach(corr -> {
				//timeshift for T
				double timeShift = correctionRefs.stream().filter(corrRef -> corrRef.getGlobalCMTID().equals(corr.getGlobalCMTID()))
						.filter(corrRef -> corrRef.getStation().equals(corr.getStation()))
						.findFirst().get().getTimeshift();
				//maximum amplitude for T
				double amplitudeRatio = correctionRefs.stream().filter(corrRef -> corrRef.getGlobalCMTID().equals(corr.getGlobalCMTID()))
						.filter(corrRef -> corrRef.getStation().equals(corr.getStation()))
						.findFirst().get().getAmplitudeRatio();
				//component of reference file
				SACComponent refComp = correctionRefs.stream().filter(corrRef -> corrRef.getGlobalCMTID().equals(corr.getGlobalCMTID()))
						.filter(corrRef -> corrRef.getStation().equals(corr.getStation()))
						.findFirst().get().getComponent();
				
				//add correction inf of R
				correctionOuts.add(new StaticCorrection(corr.getStation()
						, corr.getGlobalCMTID()
						, corr.getComponent()
						, corr.getSynStartTime()
						, timeShift
						, amplitudeRatio));
				//add correction inf of T
				correctionOuts.add(new StaticCorrection(corr.getStation()
						, corr.getGlobalCMTID()
						, refComp
						, corr.getSynStartTime()
						, timeShift
						, amplitudeRatio));
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			StaticCorrectionFile.write(correctionOuts, outPath, StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}

}
