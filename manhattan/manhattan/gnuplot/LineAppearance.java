package manhattan.gnuplot;

/**
 * 
 * gnuplot　のプロットの設定
 * 
 * @version 0.0.2
 * @since 2013/9/25
 * 
 * @author Kensuke Konishi
 *
 */
public class LineAppearance {

	
	/**
	 * 線種　　－１：１０
	 */
	private int linetype = 1;
	 
	/**
	 * 線の色
	 */
	private GnuplotColorNames linecolor = GnuplotColorNames.black;
	
	
	/**
	 * 線の太さ
	 */
	private int linewidth = 1;
	
	/**
	 * データをどう使うか　using ????? の　???の分　
	 * （例　1:3, 1:($3+$1)）
	 */
	private String plotPart;
	
	/**
	 * plotPartの情報があるか
	 */
	private boolean plotPartSetting;
	
	
	
	/**
	 * using ????
	 * @param plotPart ????の部分　（例　1:3, 1:($3+$1)）
	 */
	public void setPlotPart(String plotPart) {
			
		this.plotPart = plotPart;
		plotPartSetting = true;
	}




	public int getLinetype() {
		return linetype;
	}




	public void setLinetype(int linetype) {
		this.linetype = linetype;
	}






	public GnuplotColorNames getLinecolor() {
		return linecolor;
	}






	public void setLinecolor(GnuplotColorNames linecolor) {
		this.linecolor = linecolor;
	}






	public int getLinewidth() {
		return linewidth;
	}

	
	private boolean withLine = true;





	public boolean isWithLine() {
		return withLine;
	}




	public void setWithLine(boolean withLine) {
		this.withLine = withLine;
	}




	public void setLinewidth(int linewidth) {
		this.linewidth = linewidth;
	}


	public String getString(){
		String str = " linetype "+ linetype+" linecolor rgbcolor \""+linecolor.nameColorName()+"\" linewidth "+linewidth;
		if(withLine)
			str = str+" w l";
		
		if (plotPartSetting)
			return "u "+plotPart+str;
		else
			return str;
	}
	



}
