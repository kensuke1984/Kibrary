package io.github.kensuke1984.anisotime;

/**
 * Modes of computation
 * <p>
 * RAY_PARAMETER, EPICENTRAL_DISTANCE
 *
 * @author Kensuke Konishi
 * @version 0.0.4
 */
enum ComputationMode {
    EPICENTRAL_DISTANCE, RAY_PARAMETER,;

    @Override
    public String toString() {
        switch (this) {
            case EPICENTRAL_DISTANCE:
                return "Mode:Epicentral Distance";
            case RAY_PARAMETER:
                return "Mode:Rayparameter";
            default:
                throw new RuntimeException("Error");
        }

    }
}
