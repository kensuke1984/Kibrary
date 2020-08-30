package io.github.kensuke1984.kibrary.util.globalcmt;

import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Location;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.function.Predicate;

/**
 * @author Kensuke Konishi
 * @version 0.0.1
 */

class GlobalCMTSearchTest {

    public static void main(String[] args) throws IOException {
        System.out.println(eventList().size());
//        findGlobalCMTID();
    }

    private static HorizontalPosition usCenter = new HorizontalPosition(40, -100);

    private static Set<GlobalCMTID> eventList() {
        return GlobalCMTSearch.search(d -> d.getCmtLocation().getEpicentralDistance(usCenter) < 80 &&
                d.getCMTTime().isAfter(LocalDate.of(2004, 1, 1).atTime(0, 0)) && d.getCmtLocation().getR() < 6361);
    }

    private static LocalDateTime toTime(String line) {
        String[] parts = line.split("\\s");
        String[] time = parts[3].split(":");
        return LocalDateTime.of(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]), Integer.parseInt(parts[2]),
                Integer.parseInt(time[0]), Integer.parseInt(time[1]), Integer.parseInt(time[2]));
    }

    private static Location toLocation(String line) {
        String[] parts = line.split("\\s");
        return new Location(Double.parseDouble(parts[5]), Double.parseDouble(parts[6]),
                6371 - Double.parseDouble(parts[4]));
    }

    private static boolean closeEnough(Location loc1, Location loc2) {
        return Math.abs(loc1.getR() - loc2.getR()) < 30 && Math.abs(loc1.getLatitude() - loc2.getLatitude()) < 30 &&
                Math.abs(loc1.getLongitude() - loc2.getLongitude()) < 30;
//return true;
    }

    private static GlobalCMTID findGlobalCMTID(LocalDateTime time, Location eventLoc) {
        GlobalCMTSearch globalCMTSearch = new GlobalCMTSearch(time.toLocalDate());
        Predicate<GlobalCMTData> predicate = d -> {
            if (d.getCMTTime().toLocalDate().getDayOfYear() != time.getDayOfYear() ||
                    d.getCMTTime().getYear() != time.getYear()) {
                return false;
            }
            System.out.println(
                    (time.toLocalTime().toSecondOfDay() + " " + d.getCMTTime().toLocalTime().toSecondOfDay()) + " " +
                            closeEnough(d.getCmtLocation(), eventLoc) + " " + Math.abs(
                            time.toLocalTime().toSecondOfDay() - d.getCMTTime().toLocalTime().toSecondOfDay()));
            return closeEnough(d.getCmtLocation(), eventLoc) &&
                    Math.abs(time.toLocalTime().toSecondOfDay() - d.getCMTTime().toLocalTime().toSecondOfDay()) < 100;
        };
//        Set<GlobalCMTID> search = GlobalCMTSearch.search(predicate);
//        Set<GlobalCMTID> search = globalCMTSearch.addPredicate(predicate).search();
        Set<GlobalCMTID> search = globalCMTSearch.addPredicate(d -> true).search();
        for (GlobalCMTID globalCMTID : search) {
//            System.out.println(globalCMTID);
        }
        System.out.println(search.size());
        if (search.size() != 1) return null;
        return search.stream().findAny().get();

    }



}

