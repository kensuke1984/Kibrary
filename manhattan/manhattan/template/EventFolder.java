package manhattan.template;

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

import filehandling.sac.SACFileName;
import manhattan.globalcmt.GlobalCMTID;

/**
 * イベントフォルダ
 * 
 * @version 0.0.1
 * @since 2013/9/16 {@link File}の拡張
 * 
 * @version 0.0.2
 * @since 2013/11/30
 * 
 * 
 * @version 0.0.3
 * @since 2013/12/17 {@link GlobalCMTID}に
 * 
 * @version 0.0.4
 * @since 2013/2/4 {@link #getGlobalCMTID()} fixed
 * 
 * 
 * @since 2014/9/7
 * @version 0.0.5 to Java 8
 * 
 * @version 0.0.6
 * @since 2015/2/17 Constructors changed.
 * 
 * @version 0.0.7
 * @since 2015/7/13 {@link Path}
 * 
 * @version 0.0.8
 * @since 2015/8/17 {@link #moveSacFile(Predicate, Path, CopyOption...)}
 *        installed
 * 
 * @version 0.0.8.1
 * @since 2015/9/12 sacFilename...
 * 
 * @version 0.0.8.2
 * 
 * @author Kensuke Konishi
 * 
 */

public class EventFolder extends File {

	private static final long serialVersionUID = 8698976273645876402L;

	/**
	 * global CMT ID
	 */
	private GlobalCMTID eventID;

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
	 * Move sacfiles which satisfies an input sacPredicate.
	 * For example, if you want to move all synthetic sac files,
	 * 
	 * predicate is  sfn &rarr; sfn.isSYN()
	 * 
	 * @param predicate
	 *            Sac files satisfying the sacPredicate will be moved.
	 * @param movePath
	 *            Path of a trash
	 * @param options
	 *            for copying
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public void moveSacFile(Predicate<SACFileName> predicate, Path movePath, CopyOption... options)
			throws IOException {
		Files.createDirectories(movePath);
		sacFileSet().parallelStream().filter(predicate).map(name -> name.toPath()).forEach(path -> {
			try {
				Files.move(path, movePath.resolve(path.getFileName()), options);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		try {
			Files.delete(movePath);
		} catch (DirectoryNotEmptyException e) {
		}
	}

	/**
	 * @return (<b>unmodifiable</b>) Set of all SAC files in this including observed, synthetic and
	 *         partial derivatives.
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public Set<SACFileName> sacFileSet() throws IOException {
		try (Stream<Path> stream = Files.list(this.toPath())) {
			return stream.filter(SACFileName::isSacFileName).map(SACFileName::new).collect(Collectors.toSet());
		}
	}

	
	/**
	 * If you want to collect observed sac files,
	 * filter is name &rarr; !name.isOBS()
	 * 
	 * @param filter
	 *            for listing. if a sac file is true in it, the sac file is cut.
	 * @return (<b>unmodifiable</b>) Set of all SAC files in the event folder without those satisfying
	 *         the predicate
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public Set<SACFileName> sacFileSet(Predicate<SACFileName> filter) throws IOException {
		Set<SACFileName> set = sacFileSet();
		set.removeIf(filter);
		return set;
	}




}
