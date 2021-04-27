package io.github.kensuke1984.kibrary.util;

import io.github.kensuke1984.kibrary.Environment;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.spc.FormattedSPCFile;
import io.github.kensuke1984.kibrary.util.spc.SPCFile;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.SimpleEmail;
import org.apache.commons.math3.util.FastMath;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.*;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utilities for a workpath containing event folders which have SAC files. also
 * this contains various useful static methods.
 *
 * @author Kensuke Konishi
 * @version 0.1.11
 */
public final class Utilities {

    public static void deleteOnExit(Path path) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                Files.deleteIfExists(path);
            } catch (IOException ignored) {
            }
        }));
    }

    private Utilities() {
    }

    /**
     * @param path      Path of the target
     * @param algorithm MD5, SHA-256, ...
     * @return byte string
     * @throws IOException              if any
     * @throws NoSuchAlgorithmException if any
     */
    public static String checksum(Path path, String algorithm) throws IOException, NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance(algorithm);
        try (DigestInputStream digestInputStream = new DigestInputStream(
                new BufferedInputStream(Files.newInputStream(path)), md)) {
            while (digestInputStream.read() != -1) ;
        }
        StringBuilder result = new StringBuilder();
        for (byte b : md.digest())
            result.append(String.format("%02x", b));
        return result.toString();
    }

    /**
     * e.g. num=2, values={1,2,3,4} &rarr; {1,1}, {1,2}, {1,3}, ...{4,3},{4,4}
     *
     * @param num    number of elements for each pattern
     * @param values elements are chosen from these values
     * @return Array of all possible patterns of num-elements from the values.
     * Each element in the array has the array of num elements. patterns[i][j]
     * is j-th degit in the i-th pattern
     */
    public static double[][] makePatterns(int num, double[] values) {
        int patternN = (int) Math.pow(values.length, num);
        double[][] patterns = new double[patternN][num];
        int i = 0;
        do {
            for (int j = 0; j < num; j++)
                patterns[i][j] = values[(i / (int) Math.pow(values.length, j)) % values.length];
        } while (++i < patternN);
        return patterns;
    }

    /**
     * @param url URL of the file to download
     * @return path of the downloaded file (dl......tmp in TEMP)
     * @throws IOException if any
     */
    public static Path download(URL url) throws IOException {
        ReadableByteChannel rbc = Channels.newChannel(url.openStream());
        Path out = Files.createTempFile("dl", "tmp");
        try (FileOutputStream fos = new FileOutputStream(out.toFile()); FileChannel fc = fos.getChannel()) {
            fc.transferFrom(rbc, 0, Long.MAX_VALUE);
        }
        return out;
    }

    /**
     * @param url       URL of the file to download
     * @param outPath   path of the downloaded file
     * @param overwrite if this is false and the file of outPath exists, the download is cancelled.
     * @throws IOException if any
     */
    public static void download(URL url, Path outPath, boolean overwrite) throws IOException {
        if (!overwrite && Files.exists(outPath)) throw new FileAlreadyExistsException(outPath + " already exists.");
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        long fileSize = httpConnection.getContentLength();
        JProgressBar bar = null;
        JFrame frame = null;
        if (0 < fileSize && !GraphicsEnvironment.isHeadless()) {
            bar = new JProgressBar(0, (int) fileSize);
            frame = new JFrame("Downloading a file");
            frame.setResizable(false);
            frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            frame.setSize(300, 70);
            frame.add(bar);
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        }
        try (FileOutputStream fos = new FileOutputStream(outPath.toFile());
             BufferedInputStream bufferedInputStream = new BufferedInputStream(url.openStream())) {
            byte[] buf = new byte[8192];
            int x = 0;
            int downloaded = 0;
            while (0 <= (x = bufferedInputStream.read(buf))) {
                fos.write(buf, 0, x);
                if (Objects.nonNull(bar)) bar.setValue((downloaded += x));
            }
        } finally {
            if (Objects.nonNull(frame)) frame.dispose();
        }
    }


    /**
     * Extract a zipfile into outRoot
     *
     * @param zipPath path of a zip file to extract
     * @param outRoot path of a target path (folder)
     * @param options options for writing
     */
    public static void extractZip(Path zipPath, Path outRoot, OpenOption... options) throws IOException {
        try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(Files.newInputStream(zipPath)))) {
            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                Path outPath = outRoot.resolve(entry.getName());
                try (BufferedOutputStream bufferedOutputStream = new BufferedOutputStream(
                        Files.newOutputStream(outPath, options))) {
                    if (IOUtils.copy(zis, bufferedOutputStream) < 0)
                        throw new RuntimeException("Zip file could not be extracted without errors.");
                }
            }
        }
    }

    /**
     * Change the input to an intelligible expression.
     *
     * @param nanoSeconds [ns] time
     * @return ?d, ?h, ?min and ?s
     */
    public static String toTimeString(long nanoSeconds) {
        long used = 0;
        long day = TimeUnit.NANOSECONDS.toDays(nanoSeconds);
        used += TimeUnit.DAYS.toNanos(day);
        long hour = TimeUnit.NANOSECONDS.toHours(nanoSeconds - used);
        used += TimeUnit.HOURS.toNanos(hour);
        long min = TimeUnit.NANOSECONDS.toMinutes(nanoSeconds - used);
        used += TimeUnit.MINUTES.toNanos(min);
        double sec = (nanoSeconds - used) / 1000000000.0;
        return (day == 0 ? "" : day + "d, ") + (hour == 0 ? "" : hour + "h, ") + (min == 0 ? "" : min + " min and ") +
                sec + " s";
    }

    /**
     * @return String in the clipboard
     * @throws UnsupportedFlavorException if the clipboard has any that can not be string.
     * @throws IOException                if an I/O error occurs.
     */
    public static String getClipBoard() throws UnsupportedFlavorException, IOException {
        return Toolkit.getDefaultToolkit().getSystemClipboard().getData(DataFlavor.stringFlavor).toString();
    }

    /**
     * @return string read by standard input (System.in)
     * @throws IOException if any
     */
    public static String readInputLine() throws IOException {
        return new BufferedReader(new InputStreamReader(System.in)).readLine();
    }

    /**
     * @param variance variance
     * @param n        Number of independent data
     * @param k        Degree of freedom
     * @return aic
     */
    public static double computeAIC(double variance, int n, int k) {
        double log2pi = Math.log(2 * Math.PI);
        return n * (log2pi + Math.log(variance) + 1) + 2 * k + 2;
    }

    /**
     * @param <T>  The result type.
     * @param task to put in another thread
     * @return Future of the task
     */
    public static <T> Future<T> run(Callable<T> task) {
        FutureTask<T> ft = new FutureTask<>(task);
        new Thread(ft).start();
        return ft;
    }

    /**
     * @param path {@link Path} for search of {@link GlobalCMTID}
     * @return <b>unmodifiable</b> Set of Global CMT IDs in the path
     * @throws IOException if an I/O error occurs
     */
    public static Set<GlobalCMTID> globalCMTIDSet(Path path) throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            return Collections.unmodifiableSet(
                    stream.filter(dir -> GlobalCMTID.isGlobalCMTID(dir.getFileName().toString()))
                            .map(dir -> new GlobalCMTID(dir.getFileName().toString())).collect(Collectors.toSet()));
        }
    }

    /**
     * @param path Path of a folder containing event folders.
     * @return Set of {@link EventFolder} in the workPath
     * @throws IOException if an I/O error occurs
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
     * @param path of a folder containing event folders which have SAC files.
     * @return <b>Unmodifiable</b> Set of sac in event folders under the path
     * @throws IOException if an I/O error occurs.
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
     * @param workPath where this looks for event folders
     * @param process  {@link Consumer} for each event
     * @param timeout  timeout for the process
     * @param unit     unit of the timeout
     * @return elapsed time [nano second]
     * @throws InterruptedException if the process takes over 30 minutes
     * @throws IOException          if an I/O error occurs
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
     * @param path      working path
     * @param predicate if true with a sacfile in event folders, the file is moved to
     *                  the directory.
     * @throws InterruptedException if the process takes over 30 minutes
     * @throws IOException          if an I/O error occurs
     */
    public static void moveSacfile(Path path, Predicate<SACFileName> predicate)
            throws IOException, InterruptedException {
        String directoryName = "movedSACFiles" + getTemporaryString();
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
     * @param n the effective digit
     * @param d value to change
     * @return changed value which effective digit is n
     */
    public static double toSignificantFigure(int n, double d) {
        if (n < 1) throw new IllegalArgumentException("invalid input n");
        long log10 = (long) Math.floor(Math.log10(Math.abs(d)));
        double power10 = FastMath.pow(10, log10 - n + 1);
        return Math.round(d / power10) * power10;
    }

    /**
     * @param n     number of decimal places
     * @param value to fix
     * @return string for fixed value
     */
    public static String fixDecimalPlaces(int n, double value) {
        double factor = Math.pow(10, n);
        double fixedValue = Math.round(value * factor) / factor;
        int integerPart = (int) Math.floor(fixedValue);
        int decimalPlaces = (int) Math.round((fixedValue - integerPart) * factor);
        if (n == 0) return String.valueOf(integerPart);
        return integerPart + "." + StringUtils.leftPad(Integer.toString(decimalPlaces), n, "0");
    }

    /**
     * @param path {@link Path} to look for {@link FormattedSPCFile} in
     * @return set of {@link SPCFile} in the dir
     * @throws IOException if an I/O error occurs
     */
    public static Set<SPCFile> collectSpcFileName(Path path) throws IOException {
        try (Stream<Path> stream = Files.list(path)) {
            return stream.filter(FormattedSPCFile::isFormatted).map(FormattedSPCFile::new).collect(Collectors.toSet());
        }
    }

    /**
     * print all input objects using {@link java.io.PrintStream#println()}
     *
     * @param objs to be printed
     */
    public static void println(Object... objs) {
        System.out.println(Arrays.stream(objs).map(Object::toString).collect(Collectors.joining(" ")));
    }

    /**
     * @param srcPath       {@link Path} of the file to be moved
     * @param destDirectory {@link Path} of the destination directory
     * @param createDestDir If {@code true} create the destination directory, otherwise if
     *                      {@code false} throw an IOException
     * @param options       for copying
     * @throws IOException if an I/O error occurs
     */
    public static void moveToDirectory(Path srcPath, Path destDirectory, boolean createDestDir, CopyOption... options)
            throws IOException {
        if (createDestDir) Files.createDirectories(destDirectory);
        Files.move(srcPath, destDirectory.resolve(srcPath.getFileName()), options);
    }


    /**
     * @param targetPath    even if a target path is a relative path, the symlink is for its absolute path.
     * @param destDirectory in which the symlink is created.
     * @param createDestDir if this value is true and the destDirectory does not exist, this method creates the directory.
     * @param options       for copying
     * @throws IOException if any
     */
    public static void createLinkInDirectory(Path targetPath, Path destDirectory, boolean createDestDir,
                                             CopyOption... options) throws IOException {
        System.out.println(destDirectory.resolve(targetPath.getFileName()));
        if (createDestDir) Files.createDirectories(destDirectory);
        Files.createSymbolicLink(destDirectory.resolve(targetPath.getFileName()), targetPath.toAbsolutePath());
    }

    /**
     * Changes an input double value to a string. The value is rounded to have n
     * decimal places.
     *
     * @param n the number of decimal places (Note that if decimal is 0, this
     *          value will be ignored)
     * @param d to be changed
     * @return String with d expressing .
     */
    public static String toStringWithD(int n, double d) {
        int intValue = (int) d;
        double decimal = d - intValue;
        decimal *= Math.pow(10, n);
        int decimalInt = (int) Math.round(decimal);
        return decimalInt == 0 ? String.valueOf(intValue) : intValue + "d" + decimalInt;
    }

    public static void sendMail(String address, String title, String... bodies) throws URISyntaxException, IOException {
        String body = String.join("%0A", bodies);
        URI uri = new URI("mailto:" + address + "?subject=" + title.replace(" ", "%20") + "&body=" +
                body.replace(" ", "%20").replace("\n", "%0A"));
        Desktop.getDesktop().mail(uri);
    }

    /**
     * @param subject       of the mail
     * @param to            address of the mail
     * @param lines         mail
     * @param authenticator for Gmail
     * @throws Exception if any
     */
    public static void sendGmail(String subject, String to, String[] lines, DefaultAuthenticator authenticator)
            throws Exception {
        Email email = new SimpleEmail();
        email.setHostName("smtp.googlemail.com");
        email.setSmtpPort(465);
        email.setAuthenticator(authenticator);
        email.setSSLOnConnect(true);
        email.setFrom(Environment.getEmail());
        email.setSubject(subject);
        email.setMsg(String.join("\n", lines));
        email.addTo(to);
        email.send();
    }

    /**
     * Input dialog or input prompt shows up.
     * Your input is hidden.
     *
     * @param phrase key for the password
     * @return password, secret phrase, ...
     */
    public static String getPassword(String phrase) throws InterruptedException {
        return GraphicsEnvironment.isHeadless() ?
                String.copyValueOf(System.console().readPassword("Password for " + phrase)) :
                PasswordInput.getPassword(phrase);
    }
}
