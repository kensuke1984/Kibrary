package filehandling.spc;

/**
 * SpcFileの中身 par? (?:0 - 5, A, C, F, L, N), PB, PF
 * 
 * 
 * @since 2014/10/31
 * @version 0.0.2 {@link #PARQ} added.
 * 
 * @version 0.0.3
 * @since 2015/8/21
 * {@link #PAR0} added 
 * 
 * @author Kensuke
 * 
 */
public enum SpcFileType {
	PAR0, PAR1, PAR2, PAR3, PAR4, PAR5, PARA, PARC, PARF, PARL, PARN, PARQ, PB, PF, SYNTHETIC;
}
