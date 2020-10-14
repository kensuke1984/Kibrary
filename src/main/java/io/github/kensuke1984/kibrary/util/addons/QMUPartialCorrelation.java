package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.ObservationEquation;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

import org.apache.commons.math3.linear.RealMatrix;

public class QMUPartialCorrelation {

	public static void main(String[] args) throws IOException {
		if (args.length != 5) {
			throw new RuntimeException("partialID partial basicID basic unknowns");
		}
		Path dataPath = Paths.get(args[1]);
		Path idPath = Paths.get(args[0]);
		Path unknownsPath = Paths.get(args[4]);
		Path basicDataPath = Paths.get(args[3]);
		Path basicIDPath = Paths.get(args[2]);
		
		PartialID[] partialIDs = PartialIDFile.read(idPath, dataPath);
		List<UnknownParameter> unknowns = UnknownParameterFile.read(unknownsPath);
		BasicID[] basicIDs = BasicIDFile.read(basicIDPath, basicDataPath);
		Dvector dVector = new Dvector(basicIDs);
		
		ObservationEquation equation = new ObservationEquation(partialIDs, unknowns, dVector, false, false, null, null, null, null);
		RealMatrix ata = equation.getAtA();
		
		Path outpath = Paths.get("parMU_Q_correlation.inf");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
			for (int i = 0; i < unknowns.size(); i++) {
				UnknownParameter iPar = unknowns.get(i);
				double iloc = iPar.getLocation().getR();
				if (!iPar.getPartialType().equals(PartialType.PAR2))
					continue;
				for (int j = 0; j < unknowns.size(); j++) {
					UnknownParameter jPar = unknowns.get(j);
					double jloc = jPar.getLocation().getR();
					if (jPar.getPartialType().equals(PartialType.PARQ)
							&& iloc == jloc) {
						double dot = ata.getEntry(i, j) / Math.sqrt(ata.getEntry(i, i) * ata.getEntry(j, j));
						double depth = iPar.getLocation().getR();
						
						pw.println(depth + " " + dot);
						break;
					}
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
