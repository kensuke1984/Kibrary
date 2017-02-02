package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;

import java.io.File;
import java.io.IOException;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility for an event folder
 *
 * @author Kensuke Konishi
 * @version 0.0.8.4.1
 */

public class EventFolder extends File {

    private static final long serialVersionUID = 8698976273645876402L;

    private GlobalCMTID eventID;

    public EventFolder(File parent, String child) {
        super(parent, child);
        eventID = new GlobalCMTID(getName());
    }

    public EventFolder(String parent, String child) {
        super(parent, child);
        eventID = new GlobalCMTID(getName());
    }

    public EventFolder(String pathname) {
        super(pathname);
        eventID = new GlobalCMTID(getName());
    }

    public EventFolder(Path path) {
        this(path.toString());
    }

    /**
     * @return {@link GlobalCMTID} of this
     */
    public GlobalCMTID getGlobalCMTID() {
        return eventID;
    }

    @Override
    public String toString() {
        return eventID.toString();
    }

    /**
     * Move sacfiles which satisfies an input sacPredicate. For example, if you
     * want to move all synthetic sac files,
     * <p>
     * predicate is sfn &rarr; sfn.isSYN()
     *
     * @param predicate Sac files satisfying the sacPredicate will be moved.
     * @param movePath  Path of a trash
     * @param options   for copying
     * @throws IOException if an I/O error occurs
     */
    public void moveSacFile(Predicate<SACFileName> predicate, Path movePath, CopyOption... options) throws IOException {
        Files.createDirectories(movePath);
        sacFileSet().parallelStream().filter(predicate).map(SACFileName::toPath).forEach(path -> {
            try {
                Files.move(path, movePath.resolve(path.getFileName()), options);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        try {
            Files.delete(movePath);
        } catch (DirectoryNotEmptyException e) {
            e.printStackTrace();
        }
    }

    /**
     * @return <b>unmodifiable</b> Set of all SAC files in this including
     * observed, synthetic and partial derivatives.
     * @throws IOException if an I/O error occurs
     */
    public Set<SACFileName> sacFileSet() throws IOException {
        try (Stream<Path> stream = Files.list(toPath())) {
            return stream.filter(SACFileName::isSacFileName).map(SACFileName::new).collect(Collectors.toSet());
        }
    }

}
