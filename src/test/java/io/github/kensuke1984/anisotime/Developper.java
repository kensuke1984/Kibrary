package io.github.kensuke1984.anisotime;

import io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class Developper {
    private Developper() {
    }

    private static void catalog() {
        Raypath raypath = new Raypath(680);
        System.out.println("track");
        double delta = Math.toDegrees(raypath.computeDelta(Phase.S, 6271));
        System.out.println(delta);
        Path path = Paths.get("/tmp/rayray");
        try (ObjectOutputStream o = new ObjectOutputStream(Files.newOutputStream(path))) {
            o.writeObject(raypath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        Raypath reray;
        try (ObjectInputStream oi = new ObjectInputStream(Files.newInputStream(path))) {
            reray =(Raypath) oi.readObject();
            System.out.println("maker");
            double delta1 = Math.toDegrees(reray.computeDelta(Phase.S,6271));
            System.out.println(delta1);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws IOException {
//        catalog();
        PolynomialStructure.PREM.writePSV(Paths.get("/tmp/prem.model"));
    }


}
