package io.github.kensuke1984.kibrary.util.globalcmt;

import io.github.kensuke1984.kibrary.datacorrection.MomentTensor;
import io.github.kensuke1984.kibrary.util.Location;

import java.time.LocalDateTime;

/**
 * Data for global CMT data used frequently.
 *
 * @author Kensuke Konishi
 * @version 0.0.1.0.1
 */
public interface GlobalCMTData {

    double getMb();

    double getMs();

    MomentTensor getCmt();

    Location getCmtLocation();

    LocalDateTime getCMTTime();

    double getHalfDuration();

    Location getPDELocation();

    LocalDateTime getPDETime();

    GlobalCMTID getGlobalCMTID();
    
    void setCMT(MomentTensor mt);
    
    double getTimeDifference();

	String getHypocenterReferenceCatalog();
	
	String getGeographicalLocationName();
    
}
