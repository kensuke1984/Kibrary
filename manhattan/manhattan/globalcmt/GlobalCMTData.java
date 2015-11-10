/**
 * 
 */
package manhattan.globalcmt;

import java.time.LocalDateTime;

import manhattan.datacorrection.MomentTensor;
import manhattan.template.Location;

/**
 * @author kensuke
 * @since 2015/04/14
 * @version 0.0.1
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

}
