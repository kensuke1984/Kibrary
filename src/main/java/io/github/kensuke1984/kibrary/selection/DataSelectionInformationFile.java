package io.github.kensuke1984.kibrary.selection;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public class DataSelectionInformationFile {

	public static void main(String[] args) throws IOException {
		Path infoPath = Paths.get(args[0]);
		read(infoPath).stream().forEach(info -> {
			System.out.println(info);
		});
	}
	
	public static List<DataSelectionInformation> read(Path infoPath) throws IOException {
		List<DataSelectionInformation> infoList = new ArrayList<>();
		
		Files.readAllLines(infoPath).stream().forEach(line -> {
			String[] s = line.split("\\s+");
			Station station = new Station(s[2], new HorizontalPosition(Double.parseDouble(s[3]), Double.parseDouble(s[4])), s[5]);
			Phase[] phases = Stream.of(s[8].split(",")).map(string -> Phase.create(string)).toArray(Phase[]::new);
			
			TimewindowInformation timewindow = new TimewindowInformation(Double.parseDouble(s[0]), Double.parseDouble(s[1]), station,
					new GlobalCMTID(s[6]), SACComponent.valueOf(s[7]), phases);
			
			DataSelectionInformation info = new DataSelectionInformation(timewindow, Double.parseDouble(s[9]),
					Double.parseDouble(s[10]), Double.parseDouble(s[11]), Double.parseDouble(s[12]), Double.parseDouble(s[13]), Double.parseDouble(s[14]));
			
			infoList.add(info);
		});
		
		return infoList;
	}
	
	public static void write(Path outpath, List<DataSelectionInformation> infoList) throws IOException {
		PrintWriter writer = new PrintWriter(Files.newBufferedWriter(outpath,
						StandardOpenOption.CREATE, StandardOpenOption.APPEND));
		
		for (DataSelectionInformation info : infoList)
			writer.println(info);
		
		writer.close();
	}
}
