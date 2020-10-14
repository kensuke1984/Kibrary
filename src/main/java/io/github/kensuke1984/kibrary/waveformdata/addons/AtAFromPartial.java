package io.github.kensuke1984.kibrary.waveformdata.addons;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.waveformdata.PartialID;
import io.github.kensuke1984.kibrary.waveformdata.PartialIDFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AtAFromPartial {

	public static void main(String[] args) {
		Path partialIDPath = Paths.get(args[0]);
		Path partialPath = Paths.get(args[1]);
		
		try {
			PartialID[] partialsNoData = PartialIDFile.read(partialIDPath);
			Set<GlobalCMTID> eventSet = Stream.of(partialsNoData).map(par -> par.getGlobalCMTID()).collect(Collectors.toSet());
			
			eventSet.stream().forEach(event -> {
				try {
					PartialIDFile.read(partialIDPath, partialPath, id -> id.getGlobalCMTID().equals(event));
					
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
