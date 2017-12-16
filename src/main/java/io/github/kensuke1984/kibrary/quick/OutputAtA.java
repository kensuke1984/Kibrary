package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.inversion.ObservationEquation;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.nio.file.Path;
import java.nio.file.Paths;

public class OutputAtA {

	public static void main(String[] args) {
		Path partialPath = Paths.get("partial.dat");
		Path partialIDPath = Paths.get("partialID.dat");
		
//		PartialID[] partialIDs = PartialIDFile.readPartialIDandDataFile(partialIDPath, partialPath);
		
//		ObservationEquation eq = new ObservationEquation(partialIDs, parameterList, dVector, time_source, time_receiver, combinationType, nUnknowns, unknownParameterWeightType)

	}

}
