package io.github.kensuke1984.kibrary.util.sac;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;

import io.github.kensuke1984.kibrary.external.SAC;
import io.github.kensuke1984.kibrary.util.Trace;

/**
 * Read/Write of a SAC file. (SAC: seismic analysis code)
 *
 * @author Kensuke Konishi
 * @version 0.1.1.1
 * @see <a href=http://ds.iris.edu/ds/nodes/dmc/forms/sac/>SAC</a>
 */
public final class SACUtil {

    private SACUtil() {
    }

    /**
     * By rotating hoge.E and hoge.N, output hoge.R and hoge.T The rotation is
     * done by SAC.
     *
     * @param sacEPath    SAC file which component is E. must exist.
     * @param sacNPath    SAC file which component is N. must exist.
     * @param outputRPath for output SAC with respect to R
     * @param outputTPath for output SAC with respect to T
     * @return if the output is successful
     * @throws IOException if an I/O error occurs. If sacEPath or sacNPath does not
     *                     exist, if output Paths already exist.
     */
    public static boolean rotate(Path sacEPath, Path sacNPath, Path outputRPath, Path outputTPath) throws IOException {
        if (Files.exists(outputRPath)) throw new FileAlreadyExistsException(outputRPath.toString());
        if (Files.exists(outputTPath)) throw new FileAlreadyExistsException(outputTPath.toString());

        // read headers of the input files
        Map<SACHeaderEnum, String> mapE = readHeader(sacEPath);
        Map<SACHeaderEnum, String> mapN = readHeader(sacNPath);

        int npts = Integer.parseInt(mapE.get(SACHeaderEnum.NPTS));
        if (npts != Integer.parseInt(mapN.get(SACHeaderEnum.NPTS))) return false;

        double cmpazE = Double.parseDouble(mapE.get(SACHeaderEnum.CMPAZ));
        double cmpazN = Double.parseDouble(mapN.get(SACHeaderEnum.CMPAZ));
        double dCmpaz = Math.abs(cmpazE - cmpazN);
        if (dCmpaz != 90) return false;

        // sacを開く
        try (SAC sacProcess = SAC.createProcess()) {
            Path rPath = outputRPath.getFileName();
            Path tPath = outputTPath.getFileName();
            sacProcess.inputCMD("cd " + sacNPath.toAbsolutePath().getParent());
            sacProcess.inputCMD("r " + sacNPath.getFileName() + " " + sacEPath.getFileName());
            sacProcess.inputCMD("rotate r");
            sacProcess.inputCMD("w " + rPath + " " + tPath);
            sacProcess.inputCMD("r " + rPath);
            sacProcess.inputCMD("chnhdr kcmpnm \"radial\"");
            sacProcess.inputCMD("write over");
            sacProcess.inputCMD("r " + tPath);
            sacProcess.inputCMD("chnhdr kcmpnm \"trnsvers\"");
            sacProcess.inputCMD("write over");
        }
        return true;

    }

    /**
     * @param outPath   for output (If the file exists, it will be overwritten)
     * @param headerMap of output SAC
     * @param data      of output SAC. The length must be same as NPTS.
     * @param options   Options for output
     * @throws IOException if an I/O error occurs
     */
    public static void writeSAC(Path outPath, Map<SACHeaderEnum, String> headerMap, double[] data,
                                OpenOption... options) throws IOException {
        if (Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS)) != data.length)
            throw new IllegalArgumentException("NPTS is invalid");
        try (SACOutputStream stream = new SACOutputStream(
                new BufferedOutputStream(Files.newOutputStream(outPath, options)))) {
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.DELTA))); // 0
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.DEPMIN))); // 1
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.DEPMAX))); // 2
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.SCALE))); // 3
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.ODELTA))); // 4
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.B))); // 5
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.E))); // 6
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.O))); // 7
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.A))); // 8
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num9))); // 9
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T0))); // 10
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T1))); // 11
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T2))); // 12
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T3))); // 13
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T4))); // 14
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T5))); // 15
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T6))); // 16
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T7))); // 17
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T8))); // 18
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.T9))); // 19
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.F))); // 20
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP0))); // 21
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP1))); // 22
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP2))); // 23
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP3))); // 24
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP4))); // 25
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP5))); // 26
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP6))); // 27
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP7))); // 28
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP8))); // 29
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.RESP9))); // 30
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.STLA))); // 31
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.STLO))); // 32
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.STEL))); // 33
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.STDP))); // 34
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.EVLA))); // 35
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.EVLO))); // 36
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.EVEL))); // 37
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.EVDP))); // 38
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.MAG))); // 39
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER0))); // 40
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER1))); // 41
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER2))); // 42
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER3))); // 43
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER4))); // 44
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER5))); // 45
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER6))); // 46
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER7))); // 47
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER8))); // 48
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.USER9))); // 49
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.DIST))); // 50
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.AZ))); // 51
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.BAZ))); // 52
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.GCARC))); // 53
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num54))); // 54
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num55))); // 55
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.DEPMEN))); // 56
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.CMPAZ))); // 57
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.CMPINC))); // 58
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.XMINIMUM))); // 59
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.XMAXIMUM))); // 60
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.YMINIMUM))); // 61
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.YMAXIMUM))); // 62
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num63))); // 63
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num64))); // 64
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num65))); // 65
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num66))); // 66
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num67))); // 67
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num68))); // 68
            stream.writeSACDouble(Double.parseDouble(headerMap.get(SACHeaderEnum.num69))); // 69
            // / int
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NZYEAR))); // 70
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NZJDAY))); // 71
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NZHOUR))); // 72
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NZMIN))); // 73
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NZSEC))); // 74
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NZMSEC))); // 75
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NVHDR))); // 76
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NORID))); // 77
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NEVID))); // 78
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NPTS))); // 79
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num80))); // 80
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NWFID))); // 81
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NXSIZE))); // 82
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.NYSIZE))); // 83
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num84))); // 84
            // /////////enumerized
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IFTYPE))); // 85
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IDEP))); // 86
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IZTYPE))); // 87
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num88))); // 88
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IINST))); // 89
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.ISTREG))); // 90
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IEVREG))); // 91
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IEVTYP))); // 92
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IQUAL))); // 93
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.ISYNTH))); // 94
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IMAGTYP))); // 95
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.IMAGSRC))); // 96
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num97))); // 97
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num98))); // 98
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num99))); // 99
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num100))); // 100
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num101))); // 101
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num102))); // 102
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num103))); // 103
            stream.writeSACInt(Integer.parseInt(headerMap.get(SACHeaderEnum.num104))); // 104
            // / BOOLEAN
            stream.writeSACBoolean(Boolean.parseBoolean(headerMap.get(SACHeaderEnum.LEVEN))); // 105
            stream.writeSACBoolean(Boolean.parseBoolean(headerMap.get(SACHeaderEnum.LPSPOL))); // 106
            stream.writeSACBoolean(Boolean.parseBoolean(headerMap.get(SACHeaderEnum.LOVROK))); // 107
            stream.writeSACBoolean(Boolean.parseBoolean(headerMap.get(SACHeaderEnum.LCALDA))); // 108
            stream.writeSACBoolean(Boolean.parseBoolean(headerMap.get(SACHeaderEnum.num109))); // 109
            // / String
            stream.writeSACString(headerMap.get(SACHeaderEnum.KSTNM), 8); // 110-111
            stream.writeSACString(headerMap.get(SACHeaderEnum.KEVNM), 16); // 112-115
            stream.writeSACString(headerMap.get(SACHeaderEnum.KHOLE), 8); // 116-117
            stream.writeSACString(headerMap.get(SACHeaderEnum.KO), 8); // 118-119
            stream.writeSACString(headerMap.get(SACHeaderEnum.KA), 8); // 120-121
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT0), 8); // 122-123
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT1), 8); // 124-125
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT2), 8); // 126-127
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT3), 8); // 128-129
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT4), 8); // 130-131
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT5), 8); // 132-133
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT6), 8); // 134-135
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT7), 8); // 136-137
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT8), 8); // 138-139
            stream.writeSACString(headerMap.get(SACHeaderEnum.KT9), 8); // 140-141
            stream.writeSACString(headerMap.get(SACHeaderEnum.KF), 8); // 142-143
            stream.writeSACString(headerMap.get(SACHeaderEnum.KUSER0), 8); // 144-145
            stream.writeSACString(headerMap.get(SACHeaderEnum.KUSER1), 8); // 146-147
            stream.writeSACString(headerMap.get(SACHeaderEnum.KUSER2), 8); // 148-149
            stream.writeSACString(headerMap.get(SACHeaderEnum.KCMPNM), 8); // 150-151
            stream.writeSACString(headerMap.get(SACHeaderEnum.KNETWK), 8); // 152-153
            stream.writeSACString(headerMap.get(SACHeaderEnum.KDATRD), 8); // 154-155
            stream.writeSACString(headerMap.get(SACHeaderEnum.KINST), 8); // 156-157

            for (double d : data) stream.writeSACDouble(d);
        }

    }

    /**
     * The trace is DEEP copied.
     *
     * @param sacPath to extract waveform from
     * @return DEEP copied {@link Trace} from waveform in the sacFile
     * @throws IOException If sacFile does not exist, if an I/O error occurs.
     */
    public static Trace createTrace(Path sacPath) throws IOException {
        Map<SACHeaderEnum, String> header = readHeader(sacPath);
        double[] timeAxis = new double[Integer.parseInt(header.get(SACHeaderEnum.NPTS))];
        double delta = Double.parseDouble(header.get(SACHeaderEnum.DELTA));
        double b = Double.parseDouble(header.get(SACHeaderEnum.B));
        Arrays.parallelSetAll(timeAxis, i -> i * delta + b);
        return new Trace(timeAxis, readSACData(sacPath));
    }

    /**
     * Read header values in a sacFile.
     *
     * @param sacPath to read
     * @return {@link Map} of {@link SACHeader} in the file
     * @throws IOException If sacFile does not exist, if an I/O error occurs.
     */
    public static Map<SACHeaderEnum, String> readHeader(Path sacPath) throws IOException {
        Map<SACHeaderEnum, String> headerMap = new EnumMap<>(SACHeaderEnum.class);
        try (SACInputStream stream = new SACInputStream(sacPath)) {
            headerMap.put(SACHeaderEnum.DELTA, Float.toString(stream.readFloat())); // 0
            headerMap.put(SACHeaderEnum.DEPMIN, Float.toString(stream.readFloat())); // 1
            headerMap.put(SACHeaderEnum.DEPMAX, Float.toString(stream.readFloat())); // 2
            headerMap.put(SACHeaderEnum.SCALE, Float.toString(stream.readFloat())); // 3
            headerMap.put(SACHeaderEnum.ODELTA, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.B, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.E, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.O, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.A, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num9, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T0, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T1, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T2, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T3, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T4, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T5, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T6, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T7, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T8, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.T9, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.F, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP0, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP1, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP2, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP3, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP4, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP5, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP6, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP7, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP8, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.RESP9, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.STLA, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.STLO, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.STEL, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.STDP, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.EVLA, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.EVLO, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.EVEL, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.EVDP, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.MAG, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER0, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER1, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER2, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER3, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER4, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER5, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER6, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER7, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER8, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.USER9, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.DIST, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.AZ, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.BAZ, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.GCARC, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num54, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num55, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.DEPMEN, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.CMPAZ, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.CMPINC, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.XMINIMUM, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.XMAXIMUM, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.YMINIMUM, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.YMAXIMUM, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num63, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num64, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num65, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num66, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num67, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num68, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.num69, Float.toString(stream.readFloat()));
            headerMap.put(SACHeaderEnum.NZYEAR, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NZJDAY, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NZHOUR, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NZMIN, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NZSEC, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NZMSEC, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NVHDR, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NORID, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NEVID, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NPTS, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num80, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NWFID, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NXSIZE, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.NYSIZE, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num84, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IFTYPE, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IDEP, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IZTYPE, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num88, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IINST, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.ISTREG, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IEVREG, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IEVTYP, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IQUAL, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.ISYNTH, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IMAGTYP, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.IMAGSRC, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num97, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num98, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num99, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num100, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num101, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num102, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num103, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.num104, Integer.toString(stream.readInt()));
            headerMap.put(SACHeaderEnum.LEVEN, Boolean.toString(stream.readSACBoolean()));
            headerMap.put(SACHeaderEnum.LPSPOL, Boolean.toString(stream.readSACBoolean()));
            headerMap.put(SACHeaderEnum.LOVROK, Boolean.toString(stream.readSACBoolean()));
            headerMap.put(SACHeaderEnum.LCALDA, Boolean.toString(stream.readSACBoolean()));
            headerMap.put(SACHeaderEnum.num109, Boolean.toString(stream.readSACBoolean()));
            headerMap.put(SACHeaderEnum.KSTNM, stream.readString(8));
            headerMap.put(SACHeaderEnum.KEVNM, stream.readString(16));
            headerMap.put(SACHeaderEnum.KHOLE, stream.readString(8));
            headerMap.put(SACHeaderEnum.KO, stream.readString(8));
            headerMap.put(SACHeaderEnum.KA, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT0, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT1, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT2, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT3, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT4, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT5, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT6, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT7, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT8, stream.readString(8));
            headerMap.put(SACHeaderEnum.KT9, stream.readString(8));
            headerMap.put(SACHeaderEnum.KF, stream.readString(8));
            headerMap.put(SACHeaderEnum.KUSER0, stream.readString(8));
            headerMap.put(SACHeaderEnum.KUSER1, stream.readString(8));
            headerMap.put(SACHeaderEnum.KUSER2, stream.readString(8));
            headerMap.put(SACHeaderEnum.KCMPNM, stream.readString(8));
            headerMap.put(SACHeaderEnum.KNETWK, stream.readString(8));
            headerMap.put(SACHeaderEnum.KDATRD, stream.readString(8));
            headerMap.put(SACHeaderEnum.KINST, stream.readString(8));

        }

        return headerMap;
    }

    /**
     * Waveform in sac file.
     *
     * @param sacPath {@link Path} for a sac file
     * @return double[] of waveform data
     * @throws IOException if sacPath does not exist or if an I/O error occurs
     */
    public static double[] readSACData(Path sacPath) throws IOException {
        try (SACInputStream stream = new SACInputStream(sacPath)) {
            stream.skipBytes(79 * 4);
            int npts = stream.readInt();
            double[] data = new double[npts];
            stream.skipBytes(632 - 80 * 4);
            // stream.skipBytes(632);
            // float(4) * 70, int(4) * 40, String (8) * 22 + (16)
            // 4* 70 + 4* 40 + 8* 22 +16 = 632
            for (int i = 0; i < npts; i++)
                data[i] = stream.readFloat();
            return data;
        }

    }

}
