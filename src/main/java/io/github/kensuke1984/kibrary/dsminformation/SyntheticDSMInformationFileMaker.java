package io.github.kensuke1984.kibrary.dsminformation;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.util.EventFolder;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTCatalog;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTData;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.spc.SPC_SAC;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;
import java.io.File;
import java.nio.charset.Charset;

import org.apache.commons.io.IOUtils;

/**
 * Make DSM information files for event folders under a work folder.
 *
 * @author Kensuke Konishi
 * @version 0.2.3
 */
public class SyntheticDSMInformationFileMaker implements Operation {

	private Properties PROPERTY;
	/**
	 * work folder
	 */
	private Path workPath;
	/**
	 * Number of steps in frequency domain.
	 * It must be a power of 2.
	 */
	private int np;
	/**
     * Time length [s].
     * It must be a power of 2 divided by 10.(2<sup>n</sup>/10)
	 */
	private double tlen;
	/**
	 * components to be used
	 */
	private Set<SACComponent> components;
	/**
	 * Information file name is header_[psv,sh].inf (default:PREM)
	 */
	private String header;
	/**
	 * structure file instead of PREM
	 */
	private Path structurePath;
	private Path timewindowPath;
	private boolean usewindow;
	private boolean syntheticDataset;
	private boolean specfemDataset;
	
	private SyntheticDSMInformationFileMaker(Properties property) throws IOException {
		this.PROPERTY = (Properties) property.clone();
		set();
	}

	public static void writeDefaultPropertiesFile() throws IOException {
		Path outPath = Paths
				.get(SyntheticDSMInformationFileMaker.class.getName() + Utilities.getTemporaryString() + ".properties");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
			pw.println("manhattan SyntheticDSMInformationFileMaker");
			pw.println("##SacComponents to be used (Z R T)");
			pw.println("#components");
			pw.println("##Path of a work folder (.)");
			pw.println("#workPath");
			pw.println("##header for names of information files, header_[psv, sh].inf, (PREM)");
			pw.println("#header");
			pw.println("##Path of a structure file you want to use. ()");
			pw.println("#structureFile");
			pw.println("##tlen must be a power of 2 over 10 (3276.8)");
			pw.println("#tlen");
			pw.println("##np must be a power of 2 (512)");
			pw.println("#np");
			pw.println("##Synthetic Dataset (false)");
			pw.println("#syntheticDataset");
			pw.println("##SPECFEM3D_GLOBE test dataset (false)");
			pw.println("#specfemDataset");
			pw.println("#timewindowPath");
		}
		System.err.println(outPath + " is created.");
	}

    /**
     * @param args [parameter file name]
     * @throws IOException if any
     */
	public static void main(String[] args) throws Exception {
		Properties property = new Properties();
		if (args.length == 0) property.load(Files.newBufferedReader(Operation.findPath()));
		else if (args.length == 1) property.load(Files.newBufferedReader(Paths.get(args[0])));
		else throw new IllegalArgumentException("too many arguments. It should be 0 or 1(property file name)");
		long start = System.nanoTime();
        System.err.println(SyntheticDSMInformationFileMaker.class.getName() + " is going.");
		SyntheticDSMInformationFileMaker sdif = new SyntheticDSMInformationFileMaker(property);
		sdif.run();
		System.err.println(SyntheticDSMInformationFileMaker.class.getName() + " finished in " +
                Utilities.toTimeString(System.nanoTime() - start));
	}

	private void checkAndPutDefaults() {
		if (!PROPERTY.containsKey("workPath")) PROPERTY.setProperty("workPath", "");
		if (!PROPERTY.containsKey("components")) PROPERTY.setProperty("components", "Z R T");
		if (!PROPERTY.containsKey("tlen")) PROPERTY.setProperty("tlen", "3276.8");
		if (!PROPERTY.containsKey("np")) PROPERTY.setProperty("np", "512");
		if (!PROPERTY.containsKey("header")) PROPERTY.setProperty("header", "PREM");
		if (!PROPERTY.containsKey("syntheticDataset")) PROPERTY.setProperty("syntheticDataset", "false");
		if (!PROPERTY.containsKey("specfemDataset")) PROPERTY.setProperty("specfemDataset", "false");
		if (!PROPERTY.containsKey("timewindowPath")) PROPERTY.setProperty("timewindowPath", "");
		// write additional info
		PROPERTY.setProperty("CMTcatalogue", GlobalCMTCatalog.getCatalogPath().toString());
	}
	
	private void set() throws IOException {
		checkAndPutDefaults();
		workPath = Paths.get(PROPERTY.getProperty("workPath"));
		if (!Files.exists(workPath)) throw new NoSuchFileException("The workPath: " + workPath + " does not exist");
		components = Arrays.stream(PROPERTY.getProperty("components").split("\\s+")).map(SACComponent::valueOf)
				.collect(Collectors.toSet());
		np = Integer.parseInt(PROPERTY.getProperty("np").split("\\s+")[0]);
		tlen = Double.parseDouble(PROPERTY.getProperty("tlen").split("\\s+")[0]);
		header = PROPERTY.getProperty("header").split("\\s+")[0];
		if (PROPERTY.containsKey("structureFile"))
			structurePath = Paths.get(PROPERTY.getProperty("structureFile").split("\\s+")[0]);
		else
			structurePath = Paths.get("PREM");
		syntheticDataset = Boolean.parseBoolean(PROPERTY.getProperty("syntheticDataset"));
		specfemDataset = Boolean.parseBoolean(PROPERTY.getProperty("specfemDataset"));
		
		usewindow = PROPERTY.getProperty("timewindowPath") != "";
		timewindowPath = Paths.get(PROPERTY.getProperty("timewindowPath"));
	}

	/**
	 * @author Kensuke Konishi
	 * @author anselme add models, options for synthetic/specfem dataset, ...
	 */
	@Override
	public void run() throws Exception {
		Set<EventFolder> eventDirs = Utilities.eventFolderSet(workPath);
		String modelName = structurePath.toString().trim().toUpperCase();
		PolynomialStructure ps = null;
		
		//use only events in timewindow file
		if (usewindow) {
			Set<GlobalCMTID> idInWindow =
					TimewindowInformationFile.read(timewindowPath).stream()
					.map(tw -> tw.getGlobalCMTID()).collect(Collectors.toSet());
		}
		
		// PREM_3600_RHO_3 : PREM is a 3% rho (density) discontinuity at radius 3600 km
		if (!modelName.contains("/") && modelName.contains("_")) {
			System.out.println("Using " + modelName + ". Adding perturbations");
			String[] ss = modelName.split("_");
			modelName = ss[0];
			String[] range = ss[1].split("-");
			double r1 = Double.parseDouble(range[0]);
			double r2 = Double.parseDouble(range[1]);
			Map<String, Double> quantityPercentMap = new HashMap<>();
			for (int i = 2; i < ss.length; i++) {
				String[] quantity_percent = ss[i].split("-");
				double percent = quantity_percent[1].startsWith("M") ? -1 * Double.parseDouble(quantity_percent[1].substring(1)) / 100.
						: Double.parseDouble(quantity_percent[1]) / 100.;
				quantityPercentMap.put(quantity_percent[0], percent);
			}
			if (modelName.equals("MIASP91")) {
				ps = PolynomialStructure.MIASP91;
				for (String quantity : quantityPercentMap.keySet()) {
					System.out.println("Adding " + quantity + " " + quantityPercentMap.get(quantity)*100 + "% discontinuity");
					if (quantity.equals("RHO"))
						ps = ps.addRhoDiscontinuity(r1, r2, quantityPercentMap.get(quantity));
					else if (quantity.equals("VS"))
						ps = ps.addVsDiscontinuity(r1, r2, quantityPercentMap.get(quantity));
				}
			}
			else if (modelName.equals("PREM")) {
				ps = PolynomialStructure.PREM;
				for (String quantity : quantityPercentMap.keySet()) {
					System.out.println("Adding " + quantity + " " + quantityPercentMap.get(quantity)*100 + "% discontinuity");
					if (quantity.equals("RHO"))
						ps = ps.addRhoDiscontinuity(r1, r2, quantityPercentMap.get(quantity));
					else if (quantity.equals("VS"))
						ps = ps.addVsDiscontinuity(r1, r2, quantityPercentMap.get(quantity));
				}
			}
			else if (modelName.equals("AK135")) {
				ps = PolynomialStructure.AK135;
				for (String quantity : quantityPercentMap.keySet()) {
					System.out.println("Adding " + quantity + " " + quantityPercentMap.get(quantity)*100 + "% discontinuity");
					if (quantity.equals("RHO"))
						ps = ps.addRhoDiscontinuity(r1, r2, quantityPercentMap.get(quantity));
					else if (quantity.equals("VS"))
						ps = ps.addVsDiscontinuity(r1, r2, quantityPercentMap.get(quantity));
				}
			}
			else
				throw new RuntimeException("Model not implemented yet");
		}
		else {
			switch (modelName) {
			case "PREM":
				System.err.println("Using PREM");
				ps = PolynomialStructure.PREM;
				break;
			case "AK135":
				System.err.println("Using AK135");
				ps = PolynomialStructure.AK135;
				break;
			case "AK135-ELASTIC":
				System.err.println("Using AK135 elastic");
				ps = PolynomialStructure.AK135_elastic;
				break;
			case "MIASP91":
				System.err.println("Using MIASP91");
				ps = PolynomialStructure.MIASP91;
				break;
			case "IPREM":
				System.err.println("Using IPREM");
				ps = PolynomialStructure.ISO_PREM;
				break;
			case "TNASNA":
				System.err.println("Using TNASNA");
				ps = PolynomialStructure.TNASNA;
				break;
			case "TBL50":
				System.err.println("Using TBL50");
				ps = PolynomialStructure.TBL50;
				break;
			case "MAK135":
				System.err.println("Using MAK135");
				ps = PolynomialStructure.MAK135;
				break;
			default:
				System.err.println("Using " + structurePath);
				ps = new PolynomialStructure(structurePath);
			}
		}
			
		Path outPath = workPath.resolve("synthetic" + Utilities.getTemporaryString());
		Files.createDirectories(outPath);
		
		if (PROPERTY != null)
			writeProperties(outPath.resolve("dsmifm.properties"));
		
		//synthetic station set
		Set<Station> synStationSet = new HashSet<>();
		if (syntheticDataset) {
			for (int i = 1; i <= 170; i+=1) {
				double distance = i;
				String stationName = String.format("%03d", i);
				Station station = new Station(stationName
						, new HorizontalPosition(0, distance), "DSM");
				synStationSet.add(station);
			}
		}
		
		//specfem test dataset
		if (specfemDataset) {
			Set<Station> specfemStationSet = IOUtils.readLines(SyntheticDSMInformationFileMaker.class.getClassLoader()
					.getResourceAsStream("specfem_stations.inf"), Charset.defaultCharset())
				.stream().map(s -> Station.createStation(s)).collect(Collectors.toSet());
			try {
				GlobalCMTData id = new GlobalCMTID("060994A").getEvent();
				Path eventOut = outPath.resolve(id.toString());
				SyntheticDSMInfo info = new SyntheticDSMInfo(ps, id, specfemStationSet, header, tlen, np);
				Files.createDirectories(eventOut.resolve(header));
				info.writePSV(eventOut.resolve(header + "_PSV.inf"));
				info.writeSH(eventOut.resolve(header + "_SH.inf"));
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		for (EventFolder eventDir : eventDirs) {
			try {
				Set<Station> stations = eventDir.sacFileSet().stream()
						.filter(name -> name.isOBS() && components.contains(name.getComponent())).map(name -> {
							try {
								return name.readHeader();
							} catch (Exception e2) {
								return null;
							}
						}).filter(Objects::nonNull).map(Station::of).collect(Collectors.toSet());
				if (syntheticDataset)
					stations = synStationSet;
				if (stations.isEmpty())
					continue;
				int numberOfStation = (int) stations.stream().map(Station::toString).count();
				if (numberOfStation != stations.size())
					System.err.println("!Caution there are stations with the same name and different positions in "
							+ eventDir);
				Path eventOut = outPath.resolve(eventDir.toString());
				
				if (eventDir.getGlobalCMTID().getEvent() != null) {
					SyntheticDSMInfo info = new SyntheticDSMInfo(ps, eventDir.getGlobalCMTID().getEvent(), stations, header, tlen, np);
					Files.createDirectories(eventOut.resolve(header));
					info.writePSV(eventOut.resolve(header + "_PSV.inf"));
					info.writeSH(eventOut.resolve(header + "_SH.inf"));
				}
				else {
					System.out.println(eventDir.getGlobalCMTID() + "is not in the catalog");
				}
			} catch (Exception e) {
				System.err.println("Error on " + eventDir);
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public Properties getProperties() {
		return (Properties) PROPERTY.clone();
	}

	@Override
	public Path getWorkPath() {
		return workPath;
	}
}
