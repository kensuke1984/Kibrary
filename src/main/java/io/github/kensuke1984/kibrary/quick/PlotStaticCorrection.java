package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PlotStaticCorrection {

	public static void main(String[] args) throws IOException {
		Path waveformIDPath = Paths.get(args[0]);
		Path waveformPath = Paths.get(args[1]);
		BasicID[] ids = BasicIDFile.read(waveformIDPath, waveformPath);
		Path outpath = Paths.get("timeshift_" + args[0]);
		
		Dvector dVector = new Dvector(ids);
		
		PrintWriter pw = new PrintWriter(outpath.toFile());
		for (int i = 0; i < dVector.getNTimeWindow(); i++) {
			double dt = dVector.getObsIDs()[i].getStartTime() - dVector.getSynIDs()[i].getStartTime();
			pw.write(dVector.getObsIDs()[i].getStation() + " " + dVector.getObsIDs()[i].getGlobalCMTID() + " " + dt + "\n");
		}
		
		pw.close();
	}

}
