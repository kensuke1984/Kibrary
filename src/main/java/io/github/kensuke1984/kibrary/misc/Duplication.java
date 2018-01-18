/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import io.github.kensuke1984.kibrary.external.SAC;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.sac.SACExtension;

/**
 * @version 0.0.1
 * @since 2018/01/14
 * @author Yuki
 *
 */
public class Duplication {
	
	static double[] latitudes = {0, 0, 0};
	static double[] longitudes = {0, 0, 0};
	static Path twPath = Paths.get("");
	static String[] nStNames = {"", "", ""};

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		// TODO 自動生成されたメソッド・スタブ
		
		HorizontalPosition hp = new HorizontalPosition(latitudes[0], longitudes[0]);
		
		Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(twPath);
		timewindows.stream()
			.filter(tw -> tw.getStation().getPosition().equals(hp))
			.forEach(tw -> {
				String evtDir = tw.getGlobalCMTID().toString();
				Path sacSynPath = Paths.get(evtDir+"/"+tw.getStation()+"."+tw.getGlobalCMTID().toString()+"."+SACExtension.Tsc);
				Path sacObsPath = Paths.get(evtDir+"/"+tw.getStation()+"."+tw.getGlobalCMTID().toString()+"."+SACExtension.T);
				Path newSynPath = Paths.get(evtDir+"/"+nStNames[0]+"."+tw.getGlobalCMTID().toString()+"."+SACExtension.Tsc);
				Path newObsPath = Paths.get(evtDir+"/"+nStNames[0]+"."+tw.getGlobalCMTID().toString()+"."+SACExtension.T);
				try {
					modifySAC(sacObsPath);
					modifySAC(sacSynPath);
					sacSynPath.toFile().renameTo(newSynPath.toFile());
					sacObsPath.toFile().renameTo(newObsPath.toFile());
				} catch (IOException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				} catch (InterruptedException e) {
					// TODO 自動生成された catch ブロック
					e.printStackTrace();
				}
			});
	}
	
	private static void modifySAC(Path sacPath) throws IOException, InterruptedException {
		try (SAC sacD = SAC.createProcess()) {
			String cwd = sacPath.getParent().toString();
			sacD.inputCMD("cd " + cwd);// set current directory
			sacD.inputCMD("r " + sacPath.getFileName());// read
			sacD.inputCMD("ch lovrok true");// overwrite permission
			
			//change station name
			if (sacPath.toString().contains("Tsc") || sacPath.toString().contains("T"))
				sacD.inputCMD("ch KSTNM "+nStNames[0]);
				
			sacD.inputCMD("w over");
		}
	}

}
