package io.github.kensuke1984.kibrary.inversion.sourceTimeFunction;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrection;
import io.github.kensuke1984.kibrary.datacorrection.StaticCorrectionFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

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
import java.util.List;
import java.util.Set;

import edu.sc.seis.TauP.TauModelException;
import edu.sc.seis.TauP.TauP_Time;

class ReadStaticCorrection{	
	public static TimewindowInformation getcorrection(GlobalCMTID eventID, TimewindowInformation timewindow, Set<StaticCorrection> corrections){		
       	Station station = timewindow.getStation();
       	SACComponent component = timewindow.getComponent();
    	for(StaticCorrection correction : corrections){
       		if(correction.getStation().equals(station) && correction.getComponent().equals(component) && correction.getGlobalCMTID().equals(eventID)){
       			if( Math.abs(correction.getSynStartTime() - timewindow.getStartTime()) < 0.50){
       			double shift = correction.getTimeshift();
       			double startTime = correction.getSynStartTime() - shift ;
    	       	double endTime = correction.getSynStartTime() - shift + 40.;
        		TimewindowInformation tw = new TimewindowInformation(startTime, endTime
        				, station, eventID, component, new Phase[] {Phase.P});
       			return tw;
       			}
       		}	        
       	}
		return null;
				
	}

	public static TimewindowInformation gettimeshift(GlobalCMTID eventID, TimewindowInformation timewindow, double shift){		
       	Station station = timewindow.getStation();
       	SACComponent component = timewindow.getComponent();
	    double startTime = timewindow.getStartTime() - shift;
	   	double endTime = timewindow.getEndTime() - shift;
	    TimewindowInformation tw = new TimewindowInformation(startTime, endTime, station, eventID, component, new Phase[] {Phase.P});
	    return tw;

				
	}
	
	

}