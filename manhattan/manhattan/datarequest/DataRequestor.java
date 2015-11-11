/**
 * 
 */
package manhattan.datarequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Set;

import manhattan.globalcmt.GlobalCMTID;
import manhattan.globalcmt.GlobalCMTSearch;
import manhattan.template.Utilities;

/**
 * 
 * It makes a data requesting mail. Information: {@link parameter.DataRequestor}
 * 
 * @since 2015/02/09
 * @author kensuke
 * 
 * @version 0.0.4
 * 
 *
 */
class DataRequestor extends parameter.DataRequestor {

	/**
	 * @param args
	 *            Request Mode: [parameter file name]
	 */
	public static void main(String[] args) throws IOException {
		DataRequestor dr = null;
		if (args.length == 1) {
			Path parameterPath = Paths.get(args[0]);
			if (!Files.exists(parameterPath))
				throw new NoSuchFileException(args[0]);
			dr = new DataRequestor(parameterPath);
		} else if (args.length == 0)
			dr = new DataRequestor(null);
		else
			throw new IllegalArgumentException("Usage: [parameter file name]");
		dr.request();

	}

	private Set<GlobalCMTID> requestedIDs;

	private void request() {
		requestedIDs = listIDs();
		System.out.println(requestedIDs.size() + " events are found.");
		System.out.println("Label contains \"" + date + "\"");
		try {
			System.out.println("Sending requests in 10 sec");
			Thread.sleep(1000 * 10);
		} catch (Exception e2) {
		}
		requestedIDs.forEach(id -> {
			BreakFastMail m = createBreakFastMail(id);
			try {
				System.err.println("Sending a request for " + id);
				m.sendIris();
				Thread.sleep(300 * 1000);
			} catch (Exception e) {
				System.out.println(m.getLabel() + " was not sent");
				e.printStackTrace();
			}
			output(m);
		});
	}

	private static void output(BreakFastMail mail) {
		Path out = Paths.get(mail.getLabel() + ".mail");
		try {
			Files.write(out, Arrays.asList(mail.getLines()));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private DataRequestor() throws IOException {
		this(null);
	}

	private DataRequestor(Path parameterPath) throws IOException {
		super(parameterPath);
	}

	private String date = Utilities.getTemporaryString();

	/**
	 * output a break fast mail for the input id
	 * 
	 * @param id
	 *            of {@link GlobalCMTID}
	 */
	private BreakFastMail createBreakFastMail(GlobalCMTID id) {
		Channel[] channels = Channel.listChannels(networks, id, ChronoUnit.MINUTES, headAdjustment, ChronoUnit.MINUTES,
				footAdjustment);
		return new BreakFastMail(System.getProperty("user.name"), institute, mail, email, phone, fax,
				id.toString() + "." + date, media, channels);
	}

	private Set<GlobalCMTID> listIDs() {
		GlobalCMTSearch search = new GlobalCMTSearch(startDate, endDate);
		search.setLatitudeRange(lowerLatitude, upperLatitude);
		search.setLongitudeRange(lowerLongitude, upperLongitude);
		search.setMwRange(lowerMw, upperMw);
		search.setDepthRange(lowerDepth, upperDepth);
		return search.search();
	}

}
