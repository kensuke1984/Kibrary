package io.github.kensuke1984.kibrary;

import io.github.kensuke1984.kibrary.datacorrection.FujiStaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.TakeuchiStaticCorrection;
import io.github.kensuke1984.kibrary.datarequest.DataRequestor;
import io.github.kensuke1984.kibrary.dsminformation.InformationFileMaker;
import io.github.kensuke1984.kibrary.dsminformation.SshDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.dsminformation.SyntheticDSMInformationFileMaker;
import io.github.kensuke1984.kibrary.external.gmt.RaypathDistribution;
import io.github.kensuke1984.kibrary.firsthandler.FirstHandler;
import io.github.kensuke1984.kibrary.inversion.CheckerBoardTest;
import io.github.kensuke1984.kibrary.inversion.LetMeInvert;
import io.github.kensuke1984.kibrary.selection.DataSelection;
import io.github.kensuke1984.kibrary.selection.FilterDivider;
import io.github.kensuke1984.kibrary.selection.SecondHandler;
import io.github.kensuke1984.kibrary.timewindow.TimewindowMaker;
import io.github.kensuke1984.kibrary.util.spc.SPC_SAC;
import io.github.kensuke1984.kibrary.waveformdata.ObservedSyntheticDatasetMaker;
import io.github.kensuke1984.kibrary.waveformdata.Partial1DDatasetMaker;
import io.github.kensuke1984.kibrary.waveformdata.PartialDatasetMaker;

import java.util.Arrays;

/**
 * The list of names of manhattan (operation)
 *
 * @author Kensuke Konishi
 * @version 0.0.5.2
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
    Partial1DDatasetMaker(10, Partial1DDatasetMaker.class), //
    PartialDatasetMaker(11, PartialDatasetMaker.class), //
    RaypathDistribution(12, RaypathDistribution.class), //
    SecondHandler(13, SecondHandler.class), //
    SPC_SAC(14, SPC_SAC.class), //
    SshDSMInformationFileMaker(15, SshDSMInformationFileMaker.class), //
    SyntheticDSMInformationFileMaker(16, SyntheticDSMInformationFileMaker.class), //
    TakeuchiStaticCorrection(17, TakeuchiStaticCorrection.class), //
    TimewindowMaker(18, TimewindowMaker.class),//
    ;

    private Class<? extends Operation> c;
    private int value;

    Manhattan(int n, Class<? extends Operation> c) {
        value = n;
        this.c = c;
    }

    public static void printList() {
        Arrays.stream(values()).sorted().forEach(m -> System.out.println(m.c.getSimpleName() + " " + m.value));
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
