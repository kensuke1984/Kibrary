package io.github.kensuke1984.kibrary.timewindow.addons;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauPException;
import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.util.Trace;

import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
public class Pick {

    public static void main(String[] args) throws IOException, TauModelException, TauPException {
        for (SACFileName name : Utilities
                .sacFileNameSet(Paths.get("/home/kensuke/secondDisk/CentralAmerica/obs/divide"))) {
            System.out.println(name);
//            pickSMaxMin(name);
//            System.exit(0);
        }
    }


    private static void pickSMaxMin(SACFileName name) throws IOException {
        SACHeaderData synHeader = name.readHeader();
        SACData data = name.read();
        double start = synHeader.getValue(SACHeaderEnum.T0) - 20;
        double end = synHeader.getValue(SACHeaderEnum.T1) + 20;
        Trace trace = data.createTrace().cutWindow(start, end);
        double x_max = trace.getXforMaxValue();
        double x_min = trace.getXforMinValue();
        int[] downIndex = trace.getIndexOfDownwardConvex();
        int[] upIndex = trace.getIndexOfUpwardConvex();
        if (downIndex.length < 2 || upIndex.length < 2)

        {
            Path trash = name.toPath().resolveSibling("trash");
            Files.createDirectories(trash);
            Files.move(name.toPath(), trash.resolve(name.getName()));
            return;
        }
//        System.out.println(x_min + " " + x_max);
        data.setTimeMarker(SACHeaderEnum.T2, x_min).setTimeMarker(SACHeaderEnum.T3, x_max)
                .setTimeMarker(SACHeaderEnum.T6, trace.getXAt(downIndex[1]))
                .setTimeMarker(SACHeaderEnum.T7, trace.getXAt(upIndex[1])).writeSAC(name);
    }



    private static void pickSScS(Path sacPath) throws IOException, TauPException, TauModelException {
        SACFileName name = new SACFileName(sacPath);
        SACData data = name.read();
        Path trash = Paths.get("/home/kensuke/secondDisk/CentralAmerica/premsyn/divide/trash");
        double eventR = 6371 - data.getValue(SACHeaderEnum.EVDP);
        double gcarc = data.getValue(SACHeaderEnum.GCARC);
        if (95 < gcarc) {
            Files.move(sacPath, trash.resolve(sacPath.getFileName()));
            return;
        }

        Set<Phase> s = Collections.singleton(Phase.S);
        Set<Phase> scs = Collections.singleton(Phase.ScS);
    }
}

