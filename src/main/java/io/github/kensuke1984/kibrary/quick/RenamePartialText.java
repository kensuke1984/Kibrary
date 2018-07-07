package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;


public class RenamePartialText {

	public static void main(String[] args) {
		Path inversionPath = Paths.get(".");
		
		try {
			InversionResult ir = new InversionResult(inversionPath);
			List<BasicID> ids = ir.getBasicIDList();
			Path partialDir = inversionPath.resolve("partial");
			for (int i = 0; i < ids.size(); i++) {
				String originalFileName = searchTxtName(ids.get(i), inversionPath);
				String newFileName = getTxtName(ids.get(i), i);
				File originalFile = partialDir.resolve(originalFileName).toFile();
				File newFile = partialDir.resolve(newFileName).toFile();
				
				System.out.println(originalFile + " " + newFile);
				
				originalFile.renameTo(newFile);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String searchTxtName(BasicID id, Path inversionDir) {
		File rootDir = inversionDir.resolve("partial/" + id.getGlobalCMTID().toString()).toFile();
		String rootFileName = id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent()
				+ "."; 
		File[] files = rootDir.listFiles(file -> file.getName().startsWith(rootFileName));
		if (files.length != 1)
			System.out.println("Found multiple files for " + rootFileName);
		return id.getGlobalCMTID().toString() + "/" + files[0].getName();
	}
	
	private static String getTxtName(BasicID id, int idIndex) {
		return id.getGlobalCMTID() + "/" + id.getStation() + "." + id.getGlobalCMTID() + "." + id.getSacComponent()
				+ "." + idIndex + ".txt";
	}

}
