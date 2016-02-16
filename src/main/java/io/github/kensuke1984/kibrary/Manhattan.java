package io.github.kensuke1984.kibrary;

import java.util.Arrays;

/**
 * 
 * The list of names of manhattan (operation)
 * 
 * @author kensuke
 * @version 0.0.3
 */
public enum Manhattan {
	SpcSAC(0, io.github.kensuke1984.kibrary.util.spc.SpcSAC.class), FilterDivider(1,
			io.github.kensuke1984.kibrary.selection.FilterDivider.class), TimewindowMaker(2,
					io.github.kensuke1984.kibrary.timewindow.TimewindowMaker.class), SyntheticDSMInformationFileMaker(3,
							io.github.kensuke1984.kibrary.dsminformation.SyntheticDSMInformationFileMaker.class), SshDSMInformationFileMaker(
									4,
									io.github.kensuke1984.kibrary.dsminformation.SshDSMInformationFileMaker.class), ObservedSyntheticDatasetMaker(
											5,
											io.github.kensuke1984.kibrary.waveformdata.ObservedSyntheticDatasetMaker.class), CheckerBoardTest(
													6,
													io.github.kensuke1984.kibrary.inversion.CheckerBoardTest.class), DataRequestor(
															7,
															io.github.kensuke1984.kibrary.datarequest.DataRequestor.class), DataSelection(
																	8,
																	io.github.kensuke1984.kibrary.selection.DataSelection.class), FirstHandler(
																			9,
																			io.github.kensuke1984.kibrary.firsthandler.FirstHandler.class), SecondHandler(
																					10,
																					io.github.kensuke1984.kibrary.selection.SecondHandler.class), RaypathDistribution(
																							11,
																							io.github.kensuke1984.kibrary.external.gmt.RaypathDistribution.class);

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
