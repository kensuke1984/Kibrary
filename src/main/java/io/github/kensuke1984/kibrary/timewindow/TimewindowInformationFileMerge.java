/**
 * 
 */
package io.github.kensuke1984.kibrary.timewindow;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;
import java.util.Set;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

/**
 * @version 0.0.1
 * @since 2018/05/16
 * @author Yuki
 *
 */
public class TimewindowInformationFileMerge {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		if(args.length != 3 || args[0] == "--help") {
			System.err.println("usage: timewindow file src1(T), timewindow file src2(R), timewindow file output.");
			return;
		}	
		Path twiPathRef = Paths.get(args[0]);
		Path twiPath = Paths.get(args[1]);
		Path outPath = Paths.get(args[2]);
		Set<TimewindowInformation> twiOut = new HashSet<>();
		try {
			Set<TimewindowInformation> twiRefs = TimewindowInformationFile.read(twiPathRef);
			Set<TimewindowInformation> twis = TimewindowInformationFile.read(twiPath);
			twis.stream().forEach(twi -> {
				//start time for T
				double startTime = twiRefs.stream().filter(twiRef -> twiRef.getGlobalCMTID().equals(twi.getGlobalCMTID()))
						.filter(twiRef -> twiRef.getStation().equals(twi.getStation()))
						.findFirst().get().getStartTime();
				//end time for T
				double endTime = twiRefs.stream().filter(twiRef -> twiRef.getGlobalCMTID().equals(twi.getGlobalCMTID()))
						.filter(twiRef -> twiRef.getStation().equals(twi.getStation()))
						.findFirst().get().getEndTime();
				//component of reference file
				SACComponent refComp = twiRefs.stream().filter(twiRef -> twiRef.getGlobalCMTID().equals(twi.getGlobalCMTID()))
						.filter(twiRef -> twiRef.getStation().equals(twi.getStation()))
						.findFirst().get().getComponent();
				//phases of reference file
				Phase[] refPhases = twiRefs.stream().filter(twiRef -> twiRef.getGlobalCMTID().equals(twi.getGlobalCMTID()))
						.filter(twiRef -> twiRef.getStation().equals(twi.getStation()))
						.findFirst().get().getPhases();
				
				//add correction inf of R
				twiOut.add(new TimewindowInformation(startTime
						, endTime
						, twi.getStation()
						, twi.getGlobalCMTID()
						, twi.getComponent()
						, refPhases));
				//add correction inf of T
				twiOut.add(new TimewindowInformation(startTime
						, endTime
						, twi.getStation()
						, twi.getGlobalCMTID()
						, refComp
						, refPhases));
			});
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			TimewindowInformationFile.write(twiOut, outPath, StandardOpenOption.CREATE);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
