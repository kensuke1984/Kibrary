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
 */
public abstract class SPCFile extends File {

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
    public abstract SpcFileComponent getMode();

    /**
     * @return type (PAR0, .., PARQ, synthetic) of the file.
     */
    public abstract SpcFileType getFileType();

    /**
     * @return ID for the observer (station)
     */
    public abstract String getObserverID();

    /**
     * @return if this is synthetic (not partial)
     */
    public abstract boolean isSynthetic();

    /**
     * @return output of DSM
     */
    public abstract DSMOutput read() throws IOException;

}
