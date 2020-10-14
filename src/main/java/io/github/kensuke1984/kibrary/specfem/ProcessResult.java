package io.github.kensuke1984.kibrary.specfem;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ProcessResult {

	public static void main(String[] args) {
		Path root = Paths.get(".");
		try {
			createSACScript(root);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void createSACScript(Path root) throws IOException {
		Path sacmacroFile = root.resolve("process.sacm");
		Path basicsacShell = root.resolve("runbasicsac.sh");
		Path shellScript = root.resolve("runsac.sh");
		Path mvtoeventScript = root.resolve("mvtoeventdir.sh");
		
		PrintWriter writer = new PrintWriter(sacmacroFile.toFile());
		writer.println("SETBB dir $1\n"
				+ "DO file WILD %dir%/*sac\n"
				+ "r $file\n"
				+ "interpolate delta 0.05\n"
				+ "if &1,KCMPNM EQ \"MXT\"\n"
				+ "SETBB COMP \"T\"\n"
				+ "elseif &1,KCMPNM EQ \"MXR\"\n"
				+ "SETBB COMP \"R\"\n"
				+ "elseif &1,KCMPNM EQ \"MXZ\"\n"
				+ "SETBB COMP \"Z\"\n"
				+ "endif\n"
				+ "chnhdr KCMPNM %COMP\n"
				+ "dif five\n"
				+ "mul -1\n"
				+ "w %dir%/&1,KSTNM&.&1,KEVNM&.%COMP%sc\n"
				+ "ENDDO"
				);
		writer.close();
		
		writer = new PrintWriter(basicsacShell.toFile());
		writer.println("sac <<EOF\n"
				+ "macro process.sacm $1\n"
				+ "quit\n"
				+ "EOF"
				);
		writer.close();
		
		writer = new PrintWriter(shellScript.toFile());
		writer.println("#!/bin/sh\n"
				+ "#To run in a folder that contains the event folders with SAC files output by SPECFEM3D_GLOBE\n"
				+ "export SAC_DISPLAY_COPYRIGHT=0\n"
				+ "for i in ./run00*/OUTPUT_FILES\n"
				+ "do\n"
				+ "   echo $i\n"
				+ "   sh runbasicsac.sh $i\n"
				+ "done"
				);
		writer.close();
		
		writer = new PrintWriter(mvtoeventScript.toFile());
		writer.println("#!/bin/sh\n"
				+ "for i in run00*\n"
				+ "do\n"
				+ " cd $i\n"
				+ " dir=$(awk 'NR==2 {print $3}' DATA/CMTSOLUTION)\n"
				+ " echo $dir\n"
				+ " mkdir $dir\n"
				+ " mv OUTPUT_FILES/*sc $dir\n"
				+ " cd ..\n"
				+ "done"
				);
		writer.close();
	}
}
