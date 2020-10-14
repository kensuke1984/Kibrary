package io.github.kensuke1984.anisotime;

/**
 * @author Kensuke Konishi
 * @version 0.0.1b
 */
interface Located extends PathPart {

    Located EMISSION = new Located() {

        @Override
        public PassPoint getPassPoint() {
            return PassPoint.SEISMIC_SOURCE;
        }

        @Override
        public boolean isEmission() {
            return true;
        }

        @Override
        public String toString() {
            return "EMISSION at the seismic source";
        }
    };

    /**
     * @return where the PassPart locates
     */
    PassPoint getPassPoint();

    /**
     * reflection in such as PP, SS... bottomside reflection at the surface.
     */
    Located SURFACE_REFLECTION = new Located() {

        @Override
        public String toString() {
            return "BOTTOMSIDE REFLECTION at the Earth's surface";
        }


        @Override
        public PassPoint getPassPoint() {
            return PassPoint.EARTH_SURFACE;
        }


        @Override
        public boolean isBottomsideReflection() {
            return true;
        }


    };
    /**
     * penetration at CMB
     */
    Located CMB_PENETRATION = new Located() {


        @Override
        public PassPoint getPassPoint() {
            return PassPoint.CMB;
        }


        @Override
        public String toString() {
            return "PENETRATION at CMB";
        }

        @Override
        public boolean isPenetration() {
            return true;
        }
    };

    /**
     * penetration at ICB
     */
    Located ICB_PENETRATION = new Located() {


        @Override
        public PassPoint getPassPoint() {
            return PassPoint.ICB;
        }


        @Override
        public String toString() {
            return "PENETRATION at ICB";
        }

        @Override
        public boolean isPenetration() {
            return true;
        }
    };
    /**
     * means *K)<b>i</b>(K*, topside reflection at ICB.
     */
    Located OUTERCORE_SIDE_REFLECTION = new Located() {


        @Override
        public String toString() {
            return "TOPSIDE REFLECTION at ICB";
        }


        @Override
        public PassPoint getPassPoint() {
            return PassPoint.ICB;
        }


        @Override
        public boolean isTopsideReflection() {
            return true;
        }

    };
    /**
     * means II, topside reflection at ICB.
     */
    Located INNERCORE_SIDE_REFLECTION = new Located() {


        @Override
        public String toString() {
            return "BOTTOMSIDE REFLECTION at ICB";
        }


        @Override
        public PassPoint getPassPoint() {
            return PassPoint.ICB;
        }


        @Override
        public boolean isBottomsideReflection() {
            return true;
        }

    };
    /**
     * means *)<b>c</b>(*, topside reflection at CMB.
     */
    Located REFLECTION_C = new Located() {

        @Override
        public String toString() {
            return "TOPSIDE REFLECTION at CMB";
        }

        @Override
        public PassPoint getPassPoint() {
            return PassPoint.CMB;
        }


        @Override
        public boolean isTopsideReflection() {
            return true;
        }

    };
    /**
     * means *)<b>KK</b>(*, bottomside reflection at CMB.
     */
    Located REFLECTION_K = new Located() {
        @Override
        public PassPoint getPassPoint() {
            return PassPoint.CMB;
        }

        @Override
        public String toString() {
            return "BOTTOMSIDE REFLECTION at CMB";
        }

        @Override
        public boolean isBottomsideReflection() {
            return true;
        }
    };

    Located BOUNCE = new Located() {
        @Override
        public boolean isBounce() {
            return true;
        }

        @Override
        public PassPoint getPassPoint() {
            return PassPoint.BOUNCE_POINT;
        }

        @Override
        public String toString() {
            return "BOUNCE";
        }

    };


}
