package io.github.kensuke1984.kibrary.util.spc;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

public class InputLLNLEARTH3D {

	public static void main(String[] args) throws IOException {
//		Path windowPath = Paths.get(args[0]);
		Path windowPath =  Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_stf_12.5-200s/selectedTimewindow_ScScutS-v2_cc05.dat");
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(windowPath);
		
		Path outpath = Paths.get("/work/anselme/CA_ANEL_NEW/syntheticPREM/filtered_stf_12.5-200s/llnl3d_input.dat");
		writeInput(outpath, timewindows);
	}
	
	private static void writeInput(Path outpath, Set<TimewindowInformation> timewindows) throws IOException {
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (TimewindowInformation window : timewindows) {
			pw.println(getLine(window, "S"));
			pw.println(getLine(window, "ScS"));
		}
		pw.close();
	}
	
	private static String getLine(TimewindowInformation window, String phase) {
		Location evtLoc = window.getGlobalCMTID().getEvent().getCmtLocation();
		HorizontalPosition staPos = window.getStation().getPosition();
		return evtLoc.getLatitude() + " " + evtLoc.getLongitude() + " " + (6371. - evtLoc.getR())
			+ " " + staPos.getLatitude() + " " + staPos.getLongitude() + " " + 0. + " " + phase 
			+ " " + window.getGlobalCMTID() + " " + window.getStation().getStationName() + " " + window.getStation().getNetwork();
	}

}
