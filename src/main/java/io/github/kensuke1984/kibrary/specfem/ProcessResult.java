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
		Path basicsacShell = root.resolve("runbascisac.sh");
		Path shellScript = root.resolve("runsac.sh");
		
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
				+ "chnhdr IDEP \"VEL\""
				+ "w %dir%/&1,KSTNM&_&1,KNETWK&.&1,KEVNM&.%COMP%sc\n"
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
				+ "export SAC_DISPLAY_COPYRIGHT=0\n"
				+ "for i in test*\n"
				+ "do\n"
				+ "   echo $i\n"
				+ "   sh basicsac.sh $i\n"
				+ "done"
				);
		writer.close();
	}
}
