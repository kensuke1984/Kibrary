package io.github.kensuke1984.kibrary.util.statistics;

import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import javax.swing.JOptionPane;

import org.apache.commons.io.input.CloseShieldInputStream;

public class EventInformationFile {
	
	public static void main (String[] args) throws IOException {
//		String[] idnames = new String[] {"200503171337A","200509260155A","200511091133A","200512232147A","200707060109A","200707120523A","200707211327A","200707232230A","200709260443A","200711160312A","200711291900A","200808262100A","200901170257A","200901292228A","201001032039A","201005160516A","201005190415A","201005241618A","201008121154A","201102251307A","201104071311A","201108150253A","201108241746A","201111072235A","201208020938A","201209011801A","201209301631A","201211101457A","201211280309A","201302091416A","201303252302A","201404112029A","201405211006A","201407291046A","201502221256A","201503102055A","201508050913A","201511260545A"};
//		String[] idnames = new String[] {"201503102055A","200707211327A","200707060109A","201211101457A","201008121154A","200711291900A","201502221256A","201005241618A","201209301631A","201108241746A","201005190415A","080704C","200709260443A","201404112029A","201208020938A","200901292228A","201303252302A","201108150253A","200707120523A","201111072235A","201302091416A","201001252252A","200503171337A","201005160516A","201001032039A","201412191949A","201503272159A","200808262100A","200512232147A","201211280309A","201209011801A","200511091133A","201009241901A","201508050913A","200509260155A","200507232009A","200510172159A","200504111454A","201501200659A","200703092101A","200711160312A","200702131456A","201410041916A","201407291046A","201405211006A","200901170257A","201011012316A","201504281856A","200707232230A","201104071311A","201511260545A","201102251307A"};
		String[] idnames = new String[] {"050304B","051900C","080704C","082699B","100104B","200503070717A","200503171337A","200504111454A","200507232009A","200509260155A","200510172159A","200511091133A","200512232147A","200702131456A","200703092101A","200707060109A","200707120523A","200707211327A","200707232230A","200709260443A","200711160312A","200711291900A","200802121250A","200808262100A","200809130932A","200901170257A","200901292228A","200905031621A","201001032039A","201001252252A","201005160516A","201005190415A","201005241618A","201008121154A","201009241901A","201010090154A","201011012316A","201102251307A","201104071311A","201105132247A","201108150253A","201108241746A","201111072235A","201206270631A","201208020938A","201209011801A","201209301631A","201211101457A","201211280309A","201302091416A","201303252302A","201404112029A","201405211006A","201405282115A","201407031906A","201407291046A","201410041916A","201410261045A","201412191949A","201501120757A","201501200659A","201502221256A","201503102055A","201503272159A","201504281856A","201508050913A","201510151007A","201511242245A","201511260545A","201512072253A","201512280655A","201601241030A","201609101008A","201610182208A","201612181330A","201701021313A","201705201758A"};
		for (String name : idnames) {
			GlobalCMTID id = new GlobalCMTID(name);
//			System.out.printf("%.1f\n",Earth.EARTH_RADIUS - id.getEvent().getCmtLocation().getR());
			System.out.println(id.getEvent().getCmt().getMw()+","+(Earth.EARTH_RADIUS-id.getEvent().getCmtLocation().getR())
					+","+id.getEvent().getHalfDuration());
		}
		System.exit(0);
		if (0 < args.length) {
			String path = args[0];
			if (!path.startsWith("/"))
				path = System.getProperty("user.dir") + "/" + path;
			Path f = Paths.get(path);
			if (Files.exists(f) && Files.isDirectory(f))
				createEventInformationFile(f);
			else
				System.out.println(f + " does not exist or is not a directory.");
		} else {
			Path workPath;
			String path = "";
			do {
				try {
					path = JOptionPane.showInputDialog("Working folder?", path);
				} catch (Exception e) {
					System.out.println("Working folder?");
					try (BufferedReader br = new BufferedReader(
							new InputStreamReader(new CloseShieldInputStream(System.in)))) {
						path = br.readLine().trim();
						if (!path.startsWith("/"))
							path = System.getProperty("user.dir") + "/" + path;
					} catch (Exception e2) {
						e2.printStackTrace();
						throw new RuntimeException();
					}
				}
				if (path == null || path.isEmpty())
					return;
				workPath = Paths.get(path);
				if (!Files.isDirectory(workPath))
					continue;
				// System.out.println(tmp.getAbsolutePath());
			} while (!Files.exists(workPath) || !Files.isDirectory(workPath));
			createEventInformationFile(workPath);
		}
	}
	
	private static void createEventInformationFile(Path workPath, OpenOption... options) throws IOException {
		Path outpath = workPath.resolve("event" + Utilities.getTemporaryString() + ".inf");
		Set<EventFolder> eventFolderSet = Utilities.eventFolderSet(workPath);
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath, options))) {
			pw.write("#GCMTID lat lon radius #transverse #vertical #radial Mw noiseToAmplitudeBeforeS noiseRatioVariance");
			for (EventFolder folder : eventFolderSet) {
				GlobalCMTID id = folder.getGlobalCMTID();
				Location location = id.getEvent().getCmtLocation();
				double Mw = id.getEvent().getCmt().getMw();
				Set<SACFileName> sacnames = folder.sacFileSet();
				int nT = (int) sacnames.stream().filter(sacname -> sacname.getComponent().equals(SACComponent.T)).count();
				int nZ = (int) sacnames.stream().filter(sacname -> sacname.getComponent().equals(SACComponent.Z)).count();
				int nR = (int) sacnames.stream().filter(sacname -> sacname.getComponent().equals(SACComponent.R)).count();
				double[] noiseToAmplitudeRatio = noiseToAmplitudeRatio(sacnames);
				pw.write(id + " " + location + " " + nT + " " + nZ + " " + nR + " " + Mw + " " + noiseToAmplitudeRatio[0] + noiseToAmplitudeRatio[1]);
			}
		}
	}
	
	private static double[] noiseToAmplitudeRatio(Set<SACFileName> sacnames) {
		double[] noise = new double[2];
		
		return noise;
	}
}
