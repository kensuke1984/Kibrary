package io.github.kensuke1984.kibrary.timewindow;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

public class CutWindowSinglePhase {

	public static void main(String[] args) throws IOException {
		Path timewindowPath = Paths.get(args[0]);
		
		
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
		
	}

}
