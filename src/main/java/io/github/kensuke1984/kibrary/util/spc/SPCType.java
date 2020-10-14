package io.github.kensuke1984.kibrary.util.spc;

/**
 * SpcFileの中身 par? (?:0 - 5, A, C, F, L, N, Q(Q<sup>-1</sup>)), PB, PF, SYNTHETIC
 *
 * @author Kensuke Konishi
 * @version 0.0.3.0.1
 * @author anselme add several partial types
 */
public enum SPCType {
	PAR0, PAR1, PAR2, PAR3, PAR4, PAR5, PARA, PARC, PARF, 
	PARL, PARN, PARQ, PB, PBSHCAT, PBPSVCAT, PF, PFSHCAT, 
	PFPSVCAT, PFSHO, SYNTHETIC, G1, G2, G3, G4, G5, G6,
	PARVS, PARVSIM;
}
