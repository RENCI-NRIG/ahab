package org.renci.ahab.libndl.ndl;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.resources.request.StitchPort;

import com.hp.hpl.jena.ontology.Individual;

import edu.uci.ics.jung.graph.util.Pair;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

public abstract class NDLGenerator {
	protected Slice slice;
	protected NdlGenerator ngen = null;
	
	abstract void generate(String rdf);
	
	
	protected void setNodeTypeOnInstance(String type, Individual ni) throws NdlException {
		if (BAREMETAL.equals(type))
			ngen.addBareMetalDomainProperty(ni);
		else
			ngen.addVMDomainProperty(ni);
		if (nodeTypes.get(type) != null) {
			Pair<String> nt = nodeTypes.get(type);
			ngen.addNodeTypeToCE(nt.getFirst(), nt.getSecond(), ni);
		}
	}

	// converting to netmask
	protected static final String[] netmaskConverter = {
		"128.0.0.0", "192.0.0.0", "224.0.0.0", "240.0.0.0", "248.0.0.0", "252.0.0.0", "254.0.0.0", "255.0.0.0",
		"255.128.0.0", "255.192.0.0", "255.224.0.0", "255.240.0.0", "255.248.0.0", "255.252.0.0", "255.254.0.0", "255.255.0.0",
		"255.255.128.0", "255.255.192.0", "255.255.224.0", "255.255.240.0", "255.255.248.0", "255.255.252.0", "255.255.254.0", "255.255.255.0",
		"255.255.255.128", "255.255.255.192", "255.255.255.224", "255.255.255.240", "255.255.255.248", "255.255.255.252", "255.255.255.254", "255.255.255.255"
	};
			

	private static final String EUCALYPTUS_NS = "eucalyptus";
	private static final String EXOGENI_NS = "exogeni";
	public static final String BAREMETAL = "ExoGENI Bare-metal";
	public static final String FORTYGBAREMETAL = "ExoGENI 40G Bare-metal";
	public static final String DOT_FORMAT = "DOT";
	public static final String N3_FORMAT = "N3";
	public static final String RDF_XML_FORMAT = "RDF-XML";
	public static final String defaultFormat = RDF_XML_FORMAT;
		
	// helper
	public static final Map<String, String> domainMap;
	static {
		Map<String, String> dm = new HashMap<String, String>();
		dm.put("RENCI (Chapel Hill, NC USA) XO Rack", "rcivmsite.rdf#rcivmsite");
		dm.put("BBN/GPO (Boston, MA USA) XO Rack", "bbnvmsite.rdf#bbnvmsite");
		dm.put("Duke CS (Durham, NC USA) XO Rack", "dukevmsite.rdf#dukevmsite");
		dm.put("UNC BEN (Chapel Hill, NC USA)", "uncvmsite.rdf#uncvmsite");
		dm.put("RENCI BEN (Chapel Hill, NC USA)", "rencivmsite.rdf#rencivmsite");
		dm.put("NICTA (Sydney, Australia) XO Rack", "nictavmsite.rdf#nictavmsite");
		dm.put("FIU (Miami, FL USA) XO Rack", "fiuvmsite.rdf#fiuvmsite");
		dm.put("UH (Houston, TX USA) XO Rack", "uhvmsite.rdf#uhvmsite");
		dm.put("UvA (Amsterdam, The Netherlands) XO Rack", "uvanlvmsite.rdf#uvanlvmsite");
		dm.put("UFL (Gainesville, FL USA) XO Rack", "uflvmsite.rdf#uflvmsite");
		dm.put("UAF (Fairbanks, AK, USA) XO Rack", "uafvmsite.rdf#uafvmsite");
		dm.put("UCD (Davis, CA USA) XO Rack", "ucdvmsite.rdf#ucdvmsite");
		dm.put("OSF (Oakland, CA USA) XO Rack", "osfvmsite.rdf#osfvmsite");
		dm.put("SL (Chicago, IL USA) XO Rack", "slvmsite.rdf#slvmsite");
		dm.put("WVN (UCS-B series rack in Morgantown, WV, USA)", "wvnvmsite.rdf#wvnvmsite");
		dm.put("NCSU (UCS-B series rack at NCSU)", "ncsuvmsite.rdf#ncsuvmsite");
		dm.put("NCSU2 (UCS-C series rack at NCSU)", "ncsu2vmsite.rdf#ncsu2vmsite");
		dm.put("TAMU (College Station, TX, USA) XO Rack", "tamuvmsite.rdf#tamuvmsite");
		dm.put("UMass (UMass Amherst, MA, USA) XO Rack", "umassvmsite.rdf#umassvmsite");
		dm.put("WSU (Detroit, MI, USA) XO Rack", "wsuvmsite.rdf#wsuvmsite");
		dm.put("UAF (Fairbanks, AK, USA) XO Rack", "uafvmsite.rdf#uafvmsite");
		dm.put("PSC (Pittsburgh, TX, USA) XO Rack", "pscvmsite.rdf#pscvmsite");
		dm.put("UNF (Jacksonville, FL) XO Rack", "unfvmsite.rdf#unfvmsite");
		dm.put("GWU (Washington DC,  USA) XO Rack", "gwuvmsite.rdf#gwuvmsite");
		dm.put("CIENA (Ottawa,  CA) XO Rack", "cienavmsite.rdf#cienavmsite");
		dm.put("RCI-RADICS", "rciradicsvmsite.rdf#rciradicsvmsite");
		dm.put("BBN-RADICS", "bbnradicsvmsite.rdf#bbnradicsvmsite");
		dm.put("CIENA2 (Hanover, MD) XO Rack", "ciena2vmsite.rdf#ciena2vmsite");
		dm.put(StitchPort.STITCHING_DOMAIN_SHORT_NAME, "orca.rdf#Stitching");
		dm.put("INNOQ XO Rack", "innoqvmsite.rdf#innoqvmsite");
		dm.put("ROCKY XO Rack", "rockyvmsite.rdf#rockyvmsite");

		domainMap = Collections.unmodifiableMap(dm);
	}
		
	public static final Map<String, String> netDomainMap;
	static {
		Map<String, String> ndm = new HashMap<String, String>();

		ndm.put("RENCI XO Rack Net", "rciNet.rdf#rciNet");
		ndm.put("BBN/GPO XO Rack Net", "bbnNet.rdf#bbnNet");
		ndm.put("Duke CS Rack Net", "dukeNet.rdf#dukeNet");
		ndm.put("UNC BEN XO Rack Net", "uncNet.rdf#UNCNet");
		ndm.put("NICTA XO Rack Net", "nictaNet.rdf#nictaNet");
		ndm.put("FIU XO Rack Net", "fiuNet.rdf#fiuNet");
		ndm.put("UH XO Rack Net", "uhNet.rdf#UHNet");
		ndm.put("NCSU XO Rack Net", "ncsuNet.rdf#ncsuNet");
		ndm.put("UvA XO Rack Net", "uvanlNet.rdf#uvanlNet");
		ndm.put("UFL XO Rack Net", "uflNet.rdf#uflNet");
		ndm.put("UCD XO Rack Net", "ucdNet.rdf#ucdNet");
		ndm.put("OSF XO Rack Net", "osfNet.rdf#osfNet");
		ndm.put("SL XO Rack Net", "slNet.rdf#slNet");
		ndm.put("UAF XO Rack Net",  "uafNet.rdf#uafNet");
		ndm.put("WVN XO Rack Net", "wvnNet.rdf#wvnNet");
		ndm.put("NCSU XO Rack Net", "ncsuNet.rdf#ncsuNet");
		ndm.put("NCSU2 XO Rack Net", "ncs2Net.rdf#ncsuNet");
		ndm.put("UNF XO Rack Net", "unfNet.rdf#unfNet");
		ndm.put("GWU XO Rack Net",  "gwuNet.rdf#gwuNet");
		ndm.put("CIENA XO Rack Net",  "cienaNet.rdf#cienaNet");

		ndm.put("I2 ION/AL2S", "ion.rdf#ion");
		ndm.put("NLR Net", "nlr.rdf#nlr");
		ndm.put("BEN Net", "ben.rdf#ben");

		netDomainMap = Collections.unmodifiableMap(ndm);
	}
		
	// various node types
	public static final Map<String, Pair<String>> nodeTypes;
	static {
		Map<String, Pair<String>> nt = new HashMap<String, Pair<String>>();
		nt.put(BAREMETAL, new Pair<String>(EXOGENI_NS, "ExoGENI-M4"));
		nt.put("Euca m1.small", new Pair<String>(EUCALYPTUS_NS, "EucaM1Small"));
		nt.put("Euca c1.medium", new Pair<String>(EUCALYPTUS_NS, "EucaC1Medium"));
		nt.put("Euca m1.large", new Pair<String>(EUCALYPTUS_NS, "EucaM1Large"));
		nt.put("Euca m1.xlarge", new Pair<String>(EUCALYPTUS_NS, "EucaM1XLarge"));
		nt.put("Euca c1.xlarge", new Pair<String>(EUCALYPTUS_NS, "EucaC1XLarge"));
		nt.put("XO Small", new Pair<String>(EXOGENI_NS, "XOSmall"));
		nt.put("XO Medium", new Pair<String>(EXOGENI_NS, "XOMedium"));
		nt.put("XO Large", new Pair<String>(EXOGENI_NS, "XOLarge"));
		nt.put("XO Extra large", new Pair<String>(EXOGENI_NS, "XOXlarge"));
		//nodeTypes = Collections.unmodifiableMap(nt);
		nodeTypes = nt;
	}



    public enum ReservationState {
        ReservationStateUnknown(0, ""),
        ReservationStateNascent(1, "Nascent"),
        ReservationStateTicketed(2, "Ticketed"),
        ReservationStateActive(3, "Active"),
        ReservationStateActiveTicketed(4, "ActiveTicketed"),
        ReservationStateClosed(5, "Closed"),
        ReservationStateCloseWait(6, "CloseWait"),
        ReservationStateFailed(7, "Failed"),
        ReservationPendingStateClosing(8,"Closing");


        ReservationState(int value, String name) {
            this.value = value;
            this.name = name;
        }

        public int value;
        public String name;
    }

    public static final Map<String, Integer> reservationStates;
    static {
        Map<String, Integer> rs = new HashMap<String, Integer>();
        rs.put(ReservationState.ReservationStateUnknown.name, ReservationState.ReservationStateUnknown.value);
        rs.put(ReservationState.ReservationStateNascent.name, ReservationState.ReservationStateNascent.value);
        rs.put(ReservationState.ReservationStateTicketed.name, ReservationState.ReservationStateTicketed.value);
        rs.put(ReservationState.ReservationStateActive.name, ReservationState.ReservationStateActive.value);
        rs.put(ReservationState.ReservationStateActiveTicketed.name, ReservationState.ReservationStateActiveTicketed.value);
        rs.put(ReservationState.ReservationStateClosed.name, ReservationState.ReservationStateClosed.value);
        rs.put(ReservationState.ReservationStateCloseWait.name, ReservationState.ReservationStateCloseWait.value);
        rs.put(ReservationState.ReservationStateFailed.name, ReservationState.ReservationStateFailed.value);
        rs.put(ReservationState.ReservationPendingStateClosing.name, ReservationState.ReservationPendingStateClosing.value);
        reservationStates = rs;
    }

    public static enum SliceState {
        NULL("NULL"), CONFIGURING("CONFIGURING"), STABLE_ERROR("STABLE WITH ERRORS"), STABLE_OK("STABLE"), CLOSING_DEAD(
                "CLOSING_DEAD");

        private String n;

        private SliceState(String s) {
            n = s;
        }

        @Override
        public String toString() {
            return n;
        }
    };
}
