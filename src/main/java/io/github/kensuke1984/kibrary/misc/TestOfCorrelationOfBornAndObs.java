package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.inversion.Dvector;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.inversion.ObservationEquation;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.inversion.UnknownParameter;
import io.github.kensuke1984.kibrary.inversion.UnknownParameterFile;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.timewindow.Timewindow;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

public class TestOfCorrelationOfBornAndObs {
	
	public static void main(String[] args) {
		Path outPath = Paths.get("/mnt/melonpan/suzuki/ALASKA/DIVIDE/newPREM/threeDPartial_5deg/Inv_noSC/CorCheck");
		File outDir = new File(outPath.toString());
		outDir.mkdirs();
		InverseMethodEnum CG = InverseMethodEnum.CONJUGATE_GRADIENT;
//		InverseMethodEnum SVD = InverseMethodEnum.SINGURAR_VALUE_DECOMPOSITION;
		TestOfCorrelationOfBornAndObs computeCor = new TestOfCorrelationOfBornAndObs();
		Path timewindowPath = Paths.get("/mnt/melonpan/suzuki/ALASKA/DIVIDE/newPREM/newSTW.dat");
		
		try {
			BasicID[] basicIDs = BasicIDFile.readBasicIDandDataFile(Paths.get("/mnt/melonpan/suzuki/ALASKA/DIVIDE/newPREM/newWaveformID.dat")
					, Paths.get("/mnt/melonpan/suzuki/ALASKA/DIVIDE/newPREM/newWaveform.dat"));
			InversionResult ir = new InversionResult(Paths.get("/mnt/melonpan/suzuki/ALASKA/DIVIDE/newPREM/threeDPartial_5deg/Inv_noSC"));
			List<UnknownParameter> unknownp = UnknownParameterFile.read(Paths.get("/mnt/melonpan/suzuki/ALASKA/DIVIDE/newPREM/threeDPartial_5deg/unknown.inf"));
//			Set<GlobalCMTID> obsIDList = ir.idSet();
//			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);

			File corFile = new File(outDir, "Correlation_"+CG.toString()+".inf");
			File parDir = new File(outDir, CG.toString());
			parDir.mkdir();
			
			for(int j=1;j<=unknownp.size();j++){
				File aveEachPar = new File(parDir, "Correlation_CG"+j+".inf");				
				ArrayList<Double> corEachBasis = new ArrayList<Double>();
				int bornOrder = j;
				System.out.println("Now at "+bornOrder+" of "+unknownp.size());
				
				
				for (int i=0; i<basicIDs.length;i++){
					GlobalCMTID obsID = basicIDs[i].getGlobalCMTID();
					String station = basicIDs[i].getStation().toString();
					double[] born = ir.bornOf(basicIDs[i], CG, j).getY();
					Stream.of(basicIDs).filter(id -> id.getGlobalCMTID().toString().equals(obsID)).filter(id -> id.getWaveformType().equals(WaveformType.SYN))
					.forEach(idSYN -> {
						BasicID idOBS = Stream.of(basicIDs).filter(id -> id.getGlobalCMTID().equals(idSYN.getGlobalCMTID()))
								.filter(id -> id.getStation().equals(idSYN.getStation()))
								.filter(id -> id.getWaveformType().equals(WaveformType.OBS))
								.findFirst()
								.get();
//					double[] obs = ir.observedOf(basicIDs[i]).getY();
					double[] obs = idOBS.getData();				
					RealVector bornWave = new ArrayRealVector(born);
//					RealVector synWave = new ArrayRealVector(idSYN.getData());
					RealVector obsWave = new ArrayRealVector(obs);
//					timewindows.stream().forEachOrdered(tw -> {				
//					});
//					RealVector bornWave = ir.bornOf(basicIDs[i], CG, j).getYVector();
//					RealVector obsWave = ir.observedOf(basicIDs[i]).getYVector();
					//ここで各震源観測点ペアのOBSとBORNのCorrelationを書き出す。
					System.out.println("Computing each waveform correlation.");
					computeCor.computeCorrelation(aveEachPar, obsWave, bornWave, obsID, station);
					double correlation = computeCor.computeCorrelation(obsWave, bornWave);
					corEachBasis.add(correlation);
					});
				
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
			}
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
