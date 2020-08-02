package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.util.Utilities;

import javax.swing.*;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;

class ANISOtimeTest {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException, URISyntaxException {
//        downloadANISOtime();
        ANISOtime.main("-mod prem -ph PKJKP -rs 180,200,1 -o /tmp/rctmp".split(" "));
//        System.out.println(Complex.class.getProtectionDomain().getCodeSource().getLocation());
    }


    private static void downloadANISOtime() throws IOException, NoSuchAlgorithmException, URISyntaxException {
        Path localPath = Paths.get(ANISOtime.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        if (Files.isDirectory(localPath))
            return;
        String localSum = Utilities.checksum(localPath, "SHA-256");
        Path path = Utilities.download(new URL("https://bit.ly/2Xdq5QI"));
        String cloudSum = Utilities.checksum(path, "SHA-256");
        if (localSum.equals(cloudSum))
            return;
        Files.move(path, Paths.get(System.getProperty("user.dir")).resolve("latest_anisotime"));
        JOptionPane.showMessageDialog(null, "Software update is found. ANISOtime restarts.");
        System.exit(55);
    }
}
