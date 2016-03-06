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

package orca.ahab.libndl;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import orca.ahab.libndl.ndl.ExistingSliceModel;
import orca.ahab.libndl.ndl.ManifestLoader;
import orca.ahab.libndl.ndl.ModifyGenerator;
import orca.ahab.libndl.ndl.NDLModel;
import orca.ahab.libndl.ndl.NewSliceModel;
import orca.ahab.libndl.ndl.RequestGenerator;
import orca.ahab.libndl.ndl.RequestLoader;
import orca.ahab.libndl.resources.manifest.LinkConnection;
import orca.ahab.libndl.resources.manifest.ManifestResource;
import orca.ahab.libndl.resources.request.BroadcastNetwork;
import orca.ahab.libndl.resources.request.ComputeNode;
import orca.ahab.libndl.resources.request.Interface;
import orca.ahab.libndl.resources.request.Network;
import orca.ahab.libndl.resources.request.Node;
import orca.ahab.libndl.resources.request.RequestReservationTerm;
import orca.ahab.libndl.resources.request.RequestResource;
import orca.ahab.libndl.resources.request.StitchPort;
import orca.ahab.libndl.resources.request.StorageNode;
import orca.ahab.libndl.util.IP4Assign;
import orca.ahab.libndl.util.IP4Subnet;
import edu.uci.ics.jung.graph.SparseMultigraph;

/**
 * Singleton class that holds shared NDLLIB request state. Since dialogs are all modal, no need for locking for now.
 * @author ibaldin
 * @author pruth
 *
 */
public class SliceGraph   {
	private Slice slice; 
	private NDLModel ndlModel;
	
	private SparseMultigraph<RequestResource, Interface> sliceGraph = new SparseMultigraph<RequestResource, Interface>();
	private SparseMultigraph<ManifestResource, Interface> manifestGraph = new SparseMultigraph<ManifestResource, Interface>();
	
	private String rawLoadedRDF; //original rdf loaded. used for reseting uncommited modifies
	
	//Obeject for managing subnets for autoIP functionallity
	private IP4Assign ipAssign;
	
	
	private static final String IMAGE_NAME_SUFFIX = "-req";
	public static final String NO_GLOBAL_IMAGE = "None";
	public static final String NO_DOMAIN_SELECT = "System select";
	public static final String NODE_TYPE_SITE_DEFAULT = "Site default";
	public static final String NO_NODE_DEPS="No dependencies";
	private static final String RDF_START = "<rdf:RDF";
	private static final String RDF_END = "</rdf:RDF>";

	
	// is it openflow (and what version [null means non-of])
	private String ofNeededVersion = null;
	private String ofUserEmail = null;
	private String ofSlicePass = null;
	private String ofCtrlUrl = null;
	
	// File in which we save
	File saveFile = null;
	
	// Reservation details
	private RequestReservationTerm term;
	private String resDomainName = null;
	
	// save the guid of the namespace of the request if it was loaded
	String nsGuid = null;
	
	private static void initialize() {
		;
	}

	
	public SliceGraph(Slice slice) {
		// clear the graph, reservation set else to defaults
		if (sliceGraph == null)
			return;
		
		Set<RequestResource> resources = new HashSet<RequestResource>(sliceGraph.getVertices());
		for (RequestResource r: resources)
			sliceGraph.removeVertex(r);
		resDomainName = null;
		term = new RequestReservationTerm();
		ofNeededVersion = null;
		ofUserEmail = null;
		ofSlicePass = null;
		ofCtrlUrl = null;
		nsGuid = null;
		saveFile = null;
		rawLoadedRDF = null;
		
		ipAssign = new IP4Assign();
		
	}
	
	public Collection<RequestResource> getResources(){
		return sliceGraph.getVertices();
	}
	
	protected SparseMultigraph<RequestResource, Interface> getGraph() {
		return sliceGraph;
	}
	
	//public boolean isNewRequest(){
	//	return true;
	//}
	
	public NDLModel getNDLModel(){
		return ndlModel;
	}
	/*************************************   Add/Delete/Get resources  ************************************/
	
	public void increaseComputeNodeCount(ComputeNode node, int addCount){
		//modify.addNodesToGroup(node.getModelResource().getURI(), addCount);
	}
	
	//deletes the node at uri from the group node
	public void deleteComputeNode(ComputeNode node, String uri){
		LIBNDL.logger().debug("Request.deleteComputeNode: node = " + node + ", uri = " + uri);
		//modify.removeNodeFromGroup(node.getURI(), uri);
	}
	
	
	/************ Build resources in jung model without adding to ndlmodel **********/
	public ComputeNode buildComputeNode(String name){
		LIBNDL.logger().debug("SliceGraph.addComputeNode: adding node " + name);
		ComputeNode node = new ComputeNode(this,name);
		sliceGraph.addVertex(node);
		
		return node;
	}
	public StorageNode buildStorageNode(String name){
		StorageNode node = new StorageNode(this,name);
		sliceGraph.addVertex(node);
		return node;
	}
	public StitchPort buildStitchPort(String name){
		StitchPort node = new StitchPort(this,name);
		sliceGraph.addVertex(node);
		return node;
	}
	public BroadcastNetwork buildLink(String name){
		BroadcastNetwork link = new BroadcastNetwork(this,name);
		sliceGraph.addVertex(link);
		return link;
	}
	public BroadcastNetwork buildBroadcastLink(String name){
		BroadcastNetwork link = new BroadcastNetwork(this,name);
		sliceGraph.addVertex(link);
		return link;
	}
	
	/************************ build resources and add them to ndl model ************/
	public ComputeNode addComputeNode(String name){
		ComputeNode node = buildComputeNode(name);
		ndlModel.add(node,name);	
		return node;
	}
	public StorageNode addStorageNode(String name){
		StorageNode node = buildStorageNode(name);
		ndlModel.add(node);
		return node;
	}
	public StitchPort addStitchPort(String name){
		StitchPort node = buildStitchPort(name);
		ndlModel.add(node);
		return node;
	}
	public Network addLink(String name){
		BroadcastNetwork link = buildLink(name);
		ndlModel.add(link);
		return link;
	}
	public BroadcastNetwork addBroadcastLink(String name){
		BroadcastNetwork link = buildBroadcastLink(name);
		ndlModel.add(link);
		return link;
	}
	
	
	public RequestResource getResourceByName(String nm){
		if (nm == null)
			return null;
		LIBNDL.logger().debug("RAW jung graph: " + sliceGraph);
		for (RequestResource n: sliceGraph.getVertices()) {
			if (nm.equals(n.getName()) && n instanceof RequestResource){
				return (RequestResource)n;
			}
		}
		return null;
	}
	
	public RequestResource getResourceByURI(String uri){
		LIBNDL.logger().debug("getResourceByURI: " + uri);
		
		if (uri == null)
			return null;
		LIBNDL.logger().debug("getResourceByURI: " + sliceGraph);
		for (RequestResource n: sliceGraph.getVertices()) {
			LIBNDL.logger().debug("getResourceByURI: " + n.getName());
			//LIBNDL.logger().debug("getResourceByURI: " + n.getURI());
			//if (n.getURI() != null && uri.equals(n.getURI()) && n instanceof RequestResource){
			//	LIBNDL.logger().debug("getResourceByURI: returning " + n.getName());
			//	return (RequestResource)n;
			//}
		}
		return null;
	}
	
	public void deleteResource(RequestResource r){
		for (Interface s: r.getInterfaces()){
			sliceGraph.removeEdge(s);
		}
		sliceGraph.removeVertex(r);
	}
	
	public void addStitch(RequestResource a, RequestResource b, Interface s){
		sliceGraph.addEdge(s, a, b);
	}
	
	public Collection<Interface> getInterfaces(){
		ArrayList<Interface> interfaces = new ArrayList<Interface>();
		
		for (Interface i: sliceGraph.getEdges()) {
			if (i instanceof Interface){
				interfaces.add((Interface)i);
			}
		}
		return interfaces;
	}
	
	public Collection<Interface> getInterfaces(RequestResource r){
		return sliceGraph.getIncidentEdges(r);
	}
	
	 
	
	public void clear(){
		//reset the whole request
	}
	
	
	
	/*************************************   Request level properties:  domain,term,user, etc. ************************************/
	
	public RequestReservationTerm getTerm() {
		return term;
	}
	
	public void setTerm(RequestReservationTerm t) {
		term = t;
	}
	
	public void setNsGuid(String g) {
		nsGuid = g;
	}
	
	public void setOfUserEmail(String ue) {
		ofUserEmail = ue;
	}
	
	public String getOfUserEmail() {
		return ofUserEmail;
	}
	
	public void setOfSlicePass(String up) {
		ofSlicePass = up;
	}
	
	public String getOfSlicePass() {
		return ofSlicePass;
	}
	
	public void setOfCtrlUrl(String cu) {
		ofCtrlUrl = cu;
	}
	
	public String getOfCtrlUrl() {
		return ofCtrlUrl;
	}
	
	/**************************************  Add/remove resources *******************************/
	public Collection<Network> getLinks(){
		ArrayList<Network> links = new ArrayList<Network>();
		
		for (RequestResource resource: sliceGraph.getVertices()) {
			if(resource instanceof Network){
				links.add((Network)resource);
			}
		}
		return links;
	}
		

	
	public Collection<BroadcastNetwork> getBroadcastLinks(){
		ArrayList<BroadcastNetwork> broadcastlinks = new ArrayList<BroadcastNetwork>();
		
		for (RequestResource resource: sliceGraph.getVertices()) {
			if(resource instanceof BroadcastNetwork){
				broadcastlinks.add((BroadcastNetwork)resource);
			}
		}
		return broadcastlinks;
	}
	
	public Collection<Node> getNodes(){
		ArrayList<Node> nodes = new ArrayList<Node>();
		
		for (RequestResource resource: sliceGraph.getVertices()) {
			if(resource instanceof Node){
				nodes.add((Node)resource);
			}
		}
		return nodes;
	}
	
	public Collection<ComputeNode> getComputeNodes(){
		ArrayList<ComputeNode> nodes = new ArrayList<ComputeNode>();
		
		for (RequestResource resource: sliceGraph.getVertices()) {
			if(resource instanceof ComputeNode){
				nodes.add((ComputeNode)resource);
			}
		}
		return nodes;
	}
	
	public Collection<StorageNode> getStorageNodes(){
		ArrayList<StorageNode> nodes = new ArrayList<StorageNode>();
		
		for (RequestResource resource: sliceGraph.getVertices()) {
			if(resource instanceof StorageNode){
				nodes.add((StorageNode)resource);
			}
		}
		return nodes;
	}	
	public Collection<StitchPort> getStitchPorts(){
		ArrayList<StitchPort> nodes = new ArrayList<StitchPort>();
		
		for (RequestResource resource: sliceGraph.getVertices()) {
			if(resource instanceof StitchPort){
				nodes.add((StitchPort)resource);
			}
		}
		return nodes;
	}	
	
	/*************************************   AutoIP mthrow e;ethods ************************************/
	public IP4Subnet setSubnet(String ip, int maskLength){
		IP4Subnet subnet = null;
		try{
			subnet = ipAssign.getSubnet((Inet4Address)InetAddress.getByName(ip), maskLength);
			subnet.markIPUsed(subnet.getStartIP());
		} catch (Exception e){
			LIBNDL.logger().warn("setSubnet warning: " + e);
		}
		return subnet;
	}
	
	public IP4Subnet allocateSubnet(int count){
		IP4Subnet subnet = null;
		try{
			subnet = ipAssign.getAvailableSubnet(count);
			subnet.markIPUsed(subnet.getStartIP());
		} catch (Exception e){
			LIBNDL.logger().warn("allocateSubnet warning: " + e);
		}
		return subnet;
	}
	
	public void autoIP(){
		LIBNDL.logger().debug("autoIP unimplemented");
	}
	
	
	/*************************************   RDF Functions:  save, load, getRDFString, etc. ************************************/
	
	public void loadRequestRDF(String rdf){
		rawLoadedRDF = rdf;
		this.ndlModel = new NewSliceModel(this,rdf);
	}
	
	public void loadManifestRDF(String rdf){
		rawLoadedRDF = rdf;
		this.ndlModel = new ExistingSliceModel(this,rdf);
	}
	
	public void save(String file){
		saveNewRequest(file);
	}
	
	public void saveNewRequest(String file){
		RequestGenerator saver = new RequestGenerator(this);
		SliceGraph r = new SliceGraph(slice);
		saver.saveRequest(file);
	}
	
	public void saveModifyRequest(String file){
		RequestGenerator saver = new RequestGenerator(this);
		SliceGraph r = new SliceGraph(slice);
		saver.saveModifyRequest(file);
	}
	
	public String getRDFString(){
		//RequestGenerator saver = new RequestGenerator(this);
		//SliceGraph r = new SliceGraph(slice);
		return ndlModel.getRequest();
	}
	

	
	
	/*************************************   Higher level Functionality:  autoip, etc. ************************************/
	
	public boolean autoAssignIPAddresses() {
		return true;
	}
	
	/*************************************   debugging ************************************/
	public String getDebugString(){
		String rtnStr = "getRequestDebugString: ";
		rtnStr += sliceGraph.toString();
		return rtnStr;
	}


	public Object getDomainInReservation() {
		// TODO Auto-generated method stub
		return null;
	}


	public void addNetworkConnection(String string) {
		// TODO Auto-generated method stub
		
	}


	public LinkConnection addLinkConnection(String string) {
		// TODO Auto-generated method stub
		return null;
	}


	public void addCrossConnect(String string) {
		// TODO Auto-generated method stub
		
	}


	public Node addNode(String string) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
}