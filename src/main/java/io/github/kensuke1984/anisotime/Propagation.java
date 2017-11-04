package io.github.kensuke1984.anisotime;

/**
 * How phases P, S(SV, SH), K, I and J(JV, JH) goes in Partitions
 * <p>
 * In the inner-core, I and J are PENETRATING if and only if a ray parameter = 0.
 *
 * @author Kensuke Konishi
 * @version 0.0.2
 */
enum Propagation {
    PENETRATING, BOUNCING, NOEXIST, DIFFRACTION,;
}
