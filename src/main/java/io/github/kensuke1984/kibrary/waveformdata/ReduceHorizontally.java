package io.github.kensuke1984.kibrary.waveformdata;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReduceHorizontally {

	public static void main(String[] args) throws IOException {
		Path ataPath = Paths.get(args[0]);
		
		AtAEntry[][][][] ata = AtAFile.read(ataPath);
		
	}

}
