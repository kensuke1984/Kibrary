package io.github.kensuke1984.anisotime;


import io.github.kensuke1984.kibrary.Environment;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;

class ANISOtimeTest {
    public static void main(String[] args) throws Exception {
        System.out.println(shouldCheckUpdate());
        args = "a b c d e".split(" ");
        System.out.println(Arrays.binarySearch(args, "e"));
//        ANISOtime.main("-mod prem -ph PKJKP -rs 180,200,1 -o /tmp/rctmp".split(" "));
//        System.out.println(Complex.class.getProtectionDomain().getCodeSource().getLocation());
    }

    private static boolean shouldCheckUpdate() throws IOException {
        Path last = Environment.KIBRARY_HOME.resolve(".anisotime_last_activation");
        if (!Files.exists(last)) {
            Files.createFile(last);
            return true;
        }
        FileTime lastModifiedTime = Files.getLastModifiedTime(last);
        LocalDate localDate = Instant.ofEpochMilli(lastModifiedTime.toMillis()).atZone(ZoneId.systemDefault()).toLocalDate();
        Files.setLastModifiedTime(last,FileTime.from(Instant.now()));
        return LocalDate.now().equals(localDate);
    }

}
