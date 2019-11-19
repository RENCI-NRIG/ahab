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

package org.renci.ahab.libndl;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.renci.ahab.libndl.ndl.ExistingSliceModel;
import org.renci.ahab.libndl.ndl.NDLModel;
import org.renci.ahab.libndl.ndl.NewSliceModel;
import org.renci.ahab.libndl.resources.common.ModelResource;
import org.renci.ahab.libndl.resources.manifest.LinkConnection;
import org.renci.ahab.libndl.resources.manifest.ManifestResource;
import org.renci.ahab.libndl.resources.request.*;
import org.renci.ahab.libndl.util.IP4Assign;
import org.renci.ahab.libndl.util.IP4Subnet;
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
	private SparseMultigraph<ModelResource, Interface> sliceGraph = new SparseMultigraph<ModelResource, Interface>();
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

		Set<ModelResource> resources = new HashSet<ModelResource>(sliceGraph.getVertices());
		for (ModelResource r: resources)
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
		this.slice = slice;

	}

	public Collection<ModelResource> getResources(){
		return sliceGraph.getVertices();
	}

	protected SparseMultigraph<ModelResource, Interface> getGraph() {
		return sliceGraph;
	}

	public void printGraph() {
		LIBNDL.logger().debug("JUNG Graph: " + sliceGraph);
	}

	public NDLModel getNDLModel(){
		if(ndlModel == null) LIBNDL.logger().debug("SliceGraph.getNDLModel ndlModel is null");
		return ndlModel;
	}

	public String enableSliceStitching(RequestResource r, String secret){
		return slice.enableSliceStitching(r, secret);
	}

	public void increaseComputeNodeCount(ComputeNode node, int addCount){
		//modify.addNodesToGroup(node.getModelResource().getURI(), addCount);
	}

	//deletes the node at uri from the group node
	public void deleteComputeNode(ComputeNode node, String uri){
		LIBNDL.logger().debug("Request.deleteComputeNode: node = " + node + ", uri = " + uri);
		//modify.removeNodeFromGroup(node.getURI(), uri);
	}

	public ComputeNode buildComputeNode(String name){
		LIBNDL.logger().debug("PRUTH_BUILD: SliceGraph.buildComputeNode: adding node " + name);
		ComputeNode node = new ComputeNode(this,name);
		sliceGraph.addVertex(node);
		return node;
	}
    public StorageNode buildStorageNode(String name){
        LIBNDL.logger().debug("PRUTH_BUILD: SliceGraph.buildStorageNode: adding node " + name);
        StorageNode node = new StorageNode(this,name);
        sliceGraph.addVertex(node);
        return node;
    }
	public StorageNode buildStorageNode(String name, long capacity, String mntPoint){
		LIBNDL.logger().debug("PRUTH_BUILD: SliceGraph.buildStorageNode: adding node " + name);
		StorageNode node = new StorageNode(this, name);
		node.setCapacity(capacity);
		node.setMntPoint(mntPoint);
		sliceGraph.addVertex(node);
		return node;
	}
	public StitchPort buildStitchPort(String name, String label, String port, long bandwidth){
		StitchPort node = new StitchPort(this,name,label,port,bandwidth);
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

    public LinkNetwork buildLinkNetwork(String name){
        LinkNetwork link = new LinkNetwork(this,name);
        sliceGraph.addVertex(link);
        return link;
    }

	public InterfaceNode2Net buildInterfaceNode2Net(Node node, Network net){
		LIBNDL.logger().debug("PRUTH_BUILD_INTERFACE: " + node + ", " + net);
		InterfaceNode2Net i = new InterfaceNode2Net(node,net,this);
		sliceGraph.addEdge(i, node, net);
		return i;
	}

	public ComputeNode addComputeNode(String name){
		ComputeNode node = buildComputeNode(name);
		node.setIsNew(true);
		ndlModel.add(node,name);
		return node;
	}
	public StorageNode addStorageNode(String name, long capacity, String mntPoint){
		StorageNode node = buildStorageNode(name, capacity, mntPoint);
		node.setIsNew(true);
		ndlModel.add(node, name);
		return node;
	}
	public StitchPort addStitchPort(String name, String label, String port, long bandwidth){
		StitchPort node = buildStitchPort(name, label, port, bandwidth);
		node.setIsNew(true);
		ndlModel.add(node, name, label, port);
		return node;
	}
	public Network addLink(String name, long bandwidth){
		BroadcastNetwork link = buildLink(name);
		link.setIsNew(true);
		ndlModel.add(link, name, bandwidth);
		return link;
	}

    public LinkNetwork addLinkNetwork(String name, long bandwidth){
        LinkNetwork link = buildLinkNetwork(name);
        link.setIsNew(true);
        ndlModel.add(link, name, bandwidth);
        return link;
    }
	public BroadcastNetwork addBroadcastLink(String name, long bandwidth){
		BroadcastNetwork link = buildBroadcastLink(name);
		link.setIsNew(true);
		ndlModel.add(link, name, bandwidth);
		return link;
	}

	public void addStitch(RequestResource a, RequestResource b, Interface s){
		if(s instanceof InterfaceNode2Net){
			ndlModel.add((InterfaceNode2Net)s);
		}
		if(s instanceof InterfaceStitchPort2StitchPort){
			ndlModel.add((InterfaceStitchPort2StitchPort)s);
		}
		sliceGraph.addEdge(s, a, b);
	}

	public void addStitch(RequestResource a, RequestResource b, Interface s, RequestResource depend){
		if(s instanceof InterfaceNode2Net){
			ndlModel.add((InterfaceNode2Net)s, depend);
		}
		sliceGraph.addEdge(s, a, b);
	}

	public RequestResource getResourceByName(String nm){
		if (nm == null)
			return null;
		LIBNDL.logger().debug("RAW jung graph: " + sliceGraph);
		for (ModelResource n: sliceGraph.getVertices()) {
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
		for (ModelResource n: sliceGraph.getVertices()) {
			LIBNDL.logger().debug("getResourceByURI: " + n.getName());
		}
		return null;
	}

    public void deleteResource(StorageNode node){
        ndlModel.remove(node);
        deleteResource((RequestResource)node);
    }

	public void deleteResource(ComputeNode node){
		ndlModel.remove(node);
		deleteResource((RequestResource)node);
	}

	public void deleteResource(BroadcastNetwork bn){
		for (Interface i: bn.getInterfaces()){
			this.deleteResource(i);
		}
		ndlModel.remove(bn);

		sliceGraph.removeVertex(bn);
		deleteResource((RequestResource)bn);
	}

    public void deleteResource(StitchPort sp){
        for (Interface i: sp.getInterfaces()){
            InterfaceNode2Net interfaceNode2Net = (InterfaceNode2Net)i;
            this.deleteResource(i);
        }
        ndlModel.remove(sp);

        sliceGraph.removeVertex(sp);
        deleteResource((RequestResource)sp);
    }

	public void deleteResource(RequestResource requestResource){
		sliceGraph.removeVertex(requestResource);
	}

	public void deleteResource(Interface i){
        //TODO: Only handles InterfaceNode2Net for now
        ndlModel.remove((InterfaceNode2Net)i);
        sliceGraph.removeEdge(i);
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

	public Collection<Network> getLinks(){
		ArrayList<Network> links = new ArrayList<Network>();

		for (ModelResource resource: sliceGraph.getVertices()) {
			if(resource instanceof Network){
				links.add((Network)resource);
			}
		}
		return links;
	}

	public Collection<BroadcastNetwork> getBroadcastLinks(){
		ArrayList<BroadcastNetwork> broadcastlinks = new ArrayList<BroadcastNetwork>();

		for (ModelResource resource: sliceGraph.getVertices()) {
			if(resource instanceof BroadcastNetwork){
				broadcastlinks.add((BroadcastNetwork)resource);
			}
		}
		return broadcastlinks;
	}

	public Collection<Node> getNodes(){
		ArrayList<Node> nodes = new ArrayList<Node>();

		for (ModelResource resource: sliceGraph.getVertices()) {
			if(resource instanceof Node){
				nodes.add((Node)resource);
			}
		}
		return nodes;
	}

	public Collection<ComputeNode> getComputeNodes(){
		ArrayList<ComputeNode> nodes = new ArrayList<ComputeNode>();

		for (ModelResource resource: sliceGraph.getVertices()) {
			if(resource instanceof ComputeNode){
				nodes.add((ComputeNode)resource);
			}
		}
		return nodes;
	}

	public Collection<StorageNode> getStorageNodes(){
		ArrayList<StorageNode> nodes = new ArrayList<StorageNode>();

		for (ModelResource resource: sliceGraph.getVertices()) {
			if(resource instanceof StorageNode){
				nodes.add((StorageNode)resource);
			}
		}
		return nodes;
	}
	public Collection<StitchPort> getStitchPorts(){
		ArrayList<StitchPort> nodes = new ArrayList<StitchPort>();

		for (ModelResource resource: sliceGraph.getVertices()) {
			if(resource instanceof StitchPort){
				nodes.add((StitchPort)resource);
			}
		}
		return nodes;
	}

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

	public void loadNewRequest(){
		rawLoadedRDF = "New Request";
		NewSliceModel model = new NewSliceModel();
		model.init(this);
		this.ndlModel = model;
	}

	public void loadRequestRDF(String rdf){
		rawLoadedRDF = rdf;
		this.ndlModel = new NewSliceModel();
		this.ndlModel.init(this,rdf);
	}

	public void loadManifestRDF(String rdf){
		LIBNDL.logger().debug("SliceGraph::loadManifestRDF");
		rawLoadedRDF = rdf;
		this.ndlModel = new ExistingSliceModel();
		((ExistingSliceModel)this.ndlModel).init(this,rdf);

		LIBNDL.logger().debug("SliceGraph::loadManifestRDF this.ndlModel = " + this.ndlModel);
	}

	public void save(String file){
		saveNewRequest(file);
	}

	public void saveNewRequest(String file){
	}

	public void saveModifyRequest(String file){
	}

	public String getRDFString(){
		return ndlModel.getRequest();
	}

	public boolean autoAssignIPAddresses() {
		return true;
	}

	public String getDebugString(){
		String rtnStr = "getRequestDebugString: ";
		for (ModelResource r : sliceGraph.getVertices()){
			rtnStr += ", Resource:" + r.getName();
		}
		return rtnStr;
	}

	public String getSliceGraphString(){
		return sliceGraph.toString();
	}

	public Object getDomainInReservation() {
		// TODO Auto-generated method stub
		return null;
	}

	public void addNetworkConnection(String string) {
        // TODO Auto-generated method stub

    }

	public LinkConnection addLinkConnection(String string) {
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
