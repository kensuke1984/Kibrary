package io.github.kensuke1984.kibrary.misc;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.external.SAC;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.util.sac.SACData;
import io.github.kensuke1984.kibrary.util.sac.SACFileName;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderData;
import io.github.kensuke1984.kibrary.util.sac.SACHeaderEnum;

/**
 * station duplicationが存在するディレクトリ.
 *
 * @author Yuki Suzuki
 * @version 0.0.1
 */
public class DuplicationBreak implements Operation {

    public DuplicationBreak(Properties property) {
        this.property = (Properties) property.clone();
        set();
    }

    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath = Paths.get(DuplicationBreak.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan DuplicationBreak");
            pw.println("##Path of a working folder (.)");
            pw.println("#workPath");
            pw.println("##SacComponents to be applied the filter (Z R T)");
            pw.println("#components");
            pw.println("##Path of a root folder containing dataset (.)");
            pw.println("#dataPath");
            pw.println("##DELTA in SAC files. The SAC files with another value of DELTA are to be ignored. (0.05)");
            pw.println("#delta");
            pw.println("##Longer limit of the period band [s] (200)");
            pw.println("#longPreiod");
            pw.println("##Shorter limit of the period band [s] (20)");
            pw.println("#shortPeriod");
            pw.println("##Path of a dupulication information file (duplication.inf)");
            pw.println("#duplicationInfFile");
        }
        System.err.println(outPath + " is created.");
    }

    /**
     * Path for the work folder
     */
    private Path workPath;

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("components")) property.setProperty("components", "Z R T");
        if (!property.containsKey("dataPath")) property.setProperty("dataPath", "");
        if (!property.containsKey("delta")) property.setProperty("delta", "0.05");
        if (!property.containsKey("highFreq")) property.setProperty("longPreiod", "200");
        if (!property.containsKey("lowFreq")) property.setProperty("shortPeriod", "20");
        if (!property.containsKey("duplicationInfFile")) property.setProperty("duplicationInfFile", "duplication.inf");
    }

    private void set() {
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));

        if (!Files.exists(workPath)) throw new RuntimeException("The workPath: " + workPath + " does not exist");

        components = Arrays.stream(property.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
                .collect(Collectors.toSet());

        dataPath = getPath("dataPath");
        delta = Double.parseDouble(property.getProperty("delta"));
        longPreiod = Double.parseDouble(property.getProperty("longPreiod"));
        shortPreiod = Double.parseDouble(property.getProperty("shortPeriod"));
        duplicationInfFile = getPath("duplicationInfFile");
    }

    private Properties property;

    private Path outPath;

    /**
     * The root folder containing event folders which have observed and synthetic SAC files to
     * be modified
     */
    private Path dataPath;

    /**
     * The value 'DELTA' in SAC files. The SAC files with another value of
     * 'DELTA' are to be ignored.
     */
    private double delta;

    /**
     * components to be applied the filter
     */
    private Set<SACComponent> components;

    /**
     * minimum frequency [Hz] フィルターバンドの最小周波数
     */
    private double longPreiod;

    /**
     * maximum frequency [Hz] フィルターバンドの最大周波数
     */
    private double shortPreiod;
    
    /**
     * Path of a duplication information file
     */
    private static Path duplicationInfFile;

    /**
     * @param args [a property file name]
     * @throws Exception if any
     */
    public static void main(String[] args) throws Exception {
        Properties property = new Properties();
        if (args.length == 0) property.load(Files.newBufferedReader(Operation.findPath()));
        else if (args.length == 1) property.load(Files.newBufferedReader(Paths.get(args[0])));
        else throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");
        DuplicationBreak dupbreak = new DuplicationBreak(property);
        long startTime = System.nanoTime();
        System.err.println(DuplicationBreak.class.getName() + " is going.");
        readDuplicationInf();
        dupbreak.run();
        System.err.println(DuplicationBreak.class.getName() + " finished in " +
                Utilities.toTimeString(System.nanoTime() - startTime));
    }

    private Runnable process(EventFolder folder) {
        return () -> {
            String eventname = folder.getName();
            try {
                Files.createDirectories(outPath.resolve(eventname));
//                readDuplicationInf();
                Set<SACFileName> set = folder.sacFileSet();
                set.removeIf(s -> !components.contains(s.getComponent()));
                set.stream().forEachOrdered(s -> {
                	modifyAndout(s);
                });
            } catch (Exception e) {
                System.err.println("Error on " + folder);
                e.printStackTrace();
            }
        };
    }

    /**
     * Modify on the sacFile and output in the outDir
     *
     * @param sacFileName a sacfilename to be filtered
     */
    private void modifyAndout(SACFileName sacFileName) {
        try {
        	SACData sacFile = sacFileName.read();
        	SACHeaderData sacHeader = sacFileName.readHeader();
        	String beforeName = sacHeader.getStation().getName();
        	int indexStationPosition = Arrays.asList(stationPosition).indexOf(sacHeader.getStation().getPosition());
        	if (indexStationPosition != -1)
        	if (stationPosition[indexStationPosition].equals(sacHeader.getStation().getPosition())
        			&& sacHeader.getValue(SACHeaderEnum.USER0) == shortPreiod
        			&& sacHeader.getValue(SACHeaderEnum.USER1) == longPreiod
        			&& sacHeader.getValue(SACHeaderEnum.DELTA) == delta
        			&& !beforeName.equals("MAP0")){
        		String afterName = newStationName[indexStationPosition];
        		String modifiedFileName = sacFileName.toString().replace(beforeName, afterName);
        		Path outPath = this.outPath.resolve(sacFileName.getGlobalCMTID() + "/" + modifiedFileName);
        		sacFile.writeSAC(outPath);
        		String cwd = outPath.getParent().toString();
        		
        		try (SAC sacP1 = SAC.createProcess()) {
                    sacP1.inputCMD("cd " + cwd);
                    sacP1.inputCMD("r " + outPath.getFileName());
                    sacP1.inputCMD("ch kstnm " + afterName);
                    sacP1.inputCMD("w over");
                } catch (Exception e) {e.printStackTrace();}
                
        	}
        	else if (beforeName.equals("MAP0")
        			&& sacFileName.isOBS()){      		
        		String afterName = newStationName[indexStationPosition];
        		String modifiedFileName = sacFileName.toString().replace(beforeName, afterName);
        		Path outPath = this.outPath.resolve(sacFileName.getGlobalCMTID() + "/" + modifiedFileName);
        		sacFile.writeSAC(outPath);
        		String cwd = outPath.getParent().toString();
//        		System.out.println(outPath);
        		try (SAC sacP2 = SAC.createProcess()) {
                    sacP2.inputCMD("cd " + cwd);
                    sacP2.inputCMD("r " + outPath.getFileName());
                    sacP2.inputCMD("ch knetwk XA");
                    sacP2.inputCMD("ch kstnm " + afterName);
                    sacP2.inputCMD("w over");
                } catch (Exception e) {e.printStackTrace();}
        	}
        	else if (beforeName.equals("MAP0")
        			&& sacFileName.isSYN()){
        		String afterName = newStationName[indexStationPosition];
        		String modifiedFileName = sacFileName.toString().replace(beforeName, afterName);
        		Path outPath = this.outPath.resolve(sacFileName.getGlobalCMTID() + "/" + modifiedFileName);
        		sacFile.writeSAC(outPath);
        	}
        	else
        		System.err.println("SAC header information is something wrong!");     
        	                   
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * old station names array
     */
    static String[] oldStationName = null;
    
    static String[] networkName = null;
    
    /**
     * station latitudes array
     */
    static double[] stationLat = null;
    
    /**
     * station longitudes array
     */
    static double[] stationLon = null;
    
    static HorizontalPosition[] stationPosition = null;
    
    /**
     * new station names array
     */
    static String[] newStationName = null;
    
    /**
     * read duplication information file
     * @throws IOException
     */
    private static void readDuplicationInf() throws IOException{
    	FileInputStream fis = new FileInputStream(duplicationInfFile.toString());
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));
		int nlines = 0;
		nlines = linesCount(nlines, fis);
		FileChannel fc = fis.getChannel();
		fc.position(0);
		oldStationName = new String[nlines];
		networkName = new String[nlines];
		stationPosition = new HorizontalPosition[nlines];
		newStationName = new String[nlines];
    	String line = null;
		int k = 0;
    	while ((line = br.readLine()) != null) {
    		String[] splitted = line.split(" ");
    		oldStationName[k] = String.valueOf(splitted[0]);
    		networkName[k] = String.valueOf(splitted[1]);
    		stationPosition[k] = new HorizontalPosition(Double.valueOf(splitted[2]), Double.valueOf(splitted[3]));
    		newStationName[k] = String.valueOf(splitted[4]);
    		
//    		System.out.println(oldStationName[k]+" "+networkName[k]+" "+stationPosition[k]+" "+newStationName[k]);
    		k++;
    	}
    }
    
    /** count the number of lines
	 * @param nlines
	 * @param fis
	 * @return
	 * @throws IOException
	 */
	private static int linesCount(int nlines, FileInputStream fis) throws IOException{
		int readChars = 0;
		byte[] c = new byte[1024];
		boolean empty = true;
		int count = 0;
		while ((readChars = fis.read(c)) != -1) {
			empty = false;
			for (int i = 0; i < readChars; ++i) {
				if (c[i] == '\n') {
					++count;
				}
			}
		}
		nlines = (count == 0 && !empty) ? 1 : count;
//		System.out.println(nlines);
		return nlines;
	}

    @Override
    public Properties getProperties() {
        return (Properties) property.clone();
    }

    @Override
    public void run() throws Exception {
    	//filter
//        setFilter(lowFreq, highFreq, np);
        Set<EventFolder> events = new HashSet<>();
        events.addAll(Files.exists(dataPath) ? Utilities.eventFolderSet(dataPath) : Collections.emptySet());
//        System.out.println(events.isEmpty());
        if (events.isEmpty()) return;
        outPath = workPath.resolve("modified" + Utilities.getTemporaryString());
        Files.createDirectories(outPath);
        ExecutorService es = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        events.stream().map(this::process).forEach(es::submit);
        es.shutdown();
        while (!es.isTerminated()) {
            Thread.sleep(100);
        }
    }

    @Override
    public Path getWorkPath() {
        return workPath;
    }

}
