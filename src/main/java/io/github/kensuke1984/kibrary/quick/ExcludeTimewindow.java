package io.github.kensuke1984.kibrary.quick;

import io.github.kensuke1984.anisotime.Phase;
import io.github.kensuke1984.kibrary.inversion.StationInformationFile;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformation;
import io.github.kensuke1984.kibrary.timewindow.TimewindowInformationFile;
import io.github.kensuke1984.kibrary.util.HorizontalPosition;
import io.github.kensuke1984.kibrary.util.Station;
import io.github.kensuke1984.kibrary.util.Utilities;
import io.github.kensuke1984.kibrary.util.addons.EventCluster;
import io.github.kensuke1984.kibrary.util.addons.Phases;
import io.github.kensuke1984.kibrary.util.globalcmt.GlobalCMTID;
import io.github.kensuke1984.kibrary.util.sac.SACComponent;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ExcludeTimewindow {

	public static void main(String[] args) {
		Path timewindowFile = Paths.get(args[0]);
//		Path timewindowASCIIFile = Paths.get(args[1]);
		Path newTimewindowFile = Paths.get("timewindow" + Utilities.getTemporaryString() + ".dat");
		
//		Path timewindowFile = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_8-200s/timewindow_PPcP.dat");
		
		String tmpS = Utilities.getTemporaryString();
		Path newTimewindowFile1 = Paths.get("timewindow1_" + tmpS + ".dat");
		Path newTimewindowFile2 = Paths.get("timewindow2_" + tmpS + ".dat");
		
//		Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"201104170158A","200911141944A","201409241116A","200809031125A"
//				,"200707211327A","200808262100A","201009130715A","201106080306A","200608250044A","201509281528A","201205280507A"
//				,"200503211223A","201111221848A","200511091133A","201005241618A","200810122055A","200705251747A","201502111857A"
//				,"201206020752A","201502021049A","200506021056A","200511171926A","201101010956A","200707120523A","201109021347A"
//				,"200711180540A","201302221201A","200609220232A","200907120612A","201211221307A","200707211534A","200611130126A"
//				,"201208020938A","201203050746A","200512232147A"})
//				.map(GlobalCMTID::new)
//				.collect(Collectors.toSet());
		
		Set<GlobalCMTID> wellDefinedEvent = Stream.of(new String[] {"200809031125A","200909301903A","200911141944A",
				"201005241618A","201111221848A","201205280507A","201206020752A","201502111857A","201409241116A",
				"201104170158A","201307022004A","201509281528A","201007261731A","200707211534A","201203050746A",
				"201302221201A","200909301903A"})
				.map(GlobalCMTID::new)
				.collect(Collectors.toSet());
		
//		Path clusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg.inf");
		Path clusterPath = Paths.get("/work/anselme/CA_ANEL_NEW/VERTICAL/syntheticPREM_Q165/filtered_stf_12.5-200s/map/cluster-6deg_forSpecfemCorrections.inf");
		List<EventCluster> clusters = null;
		try {
			clusters = EventCluster.readClusterFile(clusterPath);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		int clusterIndex = 4;
		Set<GlobalCMTID> clusterIDs = clusters.stream().filter(c -> c.getIndex() == clusterIndex).map(EventCluster::getID)
			.collect(Collectors.toSet());
		
		EventCluster cluster = clusters.stream().filter(c -> c.getIndex() == clusterIndex).findFirst().get(); 
		
//		Path eachVarianceFile = Paths.get("eachVariance.txt");
//		List<String> stationNames = new ArrayList<>();
//		List<String> networkNames = new ArrayList<>();
//		List<GlobalCMTID> recordIDList = new ArrayList<>();
//		try {
//			BufferedReader br = Files.newBufferedReader(eachVarianceFile);
//			String line = "";
//			while ((line = br.readLine()) != null) {
//				String[] ss = line.split("\\s+");
//				stationNames.add(ss[1].split("_")[0]);
//				networkNames.add(ss[2]);
//				recordIDList.add(new GlobalCMTID(ss[3]));
//			}
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		System.out.println(recordIDList.size());
		
		try {
			Set<TimewindowInformation> timewindows = TimewindowInformationFile.read(timewindowFile);
//			Set<TimewindowInformation> newTimewindows = excludeStation(timewindows, timewindowASCIIFile);
			
//			Set<Phases> phaseSet = new HashSet<>();
//			phaseSet.add(new Phases("Sdiff"));
//			phaseSet.add(new Phases("S"));
//			Set<TimewindowInformation> newTimewindows = excludePhase(timewindows, phaseSet);

			
			String[] tmpStations = new String[] {"AT42_YB","PM29_XB","PS51_XB","PS22_XB","MIGL_IV","CRLT_KO","AYDB_KO","DC02_YH","LOD_KO","ZSAL_YD","DRGR_RO","4F10_YD","ANTB_KO","WOL_BN","QLNO_IV","MSBI_GE","PAIG_HT","GUAR_IV","LAP_KO","PS26_XB","WERN_SX","FINB_GU","TRAV_GU","KARS_KO","DD01_YH","BALBd1_KO","DB01_YH","DC04_YH","CADId1_CA","NE006_NR","CTKS_KO","CORM_KO","MRGE_IV","FBE_SX","3C11_YD","6D12_YD","PM08_XB","STNC_GB","AT49_YB","RSM_IV","SIVA_GE","NE015_NR","PM37_XB","SOFL_GB","NE018_NR","DA02_YH","LOT2_RD","PS59_XB","JAVC_CZ","PLONS_CH","TEOL_IV","PGF_RD","NEUB_SX","NE009_NR","6D10_YD","ROT2_RD","PS18_XB","PM31A_XB","WIMM_SX","LAST_GE","4B12_YD","6D11_YD","GCIS_SL","AT62_YB","DE02_YH","PS30_XB","HRTX_KO","PM12_XB","PM21_XB","CAVI_KO","3D08_YD","POPM_GU","PM16_XB","PS10_XB","FBR_CA","MLFH_TH","SLE_CH","LOT_RO","IDGL_EI","MAHOd2_WM","PS35_XB","ISK_KO","MAZI_KO","BERNI_CH","MOZS_SL","PS05_XB","FETA_OE","6C03_YD","GROG_IV","4F12_YD","BCK_KO","JMB_BS","GOLD_GR","6D13_YD","SLVT_KO","BGKT_KO","SNOP_KO","PS56_XB","ORI_IV","UPC_CZ","3G11_YD","4F14_YD","VULT_IV","SSY_IV","DA09_YH","DE09_YH","VAL_EI","KLYT_KO","TIP_MN","SIMPL_CH","SULT_KO","HMDC_IV","MOCO_IV","TAUT_TH","ILLI_IV","PM24_XB","SASS_IV","MCEL_IV","LEOM_RO","WLF_GE","NRDL_GR","PS23_XB","SSFR_IV","PS52_XB","BULG_IV","PLOR1_RO","PM28_XB","FDMO_IV","SJES_SJ","MMLI_IS","6D09_YD","PLAC_IV","PS19_XB","JSA_GB","GHAJ_GE","NE014_NR","PM32_XB","ISP_GE","PM01_XB","EREN_KO","RSDY_KO","GRB1_GR","PM34_XB","PM05_XB","FVI_IV","NE007_NR","KHC_CZ","MOA_OE","ARR_RO","SKDS_SL","BALST_CH","POBL_CA","SVSK_KO","BEL_PL","AT55_YB","SULR_RO","KRBG_KO","SRPE_EE","HLGd2_GE","SCHF_SX","CSS_GE","WIT_NL","TRIV_IV","CARTd2_WM","MTLF_RD","HCRL_IV","TERO_IV","PM22_XB","AFSR_KO","CADId2_CA","ACER_IV","PS13_XB","OSSC_IV","ESPY_KO","SILT_KO","KESW_GB","HTL_GB","FUORNd2_CH","JOF_HE","6E04_YD","KWPd2_PL","PS09_XB","ENR_GU","RNF_FN","PTK_KO","NE001_NR","BZK_KO","PM09_XB","ARBE_EE","BOJS_SL","BANR_RO","KARP_GE","PM18_XB","BRIS_IV","KZIT_IS","EMBD_CH","TVSB_KO","SENK_KO","NE010_NR","GADA_KO","IWEX_EI","WIMIS_CH","ORT2_RD","NE005_NR","TRId2_MN","ZKRd2_GE","BFO_GR","DJES_SJ","CRES_SL","CING_IV","ROBS_SL","MUCR_IV","6D03_YD","KAST_GR","FIESAd1_CH","6C10_YD","COI_SS","6G07_YD","BUKL_YD","DF04_YH","LSD_GU","KBA_OE","4F02_YD","STV_GU","SC2M_GU","DALY_KO","PLN_TH","CIGN_IV","NE003_NR","IAS_RO","PM38_XB","ABG1_TH","PM25_XB","IKL_KO","PIGN_IV","PS16_XB","GELI_KO","EBR_EB","NE012_NR","DAVOX_CH","MALT_GE","PS39_XB","PABE_GE","3B13_YD","CLDR_KO","PS42_XB","SBPO_IV","6C07_YD","PS06_XB","4F07_YD","PMST_IP","GRC2_GR","SIRT_KO","PM15_XB","GAL1_GB","FAGN_IV","CERA_IV","MGR_IV","NE116_NR","PFVI_PM","HVZN_IV","VOJS_SL","TORNY_CH","6D05_YD","4F03_YD","PS53_XB","CRMI_IV","KVT_KO","MTT2_RD","MDVR_RO","ILIC_KO","6C09_YD","LOR_RD","SARI_KO","MONC_IV","VISS_SL","PS49_XB","ASQU_IV","3D14_YD","GORR_GU","NEGI_GU","MSRU_IV","4E09_YD","OBKA_OE","AIGLE_CH","SBD_BN","DPCd2_CZ","RTC_MN","ORIF_RD","PM35_XB","GBRS_SL","PRMA_IV","KOZT_KO","GOPC_CZ","PS12_XB","LADK_KO","DYA_GB","ROSF_RD","SIRR_RO","TAHT_KO","UBR_GR","JAVS_SL","PS03_XB","PM27_XB","PZZ_GU","6D04_YD","4F04_YD","CRNS_SL","MDNY_KO","ELL_KO","SUWd2_PL","BNALP_CH","ERIK_KO","GTTG_GR","DARE_KO","6D07_YD","MYKA_OE","MLSB_KO","PALZ_IV","SGG_IV","6E14_YD","MSF_FN","4F06_YD","KPL_GB","EPOS_TU","AT63_YB","GMB_IV","PS07_XB","PS41_XB","6D02_YD","CTYL_KO","RRL_GU","AVE_WM","WERD_SX","SATI_GU","PM19_XB","VTS_MN","3E13_YD","NE019_NR","4C13_YD","PM14_XB","CLTB_MN","AT43_YB","LLS_CH","PM40_XB","VAF_HE","PS58_XB","ZKRd1_GE","MSCL_IV","PS15_XB","4E07_YD","OUL_FN","CLL_GR","CIRO_GU","SOKA_OE","6C08_YD","CJR_RO","SACR_IV","PRU_CZ","PM39_XB","PS48_XB","RKY_KO","PM06_XB","VANB_KO","MCT_IV","PRA_CZ","KRLC_CZ","ADVT_KO","6C05_YD","VRTB_KO","PM03_XB","NE017_NR","YLV_KO","ATTE_IV","DOBS_SL","3B11_YD","MDUB_KO","6E11_YD","SUF_HE","NE011_NR","BIGH_GB","PM26_XB","GGNV_LX","PS17_XB","SCHD_TH","GIMEL_CH","MOSI_SI","IACL_IV","MRLC_IV","DPCd1_CZ","PS44_XB","4F09_YD","BEO_SJ","ASSEd1_GR","NE117_NR","PTMD_IV","DIGO_TU","POFI_IV","6C04_YD","ABSI_SI","TREC_CZ","CRE_IV","KTHA_GE","SFSd1_WM","GHRR_RO","PM30_XB","PM36_XB","LRW_GB","CSNA_OE","PS21_XB","RDP_IV","KOGS_SL","NE008_NR","PS37_XB","PS08_XB","SPNCd1_KO","FNVD_IV","KNDS_SL","PM17_XB","DF01_YH","KULA_KO","SENINd2_CH","HERR_RO","PTQR_IV","PM20_XB","CEL_MN","PM13_XB","NE002_NR","LPW_BN","PS34_XB","4F05_YD","ENEZ_KO","PS40_XB","SORM_MD","FOEL_GB","GORS_SL","CORL_IV","VSL_MN","RORO_GU","PM10_XB","ERZN_KO","PM23_XB","PS60_XB","6D01_YD","FIAM_IV","DKL_KO","PS14_XB","TLB_RO","MAON_IV","6D06_YD","ALJA_IV","AT41_YB","IBBN_GE","KELT_TU","EDRB_KO","PM07_XB","PS01_XB","LAUCH_CH","GRB3_GR","ILGA_TU","NE004_NR","ARCI_IV","LEF_KO","RNI2_IV","PM22B_XB","SALO_IV","NE013_NR","4F13_YD","ATVO_IV","FAVR_IV","VSU_EE","PM33_XB","EMV_CH","DAGMA_CH","PS24_XB","3F11_YD"};
			Set<String> excludedStations = Stream.of(tmpStations).collect(Collectors.toSet());
			
			if (args.length == 3) { //2
				Set<Station> stations = StationInformationFile.read(Paths.get(args[1]));
				
				Set<TimewindowInformation> newTimewindows = timewindows.parallelStream()
						.filter(tw -> stations.contains(tw.getStation()))
//						.filter(tw -> tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()) * 180. / Math.PI <= 35.)
						.collect(Collectors.toSet());
				
				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
			}
			else {
				Set<TimewindowInformation> newTimewindows = timewindows.parallelStream()
						.filter(tw ->  {
							System.out.println(cluster.getAzimuthIndex(tw.getStation().getPosition()));
							double distance = Math.toDegrees(tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()));
//							if (distance > 30. || distance < 17.)
//								return false;
//							if (distance < 30. || distance > 90.)
//								return false;
//							if (!tw.getComponent().equals(SACComponent.Z))
//								return false;
//							if (distance <= 85 || distance > 95)
//								return false;
//							if (distance < 70 || distance > 95)
//								return false;
//							if (distance < 67 || distance > 91)
//								return false;
//							if (distance < 70 || distance > 100)
//								return false;
//							if (tw.getComponent().equals(SACComponent.R))
//								return false;
//							if (distance > 80)
//								return false;
							if (distance < 70)
								return false;
//							if (distance < 60)
//								return false;
							if (!clusterIDs.contains(tw.getGlobalCMTID()))
								return false;
							if (cluster.getAzimuthIndex(tw.getStation().getPosition()) != 3) return false;
							else 
								return true;
						})
//						.filter(tw -> !tw.getGlobalCMTID().toString().startsWith("2016") && !tw.getGlobalCMTID().toString().startsWith("2017"))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("200610232100A")) && tw.getStation().getStationName().equals("ISCO")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201306081225A")) && tw.getStation().getStationName().equals("E39A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201405140338A")) && tw.getStation().getStationName().equals("D46A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201405140338A")) && tw.getStation().getStationName().equals("Y12C")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("V49A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("200911141944A")) && tw.getStation().getStationName().equals("X22A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201206020752A")) && tw.getStation().getStationName().equals("121A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201206020752A")) && tw.getStation().getStationName().equals("Y22E")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201206020752A")) && tw.getStation().getStationName().equals("Y22D")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201005241618A")) && tw.getStation().getStationName().equals("HUMO")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201005241618A")) && tw.getStation().getStationName().equals("FACU")))
//						
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("I43A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("H43A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("I42A")))
//						
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201206020752A")) && tw.getStation().getStationName().equals("K22A")))
//						
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("K39A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("J40A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("J41A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("K40A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("J39A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("K38A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("L38A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("L37A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("L36A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("M36A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("200809031125A")) && tw.getStation().getStationName().equals("T21A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("200809031125A")) && tw.getStation().getStationName().equals("U20A")))
//						.filter(tw -> !(tw.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")) && tw.getStation().getStationName().equals("TUC")))
						
//						.filter(tw -> tw.getStation().getStationName().equals("L41A"))
//						.filter(tw -> tw.getGlobalCMTID().getEvent().getCmtLocation().getEpicentralDistance(tw.getStation().getPosition()) * 180. / Math.PI >= 18.)
//						.filter(tw -> !excludedStations.contains(tw.getStation().getStationName() + "_" + tw.getStation().getNetwork()) )
						.collect(Collectors.toSet());
//						
//						.filter(tw -> tw.getGlobalCMTID().equals(new GlobalCMTID("201508050913A")) && tw.getStation().getStationName().equals("K56A"))
//						.collect(Collectors.toSet());
				
				
//				newTimewindows = timewindows.stream().filter(tw -> wellDefinedEvent.contains(tw.getGlobalCMTID()))
//						.collect(Collectors.toSet());
				
//				Predicate<HorizontalPosition> chooser = pos -> {
//					if (pos.getLatitude() <= 49 && pos.getLongitude() <= -120.5)
//						return true;
//					if (pos.getLatitude() <= 43.5 && pos.getLongitude() <= -116)
//						return true;
//					if (pos.getLatitude() <= 57.5 && pos.getLongitude() <= -126)
//						return true;
//					return false;
//				};
				
//				newTimewindows = timewindows.stream().filter(tw -> tw.getGlobalCMTID().equals(new GlobalCMTID("201111072235A"))).collect(Collectors.toSet());
//				newTimewindows = timewindows.stream().filter(tw -> tw.getGlobalCMTID().equals(new GlobalCMTID("200707211534A"))).collect(Collectors.toSet());
				
//				newTimewindows = timewindows.parallelStream().filter(tw -> tw.getGlobalCMTID().equals(new GlobalCMTID("200506021056A"))
//						&& tw.getStation().getStationName().equals("ISCO")).collect(Collectors.toSet());
				
//				newTimewindows = timewindows.parallelStream().filter(tw -> tw.getComponent().equals(SACComponent.Z) && new Phases(tw.getPhases()).equals(new Phases("P")))
//						.collect(Collectors.toSet());
				
//				Set<TimewindowInformation> newTimewindows1 = timewindows.parallelStream()
//						.filter(tw -> {
//							return chooser.test(tw.getStation().getPosition());
//						}).collect(Collectors.toSet());
//				
//				Set<TimewindowInformation> newTimewindows2 = timewindows.parallelStream()
//						.filter(tw -> {
//							return !chooser.test(tw.getStation().getPosition());
//						}).collect(Collectors.toSet());
//				
//				Map<GlobalCMTID, Integer> nWindowEventMap = new HashMap<>();
//				for (TimewindowInformation timewindow : newTimewindows) {
//					GlobalCMTID id = timewindow.getGlobalCMTID();
//					if (nWindowEventMap.containsKey(id)) {
//						int n = nWindowEventMap.get(id) + 1;
//						nWindowEventMap.replace(id, n);
//					}
//					else
//						nWindowEventMap.put(id, 1);
//				}
//				
//				Set<TimewindowInformation> newTimewindows2 = newTimewindows.parallelStream().filter(tw -> nWindowEventMap.get(tw.getGlobalCMTID()) >= 15)
//					.collect(Collectors.toSet());
//				TimewindowInformationFile.write(newTimewindows2, newTimewindowFile);
//				
//				newTimewindows = timewindows.stream().limit(1).collect(Collectors.toSet());
//				newTimewindows = timewindows.stream().filter(tw -> tw.getGlobalCMTID()
//					.equals(new GlobalCMTID("201205280507A"))).collect(Collectors.toSet());
				
//				newTimewindows = timewindows.stream().filter(tw -> tw.getStation().getStationName().equals("PH10") ||
//						tw.getStation().getStationName().equals("Y12C")).collect(Collectors.toSet());
				
				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
//				
				
//				Map<GlobalCMTID, Integer> nTransverseMap = new HashMap<>();
//				for (TimewindowInformation timewindow : timewindows) {
//					GlobalCMTID event = timewindow.getGlobalCMTID();
//					Integer itmp = new Integer(1);
//					if (nTransverseMap.containsKey(event)) {
//						itmp = nTransverseMap.get(event) + 1;
//					}
//					nTransverseMap.put(event, itmp);
//				}
//				
//				Set<TimewindowInformation> newTimewindows = timewindows.stream().filter(tw -> nTransverseMap.get(tw.getGlobalCMTID()) >= 20)
//						.collect(Collectors.toSet());
//				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
				
				
//				Set<TimewindowInformation> newTimewindows = timewindows.stream().filter(tw -> {
////					String sta = tw.getStation().getStationName();
////					if (sta.equals("FUR") || sta.equals("C03") || sta.equals("SHO"))
////						return false;
////					else if (!wellDefinedEvent.contains(tw.getGlobalCMTID()))
////						return false;
////					else 
////						return true;
//					if (tw.getGlobalCMTID().equals(new GlobalCMTID("201302091416A"))) {
//						double azimuth = Math.toDegrees(tw.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(tw.getStation().getPosition()));
//						if (azimuth >= 335 && azimuth <= 355)
//							return true;
//					}
//					return false;
//				}).collect(Collectors.toSet());
//				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
				
				
//				Set<TimewindowInformation> newTimewindows = new HashSet<>();
//				for (TimewindowInformation window : timewindows) {
//					String staName = window.getStation().getStationName();
//					String network = window.getStation().getNetwork();
//					GlobalCMTID id = window.getGlobalCMTID();
//					for (int i = 0; i < recordIDList.size(); i++) {
//						if (stationNames.get(i).equals(staName) && networkNames.get(i).equals(network)
//								&& recordIDList.get(i).equals(id)) {
//							newTimewindows.add(window);
//							break;
//						}
//					}
//				}
//				TimewindowInformationFile.write(newTimewindows, newTimewindowFile);
				
//				TimewindowInformationFile.write(newTimewindows1, newTimewindowFile1);
//				TimewindowInformationFile.write(newTimewindows2, newTimewindowFile2);
				
				//divide one event per azimuth
//				Set<TimewindowInformation> info1 = new HashSet<>();
//				Set<TimewindowInformation> info2 = new HashSet<>();
//				for (TimewindowInformation window : timewindows) {
//					if (!window.getGlobalCMTID().equals(new GlobalCMTID("201205280507A")))
//						continue;
//					double azimuth = Math.toDegrees(window.getGlobalCMTID().getEvent().getCmtLocation().getAzimuth(window.getStation().getPosition()));
//					if (azimuth >= 335 && azimuth < 340)
//						info1.add(window);
//					else if (azimuth >= 340 && azimuth < 345)
//						info2.add(window);
//				}
//				Path outwindow1 = Paths.get(timewindowFile.toString().replace(".dat", "_az335.dat"));
//				Path outwindow2 = Paths.get(timewindowFile.toString().replace(".dat", "_az340.dat"));
//				TimewindowInformationFile.write(info1, outwindow1);
//				TimewindowInformationFile.write(info2, outwindow2);
			}
		} catch (IOException e) {
			System.err.format("IOException: %s%n", e);
		}
	}
	
	private static Set<TimewindowInformation> excludeStation(Set<TimewindowInformation> timewindows
			, Path timewindowASCIIFile) throws IOException {
		BufferedReader reader = Files.newBufferedReader(timewindowASCIIFile);
		String line = null;
		Set<ReducedInfo> excludeTimewindows = new HashSet<>();
		while ((line = reader.readLine()) != null) {
			String[] s = line.trim().split(" ");
			String stationName = s[0].split("_")[0];
			GlobalCMTID id = new GlobalCMTID(s[1]);
			SACComponent component = SACComponent.valueOf(s[2]);
			double startTime = Double.parseDouble(s[3]);
			ReducedInfo info = new ReducedInfo(stationName, id, component, startTime);
			excludeTimewindows.add(info);
		}
		
		Set<TimewindowInformation> newTimewindows = new HashSet<>();
		for (TimewindowInformation tw : timewindows) {
			boolean exclude = false;
			for (ReducedInfo info : excludeTimewindows) {
				if (info.isPair(tw)) {
					exclude = true;
					break;
				}
			}
			if (exclude)
				continue;
			
			newTimewindows.add(tw);
		}
		
		return newTimewindows;
	}
	
	private static Set<TimewindowInformation> excludePhase(Set<TimewindowInformation> timewindows,
			Set<Phases> phaseSet) throws IOException {
		return timewindows.stream().filter(tw -> 
			!phaseSet.contains(new Phases(tw.getPhases()))
		).collect(Collectors.toSet());
	}
	
	private static class ReducedInfo {
		public String stationName;
		public GlobalCMTID id;
		public SACComponent component;
		public double startTime;
		
		public ReducedInfo(String stationName, GlobalCMTID id, SACComponent component, double startTime) {
			this.stationName = stationName;
			this.id = id;
			this.component = component;
			this.startTime = startTime;
		}
		
		public boolean isPair(TimewindowInformation tw) {
			if (!tw.getStation().getName().equals(stationName))
				return false;
			else if (!tw.getGlobalCMTID().equals(id))
				return false;
			else if (!tw.getComponent().equals(component))
				return false;
			else if (Math.abs(tw.getStartTime() - startTime) > 0.1)
				return false;
			else
				return true;
		}
	}
	

}
