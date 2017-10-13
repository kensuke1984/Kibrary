/**
 * 1次元のインバージョンの結果得られたδμからV+δV,ratioを計算する。
 * GNUplotで速度構造をplotするためのインプットファイルを作る
 */
package io.github.kensuke1984.kibrary.inversion;

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
import java.util.ArrayList;
import java.util.Arrays;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.SimpsonIntegrator;
import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialFunction;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

/**
 * @version 0.0.1
 * @since 2015/08/05
 * @author Yuki
 *
 */
public class OneDSvelocityStructure {
	private static PolynomialStructure prem = PolynomialStructure.PREM;
	public static void main(String[] args){
		try{
			//解のファイルをフルパスでリストしたファイル。answerListMaker.shで作る。	
		BufferedReader br = new BufferedReader(new FileReader("/Users/Yuki/Dropbox/SEASIA/syntheticCheck/CBT1D_DSM/graph/answerList2.txt"));  
		String filepath;
		int count=1;
//		Path answerListFile = Paths.get(args[0]);  //解のファイルをフルパスでリストしたファイル
		while((filepath = br.readLine()) !=null){
//			    System.out.println(filepath);
//			    if (filepath.length() !=1)
//				    throw new RuntimeException("SacFileName");
			Path answerPath = Paths.get(filepath);
//			Path parfilePath = Paths.get(args[0]);
			ArrayList<Double> intV = new ArrayList<Double>();
			double [] deltaMu = readAnswer(answerPath);
//			System.err.println(deltaMu.length);
			double[] r  =  new double[500];
			double[] test = new double[100];
			double[] rref = new double[100];
			try{
				String output = Integer.toString(count);
				PrintWriter pw = new PrintWriter(new BufferedWriter(new FileWriter("/Users/Yuki/Dropbox/SEASIA/syntheticCheck/CBT1D_DSM/graph/CG" + output + "ans.inf")));//("/Users/Yuki/Desktop/output/" + output + "_S.txt")));
			 for(int i=0;i<deltaMu.length;i++){
//				 System.err.println(i);
				r[i] = 3490+i*20;
				double radius = r[i]; 							 // r
				double mu = prem.computeMu(radius); 				 //μ(r)
				double rho = prem.getRhoAt(radius);				 //ρ(r) 
				double muPlusDeltaMu = mu + deltaMu[i]; 		 //μ+ δμ
				double vsDash = Math.sqrt(muPlusDeltaMu/rho);	 //Vs'
				double vs = prem.computeVs(radius); 				 // initial value
				double ratio = vsDash/vs*100-100;				 // perturbation(%)
				pw.println((6371-radius)+" "+vsDash+" "+vs+" "+ratio+" "+(radius-3480));
//				pw.println((6371-radius)+" "+vs+" "+vs+" "+(radius-3480));
//				 System.out.println((6371-radius)+" "+vsDash+" "+vs+" "+ratio);				
			  }
			 	for (int k=0;k<100;k++){
			 		rref[k] = 3480+k*5;
			 	}
//			 	SplineInterpolator si = new SplineInterpolator();
//				PolynomialSplineFunction v0 = si.interpolate(r, deltaMu);
//				final UnivariateFunction v1 = si.interpolate(rref, deltaMu);
//				SimpsonIntegrator simpInt = new SimpsonIntegrator();
//				simpInt.integrate(100, v0, 3480, 3530);
//				System.out.println("Integration" + simpInt);
//				Arrays.stream(r).forEach(i ->{ 
//					for (int j=0;j<r.length;i++){
//						double rdash = i+j;
//						System.out.println(rdash+" "+v0.value(rdash));
//					}
//				});
			 
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
}
