package io.github.kensuke1984.kibrary;

import io.github.kensuke1984.kibrary.datacorrection.FujiStaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.TakeuchiStaticCorrection;
import io.github.kensuke1984.kibrary.datarequest.DataRequestor;
import io.github.kensuke1984.kibrary.dsminformation.InformationFileMaker;
import io.github.kensuke1984.kibrary.axiSEM.Result;
import io.github.kensuke1984.kibrary.dsminformation.SshDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.dsminformation.SyntheticDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.external.gmt.RaypathDistribution;
import io.github.kensuke1984.kibrary.firsthandler.FirstHandler;
import io.github.kensuke1984.kibrary.inversion.CheckerBoardTest;
import io.github.kensuke1984.kibrary.inversion.LetMeInvert;
import io.github.kensuke1984.kibrary.inversion.LetMeInvert_fromAtA;
import io.github.kensuke1984.kibrary.selection.DataSelection;
import io.github.kensuke1984.kibrary.selection.FilterDivider;
import io.github.kensuke1984.kibrary.selection.SecondHandler;
import io.github.kensuke1984.kibrary.timewindow.TimewindowMaker;
import io.github.kensuke1984.kibrary.util.spc.SPC_SAC;
import io.github.kensuke1984.kibrary.waveformdata.ObservedSyntheticDatasetMaker;
import io.github.kensuke1984.kibrary.waveformdata.Partial1DDatasetMaker_v2;
import io.github.kensuke1984.kibrary.waveformdata.PartialDatasetMaker_v2;
import io.github.kensuke1984.kibrary.waveformdata.addons.ObservedSyntheticDatasetMaker_SpcTest;
import io.github.kensuke1984.kibrary.waveformdata.addons.Partial1DEnvelopeMaker;
import io.github.kensuke1984.kibrary.waveformdata.addons.Partial1DSpcMaker;
import io.github.kensuke1984.kibrary.datacorrection.SourceTimeFunctionByGridSearch;
import io.github.kensuke1984.kibrary.selection.PhaseEnvelope;
import io.github.kensuke1984.kibrary.waveformdata.addons.AtAMaker;

import java.util.Arrays;

/**
 * The list of names of manhattan (operation)
 *
 * @author Kensuke Konishi
 * @version 0.0.5.3
 */
public enum Manhattan {
	CheckerBoardTest(1, CheckerBoardTest.class), //
	DataRequestor(2, DataRequestor.class), //
	DataSelection(3, DataSelection.class), //
	FilterDivider(4, FilterDivider.class), //
	FirstHandler(5, FirstHandler.class), //
	FujiStaticCorrection(6, FujiStaticCorrection.class), //
	InformationFileMaker(7, InformationFileMaker.class), //
	LetMeInvert(8, LetMeInvert.class), //
	ObservedSyntheticDatasetMaker(9, ObservedSyntheticDatasetMaker.class), //
	Partial1DDatasetMaker(10, Partial1DDatasetMaker_v2.class), //
	PartialDatasetMaker(11, PartialDatasetMaker_v2.class), //
	PhaseEnvelope(12, PhaseEnvelope.class), //
	RaypathDistribution(13, RaypathDistribution.class), //
	Result(14, Result.class), //
	SecondHandler(15, SecondHandler.class), //
	SourceTimeFunctionByGridSearch(16, SourceTimeFunctionByGridSearch.class), //
	SPC_SAC(17, SPC_SAC.class), //
	SshDSMInformationFileMaker(18, SshDSMInformationFileMaker.class), //
	SyntheticDSMInformationFileMaker(19, SyntheticDSMInformationFileMaker.class), //
	TakeuchiStaticCorrection(20, TakeuchiStaticCorrection.class), //
	TimewindowMaker(21, TimewindowMaker.class),//
	AtAMaker(22, AtAMaker.class),//
	LetMeInvert_fromAtA(23, LetMeInvert_fromAtA.class),//
	Partial1DEnvelopeMaker(24, Partial1DEnvelopeMaker.class),//
	Partial1DSpcMaker(25, Partial1DSpcMaker.class),//
	ObservedSyntheticDatasetMaker_SpcTest(26, ObservedSyntheticDatasetMaker_SpcTest.class), //
	;

    private Class<? extends Operation> c;
    private int value;

    Manhattan(int n, Class<? extends Operation> c) {
        value = n;
        this.c = c;
    }

    public static void printList() {
        Arrays.stream(values()).sorted().forEach(m -> System.err.println(m.c.getSimpleName() + " " + m.value));
    }

    static Manhattan valueOf(int n) {
        return Arrays.stream(values()).filter(m -> m.value == n).findAny().get();
    }

    /**
     * invoke main of this with the args
     *
     * @param args to input main
     * @throws Exception if any
     */
    public void invokeMain(String[] args) throws Exception {
        c.getMethod("main", String[].class).invoke(null, (Object) args);
    }

    public void writeDefaultPropertiesFile() throws Exception {
        c.getMethod("writeDefaultPropertiesFile", (Class<?>[]) null).invoke(null, (Object[]) null);
    }

}
