package io.github.kensuke1984.kibrary.util.addons;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;

public class Phases {
		public static void main(String[] args) {
			Set<TimewindowInformation> timewindows = null;
			if (args.length == 1) {
				try {
					timewindows = TimewindowInformationFile.read(Paths.get(args[0]));
				} catch (IOException e) {
					e.printStackTrace();
				}
				
				Map<Phases, Integer> phaseNumberMap = new HashMap<>();
				for (TimewindowInformation timewindow : timewindows) {
					Phases tmpPhase = new Phases(timewindow.getPhases());
					if (phaseNumberMap.containsKey(tmpPhase)) {
						Integer i = phaseNumberMap.get(tmpPhase) + 1;
						phaseNumberMap.replace(tmpPhase, i);
					}
					else {
						Integer i = 1;
						phaseNumberMap.put(tmpPhase, i);
					}
				}
				
				Path outPath = Paths.get("phaseTable.inf");
				try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outPath, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING))) {
					phaseNumberMap.forEach((phase, i) -> {
						pw.println(phase + " " + i);
					});
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
				
		}
		Phase[] phases;
		int length;
		public Phases(Phase[] phases) {
			this.phases = phases;
			this.length = phases.length;
		}
		public Phases(String phaseString) {
			this.phases = Stream.of(phaseString.trim().split("_")).map(phasename -> Phase.create(phasename))
					.toArray(Phase[]::new);
			this.length = phases.length;
		}
		@Override
		public int hashCode() {
			return length * 31;
		}
		public int intRepresentation() {
			int i = 0;
			for (Phase phase : phases)
				i += phase.hashCode();
			return i;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Phases other = (Phases) obj;
			if (length != other.length)
				return false;
			if (intRepresentation() != other.intRepresentation())
				return false;
			return true;
		}
		@Override
		public String toString() {
			String s = "";
			if (length == 0)
				return s;
			for (int i = 0; i < length - 1; i++)
				s += phases[i].toString() + "_";
			s += phases[length - 1];
			return s;
		}
		public Set<Phase> toSet() {
			return Stream.of(phases).collect(Collectors.toSet());
		}
		public boolean contains(Phase phase) {
			return Stream.of(phases).collect(Collectors.toSet()).contains(phase);
		}
		public boolean isLowerMantle() {
			if (this.equals(new Phases(new Phase[] {Phase.S})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.S, Phase.ScS})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.ScS})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("ScSScS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("ScSScSScS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("ScSScSScSScS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("Sdiff", false)})))
				return true;
			if (this.isMixte())
				return true;
			return false;
		}
		public boolean isMixte() {
			if (this.equals(new Phases(new Phase[] {Phase.create("sSdiff", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sS", false), Phase.create("sScS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sScS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sScSScS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sScSScSScS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sScSScSScSScS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sS", false), Phase.create("sScS", false), Phase.create("ScS", false), Phase.S})))
				return true;
			return false;
		}
		public boolean isUpperMantle() {
//			return !(isLowerMantle() || isMixte());
//			return !isLowerMantle();
			if (this.equals(new Phases(new Phase[] {Phase.create("SS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("SSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("SSSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("SSSSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sSSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sSSSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sSS", false), Phase.create("SS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sS", false), Phase.create("SS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sSSS", false), Phase.create("SSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sSSSS", false), Phase.create("SSSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sSS", false), Phase.create("SSS", false)})))
				return true;
			if (this.equals(new Phases(new Phase[] {Phase.create("sSSS", false), Phase.create("SSSS", false)})))
				return true;
			return false;
		}
}
