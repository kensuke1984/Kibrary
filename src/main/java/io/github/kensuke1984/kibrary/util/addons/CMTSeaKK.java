package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.util.Set;

class CMTSeaKK {

	public static void main(String[] args) {
		try {
//			BufferedReader br = new BufferedReader(new FileReader(
//					"/mnt/melonpan/anpan/inversion/Dpp/carib/CNSN/oldName.txt"));
//			String s;
//			boolean cnsn = true;
//			while ((s = br.readLine()) != null) {
//				System.out.println(s+":");
//				int year = 0;
//				int month = 0;
//				int day = 0;
//				if (cnsn) {
//					month = Integer.parseInt(s.substring(0, 2));
//					day = Integer.parseInt(s.substring(2, 4));
//					if (s.charAt(4) == '0')
//						year = Integer.parseInt("20" + s.substring(4, 6));
//					if (s.charAt(4) == '9')
//						year = Integer.parseInt("19" + s.substring(4, 6));
//				}
//				else {
//					if (s.charAt(0) == '9')
//						year = Integer.parseInt("199" + s.charAt(1));
//					if (s.charAt(0) == '1')
//						year = Integer.parseInt("201" + s.charAt(1));
//					if (s.charAt(0) == '2') {
//						year = Integer.parseInt(s.substring(0, 4));
//						month = Integer.parseInt(s.substring(4, 6));
//						day = Integer.parseInt(s.substring(6, 8));
//					}
//					else {
//						month = Integer.parseInt(s.substring(4, 6));
//						day = Integer.parseInt(s.substring(2, 4));
//					}
//				}
//				
//				GlobalCMTSearch sea = new GlobalCMTSearch(LocalDate.of(year,
//						month, day));
			
				
//				double tmp = new GlobalCMTID("201511260545A").getEvent().getCmtLocation().getEpicentralDistance(new HorizontalPosition(61.413, -148.4585))
//						* 180 / Math.PI;
				
//				System.out.println(tmp);
//				System.out.println(new GlobalCMTID("201511260545A").getEvent().getCmtLocation());
				
//				System.out.println(new GlobalCMTID("200506021056A").getEvent().getHalfDuration());
//				System.exit(0);
			
				GlobalCMTSearch sea = new GlobalCMTSearch(LocalDate.of(2014, 5, 21), LocalDate.of(2019, 1, 1));

				sea.setLongitudeRange(-115, -75);
				sea.setLatitudeRange(9, 32);
				sea.setDepthRange(0, 750);
//				sea.setLongitudeRange(170, 220);
//				sea.setLatitudeRange(50, 70);
//				sea.setDepthRange(80, 750);
				sea.setMwRange(1., 7.5);
				
				sea.setLongitudeRange(165, 195);
				sea.setLatitudeRange(-35, -5);
				sea.setDepthRange(250, 750);
				sea.setMwRange(5.5, 6.7);
				Set<GlobalCMTID> id = sea.search();
				
				System.out.println(id.size());
				
//				Path outpath = Paths.get("/Users/Anselme/Dropbox/Kenji/UPPER_MANTLE/CA/NEW/STRUCTURE/USED/CA_EVENTS/SEISMICITY/CA_seismicity.txt");
				Path outpath = Paths.get("/Users/Anselme/Dropbox/Kenji/FWICPac/events.txt");
				
				Files.deleteIfExists(outpath);
				Files.createFile(outpath);
				
				for (GlobalCMTID i : id) {
					double distance_min = i.getEvent().getCmtLocation().getEpicentralDistance(new HorizontalPosition(58, -155))
							* 180 / Math.PI;
					double distance_max = i.getEvent().getCmtLocation().getEpicentralDistance(new HorizontalPosition(69, -155))
							* 180 / Math.PI;
					if (distance_min >= 65 && distance_max <= 100)
						Files.write(outpath, (i.toString() + " "
								+ i.getEvent().getCmtLocation().getLatitude() + " "
								+ i.getEvent().getCmtLocation().getLongitude() + " "
								+ (6371 - i.getEvent().getCmtLocation().getR()) + " "
								+ i.getEvent().getCmt().getMw() + " "
								+ distance_min + " " + distance_max
								+ "\n").getBytes(), StandardOpenOption.APPEND);
				}

//				if (id.size() > 1)
//					System.out.printf("%s: There is more than 1 event that corresponds to those parameters. Should check it%n", s);
//				else if (id.size() == 0)
//					System.out.printf("%s: No event corresponds to those parameters. Should check it%n", s);
//				boolean first = true;
//				for (GlobalCMTID i : id) {
//					try {
//							PrintWriter pw = new PrintWriter(
//									new BufferedWriter(
//											new FileWriter(
//													"/mnt/melonpan/anpan/inversion/Dpp/carib/CNSN/newName.txt", true)));
//							if(first) {
//								pw.println(i);
//								first = false;
//							}
//							pw.close();
//					} catch (IOException error) {
//						System.err.println("ファイルを作成できません");
//					}
//				}
//			}
//			br.close();
//		} catch (FileNotFoundException e) {
//			System.out.println("ファイルを開けません");
		} catch (IOException e) {
			System.out.println("データを読み出せません");
		}

	}

}
