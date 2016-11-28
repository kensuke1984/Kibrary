package io.github.kensuke1984.kibrary.util.statistics;

import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTSearch;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.stream.Collectors;


public class BobEQstatistics {
	public static void main(String[] args) throws IOException {
		int startYear = 1977;
		int endYear = 2015;
		int epochDay0 = (int) LocalDate.of(startYear, 1, 1).toEpochDay();
		int epochDayFinal = (int) LocalDate.of(endYear, 12, 31).toEpochDay();
		int epochDayMostRecent = (int) LocalDate.of(2016, 7, 31).toEpochDay();
		double[] daysInYearMw6 = new double[endYear - startYear + 1];
		double[] daysInYearMw6dot5 = new double[endYear - startYear + 1];
		double[] daysInYearMw7 = new double[endYear - startYear + 1];
		double[] past3YearsDaysByDaysMw6 = new double[epochDayMostRecent - epochDay0 + 1];
		int yearCount = 0;
		int currentYear = startYear;
		int dayCount = 0;
		for (int epochDay = epochDay0; epochDay <= epochDayFinal; epochDay++) {
			LocalDate date = LocalDate.ofEpochDay(epochDay);
			GlobalCMTSearch sea = new GlobalCMTSearch(date);
			sea.setMwRange(6.0, 11.0);
			Set<GlobalCMTID> ids = sea.search();
			Set<GlobalCMTID> idsMw6dot5 = ids.stream()
					.filter(id -> id.getEvent().getCmt().getMw() >= 6.5)
					.collect(Collectors.toSet());
			Set<GlobalCMTID> idsMw7 = ids.stream()
					.filter(id -> id.getEvent().getCmt().getMw() >= 7.)
					.collect(Collectors.toSet());
			if (ids.size() > 0) {
				daysInYearMw6[yearCount]++;
				past3YearsDaysByDaysMw6[dayCount] = 1;
			}
			if (idsMw6dot5.size() > 0)
				daysInYearMw6dot5[yearCount]++;
			if (idsMw7.size() > 0)
				daysInYearMw7[yearCount]++;
			if (LocalDate.ofEpochDay(epochDay).getYear() == currentYear + 1) {
				System.out.println(epochDay - epochDay0 + " " + currentYear);
				currentYear++;
				yearCount++;
			}
			dayCount++;
		}
		System.out.println(epochDayFinal - epochDay0 + " " + currentYear);
		for (int epochDay = epochDayFinal + 1; epochDay <= epochDayMostRecent; epochDay++) {
			LocalDate date = LocalDate.ofEpochDay(epochDay);
			GlobalCMTSearch sea = new GlobalCMTSearch(date);
			sea.setMwRange(6.0, 11.0);
			Set<GlobalCMTID> ids = sea.search();
			if (ids.size() > 0)
				past3YearsDaysByDaysMw6[dayCount] = 1;
			dayCount++;
		}
		int epochDay1979 = (int) LocalDate.of(1979, 1, 1).toEpochDay();
		int tmp = 0;
		yearCount = 0;
		double[] finalPast3YearsDaysByDaysMw6 = new double[epochDayMostRecent - epochDay1979 + 1];
		for (int epochDay = epochDay0; epochDay < epochDay1979; epochDay++) {
			tmp += past3YearsDaysByDaysMw6[epochDay];
		}
		for (int i = 0; i <= epochDayMostRecent - epochDay1979; i++) {
			finalPast3YearsDaysByDaysMw6[i] = tmp / 3.;
			tmp += past3YearsDaysByDaysMw6[i + epochDay1979 - epochDay0];
			tmp -= past3YearsDaysByDaysMw6[i];
		}
		Path root = Paths.get("/Users/anselme/Dropbox/Kenji/test");
		Path outpath = root.resolve("occurenceDaysInYear.txt");
		Path outpath2 = root.resolve("occurenceDaysByDays.txt");
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath
				, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
			pw.println("#year, 3daysMA MW>6, 3daysMA MW>6.5, 3daysMA MW>7");
			pw.println(startYear + " " + Double.NaN + " " + Double.NaN + " " + Double.NaN 
					+ " " + daysInYearMw6[0] + " " + daysInYearMw6dot5[0] + " " + daysInYearMw7[0]);
			int n = daysInYearMw6.length;
			for (int i = 1; i < n - 1; i++) {
				double ThreeDaysRAMw6 = (daysInYearMw6[i-1] + daysInYearMw6[i] + daysInYearMw6[i+1]) / 3.;
				double ThreeDaysRAMw7 = (daysInYearMw7[i-1] + daysInYearMw7[i] + daysInYearMw7[i+1]) / 3.;
				double ThreeDaysRAMw6dot5 = (daysInYearMw6dot5[i-1] + daysInYearMw6dot5[i] + daysInYearMw6dot5[i+1]) / 3.;
				double year = startYear + i;
				pw.println(year + " " + ThreeDaysRAMw6 + " " + ThreeDaysRAMw6dot5 + " " + ThreeDaysRAMw7 
						+ " " + daysInYearMw6[i] + " " + daysInYearMw6dot5[i] + " " + daysInYearMw7[i]);
			}
			pw.println(endYear + " " + Double.NaN + " " + Double.NaN + " " + Double.NaN 
					+ " " + daysInYearMw6[n-1] + " " + daysInYearMw6dot5[n-1] + " " + daysInYearMw7[n-1]);
		}
		try (PrintWriter pw = new PrintWriter(Files.newBufferedWriter(outpath2
				, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.CREATE))) {
			for (int i = 0; i < finalPast3YearsDaysByDaysMw6.length; i++) {
				LocalDate date = LocalDate.ofEpochDay(i+epochDay1979);
				String firstOfYear = "";
				if (date.getMonthValue() == 1 && date.getDayOfMonth() == 1 && date.getYear() % 5 == 0)
					firstOfYear = date.format(DateTimeFormatter.ofPattern("M/d/yyyy"));
				pw.println(i + " " + finalPast3YearsDaysByDaysMw6[i] + " " + firstOfYear);
			}
		}
	}
}
