package io.github.kensuke1984.kibrary.external.gnuplot;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * 
 * gnuplotの外観の設定
 * @author Kensuke Konishi
 * @version 0.0.1
 */
public class PlotConfiguration {

	private boolean xrange=false;
	private double xmin ;
	private double xmax ;
	

	private boolean yrange=false;
	private double ymin ;
	private double ymax;
	

	private boolean tics=false;
	private double xtics;
	private double ytics;


	/**
	 * 凡例をつけるか
	 */
	private boolean key = false;
	
	/**
	 * x軸のラベル
	 */
	protected String xlabel ="x";
	/**
	 * y軸のラベル
	 */
	protected String ylabel = "y";
	
	/**
	 * 図のタイトル
	 */
	protected String title = "title";
	
	/**
	 * sacマクロの出力
	 * @param out for output
	 */
	public void output(File out){
		try(PrintWriter pw =new PrintWriter(new BufferedWriter(new FileWriter(out)))){
			if(!key)
				pw.println("unset key");
			if(containsRatio)
				pw.println("set size ratio "+ratio);
			if (xrange)
				pw.println("set xrange ["+xmin+":"+xmax+"]");
			if (yrange)
				pw.println("set yrange ["+ymin+":"+ymax+"]");
			if (tics){
				pw.println("set xtics "+xtics);
				pw.println("set ytics "+ytics);
			}
			pw.println("set xlabel \""+ xlabel+"\"");
			pw.println("set ylabel \""+ ylabel+"\"");
			pw.println("set title \""+ title+"\"");
			pw.flush();
//			pw.close();
		}catch (Exception e) {
			System.out.println(e);
		}
		return ;
	}
	
	
	
	public double getXmin() {
		return xmin;
	}

	/**
	 * set size ratio ??? の　???
	 */
	private double ratio ;

	public double getRatio() {
		return ratio;
	}



	public void setRatio(double ratio) {
		this.ratio = ratio;
		containsRatio = true;
	}

	private boolean containsRatio;
	
	
	public void setXrange(double xmin, double xmax) {
		if (xmax<=xmin)
			throw new IllegalArgumentException("Input xmin xmax "+xmin+" "+xmax+" are invalid");
		this.xmin = xmin;
		this.xmax = xmax;
		xrange=true;
	}



	public double getXmax() {
		return xmax;
	}

	public double getYmin() {
		return ymin;
	}



	public void setYrange(double ymin, double ymax) {
		if (ymax<=ymin){
			System.out.println("Input ymin ymax "+ymin+" "+ymax+" are invalid");
			return;
		}
		this.ymin = ymin;
		this.ymax = ymax;
		yrange = true;
	}



	public double getYmax() {
		return ymax;
	}


	public String getXlabel() {
		return xlabel;
	}


	public void setXlabel(String xlabel) {
		this.xlabel = xlabel;
	}



	public String getYlabel() {
		return ylabel;
	}



	public void setYlabel(String ylabel) {
		this.ylabel = ylabel;
	}



	public String getTitle() {
		return title;
	}



	public void setTitle(String title) {
		this.title = title;
	}



}
