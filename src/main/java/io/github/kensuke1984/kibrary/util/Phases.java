package io.github.kensuke1984.kibrary.util;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github.kensuke1984.anisotime.Phase;

public class Phases {
		public static void main(String[] strings) {
			Phases phases = new Phases(new Phase[] {Phase.ScS});
			System.out.println(phases.isLowerMantle());
		}
		Phase[] phases;
		int length;
		public Phases(Phase[] phases) {
			this.phases = phases;
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
			return false;
		}
		public boolean isUpperMantle() {
//			return !(isLowerMantle() || isMixte());
			return !isLowerMantle();
		}
}
