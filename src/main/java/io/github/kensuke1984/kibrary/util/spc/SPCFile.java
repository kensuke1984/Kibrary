package io.github.kensuke1984.kibrary.util.spc;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * A name of a spectrum file made by DSM<br>
 * <p>
 * Synthetic: ObserverID.SourceID(PSV, SV).spc
 * <p>
 * Partial derivatives: ObserverID.SourceID.type(par2, PF, PB .etc).x.y.(PSV,
 * SH).spc
 * <p>
 * 'PSV', 'SH' must be upper case.
 *
 * @author Kensuke Konishi
 * @version 0.2.0
 * @author anselme add network
 */
public abstract class SPCFile extends File {

    static boolean isFormatted(String name) {
        if (!name.endsWith(".spc")) return false;
        if (!name.endsWith("PSV.spc") && !name.endsWith("SH.spc")) {
            System.err.println("SPC file name must end with [PSV, SH].spc (psv, sh not allowed anymore).");
            return false;
        }
        String[] parts = name.split("\\.");
        if (parts.length != 3 && parts.length != 7) {
            System.err.println("SPC file name must be station.GlobalCMTID(PSV, SV).spc or " +
                    "station.GlobalCMTID.type(par2, PF, PB .etc).x.y.(PSV, SH).spc");
            return false;
        }
        
        String observerID = name.split("\\.")[0].split("_")[0];
        if (parts.length == 3) {
        	String observerNetwork = name.split("\\.")[0].split("_")[1];
			// synthetics files have both station name and network name
			if (8 < observerID.length()) System.err.println(observerID + "Name of station cannot be over 8 characters");
			if (8 < observerNetwork.length()) System.err.println(observerNetwork + "Name of network cannot be over 8 characters");
		}
        else {
        	// bp and fp files have only a station name
        	if (8 < observerID.length()) {
	            System.err.println("Name of station cannot be over 8 characters.");
	            return false;
        	}
        }

        return true;
    }

    /**
     * @param fileName file name for chack
     * @return if the fileName is synthetic (not partial)
     */
    static boolean isSynthetic(String fileName) {
        return fileName.split("\\.").length == 3;
    }

    SPCFile(String pathname) {
        super(pathname);
    }

    SPCFile(String parent, String child) {
        super(parent, child);
    }

    SPCFile(File parent, String child) {
        super(parent, child);
    }

    SPCFile(URI uri) {
        super(uri);
    }

    SPCFile(Path path) {
        this(path.toString());
    }

    /**
     * @return ID of source
     */
    public abstract String getSourceID();

    /**
     * @return psv or sh
     */
    public abstract SPCMode getMode();

    /**
     * @return type (PAR0, .., PARQ, synthetic) of the file.
     */
    public abstract SPCType getFileType();

    /**
     * @return ID for the observer (station)
     */
    public abstract String getObserverID();
    
    /**
     * @return NETWORK for the observer
     * @author anselme
     */
    public abstract String getObserverNetwork();

    /**
     * @return if this is synthetic (not partial)
     */
    public abstract boolean isSynthetic();

    /**
     * @return output of DSM
     * @throws IOException if an I/O error occurs
     */
    public abstract DSMOutput read() throws IOException;

}
