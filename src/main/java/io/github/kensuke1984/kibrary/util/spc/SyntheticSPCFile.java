package io.github.kensuke1984.kibrary.util.spc;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
public class SyntheticSPCFile extends SPCFile {

    private static final long serialVersionUID = 2907815031281708657L;

    SyntheticSPCFile(String pathname) {
        super(pathname);
        readName(getName());
    }

    SyntheticSPCFile(String parent, String child) {
        super(parent, child);
        readName(getName());
    }

    SyntheticSPCFile(File parent, String child) {
        super(parent, child);
        readName(getName());
    }

    SyntheticSPCFile(URI uri) {
        super(uri);
        readName(getName());
    }

    SyntheticSPCFile(Path path) {
        this(path.toString());
    }

    /**
     * spheroidal mode PSV, toroidal mode SH
     */
    private SpcFileComponent mode;
    /**
     * PB: backward or PF: forward, PAR2: mu
     */
    private SpcFileType fileType;
    private String observerID;
    private String sourceID;

    private void readName(String fileName) {
        String[] parts = fileName.split("\\.");
        if (parts.length != 3)
            throw new IllegalArgumentException(fileName + " is not a valid synthetic SPC file name.");
        observerID = parts[0];
        sourceID = parts[1].replace("PSV", "").replace("SH", "");
        mode = fileName.endsWith("PSV.spc") ? SpcFileComponent.PSV : SpcFileComponent.SH;
    }


    @Override
    public String getSourceID() {
        return sourceID;
    }

    @Override
    public SpcFileComponent getMode() {
        return mode;
    }

    @Override
    public SpcFileType getFileType() {
        return SpcFileType.SYNTHETIC;
    }

    @Override
    public String getObserverID() {
        return observerID;
    }

    @Override
    public boolean isSynthetic() {
        return true;
    }

    @Override
    public DSMOutput read() throws IOException {
        return null;
    }
}
