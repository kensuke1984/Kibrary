package io.github.kensuke1984.kibrary.util;

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

class CMTSeaAB {

	public static void main(String[] args) {
//		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss");
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy/MM/dd");
		
		try {
			// フィイルを開く(BufferedReaderオブジェクトを作る)
			BufferedReader br = new BufferedReader(new FileReader("/Users/Anselme/Dropbox/Kenji/GlobalCMTIDs_UMstudy_Aleutians.txt"));
			PrintWriter pw = new PrintWriter(
					new BufferedWriter(
							new FileWriter("/Users/Anselme/Dropbox/Kenji/GlobalCMTIDs_UMstudy_Aleutians_info.txt")));
			
			// 行単位でデータを読みだす
			String s;
			while ((s = br.readLine()) != null) {
				GlobalCMTID id = new GlobalCMTID(s);
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
			}
			pw.close();
		} catch (FileNotFoundException error) {
			System.out.println("ファイルを開けません");
		} catch (IOException error) {
			System.out.println("データを読み出せません");
		}
	}

}
