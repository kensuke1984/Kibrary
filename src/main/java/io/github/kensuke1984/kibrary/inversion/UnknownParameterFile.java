package io.github.kensuke1984.kibrary.inversion;

import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.spc.PartialType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import io.github.kensuke1984.kibrary.inversion.addons.TimeReceiverSideParameter;
import io.github.kensuke1984.kibrary.inversion.addons.TimeSourceSideParameter;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

/**
 * m in Am=d
 * <p>
 * 3D(MU): PartialType lat lon r (weighting) <br>
 * 1D(PAR2): PartialType r (weighting)<br>
 * 後ろに重み付け(weighting)があれば読み込む（体積など）
 * <p>
 * TODO ３次元と１次元の混在をさける
 * <p>
 * 重みが違っても同じ種類、同じ場所の未知数があれば例外
 * <p>
 * Duplication is NOT allowed.
 *
 * @author Kensuke Konishi
 * @version 0.0.6
 */
public class UnknownParameterFile {

    private UnknownParameterFile() {
    }

	/**
	 * @param path of an unknown parameter file.
	 * @return <b>unmodifiable</b> List of unknown parameters in the path
	 * @throws IOException if an I/O error occurs.
	 */
	public static List<UnknownParameter> read(Path path) throws IOException {
		List<UnknownParameter> pars = new ArrayList<>();
		try (BufferedReader br = Files.newBufferedReader(path)) {
			String line;
			while (null != (line = br.readLine())) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#"))
					continue;
				String[] parts = line.split("\\s+");
				PartialType type = PartialType.valueOf(parts[0]);
				UnknownParameter unknown;
				switch (type) {
				case TIME_SOURCE:
					unknown = new TimeSourceSideParameter(new GlobalCMTID(parts[1]));
					pars.add(unknown);
					break;
				case TIME_RECEIVER:
					unknown = new TimeReceiverSideParameter(new Station(parts[1],
							new HorizontalPosition(Double.parseDouble(parts[3]), Double.parseDouble(parts[4])),
							parts[2]), Integer.parseInt(parts[5]));
					pars.add(unknown);
					break;
				case PARA:
				case PARC:
				case PARF:
				case PARL:
				case PARN:
				case PARQ:
				case PAR2:
				case PARVS:
				case PARVP:
				case PARG:
				case PARM:
				case PAR00:
				case PAR1:
					unknown = new Physical1DParameter(type, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]));
					pars.add(unknown);
					break;
				case A:
				case C:
				case F:
				case L:
				case N:
				case Q:
				case MU:
				case LAMBDA:
				case Vs:
				case LAMBDA2MU:
				default:
					unknown = new Physical3DParameter(type, new Location(Double.parseDouble(parts[1]),
							Double.parseDouble(parts[2]), Double.parseDouble(parts[3])), Double.parseDouble(parts[4]));
					pars.add(unknown);
				}
			}
		}
		for (int i = 0; i < pars.size() - 1; i++)
			for (int j = i + 1; j < pars.size(); j++)
				if (pars.get(i).equals(pars.get(j)))
					System.err.println("!Caution there is duplication in " + path);
//		return Collections.unmodifiableList(pars);
		return pars;
	}

    /**
     * @param outPath       for write
     * @param parameterList List of unknown parameters
     * @param options       for write
     * @throws IOException if an I/O error occurs
     */
    public static void write(Path outPath, List<UnknownParameter> parameterList, OpenOption... options)
            throws IOException {
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, options))) {
            parameterList.forEach(pw::println);
        }
    }

}
