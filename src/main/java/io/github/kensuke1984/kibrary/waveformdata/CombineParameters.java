package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.kibrary.inversion.HorizontalParameterMapping;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class CombineParameters {

	public static void main(String[] args) {
		Path ataPath = Paths.get(args[0]);
		Path atdPath = Paths.get(args[1]);
		Path unknownPath = Paths.get(args[2]);
		Path mappingPath = Paths.get(args[3]);
		
		try {
			AtAEntry[][][][] ata = AtAFile.read(ataPath);
			AtdEntry[][][][][] atd = AtdFile.readArray(atdPath);
			UnknownParameter[] originalUnknowns = UnknownParameterFile.read(unknownPath).toArray(new UnknownParameter[0]);
			
			HorizontalParameterMapping mapping = new HorizontalParameterMapping(originalUnknowns, mappingPath);
			
			
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		

	}

}
