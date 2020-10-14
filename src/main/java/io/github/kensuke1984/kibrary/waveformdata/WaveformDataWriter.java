package io.github.kensuke1984.kibrary.waveformdata;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import org.apache.commons.lang3.StringUtils;
import io.github.kensuke1984.anisotime.Phase;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Writer of BasicDataset and PartialDataset
 * <p>
 * This class create a new set of dataset files.
 *
 * @author Kensuke Konishi
 * @version 0.4.0.4
 * @author anselme added phase information
 */
public class WaveformDataWriter implements Closeable, Flushable {
    /**
     * id information file
     */
    private final Path IDPATH;
    /**
     * wavedata file
     */
    private final Path DATAPATH;
    /**
     * Because the header part is decided when this is constructed, the mode is
     * also decided(0: BasicID, 1: PartialID)
     */
    private final int MODE;
    /**
     * stream for id
     */
    private DataOutputStream idStream;
    /**
     * stream for wavedata
     */
    private DataOutputStream dataStream;
    /**
     * index map for stations
     */
    private Map<Station, Integer> stationMap;
    /**
     * index map for global CMT IDs
     */
    private Map<GlobalCMTID, Integer> globalCMTIDMap;
    /**
     * index map for perturbation location
     */
    private Map<Location, Integer> perturbationLocationMap;
	/**
	 * index map for phase name
	 */
	private Map<Phase, Integer> phaseMap;
    /**
     * index for period ranges
     */
    private double[][] periodRanges;
    /**
     * The file size (byte). (should be StartByte)
     */
    private long dataLength;
	/**
	 * This constructor is only for BasicID. All write ID must have a station,
	 * a Global CMT ID and period ranges in the input ones.
	 *
	 * @param idPath         Path for ID file (must not exist)
	 * @param dataPath       Path for data file (must not exist)
	 * @param stationSet     must contain all information of the IDs to write
	 * @param globalCMTIDSet must contain all information of the IDs to write
	 * @param periodRanges   must contain all information of the IDs to write. If you want
	 *                       to use ranges [10, 30] and [50,100] then the periodRanges
	 *                       should be new double[][]{{10,30},{50,100}}
	 * @param phases		 Array of phase names
	 * @throws IOException if an error occurs
	 */
	public WaveformDataWriter(Path idPath, Path dataPath, Set<Station> stationSet, Set<GlobalCMTID> globalCMTIDSet,
			double[][] periodRanges, Phase[] phases) throws IOException {
		this(idPath, dataPath, stationSet, globalCMTIDSet, periodRanges, phases, null);
	}
	
	/**
	 * This constructor is only for PartialID. All write ID must have a
	 * station, a Global CMT ID and period ranges in the input ones.
	 *
	 * @param idPath             Path for ID file (must not exist)
	 * @param dataPath           Path for data file (must not exist)
	 * @param stationSet         must contain all information of the IDs to write
	 * @param globalCMTIDSet     must contain all information of the IDs to write
	 * @param periodRanges       must contain all information of the IDs to write. If you want
	 *                           to use ranges [10, 30] and [50,100] then the periodRanges
	 *                           should be new double[][]{{10,30},{50,100}}
	 * @param phases			 Array of phase names
	 * @param perturbationPoints must contain all information of the IDs to write
	 * @throws IOException if an error occurs
	 */
	public WaveformDataWriter(Path idPath, Path dataPath, Set<Station> stationSet, Set<GlobalCMTID> globalCMTIDSet,
			double[][] periodRanges, Phase[] phases, Set<Location> perturbationPoints) throws IOException {
		IDPATH = idPath;
		DATAPATH = dataPath;
		if (checkDuplication(periodRanges)) throw new RuntimeException("Input periodRanges have duplication.");
		this.periodRanges = periodRanges;
        idStream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(idPath)));
        dataStream = new DataOutputStream(new BufferedOutputStream(Files.newOutputStream(dataPath)));
		dataLength = Files.size(dataPath);
		idStream.writeShort(stationSet.size());
		idStream.writeShort(globalCMTIDSet.size());
		idStream.writeShort(periodRanges.length);
		idStream.writeShort(phases.length);
		if (perturbationPoints != null) idStream.writeShort(perturbationPoints.size());
		makeStationMap(stationSet);
		makeGlobalCMTIDMap(globalCMTIDSet);
		for (int i = 0; i < periodRanges.length; i++) {
			idStream.writeDouble(periodRanges[i][0]);
			idStream.writeDouble(periodRanges[i][1]);
		}
		makePhaseMap(phases);
		if (perturbationPoints != null) makePerturbationMap(perturbationPoints);
		MODE = perturbationPoints == null ? 0 : 1;
	}
 
    private static boolean checkDuplication(double[][] periodRanges) {
        for (int i = 0; i < periodRanges.length - 1; i++)
            for (int j = i + 1; j < periodRanges.length; j++)
                if (Arrays.equals(periodRanges[i], periodRanges[j])) return true;
        return false;
    }

    public Path getIDPath() {
        return IDPATH;
    }

    public Path getDataPath() {
        return DATAPATH;
    }

    @Override
    public void close() throws IOException {
        idStream.close();
        dataStream.close();
    }

    @Override
    public void flush() throws IOException {
        idStream.flush();
        dataStream.flush();
    }

    private void makeGlobalCMTIDMap(Set<GlobalCMTID> globalCMTIDSet) throws IOException {
        int i = 0;
        globalCMTIDMap = new HashMap<>();
        for (GlobalCMTID id : globalCMTIDSet) {
            globalCMTIDMap.put(id, i++);
            idStream.writeBytes(StringUtils.rightPad(id.toString(), 15));
        }
    }

    private void makePerturbationMap(Set<Location> perturbationMap) throws IOException {
        int i = 0;
        perturbationLocationMap = new HashMap<>();
        for (Location loc : perturbationMap) {
            perturbationLocationMap.put(loc, i++);
            idStream.writeDouble(loc.getLatitude());
            idStream.writeDouble(loc.getLongitude());
            idStream.writeDouble(loc.getR());
        }
    }

	private void makePhaseMap(Phase[] phases) throws IOException {
		int i = 0;
		phaseMap = new HashMap<>();
		for (Phase phase : phases)	{
			phaseMap.put(phase, i++);
			idStream.writeBytes(StringUtils.rightPad(phase.toString(), 16));
		}
	}

    private void makeStationMap(Set<Station> stationSet) throws IOException {
        int i = 0;
        stationMap = new HashMap<>();
        for (Station station : stationSet) {
            stationMap.put(station, i++);
            idStream.writeBytes(StringUtils.rightPad(station.getName(), 8));
            idStream.writeBytes(StringUtils.rightPad(station.getNetwork(), 8));
            HorizontalPosition pos = station.getPosition();
            idStream.writeDouble(pos.getLatitude());
            idStream.writeDouble(pos.getLongitude());
        }
    }

    /**
     * Writes a waveform
     *
     * @param data waveform data
     */
    private void addWaveform(double[] data) throws IOException {
        for (double aData : data) dataStream.writeDouble(aData);
        dataLength += 8 * data.length;
    }

	/**
	 * @param basicID StartByte will be ignored and set properly in the write file.
	 * @throws IOException if an I/O error occurs
	 */
	public synchronized void addBasicID(BasicID basicID) throws IOException {
		if (MODE != 0) throw new RuntimeException("BasicID please, would you.");
		
		Integer ista = stationMap.get(basicID.STATION);
		if (ista == null) {
			throw new RuntimeException("No such station: " + basicID.STATION + " " + basicID);
		}

		switch (basicID.TYPE) {
		case OBS:
			idStream.writeBoolean(true); // if it is obs 1Byte
			break;
		case SYN:
			idStream.writeBoolean(false); // if it is obs
			break;
		default:
			throw new RuntimeException("This is a partial derivative.");
		}
		long startByte = dataLength;
		addWaveform(basicID.getData());
		idStream.writeShort(ista);
		idStream.writeShort(globalCMTIDMap.get(basicID.ID));
		idStream.writeByte(basicID.COMPONENT.valueOf());
		idStream.writeByte(getIndexOfRange(basicID.MIN_PERIOD, basicID.MAX_PERIOD));
		Phase[] phases = basicID.PHASES;
		for (int i = 0; i < 10; i++) { // 10 * 2 Byte
			if (i < phases.length) {
				idStream.writeShort(phaseMap.get(phases[i]));
			}
			else
				idStream.writeShort(-1);
		}
		
		// 4Byte * 3
		idStream.writeFloat((float) basicID.getStartTime()); // start time 
		idStream.writeInt(basicID.getNpts()); // number of points
		idStream.writeFloat((float) basicID.getSamplingHz()); // sampling Hz

        // if its convolute  true for obs
        idStream.writeBoolean(basicID.getWaveformType() == WaveformType.OBS || basicID.CONVOLUTE); // 1Byte
        idStream.writeLong(startByte); // data address 8 Byte
        
    }

    private int getIndexOfRange(double min, double max) {
        for (int i = 0; i < periodRanges.length; i++) // TODO
            if (Math.abs(periodRanges[i][0] - min) < 0.000000001 && Math.abs(periodRanges[i][1] - max) < 0.000000001)
                return i;
        throw new RuntimeException("A range is N/A");
    }

    /**
     * @param partialID {@link PartialID} must contain waveform data. StartByte will
     *                  be ignored and set properly in the write file.
     * @throws IOException if an I/O error occurs
     */
	public synchronized void addPartialID(PartialID partialID) throws IOException {
		if (partialID.TYPE != WaveformType.PARTIAL) throw new RuntimeException(
					"This is not a partial derivative. " + Thread.currentThread().getStackTrace()[1].getMethodName());
		if (MODE != 1) throw new RuntimeException("No Partial please, would you.");
		long startByte = dataLength;
		addWaveform(partialID.getData());
		idStream.writeShort(stationMap.get(partialID.STATION));
		idStream.writeShort(globalCMTIDMap.get(partialID.ID));
		idStream.writeByte(partialID.COMPONENT.valueOf());
		idStream.writeByte(getIndexOfRange(partialID.MIN_PERIOD, partialID.MAX_PERIOD));
		Phase[] phases = partialID.PHASES;
		for (int i = 0; i < 10; i++) { // 10 * 2 Byte
			if (i < phases.length) {
				idStream.writeShort(phaseMap.get(phases[i]));
			}
			else
				idStream.writeShort(-1);
		}
		idStream.writeFloat((float) partialID.START_TIME); // start time 4 Byte
		idStream.writeInt(partialID.NPTS); // データポイント数 4 Byte
		idStream.writeFloat((float) partialID.SAMPLINGHZ); // sampling Hz 4 Byte
		// convolutionされているか
		idStream.writeBoolean(partialID.CONVOLUTE); // 1Byte
		idStream.writeLong(startByte); // データの格納場所 8 Byte
		// partial type 1 Byte
		idStream.writeByte(partialID.getPartialType().getValue());
		idStream.writeShort(perturbationLocationMap.get(partialID.POINT_LOCATION));
	}
}
