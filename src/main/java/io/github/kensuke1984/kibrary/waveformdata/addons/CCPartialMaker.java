package io.github.kensuke1984.kibrary.waveformdata.addons;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.apache.commons.math3.complex.Complex;
import org.apache.commons.math3.transform.DftNormalization;
import org.apache.commons.math3.transform.FastFourierTransformer;
import org.apache.commons.math3.transform.TransformType;

import io.github.kensuke1984.kibrary.inversion.Physical3DParameter;
import io.github.kensuke1984.kibrary.util.Location;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Trace;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.WaveformType;
import io.github.kensuke1984.kibrary.util.spc.PartialType;
import io.github.kensuke1984.kibrary.waveformdata.BasicID;
import io.github.kensuke1984.kibrary.waveformdata.BasicIDFile;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

public class CCPartialMaker {

	Path waveformPath;
	
	Path waveformIDPath;
	
	Path partialPath;
	
	Path partialIDPath;
	
	BasicID[] waveforms;
	
	PartialID[] partials;
	
	List<PartialID> partialsI;
	
	List<PartialID> partialsJ;
	
	double[] uI;
	
	double[] uJ;
	
	public static void main(String[] args) {
		Path waveformIDPath = Paths.get(args[0]);
		Path waveformPath = Paths.get(args[1]);
		Path partialIDPath = Paths.get(args[2]);
		Path partialPath = Paths.get(args[3]);
		
		CCPartialMaker ccpartialmaker = new CCPartialMaker(waveformIDPath, waveformPath, partialIDPath, partialPath);
		
		ccpartialmaker.run();
		
		ccpartialmaker.writePartials(Paths.get("."));
	}
	
	public CCPartialMaker(Path waveformIDPath, Path waveformPath, Path partialIDPath, Path partialPath) {
		this.waveformIDPath = waveformIDPath;
		this.waveformPath = waveformPath;
		this.partialIDPath = partialIDPath;
		this.partialPath = partialPath;
		
		set();
	}
	
	private void set() {
		try {
			waveforms = BasicIDFile.read(waveformIDPath, waveformPath);
			waveforms = Arrays.stream(waveforms).filter(bid -> bid.getWaveformType().equals(WaveformType.SYN)).collect(Collectors.toList()).toArray(new BasicID[0]);
			partials = PartialIDFile.read(partialIDPath, partialPath);
			
			ccPartials = new ArrayList<>();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void run() {
		Set<GlobalCMTID> events = Arrays.stream(waveforms).parallel()
				.map(bid -> bid.getGlobalCMTID()).collect(Collectors.toSet());
		
		for (GlobalCMTID event : events) {
			List<BasicID> eventWaveforms = Arrays.stream(waveforms).filter(bid -> bid.getGlobalCMTID().equals(event)).collect(Collectors.toList());
			List<Station> eventStations = Arrays.stream(waveforms).filter(bid -> bid.getGlobalCMTID().equals(event)).map(bid -> bid.getStation()).collect(Collectors.toList());
			
			List<PartialID> eventPartials = Arrays.stream(partials).filter(pid -> pid.getGlobalCMTID().equals(event)).collect(Collectors.toList());
			
			for (int ista = 0; ista < eventStations.size(); ista++) {
				Station staI = eventStations.get(ista);
				uI = eventWaveforms.stream().filter(bid -> bid.getStation().equals(staI)).findFirst().get().getData();
				
				partialsI = eventPartials.stream().filter(pid -> pid.getStation().equals(staI)).collect(Collectors.toList());
				
				for (int jsta = ista; jsta < eventStations.size(); jsta++) {
					Station staJ = eventStations.get(jsta);
					uJ = eventWaveforms.stream().filter(bid -> bid.getStation().equals(staJ)).findFirst().get().getData();
					
					partialsJ = eventPartials.stream().filter(pid -> pid.getStation().equals(staJ)).collect(Collectors.toList());
					
					// run in parallel
					System.err.println("Working for " + event + " " + staI + " " + staJ + "...");
					
					int N_THREADS = Runtime.getRuntime().availableProcessors();
					ExecutorService execs = Executors.newFixedThreadPool(N_THREADS);
					List<Callable<Object>> todo = new ArrayList<Callable<Object>>();
					
					for (int iUnknown = 0; iUnknown < partialsI.size(); iUnknown++) {
						if (!partialsI.get(iUnknown).getPerturbationLocation().equals(partialsJ.get(iUnknown).getPerturbationLocation()))
							throw new RuntimeException("Location mismatch");
						todo.add(Executors.callable(new Worker(staI, staJ, iUnknown)));
					}
					
					try {
						System.err.println("Computing " + todo.size() + " tasks");
						long t1i = System.currentTimeMillis();
						List<Future<Object>> answers = execs.invokeAll(todo);
						long t1f = System.currentTimeMillis();
						System.err.println("Completed in " + (t1f-t1i)*1e-3 + " s");
						todo = new ArrayList<>();
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					
					execs.shutdown();
					while (!execs.isTerminated()) {
						try {
							Thread.sleep(100);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					
				}
			}
			
		}
	}
	
	public void writePartials(Path rootPath) {
		System.err.println("Writing partials...");
		
		Path dir = rootPath.resolve("CC_PARTIALS");
		try {
			Files.createDirectory(dir);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		List<Station> stations = Arrays.stream(waveforms).map(bid -> bid.getStation()).collect(Collectors.toList());
		for (int ista = 0; ista < stations.size(); ista++) {
			Station staI = stations.get(ista);
			List<CCPartial> ccParI = ccPartials.parallelStream().filter(ccpar -> ccpar.staI.equals(staI)).collect(Collectors.toList());
			for (int jsta = ista; jsta < stations.size(); jsta++) {
				Station staJ = stations.get(jsta);
				List<CCPartial> ccParIJ = ccParI.parallelStream().filter(ccpar -> ccpar.staJ.equals(staJ)).collect(Collectors.toList());
				
				Path outpath = dir.resolve("partial_"  + staI.getName()
				+ "_" + staJ.getName() + ".txt");
				PrintWriter pw;
				try {
					pw = new PrintWriter(outpath.toFile());
					for (int iunk = 0; iunk < ccParIJ.size(); iunk++) {
						Trace partialCorr = ccParIJ.get(iunk).trace;
						Location loc = ccParIJ.get(iunk).unknownParameter.getLocation();
						PartialType type = ccParIJ.get(iunk).unknownParameter.getPartialType();
						pw.print(type + " " + loc + " ");
						for (int it = 0; it < partialCorr.getLength(); it++) {
							pw.print(partialCorr.getYAt(it) + " ");
						}
						pw.println();
					}
					pw.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				}
				
			}
		}
		
		System.err.println("Done!");
	}
	
	
	public class CCPartial {
		Station staI;
		
		Station staJ;
		
		Physical3DParameter unknownParameter;
		
		Trace trace;
		
		public CCPartial(Station staI, Station staJ, Physical3DParameter unknownParameter, Trace trace) {
			this.staI = staI;
			this.staJ = staJ;
			this.unknownParameter = unknownParameter;
			this.trace = trace;
		}
	}
	
	List<CCPartial> ccPartials;
	
	public class Worker implements Runnable {
		
		protected final FastFourierTransformer fft = new FastFourierTransformer(DftNormalization.STANDARD);
		
		int nn;
		
		int np;
		
		int n;
		
		Station staI;
		
		Station staJ;
		
		int iUnknown;
		
		public Worker(Station staI, Station staJ, int iUnknown) {
			this.staI = staI;
			this.staJ = staJ;
			this.iUnknown = iUnknown;
			
			// set length for fft
			n = uI.length;
			nn = Integer.highestOneBit(n);
			if (nn < n)
				nn *= 2;
			
			np = nn;
		}
		
//		@Override
//		public void run() {
//			Complex[] ufI = forwardFourierTransform(uI);
//			Complex[] ufJ = forwardFourierTransform(uJ);
//				
//			Complex[] pfI = forwardFourierTransform();
//			Complex[] pfJ = forwardFourierTransform(partialsJ.get(iUnknown).getData());
//			
//			Complex[] ccparf = new Complex[np];
//			for (int ip = 0; ip < np; ip++) {
//				ccparf[ip] = ufJ[ip].multiply(pfI[ip].conjugate())
//						.add( ufI[ip].conjugate().multiply(pfJ[ip]) );
//			}
//			
//			double[] ccpar = inverseFourierTransform(ccparf);
//			
//			Physical3DParameter unknownParameter = new Physical3DParameter(partialsI.get(iUnknown).getPartialType(),
//					partialsI.get(iUnknown).getPerturbationLocation(), 50.);
//			CCPartial tmp = new CCPartial(staI, staJ, unknownParameter, ccpar);
//			
//			synchronized (ccPartials) {
//				ccPartials.add(tmp);
//			}
//				
//		}
		
		@Override
		public void run() {
			double[] data = new double[2 * n + 1];
			double[] xs = new double[2 * n + 1];
			
			double[] pI = partialsI.get(iUnknown).getData();
			double[] pJ = partialsJ.get(iUnknown).getData();
			
			for (int it = 0; it < n; it++) {
				data[it + n] = 0;
				for (int jt = 0; jt < n - it; jt++) {
					data[it + n] += pI[jt] * uJ[it + jt] + uI[jt] * pJ[it + jt];
				}
				xs[it + n] = it;
			}
			
			for (int it = -n; it < 0; it++) {
				data[it + n] = 0;
				for (int jt = -it; jt < n; jt++) {
					data[it + n] += pI[jt] * uJ[it + jt] + uI[jt] * pJ[it + jt];
				}
				xs[it + n] = it;
			}
			
			Physical3DParameter unknownParameter = new Physical3DParameter(partialsI.get(iUnknown).getPartialType(),
					partialsI.get(iUnknown).getPerturbationLocation(), 50.);
			CCPartial tmp = new CCPartial(staI, staJ, unknownParameter, new Trace(xs, data));
			
			synchronized (ccPartials) {
				ccPartials.add(tmp);
			}
				
		}
		
		
		private Complex[] forwardFourierTransform(double[] ut) {
			double[] utn = Arrays.copyOf(ut, nn);
			
			//taper
			
			//fft
			Complex[] ufn = fft.transform(utn, TransformType.FORWARD);
			
			return ufn;
		}
		
		private double[] inverseFourierTransform(Complex[] uf) {
			//taper
			
			//fft
			Complex[] ut = fft.transform(uf, TransformType.INVERSE);
			
			double[] uut = new double[n];
			for (int i = 0; i < n; i++)
				uut[i] = ut[i].getReal();
			
			return uut;
		}
	}

}
