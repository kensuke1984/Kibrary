/**
 * 
 */
package io.github.kensuke1984.kibrary.misc;


import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.sun.corba.se.spi.orb.Operation;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.inversion.InverseMethodEnum;
import io.github.kensuke1984.kibrary.inversion.InversionResult;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;

/**
 * @version 0.0.1
 * @since 2017/01/20
 * @author Yuki
 *
 */
public class BornCreate {
	
	static InverseMethodEnum CG = InverseMethodEnum.CONJUGATE_GRADIENT;
//	static InverseMethodEnum SVD = InverseMethodEnum.SVD;
	
	public static void main(String[] args) {
		Thread thread = new MyThread();
		thread.start();
		BornCreate bc = new BornCreate();
//		bc.run();		
	}
	
	static class MyThread extends Thread {
//        @Override
        public void run() {
        	try {
        		Path outPath = Paths.get("");
        		File outDir = new File(outPath.toString());
        		if (!outDir.exists()){
        			outDir.mkdirs();
        			File parDir = new File(outDir, CG.toString());
        			parDir.mkdir();
        		}		
        		
				InversionResult ir = new InversionResult(Paths.get(""));
				int max = ir.getUnknownParameterList().size();
				for (int i=20; i<50; i++) {
					ir.createBorn(CG, i);
				}
			} catch (IOException e) {
				// TODO 自動生成された catch ブロック
				e.printStackTrace();
			}
        }
	}   

}
