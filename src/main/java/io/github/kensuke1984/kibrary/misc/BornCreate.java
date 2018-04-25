/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;


import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;

/**
 * @version 0.0.1
 * @since 2017/01/20
 * @author Yuki
 *
 */
public class BornCreate {
	
	static InverseMethodEnum CG = InverseMethodEnum.CONJUGATE_GRADIENT;
//	static InverseMethodEnum SVD = InverseMethodEnum.SVD;
	
	public static void main(String[] args) {
		if (args.length<6)
			System.err.println("USAGE: output dir, Inversion method, timewindow path, Inversion result dir., min, max.");
		else if (args[0].equals("--help"))
			System.err.println("USAGE: output dir, Inversion method, timewindow path, Inversion result dir., min, max.");
		Path outPath = Paths.get(args[0]);
		File outDir = new File(outPath.toString());
		if (!outDir.exists()){
		outDir.mkdirs();
		}
		InverseMethodEnum ime = InverseMethodEnum.valueOf(args[1]);
//		BornCreate computeCor = new BornCreate();
		Path timewindowPath = Paths.get(args[2]);
		
		try {
			Path irpath = Paths.get(args[3]);
			InversionResult ir = new InversionResult(irpath);
			List<UnknownParameter> unknownp = UnknownParameterFile.read(irpath.resolve("unknown.inf"));
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);

			File corFile = new File(outDir, "Correlation_"+ime.toString()+".inf");
			File parDir = new File(outDir, ime.toString());
			parDir.mkdir();
			
			int min = Integer.parseInt(args[4]);
			int max = Integer.parseInt(args[5]);
			
			for(int j=min;j<=max;j++){
//				File aveEachPar = new File(parDir, "Correlation_"+ime+j+".inf");				
				ArrayList<Double> corEachBasis = new ArrayList<Double>();
				int bornOrder = j;
				System.out.println("Now at "+bornOrder+" of "+unknownp.size());
				ir.createBorn(ime, j);
//				/**
					timewindows.stream().forEachOrdered(tw -> {
//						GlobalCMTID obsID = tw.getGlobalCMTID();
//						String station = tw.getStation().toString();
						ir.getBasicIDList().stream().filter(id -> id.getGlobalCMTID().equals(tw.getGlobalCMTID()))
						.filter(id -> id.getStation().equals(tw.getStation()))
						.forEach(id -> {
							try{
							ir.createBorn(ime, bornOrder);
//						RealVector bornWave = ir.bornOf(id, ime, bornOrder).getYVector();
//						RealVector obsWave = ir.observedOf(id).getYVector();
						//ここで各震源観測点ペアのOBSとBORNのCorrelationを書き出す。
//						System.out.println("Computing each waveform correlation.");
//						computeCor.computeCorrelation(aveEachPar, obsWave, bornWave, obsID, station);
//						double correlation = computeCor.computeCorrelation(obsWave, bornWave);
//						corEachBasis.add(correlation);	
							} catch (IOException e) {
								e.printStackTrace();
							}
						});
//						}
					});
//				**/
				try(PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(corFile, true)))){
					int counter = 0;
					double ave = 0;
					System.out.println("Computing average correlation of "+j+"th basis vector.");
					for(double cor : corEachBasis){
						ave = ave + cor;
						counter = counter + 1;
					}
					ave = ave / counter ;
					pw.println(ave +" "+ j);
				} catch (IOException e){
					e.printStackTrace();
				}
				
		}	//ここまで各basis vector毎のCorrelationの書き出し		
		} catch (IOException e) {
			e.printStackTrace();
		} catch (DimensionMismatchException e) {
			e.printStackTrace();
		}	
	} 
	
	public double computeCorrelation (RealVector obsWave, RealVector bornWave) {		
		double obs2 = obsWave.dotProduct(obsWave);
		double born2 = bornWave.dotProduct(bornWave);
		double cor = obsWave.dotProduct(bornWave);
		cor /= Math.sqrt(obs2 * born2);
		cor = Math.round(cor * 100) / 100.0;
		return cor;
	}
	
	public void computeCorrelation (File outfile, RealVector obsWave, RealVector bornWave, int parNum) {		
		try(PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outfile, true)))){
		double obs2 = obsWave.dotProduct(obsWave);
		double born2 = bornWave.dotProduct(bornWave);
		double cor = obsWave.dotProduct(bornWave);
		cor /= Math.sqrt(obs2 * born2);
		cor = Math.round(cor * 100) / 100.0;
		pw.println(cor);
	}catch (Exception e){
		System.out.println(e);
	}
	}
	
	public void computeCorrelation (File outfile, RealVector obsWave, RealVector bornWave, GlobalCMTID eventID, String stationName) {		
		try(PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(outfile, true)))){
		double obs2 = obsWave.dotProduct(obsWave);
		double born2 = bornWave.dotProduct(bornWave);
		double cor = obsWave.dotProduct(bornWave);
		cor /= Math.sqrt(obs2 * born2);
		cor = Math.round(cor * 100) / 100.0;
		pw.println(cor+" "+eventID+" "+stationName);
		pw.close();
	}catch (Exception e){
		System.out.println(e);
	}	
	}

}
