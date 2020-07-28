package io.github.kensuke1984.anisotime;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */
class PhaseTest {


    private static void transmitWaves(){
        Phase phase = Phase.create("s^220P");
        PathPart[] passParts = phase.getPassParts();
        for (PathPart passPart : passParts) {
            System.out.println(passPart);
        }
    }

    public static void main(String[] args) {
        transmitWaves();
    }
}
