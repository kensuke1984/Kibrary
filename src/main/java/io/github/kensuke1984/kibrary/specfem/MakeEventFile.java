package io.github.kensuke1984.kibrary.specfem;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;

public class MakeEventFile {

	public static void main(String[] args) {
		GlobalCMTID event = new GlobalCMTID("101202H");
		String specfem_event_str = MakeRunFolder.cmtSolutionString(event);
		System.out.println(specfem_event_str);
	}

}
