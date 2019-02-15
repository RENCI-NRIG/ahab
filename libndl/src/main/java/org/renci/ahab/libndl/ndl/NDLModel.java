package org.renci.ahab.libndl.ndl;


import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.renci.ahab.libndl.LIBNDL;
import org.renci.ahab.libndl.SliceGraph;
import org.renci.ahab.libndl.resources.common.ModelResource;
import org.renci.ahab.libndl.resources.request.*;
import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import edu.uci.ics.jung.graph.util.Pair;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

public abstract class NDLModel {
	//reference to the whole jena model
	OntModel jenaModel = null;

	/* map of RequestResource in slice changes to ndl Resource */
	protected Map<ModelResource, Resource> request2NDLMap;
    private static final String XML_SCHEMA_INTEGER = "http://www.w3.org/2001/XMLSchema#integer";


    /* ndl generation */
	protected NdlGenerator ngen;
	protected Individual reservation;

	abstract public void init(SliceGraph sliceGraph, String rdf);
	abstract public boolean isNewSlice();

	abstract public void add(ComputeNode cn, String name);
	abstract public void add(BroadcastNetwork bn, String name, long bandwidth);
	abstract public void add(StitchPort sp, String name, String label, String port);
	abstract public void add(InterfaceNode2Net i);
	abstract public void add(InterfaceNode2Net i, RequestResource depend);
	abstract public void add(StorageNode sn, String name);
    abstract public void add(LinkNetwork bn, String name, long bandwidth);
    abstract public void remove(ComputeNode cn);
	abstract public void remove(BroadcastNetwork bn);
	abstract public void remove(StitchPort sp);
	abstract public void remove(InterfaceNode2Net i);
	abstract public void remove(StorageNode sn);

	protected NDLModel(){
		request2NDLMap = new HashMap<ModelResource,Resource>();
	}

	protected void mapRequestResource2ModelResource(ModelResource r, Resource i){
		request2NDLMap.put(r,i);
	}

	protected Resource getModelResource(ModelResource cn){
		return request2NDLMap.get(cn);
	}

	protected void setJenaModel(OntModel om){
		jenaModel = om;
	}

	protected OntModel getJenaModel(){
		return jenaModel;
	}

	public void printRequest2NDLMap(){
		LIBNDL.logger().debug("NDLModle::printRequest2NDLMap: " + request2NDLMap);
	}

	protected Logger logger(){
		return LIBNDL.logger();
	}

	abstract public String getRequest();

    public String getURL(ModelResource modelResource){
    	return this.getModelResource(modelResource).getURI();
    }
    public void setURL(ModelResource modelResource, String url){
    	//not implemented.  should it be?  i'm not sure.
    }

	public String getGUID(ModelResource modelResource){
		return NdlCommons.getGuidProperty(this.getModelResource(modelResource));
	}
	public void setGUID(ModelResource modelResource, String guid){
		//not implemented
	}

	//Method that gets the GUID required for slice2slice stitching
	//For networks this is the GUID of the manifest object that represents this requested network.
	//This is only allowed for intradomain networks
	public String getStitchingGUID(ModelResource modelResource){
		//TODO: needs checks to see what type of resource this is.
		//For now this might only work with intradomain bcast networks and nodes.

		Resource r = this.getModelResource(modelResource);
		Statement st = r.getProperty(NdlCommons.requestMessage);
		LIBNDL.logger().debug("NDLModel message = " + st.getLiteral());

		String[] tokens = st.getLiteral().getString().split(" ");
		String stitchingGUID = "";
		if(tokens.length >= 2){
			stitchingGUID = tokens[1];
		}

		return stitchingGUID;
	}

	public void setImage(ComputeNode cn, String imageURL, String imageHash, String shortName){
		try{
			Individual imageIndividual = ngen.declareDiskImage(imageURL, imageHash, shortName);
			ngen.addDiskImageToIndividual(imageIndividual, (Individual)this.getModelResource(cn));
		}catch (ClassCastException e){
			LIBNDL.logger().error("Cannot cast ComputeNode resource to individual. " + cn.getName());
		}catch (NdlException e){
			LIBNDL.logger().error("NdlException setting image for " + cn.getName());
		}
	}

	public String getImageURL(ComputeNode cn) {
		return NdlCommons.getIndividualsImageURL(this.getModelResource(cn));
	}

	public String getImageHash(ComputeNode cn) {
		return NdlCommons.getIndividualsImageHash(this.getModelResource(cn));
	}

	public String getImageShortName(ComputeNode cn) {
		//getImageShortName not implemented
		return "getImageShortName not implemented";
		//return NdlCommons.getIndividualsImageURL(this.getModelResource(cn));
	}

	public void setBandwidth(LinkNetwork linkConnection, long b) {
	}
	public void setBandwidth(BroadcastNetwork broadcastNetwork, long b) {
	}

	public Long getBandwidth(BroadcastNetwork broadcastNetwork) {
		return null;
	}

    public Long getBandwidth(LinkNetwork broadcastNetwork) {
        return null;
    }

	public String getName(ModelResource cn) {
		//return this.getModelResource(cn).getLocalName();
		return this.getPrettyName(this.getModelResource(cn));
	}


	public void setName(ModelResource cn) {
		// TODO Auto-generated method stub

	}

	//Jena helper method
	private Resource getType(Resource r){
		if(r == null) return null;

		return r.getProperty(new PropertyImpl("http://www.w3.org/1999/02/22-rdf-syntax-ns#type")).getResource();
	}

	//Jena helper method
     private boolean isType(Resource r, Resource resourceClass){
	        return  r.hasProperty(NdlCommons.RDF_TYPE, resourceClass);
	}

   //Jena helper method
   //Returns the link object from the request for a given stitchport object in the request
     private Resource getLinkFromStitchPort(Resource sp){
    	 LIBNDL.logger().debug("^^^^^^^^^^^^^^^^^^^^ getLinkFromStitchPort: BEGIN");
    	 Iterator i = null;

    	 OntModel om = (OntModel) sp.getModel();
    	 //get interface to link
    	 Resource spIface = null;
    	 for (i = om.listStatements(sp, new PropertyImpl("http://geni-orca.renci.org/owl/topology.owl#hasInterface"), (RDFNode) null); i.hasNext();){
    		 Statement st = (Statement) i.next();
    		 LIBNDL.logger().debug("FOUND Statement subject: " + st.getSubject() + ", predicate: " + st.getPredicate() + ", resource  " + st.getResource());
    		 LIBNDL.logger().debug("resource type: " + getType(st.getResource()));
    		spIface = st.getResource();
    	 }
    	 LIBNDL.logger().debug("Getting link");
    	 Resource spLink = null;
    	 for (i = om.listStatements(null, new PropertyImpl("http://geni-orca.renci.org/owl/topology.owl#hasInterface"), (RDFNode) spIface); i.hasNext();){
    		 Statement st = (Statement) i.next();
    		 LIBNDL.logger().debug("FOUND Statement subject: " + st.getSubject() + ", predicate: " + st.getPredicate() + ", resource  " + st.getResource());
    		 try{
    			 LIBNDL.logger().debug("subject type: " + getType(st.getSubject()) + ", resource type: " + getType(st.getResource()));
    		 } catch (Exception e){
    			 LIBNDL.logger().debug("error getting type for statement");
    		 }
    		 //http://geni-orca.renci.org/owl/topology.owl#NetworkConnection
    		if (isType(st.getSubject(),NdlCommons.topologyNetworkConnectionClass)) {
    			 spLink = st.getSubject();
    			 break;
    		 }
    	 }
    	 LIBNDL.logger().debug("link = " + spLink);

    	 LIBNDL.logger().debug("^^^^^^^^^^^^^^^^^^^^ getLinkFromStitchPort: END");
    	 return spLink;

	}

    private Collection<Resource> getVLANsFromLink(Resource l){
    	LIBNDL.logger().debug("getVLANsFromLink:BEGIN");
    	ArrayList<Resource> rtnList = new ArrayList<Resource>();

    	Iterator i = null;

    	OntModel om = this.getJenaModel();
    	LIBNDL.logger().debug("om = " + om);
    	i = om.listStatements(null, NdlCommons.inRequestNetworkConnection, (RDFNode) l);
    	LIBNDL.logger().debug("i = " + i);
    	for (i = om.listStatements(null, NdlCommons.inRequestNetworkConnection, (RDFNode) l); i.hasNext();){
    		Statement st = (Statement) i.next();
    		LIBNDL.logger().debug("FOUND Statement subject: " + st.getSubject() + ", predicate: " + st.getPredicate() + ", resource  " + st.getResource());
    		LIBNDL.logger().debug("resource type: " + getType(st.getSubject()));

    		if (isType(st.getSubject(),NdlCommons.topologyCrossConnectClass)) {
    			LIBNDL.logger().debug("adding vlan: " + st.getSubject());
    			 rtnList.add(st.getSubject());
    		 }
    	}

    	LIBNDL.logger().debug("getVLANsFromLink:END");
    	return rtnList;
    }

    private String getStateOfLink(Resource l){
    	boolean active = true;

    	LIBNDL.logger().debug("VLANs from Link " + l);
    	for  (Resource r : getVLANsFromLink(l)){
    		LIBNDL.logger().debug("VLAN: " + r + ", state: " + NdlCommons.getResourceStateAsString(r));

    		if(NdlCommons.getResourceStateAsString(r).equals("Failed")){
    			return "Failed";
    		}

    		if(!NdlCommons.getResourceStateAsString(r).equals("Active")){
    			active = false;
    		}
    	}

    	if (active) {
    		return "Active";
    	} else {
    		return "Building";
    	}
    }

    private Collection<Resource> getVLANsFromBroadcastLink(Resource l){
    	LIBNDL.logger().debug("getVLANsFromBroadcastLink:BEGIN. l = " + l + ", type = " + getType(l));

    	ArrayList<Resource> rtnList = new ArrayList<Resource>();
    	Iterator i = null;
    	OntModel om = (OntModel) l.getModel();

    	for (i = om.listStatements(null, NdlCommons.inRequestNetworkConnection, (RDFNode) l); i.hasNext();){
    		Statement st = (Statement) i.next();
    		LIBNDL.logger().debug("FOUND Statement subject: " + st.getSubject() + ", predicate: " + st.getPredicate() + ", resource  " + st.getResource());
    		LIBNDL.logger().debug("resource type: " + getType(st.getSubject()));

    		if (isType(st.getSubject(),NdlCommons.topologyCrossConnectClass)) {

    			LIBNDL.logger().debug("adding vlan: " + st.getSubject());
    			 rtnList.add(st.getSubject());
    		 }
    	}

    	LIBNDL.logger().debug("getVLANsFromBroadcastLink:END");
    	return rtnList;
    }

    private String getStateOfBroadcastLink(Resource l){
    	boolean active = true;

    	LIBNDL.logger().debug("VLANs from Link " + l);
    	for  (Resource r : getVLANsFromBroadcastLink(l)){
    		LIBNDL.logger().debug("VLAN: " + r + ", state: " + NdlCommons.getResourceStateAsString(r));

				try{
    		if(NdlCommons.getResourceStateAsString(r).equals("Failed")){
    			return "Failed";
    		}

    		if(!NdlCommons.getResourceStateAsString(r).equals("Active")){
    			active = false;
    		}
			  }catch (Exception e){
				  return "Null";
			  }
    	}

    	if (active) {
    		return "Active";
    	} else {
    		return "Building";
    	}
    }

	public String getState(ModelResource cn) {
		return this.getState(this.getModelResource(cn));
	}

	public String getState(Resource r) {
		LIBNDL.logger().debug("XXXXXXXXXXXXXXXXXXXXXXXXXXXXX  NDLModel.getState XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX");
		LIBNDL.logger().debug("Resource : " + r + ", type: " + getType(r));

		if(NdlCommons.getResourceStateAsString(r) != null){
			LIBNDL.logger().debug("Getting state directly");
			return NdlCommons.getResourceStateAsString(r);
		}

		if (NdlCommons.isStitchingNode(r)){
			Resource link =  getLinkFromStitchPort(r);
			return getStateOfLink(link);
		}

		if (isType(r,NdlCommons.topologyNetworkConnectionClass)) {
 			LIBNDL.logger().debug("Getting state of NdlCommons.isLinkConnection(r)");
 			return getStateOfBroadcastLink(r);
 		}

		//works for compute nodes (and maybe some other things)
		return null; //NdlCommons.getResourceStateAsString(r);
	}

	public void setNodeType(ComputeNode computeNode, String nodeType) {
		try{
			Individual ni = (Individual)this.getModelResource(computeNode);

			if (NDLGenerator.BAREMETAL.equals(nodeType))
				ngen.addBareMetalDomainProperty(ni);
			else if (NDLGenerator.FORTYGBAREMETAL.equals(nodeType))
				ngen.addFourtyGBareMetalDomainProperty(ni);
			else
				ngen.addVMDomainProperty(ni);
			if (NDLGenerator.nodeTypes.get(nodeType) != null) {
				Pair<String> nt = NDLGenerator.nodeTypes.get(nodeType);
				ngen.addNodeTypeToCE(nt.getFirst(), nt.getSecond(), ni);
			}

		}catch (ClassCastException e){
			LIBNDL.logger().error("Cannot cast ComputeNode resource to individual. " + computeNode.getName());
		}catch (NdlException e){
			LIBNDL.logger().error("NdlException setting image for " + computeNode.getName());
		}
	}

	public String getNodeType(ComputeNode computeNode) {
		// TODO Auto-generated method stub
		Resource ceType = NdlCommons.getSpecificCE(this.getModelResource(computeNode));
		return RequestGenerator.reverseNodeTypeLookup(ceType);
	}

	public void setPostBootScript(ComputeNode computeNode, String postBootScript) {
		try{
			if ((postBootScript != null) && (postBootScript.length() > 0)) {
				ngen.addPostBootScriptToCE(postBootScript, (Individual)this.getModelResource(computeNode));
			}
		}
		catch (ClassCastException e){
			LIBNDL.logger().error("Cannot cast ComputeNode resource to individual. " + computeNode.getName());
		}catch (NdlException e){
			LIBNDL.logger().error("NdlException setting image for " + computeNode.getName());
		}

	}

	public String getPostBootScript(ComputeNode computeNode) {
		return  NdlCommons.getPostBootScript(this.getModelResource(computeNode));
	}

	public List<String>  getManagementServices(ComputeNode computeNode) {
		List<String> services = NdlCommons.getNodeServices(this.getModelResource(computeNode));
		return services;
	}

	public String getMacAddress(InterfaceNode2Net interfaceNode2Net) {
		String mac = null;
		try {
			Resource interfaceResource = this.getModelResource(interfaceNode2Net);
			LIBNDL.logger().debug("NDLModel::getMacAddress:  interfaceIndivdual = " + interfaceResource);
			LIBNDL.logger().debug("NDLModel::getMacAddress:  interfaceIndivdual.getName = " + interfaceNode2Net.getName());
			mac = NdlCommons.getAddressMAC(interfaceResource);
			LIBNDL.logger().debug("NDLModel::getMacAddress: macStr = " + mac);

		} catch (Exception e) {
			LIBNDL.logger().debug("NDLModel::getMacAddress");
			e.printStackTrace();
		}


		return mac;
	}

	public String getIP(InterfaceNode2Net interfaceNode2Net) {
		String ip = null;
		try {
			Resource interfaceResource = this.getModelResource(interfaceNode2Net);
			if(interfaceResource != null) {
                LIBNDL.logger().debug("NDLModel::getIP:  interfaceIndivdual = " + interfaceResource);
                LIBNDL.logger().debug("NDLModel::getIP:  interfaceIndivdual.getName = " + interfaceNode2Net.getName());
                Resource ipResource = null;
                if(interfaceResource.getProperty(NdlCommons.ip4LocalIPAddressProperty) != null ) {
                    ipResource = interfaceResource.getProperty(NdlCommons.ip4LocalIPAddressProperty).getResource();
                    LIBNDL.logger().debug("NDLModel::getIP: ipResource = " + ipResource);
                    ip = NdlCommons.getLabelID(ipResource);
                    LIBNDL.logger().debug("NDLModel::getIP: ipStr = " + ip);
                }
            }
		} catch (Exception e) {
            e.printStackTrace();
        }
		return ip;
	}
	public void setIP(InterfaceNode2Net interfaceNode2Net, String ipAddress) {
		try {
			Individual interfaceIndivdual = (Individual) this.getModelResource(interfaceNode2Net);
			LIBNDL.logger().debug("NDLModel::setIP:  interfaceIndivdual = " + interfaceIndivdual);
			LIBNDL.logger().debug("NDLModel::setIP:  interfaceIndivdual.getName = " + interfaceNode2Net.getName());
			ngen.addUniqueIPToIndividual(ipAddress, interfaceNode2Net.getName(), interfaceIndivdual);
		} catch (NdlException e) {
			e.printStackTrace();
		}

	}
	public String getNetMask(InterfaceNode2Net interfaceNode2Net) {
		return NdlCommons.getInterfaceNetmask(getModelResource(interfaceNode2Net));
	}
	public void setNetMask(InterfaceNode2Net interfaceNode2Net, String netmask) {
		try {
			Individual ipInd = ngen.getRequestIndividual(interfaceNode2Net.getName());
			ngen.addNetmaskToIP(ipInd, netmask);
		} catch (NdlException e) {
			e.printStackTrace();
		}

	}

	public void setDomain(RequestResource requestResource, String d) {
		try{
			Individual domI = ngen.declareDomain(NDLGenerator.domainMap.get(d));
			ngen.addNodeToDomain(domI, (Individual)this.getModelResource(requestResource));
		}catch (ClassCastException e){
			LIBNDL.logger().error("Cannot cast ComputeNode resource to individual. " + requestResource.getName());
		}catch (NdlException e){
			LIBNDL.logger().error("NdlException setting image for " + requestResource.getName());
		}
	}


	public String getDomain(RequestResource requestResource) {
		if(this.getModelResource(requestResource) instanceof com.hp.hpl.jena.rdf.model.impl.ResourceImpl){
			//Special case for nodes that are already in the manifest (i.e. are instances of ResourceImpl
			return RequestGenerator.reverseLookupDomain(NdlCommons.getDomain(this.getModelResource(requestResource)));
		}

		//General case for regular resources
		return NdlCommons.getDomain((Individual)this.getModelResource(requestResource)).getLocalName();
	}

	/**
	 * Hacks that should be in ndlcommons
	 */

	// sometimes getLocalName is not good enough
    // so we strip off orca name space and call it a day
    protected String getTrueName(Resource r) {
        if (r == null)
            return null;

        return StringUtils.removeStart(r.getURI(), NdlCommons.ORCA_NS);
    }

    protected String getPrettyName(Resource r) {
        String rname = getTrueName(r);
        int start_index = rname.indexOf('#');
        int end_index = rname.indexOf('/');
        if(start_index > 0 && end_index == -1){
            rname = rname.substring(start_index + 1);
        } else if (start_index > 0 && end_index > 0 && end_index > start_index) {
            rname = rname.substring(start_index + 1,end_index);
        }
        return rname;
    }
}
