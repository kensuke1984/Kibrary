package io.github.kensuke1984.kibrary.quick;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import io.github.kensuke1984.anisotime.PolynomialStructure;

public class AnsotimeTest {

	public static void main(String[] args) {
		Path structurePath = Paths.get("/Users/Anselme/Dropbox/Kenji/anisoTimePaper/homogeneousmedia/homo.poly");
		try {
			io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure dsmStructure = new io.github.kensuke1984.kibrary.dsminformation.PolynomialStructure(structurePath);
			PolynomialStructure structure = new PolynomialStructure(dsmStructure);
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
