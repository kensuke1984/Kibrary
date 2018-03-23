package io.github.kensuke1984.kibrary.datarequest;

import io.github.kensuke1984.kibrary.Environment;
import io.github.kensuke1984.kibrary.util.Utilities;
import org.apache.commons.net.ftp.*;

import java.io.BufferedOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

/**
 * FTP access to IRIS server. OHP will be prepared
 *
 * @author Kensuke Konishi
 * @version 0.0.1.1
 */
final class DataTransfer {

    /**
     * user PATH in IRIS
     */
    private static final String IRIS_USER_PATH = "/pub/userdata/" + Environment.getUserName() + "/";

    private DataTransfer() {
    }

    public static final String IRIS_FTP = "ftp.iris.washington.edu";

    private static void get(String date, Path outPath) {
        // create an FTPClient
        FTPClient ftpclient = new FTPClient();
        try {
            // connect
            ftpclient.connect(IRIS_FTP);
            int reply = ftpclient.getReplyCode();
            if (!FTPReply.isPositiveCompletion(reply)) throw new RuntimeException("connect fail");
            // login
            if (!ftpclient.login("anonymous", "password")) throw new RuntimeException("login fail");
            // passive mode
            ftpclient.enterLocalPassiveMode();
            // binary mode
            ftpclient.setFileType(FTP.BINARY_FILE_TYPE);
            // ftpclient.changeWorkingDirectory(userPath);
            FTPFileFilter fff = file -> date.equals("*") || date.equals("-c") ? file.getName().endsWith(".seed") :
                    file.getName().endsWith(".seed") && file.getName().contains(date);
            FTPFile[] ffiles = ftpclient.listFiles(IRIS_USER_PATH, fff);
            System.out.println(ffiles.length + " seed files are found in the server.");
            for (FTPFile f : ffiles)
                System.out.println(f);
            if (date.equals("-c")) return;
            System.out.println("Downloading in 10 s");
            Thread.sleep(10 * 1000);
            Files.createDirectories(outPath);
            for (FTPFile ffile : ffiles)
                try (BufferedOutputStream ostream = new BufferedOutputStream(
                        Files.newOutputStream(outPath.resolve(ffile.getName()), StandardOpenOption.CREATE_NEW))) {
                    // ファイル受信
                    System.out.println("receiving " + ffile.getName());
                    ftpclient.retrieveFile(IRIS_USER_PATH + "/" + ffile.getName(), ostream);
                    // System.out.println(m);
                } catch (Exception e) {
                    e.printStackTrace();
                }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (ftpclient.isConnected()) try {
                ftpclient.disconnect();
            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * @param args [option] [tag]<br>
     *             If option -c, then check the number of files in the server,
     *             else FTP [date string] to get seed files(*.seed) in
     *             (/pub/userdata/`USERNAME`/) with the `tag`. If "*" (you might
     *             need "\*"), then get all seed files in the folder. <br>
     */
    public static void main(String[] args) {
        if (args.length != 1) throw new IllegalArgumentException(
                "Usage:[-c](to check the number only) [tag] or [*] (may need be \\*) if all seeds you need");
        try {
            Path outPath = Paths.get("seedsTransferredAt" + Utilities.getTemporaryString());
            get(args[0], outPath);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

}
