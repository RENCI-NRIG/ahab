/*
* Copyright (c) 2011 RENCI/UNC Chapel Hill 
*
* @author Ilia Baldine
*
* Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
* and/or hardware specification (the "Work") to deal in the Work without restriction, including 
* without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or 
* sell copies of the Work, and to permit persons to whom the Work is furnished to do so, subject to 
* the following conditions:  
* The above copyright notice and this permission notice shall be included in all copies or 
* substantial portions of the Work.  
*
* THE WORK IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
* OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF 
* MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND 
* NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT 
* HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, 
* WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, 
* OUT OF OR IN CONNECTION WITH THE WORK OR THE USE OR OTHER DEALINGS 
* IN THE WORK.
*/
package org.renci.ahab.libndl.ndl;

import java.util.Date;
import java.util.List;
import java.util.Set;
import orca.ndl.INdlRequestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlGenerator;
import orca.ndl.NdlRequestParser;
import org.apache.commons.lang.StringUtils;
import org.renci.ahab.libndl.LIBNDL;
import org.renci.ahab.libndl.SliceGraph;
import org.renci.ahab.libndl.resources.common.ModelResource;
import org.renci.ahab.libndl.resources.request.ComputeNode;
import org.renci.ahab.libndl.resources.request.InterfaceNode2Net;
import org.renci.ahab.libndl.resources.request.Network;
import org.renci.ahab.libndl.resources.request.Node;
import org.renci.ahab.libndl.resources.request.RequestReservationTerm;
import org.renci.ahab.libndl.resources.request.RequestResource;
import org.renci.ahab.libndl.resources.request.StitchPort;
import org.renci.ahab.libndl.resources.request.StorageNode;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;


public class RequestLoader extends NDLLoader  implements INdlRequestModelListener {
	
	private NDLModel ndlModel;
	private RequestReservationTerm term = new RequestReservationTerm();

	private NdlRequestParser nrp;
	
	public RequestLoader(SliceGraph sliceGraph, NDLModel ndlModel){
		this.sliceGraph = sliceGraph;
		this.ndlModel = ndlModel;
	}

	/**
	 * Load from string
	 * @param f
	 * @return
	 */
	public NdlRequestParser load(String rdf) {
		try {
			nrp = new NdlRequestParser(rdf, this);
			nrp.doLessStrictChecking(); //TODO: Should be removed...
			nrp.processRequest();
			//nrp.freeModel();
			
		} catch (Exception e) {
			LIBNDL.logger().error(e);
			LIBNDL.logger().debug("error loading graph");
			return null;
		} 
		
		return nrp;
	}

	public NdlGenerator getGenerator() {
		NdlGenerator ngen = null;
		try{
		return new NdlGenerator(nrp, LIBNDL.logger());
		} catch (Exception e){
			LIBNDL.logger().error(e);
			LIBNDL.logger().debug("error loading NdlGenerator");
		}
		return ngen;
	}
	

	public void ndlReservation(Resource i, final OntModel m) {
		
		LIBNDL.logger().debug("Reservation: " + i + ", sliceState(Request:ndlReservation) = " + NdlCommons.getGeniSliceStateName(i));
		// try to extract the guid out of the URL
		String u = i.getURI();
		String guid = StringUtils.removeEnd(StringUtils.removeStart(u, NdlCommons.ORCA_NS), "#");
		
		this.sliceGraph.setNsGuid(guid);
	}

	public void ndlReservationEnd(Literal e, OntModel m, Date end) {
		// Nothing to do
	}

	public void ndlReservationStart(Literal s, OntModel m, Date start) {
		term.setStart(start);
	}

	public void ndlReservationTermDuration(Resource t, OntModel m, int years, int months, int days,
			int hours, int minutes, int seconds) {
		term.setDuration(days, hours, minutes);
	}

	public void ndlNode(Resource ce, OntModel om, Resource ceClass, List<Resource> interfaces) {
		LIBNDL.logger().debug("Node: " + ce + " of class " + ceClass + ", ce objext class: " + ce.getClass());
		
		if (ce == null)
			return;
		
		Node newNode;
		ComputeNode newComputeNode = null;
		if (ceClass.equals(NdlCommons.computeElementClass)){
			//if(!ce.hasProperty(NdlCommons.manifestHasParent)){
			LIBNDL.logger().debug("BUILDING: Compute Node: " + ce.getLocalName() + " : found computeElementClass, parent = " + ce.hasProperty(NdlCommons.manifestHasParent));

			newNode = this.sliceGraph.buildComputeNode(ce.getLocalName());
			newComputeNode = (ComputeNode)newNode;
			ndlModel.mapRequestResource2ModelResource(newComputeNode, ce);
			LIBNDL.logger().debug("newComputeNode.getName(): " + newComputeNode.getName());
			Resource returnedResource = ndlModel.getModelResource(newComputeNode);
			LIBNDL.logger().debug("returnedResource.getLocalName(): " + returnedResource.getLocalName());
			ndlModel.printRequest2NDLMap();
		} else if (ceClass.equals(NdlCommons.serverCloudClass)) {
			LIBNDL.logger().debug("BUILDING: Group Node: " + ce.getLocalName() + " : found serverCloudClass, parent = " + ce.hasProperty(NdlCommons.manifestHasParent));
			ComputeNode newNodeGroup = this.sliceGraph.buildComputeNode(ce.getLocalName());
			ndlModel.mapRequestResource2ModelResource(newNodeGroup, ce);
			newComputeNode = newNodeGroup;
			newNodeGroup.initializeNodeCount(0);
			newNode = newNodeGroup;

			String groupUrl = NdlCommons.getRequestGroupURLProperty(ce);
			LIBNDL.logger().debug("NdlCommons.getRequestGroupURLProperty: " + groupUrl);

			String nodeUrl = ce.getURI();
			LIBNDL.logger().debug("URI: " + nodeUrl);
		}  else if (NdlCommons.isNetworkStorage(ce)) {
			LIBNDL.logger().debug("BUILDING: Storage Node: " + ce.getLocalName() );
			// storage node
			StorageNode snode = this.sliceGraph.buildStorageNode(ce.getLocalName());
			ndlModel.mapRequestResource2ModelResource(snode, ce);
			newNode = snode;
			snode.setCapacity(NdlCommons.getResourceStorageCapacity(ce));
		} else {
			// default just a node
			LIBNDL.logger().debug("BUILDING: Just a Node: " + ce.getLocalName() );
			newNode = this.sliceGraph.buildComputeNode(ce.getLocalName());
			ndlModel.mapRequestResource2ModelResource(newNode, ce);
		}
		
		sliceGraph.printGraph();
		
		LIBNDL.logger().debug("about to load domain");
		Resource domain = NdlCommons.getDomain(ce);
		if (domain != null){
			LIBNDL.logger().debug("load domain: " + RequestGenerator.reverseLookupDomain(domain));
			newNode.setDomain(RequestGenerator.reverseLookupDomain(domain));
		}
		
		if (ceClass.equals(NdlCommons.computeElementClass) || ceClass.equals(NdlCommons.serverCloudClass)){
			Resource ceType = NdlCommons.getSpecificCE(ce);
			if (ceType != null){
				newComputeNode.setNodeType(RequestGenerator.reverseNodeTypeLookup(ceType));
			}
		}

		//process image
		LIBNDL.logger().debug("about to load image");
		if (ceClass.equals(NdlCommons.computeElementClass) || ceClass.equals(NdlCommons.serverCloudClass)){
			LIBNDL.logger().debug("about to load domain: it is a compute element");
			// disk image
			Resource di = NdlCommons.getDiskImage(ce);
			if (di != null) {
				LIBNDL.logger().debug("about to load domain: it has a image");
				try {
					String imageURL = NdlCommons.getIndividualsImageURL(ce);
					String imageHash = NdlCommons.getIndividualsImageHash(ce);
					String imName = newComputeNode.getName() + "-image"; //FIX ME: not right:  why do we even have an image name???
					newComputeNode.setImage(imageURL,imageHash,imName);
				} catch (Exception e) {
					// FIXME:SliceGraph ?
					LIBNDL.logger().debug("about to load domain: hit an exception");
					;
				}
			}

			// post boot script
			String script = NdlCommons.getPostBootScript(ce);
			if ((script != null) && (script.length() > 0)) {
				newComputeNode.setPostBootScript(script);
			}
		}
	}

	/**
	 * For now deals only with p-to-p connections
	 */
	public void ndlNetworkConnection(Resource l, OntModel om, 
			long bandwidth, long latency, List<Resource> interfaces) {
		
		LIBNDL.logger().debug("NetworkConnection: " + l);
		if (l == null)
			return;
		
		if (NdlCommons.isStitchingNode(l)) {
			LIBNDL.logger().debug("BUILDING: Stitching Node: " + l.getLocalName() );
			// stitching node
			// For some reason the properties of the stitchport are stored on the interface (not here)
			StitchPort sp = this.sliceGraph.buildStitchPort(l.getLocalName(),"FAKE_LABEL","FAKE_PORT",bandwidth);
			ndlModel.mapRequestResource2ModelResource(sp, l);
		} else {
			Network ol = this.sliceGraph.buildLink(l.getLocalName());
			ndlModel.mapRequestResource2ModelResource(ol, l);
			ol.setBandwidth(bandwidth);
			ol.setLatency(latency);
			ol.setLabel(NdlCommons.getLayerLabelLiteral(l));
		}
	}

	public void ndlInterface(Resource intf, OntModel om, Resource conn, Resource node, String ip, String mask) {
	
		LIBNDL.logger().debug("Interface: " + intf + " link: " + conn + " node: " + node);
		if(intf == null){
			
			return;
		}
		
		RequestResource onode = null;
		if(node != null){
			ndlModel.printRequest2NDLMap();
			onode = this.sliceGraph.getResourceByName(node.getLocalName());
			LIBNDL.logger().debug("ndlInterface with node: " + onode + ", localName: " + node.getLocalName());
		} else {
			LIBNDL.logger().debug("ndlInterface with null node: " + intf);
		}
		RequestResource olink = null;
		if(conn != null){
			olink = this.sliceGraph.getResourceByName(conn.getLocalName());
		} else{
			LIBNDL.logger().warn("ndlInterface with null connection: " + intf);
		}
		
		if(onode == null){
			LIBNDL.logger().warn("ndlInterface with null missing node:  Interface: " + intf + ", Node: " + node);
			return;
		}
		
		//ComputeNode
		if(onode instanceof ComputeNode && olink instanceof Network){
			LIBNDL.logger().debug("stitching compute node");
			InterfaceNode2Net stitch = sliceGraph.buildInterfaceNode2Net((Node)onode, (Network)olink);
			ndlModel.mapRequestResource2ModelResource((ModelResource)stitch, intf);
			stitch.setIpAddress(ip);  
			stitch.setNetmask(mask);
			return;
		} 
		
		//StorageNode
		if(onode instanceof StorageNode){
			LIBNDL.logger().debug("stitching storage node");
			InterfaceNode2Net stitch = sliceGraph.buildInterfaceNode2Net((Node)onode, (Network)olink);
			return;
		}
		
		//StitchPort
		if(onode instanceof StitchPort){
			LIBNDL.logger().debug("stitching stitchport");
			//Why is this stuff stored in the interface? 
			//It Seems like they are properties of the stitchport itself. 
			StitchPort sp = (StitchPort)onode;
			sp.setPort(intf.toString());
			sp.setLabel(NdlCommons.getLayerLabelLiteral(intf));
			
			InterfaceNode2Net stitch = sliceGraph.buildInterfaceNode2Net((Node)onode, (Network)olink);
			return;
		}	
		
		//shouldnt get here
		LIBNDL.logger().debug("Stitching to unknown node type: " + node + ", " + node.getClass());
	}
	
	public void ndlSlice(Resource sl, OntModel m) {
		
		LIBNDL.logger().debug("Slice: " + sl + ", sliceState(sliceGraph) = " + NdlCommons.getGeniSliceStateName(sl));
		// check that this is an OpenFlow slice and get its details
		if (sl.hasProperty(NdlCommons.RDF_TYPE, NdlCommons.ofSliceClass)) {
			Resource ofCtrl = NdlCommons.getOfCtrl(sl);
			if (ofCtrl == null)
				return;
			this.sliceGraph.setOfCtrlUrl(NdlCommons.getURL(ofCtrl));
			this.sliceGraph.setOfUserEmail(NdlCommons.getEmail(sl));
			this.sliceGraph.setOfSlicePass(NdlCommons.getSlicePassword(sl));
			if ((this.sliceGraph.getOfUserEmail() == null) ||
					(this.sliceGraph.getOfSlicePass() == null) ||
					(this.sliceGraph.getOfCtrlUrl() == null)) {
					// disable OF if invalid parameters
					//this.sliceGraph.setNoOF();
					this.sliceGraph.setOfCtrlUrl(null);
					this.sliceGraph.setOfSlicePass(null);
					this.sliceGraph.setOfUserEmail(null);
			}
		}	
	}

	public void ndlReservationResources(List<Resource> res, OntModel m) {
		// nothing to do here in this case
	}
	
	public void ndlParseComplete() {
		LIBNDL.logger().debug("Done parsing.");
		// set term etc
		this.sliceGraph.setTerm(term);
		//this.sliceGraph.setDomainInReservation(reservationDomain);
	}

	public void ndlNodeDependencies(Resource ni, OntModel m, Set<Resource> dependencies) {
		LIBNDL.logger().debug("nlNodeDependencies -- SKIPPED");
	}

	/**
	 * Process a broadcast link
	 */
	public void ndlBroadcastConnection(Resource bl, OntModel om,
			long bandwidth, List<Resource> interfaces) {
		
		LIBNDL.logger().debug("BroadcastConnection: " + bl);
		
		Network ol = this.sliceGraph.buildLink(bl.getLocalName());
		ndlModel.mapRequestResource2ModelResource(ol, bl);
		ol.setBandwidth(bandwidth);
		ol.setLabel(NdlCommons.getLayerLabelLiteral(bl));
	}
}
