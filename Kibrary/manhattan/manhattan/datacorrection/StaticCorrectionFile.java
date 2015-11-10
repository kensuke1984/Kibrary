package manhattan.datacorrection;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.lang3.StringUtils;

import filehandling.sac.SACComponent;
import manhattan.globalcmt.GlobalCMTID;

/**
 * Information File of {@link StaticCorrection}
 * 
 * 1 time shift {@value #oneShiftByte} byte
 * 
 * String station name(8)<br>
 * String of {@link GlobalCMTID} (15)<br>
 * component (1)<br>
 * Float start time (s) (4) round off to the third decimal place<br>
 * Float time shift(s) (4) round off to the third decimal place.<br>
 * Float max ratio(obs/syn) (4) round off to the third decimal place
 * 
 * 
 * @version 0.1.0
 * @since 2013/12/1 use global cmt
 * 
 * 
 * @version 0.1.5
 * @since 2014/10/13 to Jave 8 start time installed.
 * 
 * @version 0.2.0
 * @since 2015/5/31 amplitude
 * 
 * @version 0.2.0.1
 * @since 2015/8/7 {@link IOException}
 * 
 * @author Kensuke
 * 
 */
public final class StaticCorrectionFile {

	private StaticCorrectionFile() {
	}

	/**
	 * @param infoPath
	 *            of the correction must exist
	 * @return (<b>thread safe</b>) Set of StaticCorrection
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static Set<StaticCorrection> read(Path infoPath) throws IOException {
		if (Files.size(infoPath) % oneShiftByte != 0)
			throw new RuntimeException(infoPath + " is not valid..");
		Set<StaticCorrection> staticCorrectionSet = new HashSet<>();
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(infoPath)))) {
			byte[] strByte = new byte[8];
			byte[] eventByte = new byte[15];
			while (true) {
				dis.read(strByte);
				String station = new String(strByte).trim();
				dis.read(eventByte);
				String eventStr = new String(eventByte).trim();
				GlobalCMTID eventName = new GlobalCMTID(eventStr); //
				SACComponent component = SACComponent.getComponent(dis.readByte());
				double startTime = dis.readFloat();
				double timeshift = dis.readFloat();
				double ratioOfMax = dis.readFloat();
				StaticCorrection shift = new StaticCorrection(station, eventName, component, startTime, timeshift,
						ratioOfMax);
				staticCorrectionSet.add(shift);
			}
		} catch (EOFException e) {
		}
		System.err.println(staticCorrectionSet.size() + " static corrections are read.");
		return Collections.unmodifiableSet(staticCorrectionSet);

	}

	/**
	 * @param correctionSet
	 *            of static correction to write
	 * @param outPath
	 *            of an output file.
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static void write(Set<StaticCorrection> correctionSet, Path outPath, OpenOption... options)
			throws IOException {
		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outPath, options)))) {
			for (StaticCorrection correction : correctionSet) {
				dos.writeBytes(StringUtils.rightPad(correction.getStationName(), 8));
				dos.writeBytes(StringUtils.rightPad(correction.getGlobalCMTID().toString(), 15));
				dos.writeByte(correction.getComponent().valueOf());
				dos.writeFloat((float) correction.getSynStartTime());
				dos.writeFloat((float) correction.getTimeshift());
				dos.writeFloat((float) correction.getAmplitudeRatio());
			}
		}

	}

	/**
	 * The number of bytes for one time shift data
	 */
	public static final int oneShiftByte = 36;

	/**
	 * Shows all static corrections in a file
	 * 
	 * @param args
	 *            [static correction file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Set<StaticCorrection> tsif = null;
		if (args.length != 0)
			tsif = StaticCorrectionFile.read(Paths.get(args[0]));
		else {
			String s = JOptionPane.showInputDialog("file?");
			if (s == null || s.equals(""))
				return;
			tsif = StaticCorrectionFile.read(Paths.get(s));
		}
		tsif.forEach(System.out::println);
	}

}
