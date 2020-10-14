package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.kibrary.util.Utilities;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MergeAtA {
	public static void main(String[] args) throws IOException {
		AtAEntry[][][][] ataSum = AtAFile.read(Paths.get(args[0]));
		
		AtAHeader header = AtAHeader.readHeader(Paths.get(args[0]));
		
		for (int i = 1; i < args.length; i++) {
			AtAEntry[][][][] ata = AtAFile.read(Paths.get(args[i]));
			ataSum = AtAFile.add(ataSum, ata);
		}
		
		Path outpath = Paths.get("ata" + Utilities.getTemporaryString() + ".dat");
		AtAFile.write(ataSum, header.getWeightingTypes(), header.getFrequencyRanges()
				, header.getUnknownParameters(), header.getPhases(), outpath);
	}
}
