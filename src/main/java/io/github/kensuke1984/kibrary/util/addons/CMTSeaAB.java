package io.github.kensuke1984.kibrary.util.addons;

import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

import java.io.BufferedReader;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class CMTSeaAB {

	public static void main(String[] args) {
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		
		Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"031704A","062003D","200503211243A","200506021056A","200511171926A","200608250044A"
				,"200609220232A","200611130126A","200705251747A","200707120523A","200707211327A","200707211534A","200711180540A","200808262100A","200809031125A"
				,"200810122055A","200907120612A","200909050358A","200909301903A","200911141944A","201001280804A","201005241618A","201009130715A","201101010956A"
				,"201104170158A","201106080306A","201109021347A","201111221848A","201203050746A","201205280507A","201206020752A","201208020938A","201302221201A"
				,"201409241116A","201502021049A","201502111857A","201509281528A","201511260545A"})
				.map(GlobalCMTID::new)
				.collect(Collectors.toSet()); // D" Carib P and S joint inversion
		
		try {
			// フィイルを開く(BufferedReaderオブジェクトを作る)
//			BufferedReader br = new BufferedReader(new FileReader("/Users/Anselme/Dropbox/Kenji/GlobalCMTIDs_UMstudy_Aleutians.txt"));
			PrintWriter pw = new PrintWriter(
					new BufferedWriter(
							new FileWriter("/Users/Anselme/Dropbox/Kenji/events_carib_PS.txt")));
			
			// 行単位でデータを読みだす
			String s;
			double meanDepth = 0;
//			while ((s = br.readLine()) != null) {
			for (GlobalCMTID id : wellDefinedEvent) {
//				GlobalCMTID id = new GlobalCMTID(s);
//				double distance1 = id.getEvent().getCmtLocation().getEpicentralDistance(new HorizontalPosition(30, -100))
//						* 180 / Math.PI;
//				double distance2 = id.getEvent().getCmtLocation().getEpicentralDistance(new HorizontalPosition(50, -100))
//						* 180 / Math.PI;
				double distance1 = id.getEvent().getCmtLocation().getEpicentralDistance(new HorizontalPosition(55, -130))
						* 180 / Math.PI;
				double distance2 = id.getEvent().getCmtLocation().getEpicentralDistance(new HorizontalPosition(35, -75))
						* 180 / Math.PI;
				pw.printf("%15s   %.1f   %.1f  %.1f %.1f %.1f %.1f%n"
						, id.toString()
//						, id.getEvent().getCMTTime().format(formatter)
						, id.getEvent().getCmtLocation().getLongitude()
						, id.getEvent().getCmtLocation().getLatitude()
						, Earth.EARTH_RADIUS - id.getEvent().getCmtLocation().getR()
//						, distance
//						, id.getEvent().getCMTTime().format(formatter)
						, id.getEvent().getCmt().getMw()
						, distance1, distance2);
				
				meanDepth += Earth.EARTH_RADIUS - id.getEvent().getCmtLocation().getR();
			}
			pw.close();
			meanDepth /= wellDefinedEvent.size();
			System.out.println(meanDepth);
		} catch (FileNotFoundException error) {
			System.out.println("ファイルを開けません");
		} catch (IOException error) {
			System.out.println("データを読み出せません");
		}
	}

}
