/**
 * 
 */
package io.github.kensuke1984.kibrary.util;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.util.FastMath;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.spc.SpcFileName;

/**
 * 
 * Utilities for a workpath containing event folders which have SAC files. also
 * this contains various useful static methods.
 * 
 * @author Kensuke Konishi
 * @version 0.1.0.7
 * 
 */
public final class Utilities {

	/**
	 * Change the input to an intelligible expression.
	 * 
	 * @param nanoSeconds
	 *            time
	 * @return ?d, ?h, ?min and ?s
	 */
	public static String toTimeString(long nanoSeconds) {
		long used = 0;
		long days = TimeUnit.NANOSECONDS.toDays(nanoSeconds);
		used += TimeUnit.DAYS.toNanos(days);
		long hours = TimeUnit.NANOSECONDS.toHours(nanoSeconds - used);
		used += TimeUnit.HOURS.toNanos(hours);
		long mins = TimeUnit.NANOSECONDS.toMinutes(nanoSeconds - used);
		used += TimeUnit.MINUTES.toNanos(mins);
		double sec = (nanoSeconds - used) / 1000000000.0;
		return (days == 0 ? "" : days + "d, ") + (hours == 0 ? "" : hours + "h, ")
				+ (mins == 0 ? "" : mins + " min and ") + sec + " s";
	}

	/**
	 * @return String in the clipboard
	 * @throws UnsupportedFlavorException
	 *             if the clipboard has any that can not be string.
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static String getClipBoard() throws UnsupportedFlavorException, IOException {
		return Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null)
				.getTransferData(DataFlavor.stringFlavor).toString();
	}

	/**
	 * @return string read by standard input (System.in)
	 * @throws IOException
	 *             if any
	 */
	public static String readInputLine() throws IOException {
		return new BufferedReader(new InputStreamReader(System.in)).readLine();
	}

	/**
	 * 独立データn, 自由度kに対してAICを計算する
	 * 
	 * @param variance
	 *            variance
	 * @param n
	 *            独立データ数
	 * @param k
	 *            自由度
	 * @return aic
	 */
	public static double computeAIC(double variance, int n, int k) {
		final double log2pi = Math.log(2 * Math.PI);
		return n * (log2pi + Math.log(variance) + 1) + 2 * k + 2;
	}

	private Utilities() {
	}

	/**
	 * @param task
	 *            to put in another thread
	 * @return Future of the task
	 */
	public static <T> Future<T> run(Callable<T> task) {
		FutureTask<T> ft = new FutureTask<>(task);
		new Thread(ft).start();
		return ft;
	}

	/**
	 * @param path
	 *            {@link Path} for search of {@link GlobalCMTID}
	 * @return (<b>unmodifiable</B>) Set of Global CMT IDs in the path
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static Set<GlobalCMTID> globalCMTIDSet(Path path) throws IOException {
		try (Stream<Path> stream = Files.list(path)) {
			return Collections
					.unmodifiableSet(stream.filter(dir -> GlobalCMTID.isGlobalCMTID(dir.getFileName().toString()))
							.map(dir -> new GlobalCMTID(dir.getFileName().toString())).collect(Collectors.toSet()));
		}
	}

	/**
	 * 引数ディレクトリ内のGlobalCMTIDに準ずるイベントフォルダ群<br>
	 * 
	 * @param path
	 *            Path of a folder containing event folders.
	 * @return (<b>unmodifiable</b>)Set of {@link EventFolder} in the workPath
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static Set<EventFolder> eventFolderSet(Path path) throws IOException {
		try (Stream<Path> stream = Files.list(path)) {
			return stream.filter(file -> GlobalCMTID.isGlobalCMTID(file.getFileName().toString()))
					.map(file -> new EventFolder(file.toString())).collect(Collectors.toSet());
		}
	}

	/**
	 * Errors in reading each event folder is just noticed. Such event folder
	 * will be ignored.
	 * 
	 * @param path
	 *            of a folder containing event folders which have SAC files.
	 * @return (<b>unmodifiable</b>)Set of sac in event folders under the path
	 * @throws IOException
	 *             if an I/O error occurs.
	 */
	public static Set<SACFileName> sacFileNameSet(Path path) throws IOException {
		return Collections.unmodifiableSet(eventFolderSet(path).stream().flatMap(eDir -> {
			try {
				return eDir.sacFileSet().stream();
			} catch (Exception e) {
				e.printStackTrace();
				return Stream.empty();
			}
		}).collect(Collectors.toSet()));
	}

	/**
	 * Runs process for all event folders under the workPath
	 * 
	 * @param workPath
	 *            where this looks for event folders
	 * @param process
	 *            {@link Consumer} for each event
	 * @param timeout
	 *            timeout for the process
	 * @param unit
	 *            unit of the timeout
	 * @return elapsed time [nano second]
	 * @throws InterruptedException
	 *             if the process takes over 30 minutes
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static long runEventProcess(Path workPath, Consumer<EventFolder> process, long timeout, TimeUnit unit)
			throws IOException, InterruptedException {
		long startTime = System.nanoTime();
		ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
		// Thread[] workers = new Thread[eventDirs.length];
		for (EventFolder eventDirectory : eventFolderSet(workPath))
			exec.submit(() -> process.accept(eventDirectory));
		exec.shutdown();
		exec.awaitTermination(timeout, unit);
		return System.nanoTime() - startTime;
	}

	/**
	 * Move SAC files that satisfies sacPredicate in event folders under the
	 * path
	 * 
	 * @param path
	 *            working path
	 * @param predicate
	 *            if true with a sacfile in event folders, the file is moved to
	 *            the directory.
	 * @throws InterruptedException
	 *             if the process takes over 30 minutes
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void moveSacfile(Path path, Predicate<SACFileName> predicate)
			throws IOException, InterruptedException {
		String directoryName = "movedSacfiles" + Utilities.getTemporaryString();
		// System.out.println(directoryName);
		Consumer<EventFolder> moveProcess = eventDirectory -> {
			try {
				eventDirectory.moveSacFile(predicate, eventDirectory.toPath().resolve(directoryName));
			} catch (Exception e) {
				e.printStackTrace();
			}
		};
		runEventProcess(path, moveProcess, 30, TimeUnit.MINUTES);
	}

	/**
	 * Create a string for temporary files or w/e
	 * 
	 * @return yyyyMMddHHmmss
	 */
	public synchronized static String getTemporaryString() {
		try {
			Thread.sleep(1000);
			return DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
		} catch (InterruptedException ie) {
			return DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
		}
	}

	/**
	 * @param n
	 *            the effective digit
	 * @param d
	 *            value to change
	 * @return changed value which effective digit is n
	 */
	public static double toSignificantFigure(int n, double d) {
		if (n < 1)
			throw new RuntimeException("invalid input n");

		final long log10 = (long) Math.floor(Math.log10(Math.abs(d)));
		final double power10 = FastMath.pow(10, log10 - n + 1);
		return Math.round(d / power10) * power10;
	}

	/**
	 * @param n
	 *            number of decimal places
	 * @param value
	 *            to fix
	 * @return string for fixed value
	 */
	public static String fixDecimalPlaces(int n, double value) {
		double factor = Math.pow(10, n);
		double fixedValue = Math.round(value * factor) / factor;
		int integerPart = (int) Math.floor(fixedValue);
		int decimalPlaces = (int) Math.round((fixedValue - integerPart) * factor);
		if (n == 0)
			return String.valueOf(integerPart);
		return integerPart + "." + StringUtils.leftPad(Integer.toString(decimalPlaces), n, "0");
	}

	/**
	 * @param path
	 *            {@link Path} to look for {@link SpcFileName} in
	 * @return set of {@link SpcFileName} in the dir
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static Set<SpcFileName> collectSpcFileName(Path path) throws IOException {
		try (Stream<Path> stream = Files.list(path)) {
			return stream.filter(SpcFileName::isSpcFileName).map(SpcFileName::new).collect(Collectors.toSet());
		}
	}

	/**
	 * @param srcPath
	 *            {@link Path} of the file to be moved
	 * @param destDirectory
	 *            {@link Path} of the destination directory
	 * @param createDestDir
	 *            If {@code true} create the destination directory, otherwise if
	 *            {@code false} throw an IOException
	 * @param options
	 *            for copyint
	 * @throws IOException
	 *             if an I/O error occurs
	 */
	public static void moveToDirectory(Path srcPath, Path destDirectory, boolean createDestDir, CopyOption... options)
			throws IOException {
		if (createDestDir)
			Files.createDirectories(destDirectory);
		Files.move(srcPath, destDirectory.resolve(srcPath.getFileName()), options);
	}

	/**
	 * Changes an input double value to a string. The value is rounded to have n
	 * decimal places.
	 * 
	 * @param n
	 *            the number of decimal places (Note that if decimal is 0, this
	 *            value will be ignored)
	 * @param d
	 *            to be changed
	 * @return String with d expressing .
	 */
	public static String toStringWithD(int n, double d) {
		int intValue = (int) d;
		double decimal = d - intValue;
		decimal *= Math.pow(10, n);
		int decimalInt = (int) Math.round(decimal);
		return decimalInt == 0 ? String.valueOf(intValue) : intValue + "d" + decimalInt;
	}

}
