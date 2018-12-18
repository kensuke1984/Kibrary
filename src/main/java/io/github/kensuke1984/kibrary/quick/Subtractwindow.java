package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Utilities;

public class Subtractwindow {

	public static void main(String[] args) throws IOException {
		Path infoPath = Paths.get(args[0]);
		Path infoToSubtractPath = Paths.get(args[1]);
		
		Set<TimewindowInformation> windows = TimewindowInformationFile.read(infoPath);
		Set<TimewindowInformation> windowsToSubtract = TimewindowInformationFile.read(infoToSubtractPath);
		
		Set<TimewindowInformation> outWindows = new HashSet<>();
		
		for (TimewindowInformation window : windows) {
			if (!windowsToSubtract.contains(window))
				outWindows.add(window);
		}

		Path outputPath = Paths.get("timewindow" + Utilities.getTemporaryString() + ".dat");
		TimewindowInformationFile.write(outWindows, outputPath);
	}

}
