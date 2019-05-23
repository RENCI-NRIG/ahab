package org.renci.ahab.libndl.ndl;

import java.util.Date;
import java.util.UUID;
import org.renci.ahab.libndl.LIBNDL;
import org.renci.ahab.libndl.SliceGraph;
import org.renci.ahab.libndl.resources.request.*;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Resource;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

public class NewSliceModel extends NDLModel {
	public NewSliceModel(){
		super();

	}


	public void init(SliceGraph sliceGraph){
		try{
			String nsGuid = UUID.randomUUID().toString();
			ngen = new NdlGenerator(nsGuid, LIBNDL.logger());
			reservation = ngen.declareReservation();


			Individual term = ngen.declareTerm();

			Date start = new Date();
			Individual tStart = ngen.declareTermBeginning(start);
			ngen.addBeginningToTerm(tStart, term);

			Individual duration = ngen.declareTermDuration(1,0,0);  // days, hours, mins

			ngen.addDurationToTerm(duration, term);
			ngen.addTermToReservation(term, reservation);

		} catch (NdlException e) {
			logger().error("NewSliceModel: " + e.getStackTrace());
		}
	}

	public void init(SliceGraph sliceGraph, String rdf) {
		logger().debug("NewSliceModel");
		RequestLoader loader = new RequestLoader(sliceGraph, this);
		loader.load(rdf);

		try {
			ngen = loader.getGenerator();
			reservation = ngen.declareReservation();
		} catch (NdlException e) {
			logger().error("NewSliceModel: " + e.getStackTrace());
		}

	}

	public boolean isNewSlice(){ return true; };

	@Override
	public void add(ComputeNode cn, String name) {
		logger().debug("NewSliceModel:add(ComputeNode)");
		try {
			Individual ni = null;
            ni = ngen.declareComputeElement(name);
            ngen.addGuid(ni, UUID.randomUUID().toString());
            ngen.addVMDomainProperty(ni);
			mapRequestResource2ModelResource(cn, ni);

			ngen.addResourceToReservation(reservation, ni);
		} catch (NdlException e) {
			logger().error("NewSliceModel:add(ComputeNode):" + e.getStackTrace());
		}

	}

	@Override
	public void add(BroadcastNetwork bn, String name, long bandwidth) {
		logger().debug("NewSliceModel:add(BroadcastNetwork)" + name);
		try {
			Individual ci = ngen.declareBroadcastConnection(name);;
			ngen.addGuid(ci, UUID.randomUUID().toString());
			ngen.addLayerToConnection(ci, "ethernet", "EthernetNetworkElement");
			ngen.addBandwidthToConnection(ci, bandwidth);  //TODO: Should be constant default value

			mapRequestResource2ModelResource(bn, ci);
			ngen.addResourceToReservation(reservation, ci);
		} catch (NdlException e) {
			logger().error("NewSliceModel:add(ComputeNode):" + e.getStackTrace());
		}
	}

	@Override
	public void add(LinkNetwork bn, String name, long bandwidth) {
		logger().debug("NewSliceModel:add(Link2)" + name);
		try {
			Individual ci = ngen.declareNetworkConnection(name);;
			ngen.addGuid(ci, UUID.randomUUID().toString());
			ngen.addLayerToConnection(ci, "ethernet", "EthernetNetworkElement");
			ngen.addBandwidthToConnection(ci, bandwidth);  //TODO: Should be constant default value

			mapRequestResource2ModelResource(bn, ci);
			ngen.addResourceToReservation(reservation, ci);
		} catch (NdlException e) {
			logger().error("NewSliceModel:add(ComputeNode):" + e.getStackTrace());
		}
	}

	@Override
	public void add(StitchPort sp, String name, String label, String port) {
		logger().debug("add(StitchPort sp) sp: " + sp);
    Individual spi = null;
    try{
    		//Add the stitchport
        spi = ngen.declareStitchingNode(name);
        mapRequestResource2ModelResource(sp, spi);
        ngen.addResourceToReservation(reservation, spi);
        ngen.addGuid(spi, UUID.randomUUID().toString());
		} catch (NdlException e){
			logger().error("ERROR: NewSliceModel::add(StitchPort) " );
			e.printStackTrace();
		}

	}

	@Override
  public void add(InterfaceStitchPort2StitchPort i){
		logger().debug("add(InterfaceStitchPort2StitchPort) i: " + i);
		try{

		StitchPort sp1=i.getSp1();
		StitchPort sp2=i.getSp2();

		Individual sp1I = ngen.getRequestIndividual(sp1.getName());
		Individual sp2I = ngen.getRequestIndividual(sp2.getName());

		Individual netI = ngen.declareNetworkConnection(sp1.getName()+"-"+sp2.getName()+"-net");
		ngen.addGuid(netI, UUID.randomUUID().toString());

		if (reservation != null) {
			ngen.addResourceToReservation(reservation, netI);
		}

		//if (sp1.getBandwidth() > 0) {
		//	ngen.addBandwidthToConnection(netI, sp1.getBandwidth());
		//}

		ngen.addLabelToIndividual(netI, sp1.getLabel());
		ngen.addLabelToIndividual(netI, sp2.getLabel());
		ngen.addLayerToConnection(netI, "ethernet", "EthernetNetworkElement");

		Individual sp1Iface = ngen.declareStitchportInterface(sp1.getPort(), sp1.getLabel());
		ngen.addInterfaceToIndividual(sp1Iface, sp1I);
		ngen.addInterfaceToIndividual(sp1Iface, netI);

		Individual sp2Iface = ngen.declareStitchportInterface(sp2.getPort(), sp2.getLabel());
		ngen.addInterfaceToIndividual(sp2Iface, sp2I);
		ngen.addInterfaceToIndividual(sp2Iface, netI);

		Individual ei = ngen.declareNetworkConnection(sp1.getName()+"-"+sp2.getName()+"-net");
		ngen.addGuid(ei, UUID.randomUUID().toString());

	} catch (NdlException e){
		logger().error("ERROR: NewSliceModel::InterfaceStitchPort2StitchPort " );
		e.printStackTrace();
	}
	}

/*
	//Add a link to the stitchport
	Individual ei = ngen.declareNetworkConnection(name+"-net");
	ngen.addGuid(ei, UUID.randomUUID().toString());
	if (reservation != null)
		ngen.addResourceToReservation(reservation, ei);
	if (sp.getBandwidth() > 0)
		ngen.addBandwidthToConnection(ei, sp.getBandwidth());
	ngen.addLabelToIndividual(ei, label);
	ngen.addLayerToConnection(ei, "ethernet", "EthernetNetworkElement");
	logger().debug("add(StitchPort sp) port: " + port);
	logger().debug("add(StitchPort sp) label: " + label);

	Individual spIface = ngen.declareStitchportInterface(port, label);
	ngen.addInterfaceToIndividual(spIface, ei);
	ngen.addInterfaceToIndividual(spIface, ni);
*/

	@Override
	public void add(InterfaceNode2Net i) {
		Resource r = this.getModelResource(i);
		Node node = i.getNode();
		Network net = i.getLink();

		try{

		Individual blI  = null; //ngen.getRequestIndividual(net.getName()+"-net"); //not sure this is right
		Individual nodeI = ngen.getRequestIndividual(node.getName());

		Individual intI;
		if (net instanceof StitchPort) {
			//blI  = ngen.getRequestIndividual(net.getName()+"-net");
			//Add a link to the stitchport
			StitchPort sp = (StitchPort)net;
			String label = sp.getLabel();
			String port = sp.getPort();

			Individual spI = ngen.getRequestIndividual(sp.getName());

			blI = ngen.declareNetworkConnection(sp.getName()+"-net");
			ngen.addGuid(blI, UUID.randomUUID().toString());

			if (reservation != null) {
				ngen.addResourceToReservation(reservation, blI);
			}

			if (sp.getBandwidth() > 0) {
				ngen.addBandwidthToConnection(blI, sp.getBandwidth());
			}

			ngen.addLabelToIndividual(blI, label);
			ngen.addLayerToConnection(blI, "ethernet", "EthernetNetworkElement");
			logger().debug("add(StitchPort sp) port: " + port);
			logger().debug("add(StitchPort sp) label: " + label);

			Individual spIface = ngen.declareStitchportInterface(port, label);
			ngen.addInterfaceToIndividual(spIface, spI);
			ngen.addInterfaceToIndividual(spIface, blI);
		} else {
			blI  = ngen.getRequestIndividual(net.getName());
		}
		intI = ngen.declareInterface(net.getName()+"-"+node.getName());

		ngen.addInterfaceToIndividual(intI, blI);

		if (nodeI == null)
			throw new NdlException("Unable to find or create individual for node " + node);

		if (intI == null)
			throw new NdlException("Unable to find or create individual for node " + node);

		ngen.addInterfaceToIndividual(intI, nodeI);
		ngen.addResourceToReservation(reservation, nodeI);
		this.mapRequestResource2ModelResource(i, intI);
		} catch (NdlException e){
			logger().error("ERROR: NewSliceModel::add(InterfaceNode2Net) " );
			e.printStackTrace();
		}
	}

	@Override
	public void add(InterfaceNode2Net i, RequestResource depend) {
	    // TODO Test and modify if needed
		Resource r = this.getModelResource(i);
		Node node = i.getNode();
		Network net = i.getLink();

		try{
			Individual blI  = null;
			Individual nodeI = ngen.getRequestIndividual(node.getName());

			Individual intI;
			if (net instanceof StitchPort) {
				blI  = ngen.getRequestIndividual(net.getName()+"-net");
			} else {
				blI  = ngen.getRequestIndividual(net.getName());
			}
			intI = ngen.declareInterface(net.getName()+"-"+node.getName());

			ngen.addInterfaceToIndividual(intI, blI);

			if (nodeI == null)
				throw new NdlException("Unable to find or create individual for node " + node);

			if (intI == null)
				throw new NdlException("Unable to find or create individual for node " + node);

			ngen.addInterfaceToIndividual(intI, nodeI);
			ngen.addResourceToReservation(reservation, nodeI);
            ngen.addDependOnToIndividual(ngen.getRequestIndividual(depend.getName()), nodeI);

			this.mapRequestResource2ModelResource(i, intI);
		} catch (NdlException e){
			logger().error("ERROR: NewSliceModel::add(InterfaceNode2Net) " );
			e.printStackTrace();
		}
	}

	@Override
	public void add(StorageNode sn, String name) {
        logger().debug("NewSliceModel:add(StorageNode)");
        try {
            Individual ni = null;
            ni = ngen.declareISCSIStorageNode(name, sn.getCapacity(), sn.getFSType(), sn.getFSParam(), sn.getMntPoint(), sn.getDoFormat());
            ngen.addGuid(ni, UUID.randomUUID().toString());
            mapRequestResource2ModelResource(sn, ni);
            ngen.declareModifyElementAddElement(reservation, ni);
        } catch (NdlException e) {
            logger().error("NewSliceModel:add(StorageNode):" + e.getStackTrace());
        }
	}

	@Override
	public void remove(ComputeNode cn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(BroadcastNetwork bn) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(StitchPort sp) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(InterfaceNode2Net i) {
		// TODO Auto-generated method stub

	}

	@Override
	public void remove(StorageNode sn) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getRequest() {
		RequestGenerator saver = new RequestGenerator(ngen);
		return saver.getRequest();
	}





}
