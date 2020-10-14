package io.github.kensuke1984.kibrary.inversion.sourceTimeFunction;

import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

class DrawDisplacement_rmNoisyWaveform{
	public static void main(String args[]){
		String eventListPath = "/mnt/doremi/anpan/inversion/upper_mantle/CA/NEW/SOURCE/USED/CA_EVENTS/eventList.txt";
		int j = 0;
    	String countoutpath = "/mnt/doremi/anpan/inversion/upper_mantle/CA/NEW/SOURCE/USED/CA_EVENTS/sourcetimefunction/Displacement/Result_rmNW2_sn8_2.txt";
    	try{
			File countfile = new File(countoutpath);
			try{
				countfile.createNewFile();
			} catch(IOException e){
				System.out.println(e);
			}			
			PrintWriter pwcountfile = new PrintWriter(new BufferedWriter(new FileWriter(countfile)));
		try{
     	  File file = new File(eventListPath);
      	  BufferedReader br = new BufferedReader(new FileReader(file));             	  
     	  String str = br.readLine();
     	  while(str != null){
    	    String[] gcmtlist = str.split("\\s+");
	   	    GlobalCMTID eventID = new GlobalCMTID(gcmtlist[0]);
	   	    System.out.println( "!!!!!!!!!!!!!!" + gcmtlist[0] + " " + j + "!!!!!!!!!!!!!!" );
	   	    makeDisplacement(eventID, pwcountfile);
		   	str = br.readLine();
	    	j = j+1;
     	  }       	          	 
     	  br.close();
		}catch(FileNotFoundException e){
	         System.out.println(e);
	   	} catch (IOException e) {
		e.printStackTrace();
	   	}
		pwcountfile.close();
    	}catch (IOException e) {
    		// TODO Auto-generated catch block
    		e.printStackTrace();
    	}
	}
	
	public static void makeDisplacement(GlobalCMTID eventID, PrintWriter pwcountfile){
    	SACComponent component = SACComponent.Z;
    	Path timewindowPath = Paths.get("/mnt/doremi/anpan/inversion/upper_mantle/CA/NEW/SOURCE/USED/CA_EVENTS/syntheticAK135_np2048/filtered_unconvolved_2-100s/selectedTimewindow_P.dat");
    	Path CorrectionPath = Paths.get("/mnt/doremi/anpan/inversion/upper_mantle/CA/NEW/SOURCE/USED/CA_EVENTS/syntheticAK135_np2048/filtered_unconvolved_2-100s/fujiStaticCorrection.dat");
    	String alloutpath = "/mnt/doremi/anpan/inversion/upper_mantle/CA/NEW/SOURCE/USED/CA_EVENTS/syntheticAK135_np2048/Displacement/all_" + eventID + "_" + component + "_rmNW2_sn8_arv.gmt";
    	
    	Set<StaticCorrection> corrections = null;
    	try {
    		corrections = StaticCorrectionFile.read(CorrectionPath).stream()
    			.filter(corr -> corr.getGlobalCMTID().equals(eventID)).collect(Collectors.toSet());
    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    	List<double[]> AllDisplacement = new ArrayList<>();
    	double[] halfDurations = new double[2];
    	try{
			File gmtfile = new File(alloutpath);
			try{
				gmtfile.createNewFile();
			} catch(IOException e){
				System.out.println(e);
			}			
			PrintWriter pwfile = new PrintWriter(new BufferedWriter(new FileWriter(gmtfile)));
			pwfile.println("#!/bin/sh");	
			// あとで変更
						pwfile.println("outputps=" + eventID + ".ps");
						pwfile.println("gmt set PS_MEDIA 23.33cx17.5c");
						pwfile.println("gmt set FONT 10p,Helvetica,black");
						pwfile.println("gmt set PS_CHAR_ENCODING ISOLatin1");
						pwfile.println("gmt pstext -R0/30/-30/30 -JX20.0c/14c -K -P -Y2c > $outputps <<END");
						pwfile.println("END");
			
						
			double windowLength = 30.;
			try {
				Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
				for(TimewindowInformation timewindow : timewindows){
					if(timewindow.getGlobalCMTID().equals(eventID) && timewindow.getComponent().equals(component)){
        			if(timewindow.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(timewindow.getStation().getPosition())*180./Math.PI > 30. &&
        					timewindow.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(timewindow.getStation().getPosition())*180./Math.PI < 90.){
						TimewindowInformation correctionTw = ReadStaticCorrection.getcorrection(eventID, timewindow, corrections);
						Station station = timewindow.getStation();
						double sttime = correctionTw.getStartTime() - 5.;
						double entime = sttime + windowLength;
						String path = "/mnt/doremi/anpan/inversion/upper_mantle/CA/NEW/SOURCE/USED/CA_EVENTS/syntheticAK135_np2048/filtered_unconvolved_2-100s/" + eventID + "/" + station.getName() + "." + eventID + "." + component;
						String synpath = "/mnt/doremi/anpan/inversion/upper_mantle/CA/NEW/SOURCE/USED/CA_EVENTS/syntheticAK135_np2048/filtered_unconvolved_2-100s/" + eventID + "/" + station.getName() + "." + eventID + "." + component + "s";
						double[] tmpDisp = getDisplacementAmplitude2Average(eventID, path, synpath, pwfile, sttime, entime);
						System.out.println("FIRST" + tmpDisp[0]);
					if(tmpDisp[0] < 999999.){
	        			System.out.println("GET" + tmpDisp[0]);
	        			getDisplacement2all(eventID, path, synpath, pwfile, sttime, entime);
	        			AllDisplacement.add(tmpDisp);
        			}
        		}
				}
        	} 
			
			}catch(IOException e){
				System.out.println(e);
			}
			if(!AllDisplacement.isEmpty()){
// AVERAGEの計算！！
			double dt = 0.050;
			int n = (int) (windowLength / dt);
//			double[] Average = new double[AllDisplacement.get(0).length];
			double[] Average = new double[n];
			for(int i =0; i<Average.length; i++){
				Average[i] =0.;
				for(int j=0; j<AllDisplacement.size();j++){
					Average[i] = Average[i] + AllDisplacement.get(j)[i];
				}
				double a = AllDisplacement.size();
				Average[i] = Average[i]/a;
			}
			halfDurations = aboutAverage(Average, pwfile);
// 0線を引く
			pwfile.println("gmt pswiggle -J -R -Z1 -BSwne"
					+ "+t\"Vertical\" "
					+ "-Bx10f1 -By10f2 "
					+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
					+ String.format("-Wthinner," + "black" )
					+ " -P -K -O >> $outputps << END");
			pwfile.println(String.format("%.2f %.2f %.7f", -10., 0., 0.));
			pwfile.println(String.format("%.2f %.2f %.7f", 30., 0., 0.));
			pwfile.println("END");
			
// AVERAGEのPLOT
				pwfile.println("gmt pswiggle -J -R -Z1 -BSwne"
						+ "+t\"Vertical\" "
						+ "-Bx10f1 -By10f2 "
						+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
						+ String.format("-Wthick," + "black" )
						+ " -P -K -O >> $outputps << END");
			for (int j=0; j < Average.length; j++) {
				pwfile.println(String.format("%.2f %.2f %.7f", j * dt, 0., Average[j]));
			}
			pwfile.println("END");
			pwfile.println("gmt pstext -R -J -O -K -P -F+jLB+f8p,Helvetica -N >> $outputps <<END");
        	pwfile.println("14 27 " + eventID);
        	pwfile.println("14 25 Mw of GCMT is " + eventID.getEvent().getCmt().getMw());
        	pwfile.println("14 23 Depth of GCMT is " + String.format("%.1f", (6371-eventID.getEvent().getCmtLocation().getR())) + " km");
        	pwfile.println("14 21 Half duration of GCMT is " + eventID.getEvent().getHalfDuration() + " s");
        	pwfile.println("END");
			
			pwfile.println("gmt pstext -R -J -O -P >> $outputps <<END\nEND");
			pwfile.println("gmt ps2raster -Tf $outputps");
			pwfile.close();
    	}
		}catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
    	pwcountfile.println(eventID + " " + String.format("%.2f", halfDurations[0]) + " " + String.format("%.2f", halfDurations[1]) + " " + AllDisplacement.size()
    			+ " " + eventID.getEvent().getCmt().getMw() + " " + (halfDurations[0] + halfDurations[1]));
	}
	
	public static double[] aboutAverage(double[] Average, PrintWriter pwfile){
		double dt = 0.050;
		double max = 0.;
		double maxtime = 0.;
		int maxnum = 0;
		for(int i=0; i<Average.length; i++){
			if(max < Average[i]){
				max = Average[i];
				maxtime = i*dt;
				maxnum = i;
			}
		}
		System.out.println("maxtime = " + maxtime );
		System.out.println("maxAmp = " + max );
		pwfile.println("gmt pswiggle -J -R -Z1 -BSwne"
				+ "+t\"Vertical\" "
				+ "-Bx10f1 -By10f2 "
				+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
				+ String.format("-Wthinner," + "green" )
				+ " -P -K -O >> $outputps << END");
		pwfile.println(String.format("%.2f %.2f %.7f", maxtime, 30., 0.));
		pwfile.println(String.format("%.2f %.2f %.7f", maxtime, -30., 0.));
		pwfile.println("END");
		pwfile.println("gmt pstext -R -J -O -K -P -F+jLB+f8p,Helvetica -N >> $outputps <<END");
    	pwfile.println((String.format("%.2f %.1f %.2f", maxtime, -20., maxtime)));
    	pwfile.println("END");
    	List<Double> fronts = new ArrayList<>();
    	List<Double> fronttimes = new ArrayList<>();
    	List<Integer> frontNums = new ArrayList<>();
    	double front = 0.;
		double fronttime = 0.;
		int frontNum = 0;
		double back = 0.;
		double backtime = 0.;
		int backNum = 0;
		for(int i=0; i<Average.length;i++){
//			if(i>0 && ((Average[i-1] < max/3. && Average[i] > max/3.) 
//					|| (Average[i-1] > max/3 && Average[i] < max/3.))){
			if(i>0 && ((Average[i-1] < max/2. && Average[i] > max/2.) 
					|| (Average[i-1] > max/2. && Average[i] < max/2.))){
				pwfile.println("gmt pswiggle -J -R -Z1 -BSwne"
						+ "+t\"Vertical\" "
						+ "-Bx10f1 -By10f2 "
						+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
						+ String.format("-Wthinner," + "green" )
						+ " -P -K -O >> $outputps << END");
				pwfile.println(String.format("%.2f %.2f %.7f", i*dt , 30., 0.));
				pwfile.println(String.format("%.2f %.2f %.7f", i*dt , -30., 0.));
				pwfile.println("END");
				pwfile.println("gmt pstext -R -J -O -K -P -F+jLB+f8p,Helvetica -N >> $outputps <<END");
	        	pwfile.println((String.format("%.2f %.1f %.2f", i * dt, -20., i*dt)));
	        	pwfile.println("END");
			}
		}
		for(int i=maxnum-1; i>0.;i--){
//			if(i>0 && (Average[i-1] < max/3. && Average[i] >= max/3.)){
			if(i>0 && (Average[i-1] < max/2. && Average[i] >= max/2.)){
				front = Average[i];
				fronttime = i*dt;
				frontNum = i;
			}
			if(Average[i] < 0.)break;
		}
		for(int i=maxnum; i<Average.length;i++){
//			if(i>0 && (Average[i-1] >= max/3. && Average[i] < max/3.)){
			if(i>0 && (Average[i-1] >= max/2. && Average[i] < max/2.)){
				back = Average[i];
				backtime = i*dt;
				backNum = i;
			}
			if(Average[i] < 0.)break;
		}
//	始点
		double a1 = 0.;
		double a2 = 0.;
		double a3 = 0.;
		double a4 = 0.;
		for(int j=frontNum; j<maxnum; j++){
			a1 = a1 + (j*dt) * Average[j];
			a2 = a2 + (j*dt);
			a3 = a3 + Average[j];
			a4 = a4 + (j*dt) * (j*dt);
		}
		double a = ((maxnum-frontNum)*a1 - a2*a3)/((maxnum-frontNum)*a4 - a2*a2);
		double b = (a4*a3 - a1*a2)/((maxnum-frontNum)*a4 - a2*a2);
		System.out.println("a is " + a + " b is " + b);
		double frontdt = -1.*b/a;
		System.out.println("Front time is " + frontdt);
		pwfile.println("gmt pswiggle -J -R -Z1 -BSwne"
				+ "+t\"Vertical\" "
				+ "-Bx10f1 -By10f2 "
				+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
				+ String.format("-Wthinner," + "blue" )
				+ " -P -K -O >> $outputps << END");
		pwfile.println(String.format("%.2f %.2f %.7f", frontdt , 30., 0.));
		pwfile.println(String.format("%.2f %.2f %.7f", frontdt , -30., 0.));
		pwfile.println("END");
		pwfile.println("gmt pstext -R -J -O -K -P -F+jLB+f8p,Helvetica -N >> $outputps <<END");
    	pwfile.println((String.format("%.2f %.1f %.2f", frontdt, -10., frontdt)));
    	pwfile.println("END");
//	終点
		double b1 = 0.;
		double b2 = 0.;
		double b3 = 0.;
		double b4 = 0.;
		for(int j=maxnum; j<backNum; j++){
			b1 = b1 + (j*dt) * Average[j];
			b2 = b2 + (j*dt);
			b3 = b3 + Average[j];
			b4 = b4 + (j*dt) * (j*dt);
		}
		double aa = ((backNum-maxnum)*b1 - b2*b3)/((backNum-maxnum)*b4 - b2*b2);
		double bb = (b4*b3 - b1*b2)/((backNum-maxnum)*b4 - b2*b2);
		double backdt = -1.*bb/aa;
		System.out.println("Back time is " + backdt);
		pwfile.println("gmt pswiggle -J -R -Z1 -BSwne"
				+ "+t\"Vertical\" "
				+ "-Bx10f1 -By10f2 "
				+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
				+ String.format("-Wthinner," + "blue" )
				+ " -P -K -O >> $outputps << END");
		pwfile.println(String.format("%.2f %.2f %.7f", backdt , 30., 0.));
		pwfile.println(String.format("%.2f %.2f %.7f", backdt , -30., 0.));
		pwfile.println("END");
		pwfile.println("gmt pstext -R -J -O -K -P -F+jLB+f8p,Helvetica -N >> $outputps <<END");
    	pwfile.println((String.format("%.2f %.1f %.2f", backdt, -10., backdt)));
    	pwfile.println("END");
//	交点
		double crossdt = -1.*(bb-b)/(aa-a);
//		double crossh = a*crossdt + b;
		double crossh = aa*crossdt + bb;
		System.out.println("Cross time is " + crossdt);
		pwfile.println("gmt pswiggle -J -R -Z1 -BSwne"
				+ "+t\"Vertical\" "
				+ "-Bx10f1 -By10f2 "
				+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
				+ String.format("-Wthinner," + "blue" )
				+ " -P -K -O >> $outputps << END");
		pwfile.println(String.format("%.2f %.2f %.7f", crossdt , 30., 0.));
		pwfile.println(String.format("%.2f %.2f %.7f", crossdt , -30., 0.));
		pwfile.println("END");
		pwfile.println("gmt pstext -R -J -O -K -P -F+jLB+f8p,Helvetica -N >> $outputps <<END");
    	pwfile.println((String.format("%.2f %.1f %.2f", crossdt, -10., crossdt)));
    	pwfile.println((String.format("%.2f %.1f %.2f", crossdt, 20., crossh)));
    	pwfile.println("END");
//	Triangle
    	pwfile.println("gmt pswiggle -J -R -Z1 -BSwne"
				+ "+t\"Vertical\" "
				+ "-Bx10f1 -By10f2 "
				+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
				+ String.format("-Wthinner," + "blue" )
				+ " -P -K -O >> $outputps << END");
		pwfile.println(String.format("%.2f %.2f %.7f", frontdt , 0., 0.));
		pwfile.println(String.format("%.2f %.2f %.7f", crossdt , 0., crossh));
		pwfile.println(String.format("%.2f %.2f %.7f", backdt , 0., 0.));
		pwfile.println("END");
		double[] halfDurations = new double[2];
		halfDurations[0] = crossdt-frontdt;
		halfDurations[1] = backdt-crossdt;
		return halfDurations;
	}
	
	
	public static void getDisplacement2all(GlobalCMTID eventID, String path, String synpath, PrintWriter pwfile, double sttime, double entime){
		SACFileName sacname = new SACFileName(path);
		SACFileName synsacname = new SACFileName(synpath);
       	try {
       		Trace trace = sacname.read().createTrace().cutWindow(sttime,entime); 
       		Trace syntrace = synsacname.read().createTrace().cutWindow(sttime,entime);
       		double max = getMax(syntrace);
       		trace2GMT(pwfile, trace, sacname.getComponent(), 0, 3, max);
       	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
       	}
	}
	
	public static double[] getDisplacementAmplitude2Average(GlobalCMTID eventID, String path, String synpath, PrintWriter pwfile, double sttime, double entime){
		SACFileName sacname = new SACFileName(path);
		SACFileName synsacname = new SACFileName(synpath);
//		double datamax = 0.;
		double[] Dispamplitude = null;
       	try {
       		Trace trace = sacname.read().createTrace().cutWindow(sttime,entime); 
       		Trace syntrace = synsacname.read().createTrace().cutWindow(sttime,entime);
       		double max = getMax(syntrace);  
//       		double max = getMax(trace);
       		double signal = Math.abs(getMax(trace));
       		double noise = Math.abs(getMax(sacname.read().createTrace().cutWindow(sttime - 50, sttime)));
       		Dispamplitude = getDisplacementAmplitude(trace, max);
       		if(signal < noise*1) { // noise*8
//       			System.out.println(signal + " " + noise + " " + sttime + " " + sacname);
       			Dispamplitude[0] = 999999999.;
//       		for(int i=0; i<Dispamplitude.length; i++){
//       			if(datamax < Dispamplitude[i])	datamax = Dispamplitude[i];
//       		}
//       		if(datamax > 20.)
//       			Dispamplitude[0] = 999999999.;
       		}
       	} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
       	}
       	return Dispamplitude;
	}	
	
//	
//	public static void getDisplacement(GlobalCMTID eventID, String path, String synpath, String outpath, double sttime, double entime){
//		try{
//			File gmtfile = new File(outpath);
//			try{
//				gmtfile.createNewFile();
//			} catch(IOException e){
//				System.out.println(e);
//			}			
//			PrintWriter pwfile = new PrintWriter(new BufferedWriter(new FileWriter(gmtfile)));
//			pwfile.println("#!/bin/sh");	
//// あとで変更
//			pwfile.println("outputps=output.ps");
//			pwfile.println("gmt set PS_MEDIA 23.33cx17.5c");
//			pwfile.println("gmt set FONT 10p,Helvetica,black");
//			pwfile.println("gmt set PS_CHAR_ENCODING ISOLatin1");
//			pwfile.println("gmt pstext -R4/20/-30/30 -JX20.0c/14c -K -P -Y2c > $outputps <<END");
//			pwfile.println("END");
//			pwfile.println("#!/bin/sh");
////			try {
////				Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowPath);
////		        for(TimewindowInformation timewindow : timewindows){
////		        	if(timewindow.getGlobalCMTID().equals(eventID) && timewindow.getStation().equals(obj)){
//		  	        //　直接指定する
////		        	Station station = timewindow.getStation();
////		        	SACComponent component = timewindow.getComponent();
//		        	SACFileName sacname = new SACFileName(path);
//		        	SACFileName synsacname = new SACFileName(synpath);
////		        	System.out.println(timewindow);
//		        	double GCARC = sacname.readHeader().getValue(SACHeaderEnum.GCARC);
//		        	pwfile.println("gmt pstext -R -J -O -K -P -F+jLB+f8p,Helvetica -N >> $outputps <<END");
//		        	pwfile.println("-3 25 " + GCARC);
//		        	pwfile.println("END");
//		        	Trace trace = sacname.read().createTrace().cutWindow(sttime,entime); 
//		        	Trace syntrace = synsacname.read().createTrace().cutWindow(sttime,entime);
//		        	double max = getMax(syntrace);
//	//	        	System.out.println("max is " + max);
//					trace2GMT(pwfile, trace, sacname.getComponent(), 0, 0, max/2.);
//				pwfile.println("pstext -R -J -O -P >> $outputps <<END");
//				pwfile.close();
//		      //  }
//			
//			}catch (IOException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
////			}catch(IOException e){
////				System.out.println(e);
////			}
//	}
//	
	public static double getMax(Trace trace){
//		double max = trace.getYVector().getMaxValue() > -trace.getYVector().getMinValue() ?
//		trace.getYVector().getMaxValue() : -trace.getYVector().getMinValue();
		double truemax = 99999999999.;
		double dt = 0.050;
		double[] time = trace.getX();
		double[] amplitude = trace.getY();
		double[] disp = new double[time.length];
		double a = 0.;
		double b = 0;
		for (int j=0; j < time.length; j++) {
	//		System.out.println(j * dt + " " + amplitude[j]);
			b = a + amplitude[j]*dt;
			disp[j] = b;
			a = b;
		}	
		double max = 0.;
		double min = 0.;
		double tmax = -1.;
		double tmin = -1.;
		for (int i=0; i< time.length; i++){
			if(disp[i] > max) {
				max = disp[i];
				tmax = time[i];
			}
			if(disp[i] < min) {
				min = disp[i];
				tmin = time[i];
			}
		}
		
//		if(Math.abs(max) >= Math.abs(min))	truemax = max;
//		if(Math.abs(max) < Math.abs(min)) truemax = min;
		truemax = tmin < tmax ? min : max;
		return truemax;
	}
	
	public static void trace2GMT(PrintWriter pwgmtfile, Trace trace, SACComponent component, double GCARC, int colornum, double max){
		pwgmtfile.println("");
		double dt = 0.050;
		String[] color = new String[4];
		color[0] = "black";
		color[1] = "red";
		color[2] = "green";
		color[3] = "grey";
			pwgmtfile.println("gmt pswiggle -J -R -Z1 -BSwne"
					+ "+t\"Vertical\" "
					+ "-Bx10f1 -By10f2 "
					+ "-Bpx+l\"reduced time (s)\" -Bpy+l\"@~D@~ (\\260)\" "
					+ String.format("-Wthinner," + color[colornum] )
					+ " -P -K -O >> $outputps << END");

		double[] time = trace.getX();
		double[] amplitude = trace.getY();
		double a = 0.;
		double b = 0;
		for (int j=0; j < time.length; j++) {
			b = a + amplitude[j]*dt;
			pwgmtfile.println(String.format("%.2f %.2f %.7f", j * dt, GCARC, b/max));
			a = b;
		}		
		pwgmtfile.println("END");
		
	}
	
	public static double[] getDisplacementAmplitude(Trace trace, double max){
		double dt = 0.050;
		double[] time = trace.getX();
		double[] amplitude = trace.getY();
		double[] Dispamplitude = new double[amplitude.length];
		double a = 0.;
		double b = 0;
		for (int j=0; j < time.length; j++) {
			b = a + amplitude[j]*dt;
			Dispamplitude[j] = b/max;
			a = b;
		}
		return Dispamplitude;
	}
	
}