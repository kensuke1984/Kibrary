package io.github.kensuke1984.kibrary;

import io.github.kensuke1984.kibrary.waveformdata.Partial1DDatasetMaker;
import io.github.kensuke1984.kibrary.waveformdata.PartialDatasetMaker;

import java.util.Arrays;
import io.github.kensuke1984.kibrary.util.spc.SpcSAC;
import io.github.kensuke1984.kibrary.selection.FilterDivider;
import io.github.kensuke1984.kibrary.timewindow.TimewindowMaker;

import io.github.kensuke1984.kibrary.dsminformation.SyntheticDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.dsminformation.SshDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.waveformdata.ObservedSyntheticDatasetMaker;
import io.github.kensuke1984.kibrary.inversion.CheckerBoardTest;
import io.github.kensuke1984.kibrary.datarequest.DataRequestor;
import io.github.kensuke1984.kibrary.selection.DataSelection;
import io.github.kensuke1984.kibrary.firsthandler.FirstHandler;
import io.github.kensuke1984.kibrary.selection.SecondHandler;
import io.github.kensuke1984.kibrary.external.gmt.RaypathDistribution;
import io.github.kensuke1984.kibrary.datacorrection.FujiStaticCorrection;
import io.github.kensuke1984.kibrary.dsminformation.InformationFileMaker;
import io.github.kensuke1984.kibrary.inversion.LetMeInvert;
import io.github.kensuke1984.kibrary.datacorrection.TakeuchiStaticCorrection;

/**
 * 
 * The list of names of manhattan (operation)
 * 
 * @author kensuke
 * @version 0.0.4
 */
public enum Manhattan {
	SpcSAC(0, SpcSAC.class), FilterDivider(1, FilterDivider.class), TimewindowMaker(2,
			TimewindowMaker.class), SyntheticDSMInformationFileMaker(3,
					SyntheticDSMInformationFileMaker.class), SshDSMInformationFileMaker(4,
							SshDSMInformationFileMaker.class), ObservedSyntheticDatasetMaker(5,
									ObservedSyntheticDatasetMaker.class), CheckerBoardTest(6,
											CheckerBoardTest.class), DataRequestor(7,
													DataRequestor.class), DataSelection(8,
															DataSelection.class), FirstHandler(9,
																	FirstHandler.class), SecondHandler(10,
																			SecondHandler.class), RaypathDistribution(
																					11,
																					RaypathDistribution.class), FujiStaticCorrection(
																							12,
																							FujiStaticCorrection.class), InformationFileMaker(
																									13,
																									InformationFileMaker.class), LetMeInvert(
																											14,
																											LetMeInvert.class), TakeuchiStaticCorrection(
																													15,
																													TakeuchiStaticCorrection.class), Partial1DDatasetMaker(
																															16,
																															Partial1DDatasetMaker.class),
PartialDatasetMaker(17,PartialDatasetMaker.class),
	;

	private int value;

	private Class<? extends Operation> c;

	private Manhattan(int n, Class<? extends Operation> c) {
		value = n;
		this.c = c;
	}

	static Manhattan valueOf(int n) {
		return Arrays.stream(values()).filter(m -> m.value == n).findAny().get();
	}

	public static void printList() {
		for (int i = 0; i < values().length; i++)
			System.out.println(i + ": " + valueOf(i).c.getName());
	}

	/**
	 * invoke main of this with the args
	 * 
	 * @param args
	 *            to input main
	 * @throws Exception
	 *             if any
	 */
	public void invokeMain(String[] args) throws Exception {
		c.getMethod("main", String[].class).invoke(null, (Object) args);
	}

	public void writeDefaultPropertiesFile() throws Exception {
		c.getMethod("writeDefaultPropertiesFile", (Class<?>[]) null).invoke(null, (Object[]) null);
	}

}
