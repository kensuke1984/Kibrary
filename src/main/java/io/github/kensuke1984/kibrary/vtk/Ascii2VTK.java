/**
 * 
 */
package io.github.kensuke1984.kibrary.vtk;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @version 0.0.1
 * @since 2018/08/29
 * @author Yuki
 *
 */
public class Ascii2VTK {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO 自動生成されたメソッド・スタブ
		Path outPath = Paths.get(args[0]);
		Path inputPath = Paths.get(args[1]);
		File outFile = new File(outPath.toString());
		File inputFIle = new File(inputPath.toString());
		
	}

}
