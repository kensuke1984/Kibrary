/**
 * 1次元のインバージョンの結果得られたδμからV+δV,ratioを計算する。
 * GNUplotで速度構造をplotするためのインプットファイルを作る
 */
package io.github.kensuke1984.kibrary.postProcess;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.spc.SpcFileName;
import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;

/**
 * @version 0.0.1
 * @since 2015/08/05
 * @author Yuki
 *
 */
public class OneDSvelocityStructure {	
	private static PolynomialStructure prem = PolynomialStructure.PREM;
	private static PolynomialStructure ak135 = PolynomialStructure.AK135;
	private static double deltaR = 20;
	private static String invMethod = "CG";
	
	public static void main(String[] args){
		Path workdirPath = Paths.get(args[0]);
		Path cgPath = Paths.get(workdirPath.resolve("CG").toString());
		Path graphPath = workdirPath.resolve("graph"+invMethod);
		Files.createDirectories(graphPath);
		Set<AnswerFileName> answerFiles = collectAnswerFileName(cgPath);
		answerFiles.stream().forEach(ans -> {
			Path answerPath = cgPath.resolve(ans.toString());
			double [] deltaMu = readAnswer();
			double[] r  =  new double[deltaMu.length];
		});
		
		try{	  
			String filepath;
			int count=1;
			while((filepath = br.readLine()) !=null){
				Path answerPath = Paths.get(filepath);
//				Path parfilePath = Paths.get(args[0]);
				try{
					String output = Integer.toString(count);
					PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter(workdirPath.toString()+"/CG" + output + "ans.inf")));//("/Users/Yuki/Desktop/output/" + output + "_S.txt")));
					for(int i=0;i<deltaMu.length;i++){
//					 System.err.println(i);
						r[i] = 3490+i*deltaR;
						double radius = r[i]; 							 // r
						double mu = prem.computeMu(radius); 				 //μ(r)
						double rho = prem.getRhoAt(radius);				 //ρ(r) 
						double muPlusDeltaMu = mu + deltaMu[i]; 		 //μ+ δμ
						double vsDash = Math.sqrt(muPlusDeltaMu/rho);	 //Vs'
						double vs = prem.computeVs(radius); 				 // initial value
						double ratio = vsDash/vs*100-100;				 // perturbation(%)
						pw.println((6371-radius)+" "+vsDash+" "+vs+" "+ratio+" "+(radius-3480));			
					}
					pw.close();
				}catch(IOException error){
					System.out.println("ファイルを作成できません");
				}
				count = count+1;
				}
				br.close();
		}catch(FileNotFoundException error){
			System.out.println("ファイルを開けません");
		}catch(IOException error){
			System.out.println("ファイルを読み出せません");
		}
	}

	/**
	 * ?.datの解のファイルを読み込んで解(deltaMu)の配列を作る	
	 * @param answerPath
	 * @return
	 */
	private static double[] readAnswer(Path answerPath){
		try{
			return Files.readAllLines(answerPath)
					.stream().mapToDouble(Double::parseDouble).toArray();
		}catch(Exception e){
			throw new RuntimeException("answer file has problem");
		}
	}
	
	/**
	 * &delta&muの配列をspline関数で補間
	 */
	private static double[] csplineIntrep(double[] deltaMu){
		SplineInterpolator sip = new SplineInterpolator();
		
		return null;
	}
	
	/**
	 * @param path
	 *            {@link Path} to look for {@link SpcFileName} in
	 * @return set of {@link SpcFileName} in the dir
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	private static Set<AnswerFileName> collectAnswerFileName(Path cgpath) throws IOException {
		try (Stream<Path> stream = Files.list(cgpath)) {
			return stream.filter(AnswerFileName::isAnswerFileName).map(AnswerFileName::new).collect(Collectors.toSet());
		}
	}
}

class AnswerFileName extends File {
	
	private String ansf;
	
	public AnswerFileName(URI uri) {
		super(uri);
	}
	
	/**
	 * @param pathname
	 */
	public AnswerFileName(String pathname) {
		this.ansf = pathname;
	}
	
	/**
	 * @param path
	 *            {@link Path} of a spectrum file
	 */
	public AnswerFileName(Path path) {
		this(path.toString());
	}
	
	/**
	 * @param path
	 *            for check
	 * @return if the filePath is a valid {@link SpcFileName}
	 */
	public static boolean isAnswerFileName(Path path) {
		return isAnswerFileName(path.toFile());
	}

	/**
	 * 
	 * @param file
	 *            {@link File} for check
	 * @return if the file is a valid {@link SpcFileName}
	 */
	public static boolean isAnswerFileName(File file) {
		String name = file.getName();
		String[] parts = name.split("\\.");
		if (!file.getName().endsWith(".txt") && !file.getName().endsWith(".txt"))
			return false;
		
		if (!file.getName().startsWith("CG") && !file.getName().startsWith("SVD"))
			return false;

		if (parts.length != 3 && parts.length != 7)
			return false;

		// answer vector name can't has less 3 letters.
		if (3 > parts[0].length())
			return false;

		return true;
	}
	
	@Override
	public String toString(){
		return ansf;
	}
	
}
