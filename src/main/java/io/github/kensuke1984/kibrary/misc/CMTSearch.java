package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Set;

import org.apache.commons.math3.util.FastMath;

import io.github.kensuke1984.kibrary.util.Earth;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

class CMTSearch {

	public static void main(String[] args) {
		try{
			//フィイルを開く(BufferedReaderオブジェクトを作る)
			String homedir = args[0];
			BufferedReader br = new BufferedReader(new FileReader(homedir+args[1]));
			Path outPath = Paths.get(homedir+args[2]);
			Files.deleteIfExists(outPath);
			Files.createFile(outPath);
			//行単位でデータを読みだす。 Header 1 lines を無視。
			int lines = 1;
			String s;
			while((s = br.readLine()) !=null){
				if (lines < 2) {
					System.out.println("File reading");
				}
				else {
					String[] parts = s.split(",");
					double stLat = Double.parseDouble(parts[1]);
					double stLon = Double.parseDouble(parts[2]);
				  System.out.println(stLat+" "+stLon);
				  GlobalCMTSearch sea = new GlobalCMTSearch(LocalDate.of(2017, 10, 1), LocalDate.of(2019, 1, 31));	//年月日の指定

				  String title = "TONGA FIJI";
				  sea.setLatitudeRange(-40, -10);	//緯度範囲の指定
				  sea.setLongitudeRange(170, 190);	//経度範囲の指定
				  sea.setDepthRange(150, 700);	//深さ範囲の指定
				  sea.setMwRange(5.5, 7.3);

				  Set<GlobalCMTID> id = sea.search();	//条件に合致するgcmtIDをidに代入


				  for (GlobalCMTID i : id){
//					  System.out.println(i);
					  double edRad = i.getEvent().getCmtLocation().getEpicentralDistance(new HorizontalPosition(stLat, stLon));
					  double gcarc = FastMath.toDegrees(edRad);
//					  System.out.println(FastMath.toDegrees(gcarc));
				  if (gcarc < 120 && gcarc > 50){
//					if(id.size() == 1) {	//条件に合致するgcmtIDが一つであるかの確認
						double lat = i.getEvent().getCmtLocation().getLatitude();
						double lon = i.getEvent().getCmtLocation().getLongitude();
						double depth = Earth.EARTH_RADIUS - i.getEvent().getCmtLocation().getR();
						LocalDate ymd = LocalDate.of(i.getEvent().getCMTTime().getYear(),
										i.getEvent().getCMTTime().getMonth(), i.getEvent().getCMTTime().getDayOfMonth());
						LocalTime hms = LocalTime.of(i.getEvent().getCMTTime().getHour(),
										i.getEvent().getCMTTime().getMinute(), i.getEvent().getCMTTime().getSecond());
						double edDeg = FastMath.toDegrees(i.getEvent().getCmtLocation()
										.getEpicentralDistance(new HorizontalPosition(20, 80)));
						double mw = i.getEvent().getCmt().getMw();
//						System.out.printf("%s %.2f %.2f %.2f %.2f %s %s %.1f\n", i, lat, lon, depth, gcarc, ymd, hms, mw);
//					}
//				  /**
						try{
							Files.write(outPath,
									String.format("%s %s/%s/%s %s:%s:%s %.2f %.2f %.2f %.1f %s\n",
											i,
											ymd.getYear(), ymd.getMonthValue(), ymd.getDayOfMonth(),
											hms.getHour(), hms.getMinute(), hms.getSecond(),
											lat, lon, depth, mw, title).getBytes(),
									StandardOpenOption.APPEND);
						}catch (IOException error){
							System.err.println("ファイルを作成できません");
						}
				  }
//					*/
//				  }
				  }
				}
				lines ++;
			}
			  br.close();	//ファイルを閉じる

			}
				 catch (FileNotFoundException erroe) {
					System.out.println("ファイルを開けません");
				} catch (IOException error) {
					System.out.println("データを読み出せません");
				}
		}

	}


