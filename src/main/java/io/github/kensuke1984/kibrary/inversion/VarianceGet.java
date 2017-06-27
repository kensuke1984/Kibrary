/**
 * 
 */
package io.github.kensuke1984.kibrary.inversion;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

/**
 * @version 0.0.1
 * @since 2017/04/27
 * @author Yuki
 *
 */
public class VarianceGet {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		if (args.length != 3) System.err.println("usage: full path to waveIDPath, waveformPath and outputPath.");
		//観測・理論波形のID情報ファイル
		Path waveIDPath = Paths.get(args[0]);
		Path waveformPath = Paths.get(args[1]);
		Path outPath = Paths.get(args[2]);
		BasicID[] ids;
		try {
			ids = BasicIDFile.readBasicIDandDataFile(waveIDPath, waveformPath);
			Dvector dVector = new Dvector(ids);
			double variance = dVector.getVariance();
			
			System.out.println(variance);
			
			BasicID[] obsIDs = dVector.getObsIDs();
//			BasicID[] synIDs = dVector.getSynIDs();
			RealVector[] obsVec = dVector.getObsVec();
//			RealVector[] synVec = dVector.getSynVec();
			RealVector[] delVec = dVector.getdVec();
			
			Path eachVariancePath = outPath.resolve("eachVariance.txt");
			
			try (PrintWriter pw1 = new PrintWriter(Files.newBufferedWriter(eachVariancePath))) {
				for (int i = 0; i < dVector.getNTimeWindow(); i++) {
					double variances = delVec[i].dotProduct(delVec[i]) / obsVec[i].dotProduct(obsVec[i]);
//					double dotProduct = obsVec[i].getMaxValue();
					pw1.println(i + " " + obsIDs[i].getStation() + " " + obsIDs[i].getStation().getNetwork() + " "
							+ obsIDs[i].getGlobalCMTID() + " " + variances);
				}
			}	
			
		} catch (IOException e) {
			// TODO 自動生成された catch ブロック
			e.printStackTrace();
		}
		
	}

}
