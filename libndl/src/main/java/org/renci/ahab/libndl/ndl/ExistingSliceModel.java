package org.renci.ahab.libndl.ndl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.renci.ahab.libndl.LIBNDL;
import org.renci.ahab.libndl.SliceGraph;
import org.renci.ahab.libndl.resources.common.ModelResource;
import org.renci.ahab.libndl.resources.manifest.ManifestResource;
import org.renci.ahab.libndl.resources.request.*;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.uci.ics.jung.graph.SparseMultigraph;
import edu.uci.ics.jung.graph.util.Pair;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;
import orca.ndl.NdlManifestParser;
import orca.ndl.NdlRequestParser;

public class ExistingSliceModel extends NDLModel{
	protected NdlManifestParser sliceModel;

	/* map of RequestResource in original slice or request to ndl Resource */
	protected Map<RequestResource,Resource> slice2NDLMap;
	
	
	public ExistingSliceModel() {
		super();
	}

	public void init(SliceGraph sliceGraph, String rdf){
		
		try{
			UserAbstractionLoader uloader = new UserAbstractionLoader(sliceGraph,this);
			sliceModel = uloader.load(rdf);
			String nsGuid = UUID.randomUUID().toString();
			ngen = new NdlGenerator(nsGuid, LIBNDL.logger(), true);
			String nm = (nsGuid == null ? "my-modify" : nsGuid + "/my-modify");
			reservation = ngen.declareModifyReservation(nm);
		} catch (Exception e){
			LIBNDL.logger().debug("Fail: ExistingSliceModel::init");
		}
	}
	
	public boolean isNewSlice(){ return false; };

	protected void mapSliceResource2ModelResource(RequestResource r, Resource i){
		slice2NDLMap.put(r,i);
	}

	protected Resource getModelResource(RequestResource r){
		return request2NDLMap.get(r);
	}

	@Override
	public String getRequest() {
        ModifyGenerator saver = new ModifyGenerator(ngen);
        return saver.getModifyRequest();
    }
	
	@Override
	public void add(ComputeNode cn, String name) {
		logger().debug("ExistingSliceModel:add(ComputeNode)");
		try {
            Individual ni = null;
            ni = ngen.declareComputeElement(name);
            ngen.addGuid(ni, UUID.randomUUID().toString());
            ngen.addVMDomainProperty(ni);
			mapRequestResource2ModelResource(cn, ni);
			ngen.declareModifyElementAddElement(reservation, ni);
		} catch (NdlException e) {
			logger().error("NewSliceModel:add(ComputeNode):" + e.getStackTrace());
		}	
		
	}

    @Override
    public void add(LinkNetwork bn, String name, long bandwidth) {
        logger().debug("NewSliceModel:add(LinkNetwork)" + name);
        try {
            Individual ci = ngen.declareNetworkConnection(name);;
            ngen.addGuid(ci, UUID.randomUUID().toString());
            ngen.addLayerToConnection(ci, "ethernet", "EthernetNetworkElement");
            ngen.addBandwidthToConnection(ci, bandwidth);  //TODO: Should be constant default value
            mapRequestResource2ModelResource(bn, ci);
            ngen.declareModifyElementAddElement(reservation, ci);
        } catch (NdlException e) {
            logger().error("NewSliceModel:add(LinkNetwork):" + e.getStackTrace());
        }
    }

	@Override
	public void add(BroadcastNetwork bn, String name, long bandwidth) {
		logger().debug("ExistingSliceModel:add(BroadcastNetwork)" + name);
		try {
			Individual ci = ngen.declareBroadcastConnection(name);
			ngen.addGuid(ci, UUID.randomUUID().toString());
			ngen.addLayerToConnection(ci, "ethernet", "EthernetNetworkElement");
			ngen.addBandwidthToConnection(ci, bandwidth);  //TODO: Should be constant default value
			mapRequestResource2ModelResource(bn, ci);
			ngen.declareModifyElementAddElement(reservation, ci);
		} catch (NdlException e) {
			logger().error("ExistingSliceModel:add(ComputeNode):" + e.getStackTrace());
		}	
	}

	@Override 
	public void add(StitchPort sp, String name, String label, String port) {
        logger().debug("add(StitchPort sp) sp: " + sp);
        Individual ni = null;
        try {
            //Add the stitchport
            ni = ngen.declareStitchingNode(name);
            mapRequestResource2ModelResource(sp, ni);
            ngen.addResourceToReservation(reservation, ni);
            ngen.addGuid(ni, UUID.randomUUID().toString());

            //Add a link to the stitchport
            Individual ei = ngen.declareNetworkConnection(name + "-net");
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

            ngen.declareModifyElementAddElement(reservation, ei);
            ngen.declareModifyElementAddElement(reservation, ni);

        } catch (NdlException e) {
            logger().error("ERROR: NewSliceModel::add(StitchPort) ");
            e.printStackTrace();
        }

    }
	
	@Override
	public void add(InterfaceNode2Net i) { 
		logger().debug("ExistingSliceModel:add(InterfaceNode2Net)");
		Resource r = this.getModelResource(i);
		Node node = i.getNode();
		Network net = i.getLink();

		try{
			Individual blI = null;
			if (net.isNew()){
				logger().debug("ExistingSliceModel:add(InterfaceNode2Net) 100:  isNew");
				if (net instanceof StitchPort) {
					logger().debug("ExistingSliceModel:add(InterfaceNode2Net) 110:  is stitchport");
					blI  = ngen.getRequestIndividual(net.getName()+"-net");
				} else {
					logger().debug("ExistingSliceModel:add(InterfaceNode2Net) 120:  !is sittchport");
					blI  = ngen.getRequestIndividual(net.getName());
				}
			} else {
				logger().debug("ExistingSliceModel:add(InterfaceNode2Net) 130:  !isNew");
				blI = ngen.declareModifiedBroadcastConnection(net.getURL());
				ngen.declareModifyElementAddElement(reservation, blI);
			}
			logger().debug("ExistingSliceModel:add(InterfaceNode2Net) 140:  blI: " + blI);
			
			Individual nodeI;
			if (node.isNew()){
				//New nodes
				nodeI = ngen.getRequestIndividual(node.getName());
			} else {
				logger().debug("ExistingSliceModel:add(InterfaceNode2Net): node.getURL = " + node.getURL());
				//Existing  node
				nodeI = ngen.declareModifiedComputeElement(node.getURL(), node.getGUID());
				ngen.declareModifyElementModifyNode(reservation, nodeI);
			}
				
			logger().debug("ExistingSliceModel:add(InterfaceNode2Net) 150:  nodeI:  " + nodeI);
			Individual intI;
			intI = ngen.declareInterface(net.getName()+"-"+node.getName());
			ngen.addInterfaceToIndividual(intI, blI);

			if (nodeI == null)
				throw new NdlException("Unable to find or create individual for node " + node);

			ngen.addInterfaceToIndividual(intI, nodeI);
			this.mapRequestResource2ModelResource(i, intI);
		} catch (NdlException e){
			logger().error("ERROR: ExistingSliceModel::add(InterfaceNode2Net) " );
			e.printStackTrace();
		}

	}

	@Override
	public void add(InterfaceNode2Net i, RequestResource depend) {
		logger().debug("ExistingSliceModel:add(InterfaceNode2Net, RequestResource)");
		Resource r = this.getModelResource(i);
		Node node = i.getNode();
		Network net = i.getLink();

		try{
			Individual blI = null;
			if (net.isNew()){
				logger().debug("ExistingSliceModel:add(InterfaceNode2Net, RequestResource) 100:  isNew");
				if (net instanceof StitchPort) {
					logger().debug("ExistingSliceModel:add(InterfaceNode2Net, RequestResource) 110:  is stitchport");
					blI  = ngen.getRequestIndividual(net.getName()+"-net");
				} else {
					logger().debug("ExistingSliceModel:add(InterfaceNode2Net, RequestResource) 120:  !is sittchport");
					blI  = ngen.getRequestIndividual(net.getName());
				}
			} else {
				logger().debug("ExistingSliceModel:add(InterfaceNode2Net, RequestResource) 130:  !isNew");
				blI = ngen.declareModifiedBroadcastConnection(net.getURL());
				ngen.declareModifyElementAddElement(reservation, blI);
			}
			logger().debug("ExistingSliceModel:add(InterfaceNode2Net, RequestResource) 140:  blI: " + blI);

			Individual nodeI;
			if (node.isNew()){
				//New nodes
				nodeI = ngen.getRequestIndividual(node.getName());
			} else {
				logger().debug("ExistingSliceModel:add(InterfaceNode2Net, RequestResource): node.getURL = " + node.getURL());
				//Existing  node
				nodeI = ngen.declareModifiedComputeElement(node.getURL(), node.getGUID());
				ngen.declareModifyElementModifyNode(reservation, nodeI);
				ngen.addDependOnToIndividual(ngen.getRequestIndividual(depend.getName()), nodeI);
            }

			logger().debug("ExistingSliceModel:add(InterfaceNode2Net, RequestResource) 150:  nodeI:  " + nodeI);
			Individual intI;
			intI = ngen.declareInterface(net.getName()+"-"+node.getName());
			ngen.addInterfaceToIndividual(intI, blI);

			if (nodeI == null)
				throw new NdlException("Unable to find or create individual for node " + node);

			ngen.addInterfaceToIndividual(intI, nodeI);
			this.mapRequestResource2ModelResource(i, intI);
		} catch (NdlException e){
			logger().error("ERROR: ExistingSliceModel::add(InterfaceNode2Net, RequestResource) " );
			e.printStackTrace();
		}
	}

	@Override
	public void add(StorageNode sn, String name) {
		logger().debug("ExistingSliceModel:add(StorageNode)");
		try {
			Individual ni = null;
			ni = ngen.declareISCSIStorageNode(name, sn.getCapacity(), sn.getFSType(), sn.getFSParam(), sn.getMntPoint(), sn.getDoFormat());
			ngen.addGuid(ni, UUID.randomUUID().toString());
			mapRequestResource2ModelResource(sn, ni);
			ngen.declareModifyElementAddElement(reservation, ni);
		} catch (NdlException e) {
			logger().error("ExistingSliceModel:add(StorageNode):" + e.getStackTrace());
		}
	}

	@Override
	public void remove(ComputeNode cn) {
		Resource ni = this.getModelResource(cn);
		try {
			// I don't understand why I can pass 'null' for the GUID
			ngen.declareModifyElementRemoveNode(reservation, ni.getURI(), null);
		} catch (NdlException e) {
			logger().error("ExistingSliceModel::remove(ComputeNode cn), Failed to declareModifyElementRemoveNode" );
			e.printStackTrace();
		}
	}

	@Override
	public void remove(BroadcastNetwork bn) {
		
		logger().debug("ExistingSliceModel::remove(BroadcastNetwork bn): " +   bn.getURL() + ", " + bn.getGUID());
		
		//TODO:  fix that it only removes Interfaces to nodes
		for (Interface i : bn.getInterfaces()){
			//this.remove((InterfaceNode2Net)i);
		}
		
		try{
			ngen.declareModifyElementRemoveLink(reservation, bn.getURL(), bn.getGUID());
		} catch (NdlException e) {
			logger().error("ExistingSliceModel::remove(BroadcastNetwork bn), Failed to declareModifyElementRemoveLink" );
			e.printStackTrace();
		}
	}

	@Override
	public void remove(StitchPort sp) {
		// TODO Auto-generated method stub
	}

	@Override
	public void remove(InterfaceNode2Net i) {
		// TODO Auto-generated method stub
		Resource ni = this.getModelResource(i);
		
		try{
			ngen.declareModifyElementRemoveNode(reservation, i.getURL(), i.getGUID());
		} catch (NdlException e) {
			logger().error("ExistingSliceModel::remove(BroadcastNetwork bn), Failed to declareModifyElementRemoveLink" );
			e.printStackTrace();
		}	
	}

	@Override
	public void remove(StorageNode sn) {
        Resource ni = this.getModelResource(sn);
        try {
            // I don't understand why I can pass 'null' for the GUID
            ngen.declareModifyElementRemoveNode(reservation, ni.getURI(), null);
        } catch (NdlException e) {
            logger().error("ExistingSliceModel::remove(StorageNode sn), Failed to declareModifyElementRemoveNode" );
            e.printStackTrace();
        }
	}
}
