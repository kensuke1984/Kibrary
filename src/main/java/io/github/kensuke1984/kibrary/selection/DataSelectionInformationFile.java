package io.github.kensuke1984.kibrary.selection;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
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

//import javax.rmi.CORBA.Util;

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
		
		writer.println("#start time, end time, station, lat, lon, network, event, component, phases, max ratio, min ratio, abs ratio, variance, cc, SN ratio");
		for (DataSelectionInformation info : infoList)
			writer.println(info);
		
		writer.close();
	}
	
	public static void outputHistograms(Path rootpath, List<DataSelectionInformation> infoList) throws IOException {
		double dVar = 0.1;
		double dCC = 0.1;
		double dRatio = 0.1;
		double maxVar = 5.;
		double maxCC = 1.;
		double maxRatio = 5.;
		int nVar = (int) (maxVar / dVar) + 1;
		int nCC = (int) (2 * maxCC / dCC) + 1;
		int nRatio = (int) (maxRatio / dRatio) + 1;
		int[] vars = new int[nVar];
		int[] ccs = new int[nCC];
		int[] ratios = new int[nRatio];
		Path varPath = rootpath.resolve("histogram_variance" + Utilities.getTemporaryString() + ".dat");
		Path corPath = rootpath.resolve("histogram_cc" + Utilities.getTemporaryString() + ".dat");
		Path ratioPath = rootpath.resolve("histogram_ratio" + Utilities.getTemporaryString() + ".dat");
		
		for (DataSelectionInformation info : infoList) {
			if (info.getVariance() > maxVar 
				 || info.getCC() > maxCC
				 || info.getAbsRatio() > maxRatio)
				continue;
			int iVar = (int) (info.getVariance() / dVar);
			int iCC = (int) ((info.getCC() + 1.) / dCC);
			int iRatio = (int) (info.getAbsRatio() / dRatio);
			vars[iVar] += 1;
			ccs[iCC] += 1;
			ratios[iRatio] += 1;
		}
		
		PrintWriter writer = new PrintWriter(Files.newBufferedWriter(varPath,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND));
		for (int i = 0; i < nVar; i++)
			writer.println(i * dVar + " " + vars[i]);
		writer.close();
		
		writer = new PrintWriter(Files.newBufferedWriter(corPath,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND));
		for (int i = 0; i < nCC; i++)
			writer.println((i * dCC - 1) + " " + ccs[i]);
		writer.close();
		
		writer = new PrintWriter(Files.newBufferedWriter(ratioPath,
				StandardOpenOption.CREATE, StandardOpenOption.APPEND));
		for (int i = 0; i < nRatio; i++)
			writer.println(i * dRatio + " " + ratios[i]);
		writer.close();
	}
	
//	public static void outputEventInfo(Path rootpath, List<DataSelectionInformation> infoList) throws IOException {
//		infoList.
//	}
}
