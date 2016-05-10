package orca.ahab.libndl.ndl;

import java.util.UUID;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.uci.ics.jung.graph.SparseMultigraph;
import orca.ahab.libndl.LIBNDL;
import orca.ahab.libndl.SliceGraph;
import orca.ahab.libndl.resources.common.ModelResource;
import orca.ahab.libndl.resources.request.BroadcastNetwork;
import orca.ahab.libndl.resources.request.ComputeNode;
import orca.ahab.libndl.resources.request.Interface;
import orca.ahab.libndl.resources.request.InterfaceNode2Net;
import orca.ahab.libndl.resources.request.Network;
import orca.ahab.libndl.resources.request.Node;
import orca.ahab.libndl.resources.request.RequestResource;
import orca.ahab.libndl.resources.request.StitchPort;
import orca.ahab.libndl.resources.request.StorageNode;
import orca.ndl.NdlCommons;
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
		} catch (NdlException e) {
			logger().error("NewSliceModel: " + e.getStackTrace());
		}
	}
	
	public void init(SliceGraph sliceGraph, String rdf) {
		logger().debug("NewSliceModel");
		RequestLoader loader = new RequestLoader(sliceGraph, this);
		loader.load(rdf);
		//NdlCommons request = loader.load(rdf);

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
			
			//if (cn.getNodeCount() > 0){
			//	if (cn.getSplittable())
			//		ni = ngen.declareServerCloud(name, cn.getSplittable());
			//	else
			//		ni = ngen.declareServerCloud(name);
			//} else {
				ni = ngen.declareComputeElement(name);
				ngen.addVMDomainProperty(ni);
			//}
			mapRequestResource2ModelResource(cn, ni);
			
			ngen.addResourceToReservation(reservation, ni);
		} catch (NdlException e) {
			logger().error("NewSliceModel:add(ComputeNode):" + e.getStackTrace());
		}	
		
	}

	@Override
	public void add(BroadcastNetwork bn, String name) {
		logger().debug("NewSliceModel:add(BroadcastNetwork)" + name);
		try {
			Individual ci = ngen.declareBroadcastConnection(name);;
			ngen.addGuid(ci, UUID.randomUUID().toString());
			ngen.addLayerToConnection(ci, "ethernet", "EthernetNetworkElement");
			ngen.addBandwidthToConnection(ci, (long)10000000);  //TODO: Should be constant default value
		
			mapRequestResource2ModelResource(bn, ci);
			ngen.addResourceToReservation(reservation, ci);
		} catch (NdlException e) {
			logger().error("NewSliceModel:add(ComputeNode):" + e.getStackTrace());
		}	
	}

	@Override 
	public void add(StitchPort sp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void add(InterfaceNode2Net i) {
		logger().debug("NewSliceModel:add(InterfaceNode2Net)");
		Resource r = this.getModelResource(i);
		Node node = i.getNode();
		Network net = i.getLink();
		
		try{
		
		Individual blI = ngen.getRequestIndividual(net.getName()); //not sure this is right
		Individual nodeI = ngen.getRequestIndividual(node.getName());
		 
		Individual intI;
		if (node instanceof StitchPort) {
			StitchPort sp = (StitchPort)node;
			if ((sp.getLabel() == null) || (sp.getLabel().length() == 0))
				throw new NdlException("URL and label must be specified in StitchPort");
			intI = ngen.declareStitchportInterface(sp.getPort(), sp.getLabel());
		} else {
			intI = ngen.declareInterface(net.getName()+"-"+node.getName());
		}
		ngen.addInterfaceToIndividual(intI, blI);
		
		if (nodeI == null)
			throw new NdlException("Unable to find or create individual for node " + node);
		
		ngen.addInterfaceToIndividual(intI, nodeI);

		// see if there is an IP address for this link on this node
//		if (node.getIp(link) != null) {
//			// create IP object, attach to interface
//			Individual ipInd = ngen.addUniqueIPToIndividual(n.getIp(l), oc.getName()+"-"+n.getName(), intI);
//			if (n.getNm(l) != null)
//				ngen.addNetmaskToIP(ipInd, netmaskIntToString(Integer.parseInt(n.getNm(l))));
//		}
		ngen.addResourceToReservation(reservation, nodeI);
		} catch (NdlException e){
			logger().error("ERROR: NewSliceModel::add(InterfaceNode2Net) " );
			e.printStackTrace();
		}
	}

	@Override
	public void add(StorageNode sn) {
		// TODO Auto-generated method stub
		
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
