package io.github.kensuke1984.kibrary.datarequest;

import io.github.kensuke1984.kibrary.Operation;
import io.github.kensuke1984.kibrary.Property;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * It makes a data requesting mail.
 *
 * @author Kensuke Konishi
 * @version 0.1.4
 */
public class DataRequestor implements Operation {

    // private String label;
    // private String[] alternateMedia;
    private String[] networks;
    private LocalDate startDate;
    private int headAdjustment;
    private int footAdjustment;

    private Path workPath;

    /**
     * including the date
     */
    private LocalDate endDate;
    private double lowerMw;
    private double upperMw;
    private double lowerLatitude;
    private double upperLatitude;
    private double lowerLongitude;
    private double upperLongitude;
    /**
     * not radius but distance from the surface
     */
    private double lowerDepth;
    /**
     * not radius but distance from the surface
     */
    private double upperDepth;
    private Set<GlobalCMTID> requestedIDs;
    private boolean send;
    private Properties property;
    private String date = Utilities.getTemporaryString();

    public DataRequestor(Properties property) {
        this.property = (Properties) property.clone();
        set();
    }

    /**
     * @param args Request Mode: [parameter file name]
     * @throws Exception file name
     */
    public static void main(String[] args) throws Exception {
        DataRequestor dr = new DataRequestor(Property.parse(args));
        dr.run();
    }

    private Path output(BreakFastMail mail) {
        Path out = workPath.resolve(mail.getLabel() + ".mail");
        try {
            Files.write(out, Arrays.asList(mail.getLines()));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return out;
    }

    public static void writeDefaultPropertiesFile() throws IOException {
        Path outPath = Paths.get(DataRequestor.class.getName() + Utilities.getTemporaryString() + ".properties");
        try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE_NEW))) {
            pw.println("manhattan DataRequestor");
            pw.println("##Path of a work folder (.)");
            pw.println("#workPath");
            pw.println("##Network names for request, must be defined");
            pw.println("##Note that it will make a request for all stations in the networks.");
            pw.println("#networks II IU _US-All");
            pw.println("##Starting date yyyy-mm-dd, must be defined");
            pw.println("#startDate 1990-01-01");
            pw.println("##End date yyyy-mm-dd, must be defined");
            pw.println("#endDate 2014-12-31");
            pw.println("##Lower limit of Mw (5.5)");
            pw.println("#lowerMw");
            pw.println("##Upper limit of Mw (6.5)");
            pw.println("#upperMw");
            pw.println("#All geometrical filter is for seismic events. (-90)");
            pw.println("#Lower limit of latitude [deg] [-90:upperLatitude)");
            pw.println("#lowerLatitude");
            pw.println("##Upper limit of latitude [deg] (lowerLatitude:90] (90)");
            pw.println("#upperLatitude");
            pw.println("##Lower limit of longitude [deg] [-180:upperLongitude) (-180)");
            pw.println("#lowerLongitude");
            pw.println("##Upper limit of longitude [deg] (lowerLongitude:360] (180)");
            pw.println("#upperLongitude");
            pw.println("##Shallower limit of DEPTH (100)");
            pw.println("#lowerDepth");
            pw.println("##Deeper limit of DEPTH (700)");
            pw.println("#upperDepth");
            pw.println("##Adjustment at the head [min], must be integer and defined");
            pw.println("#headAdjustment -10");
            pw.println("##Adjustment at the foot [min], must be integer and defined");
            pw.println("#footAdjustment 120");
            pw.println("##If you want to send emails, then set it true (false)");
            pw.println("#send");
        }
        System.err.println(outPath + " is created.");
    }

    private void checkAndPutDefaults() {
        if (!property.containsKey("workPath")) property.setProperty("workPath", "");
        if (!property.containsKey("networks")) throw new RuntimeException("No information about networks");
        if (!property.containsKey("lowerDepth")) property.setProperty("lowerDepth", "100");
        if (!property.containsKey("upperDepth")) property.setProperty("upperDepth", "700");
        if (!property.containsKey("lowerLatitude")) property.setProperty("lowerLatitude", "-90");
        if (!property.containsKey("upperLatitude")) property.setProperty("upperLatitude", "90");
        if (!property.containsKey("lowerLongitude")) property.setProperty("lowerLongitude", "-180");
        if (!property.containsKey("upperLonitude")) property.setProperty("upperLonitude", "180");
        if (!property.containsKey("lowerMw")) property.setProperty("lowerMw", "5.5");
        if (!property.containsKey("upperMw")) property.setProperty("upperMw", "6.5");
        if (!property.containsKey("startDate")) throw new RuntimeException("No information about the start date");
        if (!property.containsKey("endDate")) throw new RuntimeException("No information about the end date");
        if (!property.containsKey("footAdjustment"))
            throw new RuntimeException("No information about the foot adjustment");
        if (!property.containsKey("headAdjustment"))
            throw new RuntimeException("No information about the head adjustment");
        if (!property.containsKey("send")) property.setProperty("send", "false");
    }

    private void set() {
        checkAndPutDefaults();
        workPath = Paths.get(property.getProperty("workPath"));
        networks = property.getProperty("networks").split("\\s+");
        lowerDepth = Double.parseDouble(property.getProperty("lowerDepth"));
        lowerLatitude = Double.parseDouble(property.getProperty("lowerLatitude"));
        lowerLongitude = Double.parseDouble(property.getProperty("lowerLongitude"));
        lowerMw = Double.parseDouble(property.getProperty("lowerMw"));
        upperDepth = Double.parseDouble(property.getProperty("upperDepth"));
        upperLatitude = Double.parseDouble(property.getProperty("upperLatitude"));
        upperLongitude = Double.parseDouble(property.getProperty("upperLongitude"));
        upperMw = Double.parseDouble(property.getProperty("upperMw"));

        startDate = LocalDate.parse(property.getProperty("startDate"));
        endDate = LocalDate.parse(property.getProperty("endDate"));
        headAdjustment = Integer.parseInt(property.getProperty("headAdjustment"));
        footAdjustment = Integer.parseInt(property.getProperty("footAdjustment"));
        send = Boolean.parseBoolean(property.getProperty("send"));
    }

    /**
     * write a break fast mail for the input id
     *
     * @param id of {@link GlobalCMTID}
     * @return BreakFastMail for the id
     */
    public BreakFastMail createBreakFastMail(GlobalCMTID id) {
        Channel[] channels = Channel.listChannels(networks, id, ChronoUnit.MINUTES, headAdjustment, ChronoUnit.MINUTES,
                footAdjustment);
        return new BreakFastMail(id + "." + date, channels);
    }

    private Set<GlobalCMTID> listIDs() {
        GlobalCMTSearch search = new GlobalCMTSearch(startDate, endDate);
        search.setLatitudeRange(lowerLatitude, upperLatitude);
        search.setLongitudeRange(lowerLongitude, upperLongitude);
        search.setMwRange(lowerMw, upperMw);
        search.setDepthRange(lowerDepth, upperDepth);
        return search.search();
    }

    @Override
    public Path getWorkPath() {
        return workPath;
    }

    @Override
    public Properties getProperties() {
        return (Properties) property.clone();
    }

    @Override
    public void run() {
        requestedIDs = listIDs();
        System.out.println(requestedIDs.size() + " events are found.");
        System.out.println("Label contains \"" + date + "\"");
        requestedIDs.forEach(id -> output(createBreakFastMail(id)));
        Path sent = workPath.resolve("sent" + Utilities.getTemporaryString());
        if (send) try {
            System.err.println("Sending requests in 5 sec.");
            System.err.println("Sent mails will be in " + sent);
            Thread.sleep(1000 * 5);
        } catch (Exception e2) {
        }
        requestedIDs.forEach(id -> {
            BreakFastMail m = createBreakFastMail(id);
            try {
                Path out = output(m);
                if (!send) return;
                Files.createDirectories(sent);
                Files.move(out, sent.resolve(out.getFileName()));
                System.err.println("Sending a request for " + id);
                m.sendIris();
                Thread.sleep(300 * 1000);
            } catch (Exception e) {
                System.err.println(m.getLabel() + " was not sent");
                e.printStackTrace();
            }
        });
    }

}
