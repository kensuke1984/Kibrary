package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

public class testSAC {

	public static void main(String[] args) throws IOException {
		Path name = Paths.get("/work/anselme/TOPO/ETH/synthetics/PREM_nex400_ev7_stgl/20Hz/200602021248A/XMAS.200602021248A.Tsc");
		SACFileName sacname = new SACFileName(name);
		SACData data = sacname.read();
		Trace trace = data.createTrace();
		Path outpath = Paths.get("/work/anselme/TOPO/ETH/synthetics/PREM_nex400_ev7_stgl/20Hz/200602021248A/trace.txt");
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int i = 0; i < trace.getLength(); i++) {
			pw.println(trace.getXAt(i) + " " + trace.getYAt(i));
		}
		pw.close();
	}

}
