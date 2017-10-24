/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;

/**
 * @version 0.0.1
 * @since 2016/02/17
 * @author Yuki
 *
 */
public class VarianceGet {

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		
		//観測・理論波形のID情報ファイル
		Path waveIDPath = Paths.get(args[0]);
		System.out.println(waveIDPath);
		//観測・理論波形のデータファイル
		Path waveformPath = Paths.get(args[1]);	
		BasicID[] ids = BasicIDFile.readBasicIDandDataFile(waveIDPath, waveformPath);
		Dvector dVector = new Dvector(ids);
		double variance = dVector.getVariance();
//		dVector.
		System.out.println(variance);
		
		BasicID[] obsIDs = dVector.getObsIDs();
		BasicID[] synIDs = dVector.getSynIDs();
		RealVector[] obsVec = dVector.getObsVec();
		RealVector[] synVec = dVector.getSynVec();
		RealVector[] delVec = dVector.getdVec();
		
		Path outPath = Paths.get(args[2]);
		// each trace variance
		Path eachVariancePath = outPath.resolve("variance.txt");
		try (PrintWriter pw1 = new PrintWriter(Files.newBufferedWriter(eachVariancePath))) {
			for (int i = 0; i < dVector.getNTimeWindow(); i++) {
				double variances = delVec[i].dotProduct(delVec[i]) / obsVec[i].dotProduct(obsVec[i]);
				double dotProduct = obsVec[i].getMaxValue();
				pw1.println(i + " " + obsIDs[i].getStation() + " " + obsIDs[i].getStation().getNetwork() + " "
						+ obsIDs[i].getGlobalCMTID() + " " + variances);
			}
		}
		
	}

}
