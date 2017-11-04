package io.github.kensuke1984.anisotime;

/**
 * @author Kensuke Konishi
 * @version 0.0.1b
 */
interface Arbitrary extends Located {
    double getDepth();

    @Override
    default PassPoint getPassPoint() {
        return PassPoint.OTHER;
    }

    static Arbitrary createTransmission(double depth) {
        return new Arbitrary() {
            @Override
            public double getDepth() {
                return depth;
            }

            @Override
            public String toString() {
                return "TRANSMISSION at " + depth;
            }

            @Override
            public boolean isTransmission() {
                return true;
            }

        };
    }

    static Arbitrary createTopsideReflection(double depth) {
        return new Arbitrary() {
            @Override
            public double getDepth() {
                return depth;
            }

            @Override
            public String toString() {
                return "TOPSIDE REFLECTION at " + depth;
            }

            @Override
            public boolean isTopsideReflection() {
                return true;
            }

        };
    }

    static Arbitrary createBottomsideReflection(double depth) {
        return new Arbitrary() {

            @Override
            public double getDepth() {
                return depth;
            }

            @Override
            public String toString() {
                return "BOTTOMSIDE REFLECTION at " + depth;
            }

            @Override
            public boolean isBottomsideReflection() {
                return true;
            }
        };
    }


}
