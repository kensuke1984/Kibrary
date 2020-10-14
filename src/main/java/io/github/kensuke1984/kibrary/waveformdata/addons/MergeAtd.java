package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.kibrary.util.Utilities;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class MergeAtd {
	public static void main(String[] args) throws IOException {
		AtdEntry[][][][][] atdSum = AtdFile.readArray(Paths.get(args[0]));
		
		AtdHeader header = AtdHeader.readHeader(Paths.get(args[0]));
		
		for (int i = 1; i < args.length; i++) {
			AtdEntry[][][][][] atd = AtdFile.readArray(Paths.get(args[i]));
			atdSum = AtdFile.add(atdSum, atd);
		}
		
		List<AtdEntry> atdList = new ArrayList<>();
		int n0 = atdSum.length;
		int n1 = atdSum[0].length;
		int n2 = atdSum[0][0].length;
		int n3 = atdSum[0][0][0].length;
		int n4 = atdSum[0][0][0][0].length;
		for (int i0 = 0; i0 < n0; i0++) {
			for (int i1 = 0; i1 < n1; i1++) {
				for (int i2 = 0; i2 < n2; i2++) {
					for (int i3 = 0; i3 < n3; i3++) {
						for (int i4 = 0; i4 < n4; i4++)
							atdList.add(atdSum[i0][i1][i2][i3][i4]);
					}
				}
			}
		}
		
		Path outpath = Paths.get("atd" + Utilities.getTemporaryString() + ".dat");
		AtdFile.write(atdList, header.getWeightingTypes(), header.getFrequencyRanges()
				, header.getPartialTypes(), header.getCorrectionTypes(), outpath);
	}
}
