package manhattan.timewindow;

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
 * 
 * time window の情報をいれてあるファイル
 * 
 * 1 time window {@value #oneWindowByte} byte
 * 
 * String station (8), String of {@link GlobalCMTID} (15), component (1),<br>
 * Float starting time (4) (Round off to the third decimal place.),<br>
 * Float end time (4) (Round off to the third decimal place.), <br>
 * 
 * 
 * @version 0.2.0
 * @since 2015/9/14 Simplified.
 * 
 * 
 * @author Kensuke
 * 
 */
public final class TimewindowInformationFile {

	/**
	 * bytes for one time window information
	 */
	public static final int oneWindowByte = 32;

	private TimewindowInformationFile() {
	}

	/**
	 * @param args
	 *            [information file name]
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void main(String[] args) throws IOException {
		Set<TimewindowInformation> set = null;
		if (args.length != 0)
			set = TimewindowInformationFile.read(Paths.get(args[0]));
		else {
			String s = "";
			Path f = null;
			do {
				s = JOptionPane.showInputDialog("file?", s);
				if (s == null || s.equals(""))
					System.exit(0);
				f = Paths.get(s);
			} while (!Files.exists(f) || Files.isDirectory(f));
			set = TimewindowInformationFile.read(f);
		}
		set.forEach(System.out::println);
	}

	/**
	 * Output TimeWindowInformation
	 * 
	 * @param infoSet
	 *            Set of timewindow information
	 * @param outputPath
	 *            to write the information on
	 * @param options
	 *            for output
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static void write(Set<TimewindowInformation> infoSet, Path outputPath, OpenOption... options)
			throws IOException {
		try (DataOutputStream dos = new DataOutputStream(
				new BufferedOutputStream(Files.newOutputStream(outputPath, options)))) {
			for (TimewindowInformation info : infoSet) {
				dos.writeBytes(StringUtils.rightPad(info.getStationName(), 8));
				dos.writeBytes(StringUtils.rightPad(info.getGlobalCMTID().toString(), 15));
				dos.writeByte(info.getComponent().valueOf());
				float startTime = (float) (Math.round(info.startTime * 1000) / 1000.0);
				float endTime = (float) (Math.round(info.endTime * 1000) / 1000.0);
				dos.writeFloat((float) startTime);
				dos.writeFloat((float) endTime);
			}
		}
	}

	/**
	 * @param infoPath
	 *            of the information file to read
	 * @return (<b>unmodifiable</b>) Set of timewindow information
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static Set<TimewindowInformation> read(Path infoPath) throws IOException {
		if (Files.size(infoPath) % oneWindowByte != 0)
			throw new RuntimeException(infoPath + " has some problems.");
		Set<TimewindowInformation> infoSet = new HashSet<>();
		try (DataInputStream dis = new DataInputStream(new BufferedInputStream(Files.newInputStream(infoPath)));) {
			byte[] stationNameByte = new byte[8];
			byte[] eventNameByte = new byte[15];
			while (true) {
				dis.read(stationNameByte);
				// int i = dis.readInt();
				dis.read(eventNameByte);
				String stationName = new String(stationNameByte).trim();
				SACComponent component = SACComponent.getComponent(dis.readByte());
				GlobalCMTID id = new GlobalCMTID(new String(eventNameByte).trim());
				double startTime = dis.readFloat();
				double endTime = dis.readFloat();
				TimewindowInformation info = new TimewindowInformation(startTime, endTime, stationName, id, component);
				// System.out.println(info);
				infoSet.add(info);
			}
		} catch (EOFException e) {
		}
		System.err.println(infoSet.size() + " timewindow data were found");
		return Collections.unmodifiableSet(infoSet);

	}

}
