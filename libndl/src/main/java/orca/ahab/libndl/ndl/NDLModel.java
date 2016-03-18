package orca.ahab.libndl.ndl;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.uci.ics.jung.graph.util.Pair;
import orca.ahab.libndl.LIBNDL;
import orca.ahab.libndl.resources.common.ModelResource;
import orca.ahab.libndl.resources.request.BroadcastNetwork;
import orca.ahab.libndl.resources.request.ComputeNode;
import orca.ahab.libndl.resources.request.InterfaceNode2Net;
import orca.ahab.libndl.resources.request.RequestResource;
import orca.ahab.libndl.resources.request.StitchPort;
import orca.ahab.libndl.resources.request.StorageNode;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;

public abstract class NDLModel {
	
	
	/* map of RequestResource in slice changes to ndl Resource */
	protected Map<ModelResource, Resource> request2NDLMap; 
	
	/* ndl generation */
	protected NdlGenerator ngen;
	protected Individual reservation; 
	
	abstract public void add(ComputeNode cn, String name);
	abstract public void add(BroadcastNetwork bn);
	abstract public void add(StitchPort sp);
	abstract public void add(InterfaceNode2Net i);
	abstract public void add(StorageNode sn);
	
	abstract public void remove(ComputeNode cn);
	abstract public void remove(BroadcastNetwork bn);
	abstract public void remove(StitchPort sp);
	abstract public void remove(InterfaceNode2Net i);
	abstract public void remove(StorageNode sn);
	
	abstract public void setImage(ComputeNode cn, String imageURL, String imageHash, String shortName);
	abstract public String getImageURL(ComputeNode cn);
	abstract public String getImageHash(ComputeNode cn);
	abstract public String getImageShortName(ComputeNode cn);
	
	protected NDLModel(){
		request2NDLMap = new HashMap<ModelResource,Resource>();
		

	}

	protected void mapRequestResource2ModelResource(ModelResource r, Resource i){
		request2NDLMap.put(r,i);
	}
	
	
	protected Resource getModelResource(ModelResource cn){
		return request2NDLMap.get(cn);
	}
	
	public void printRequest2NDLMap(){
		LIBNDL.logger().debug("NDLModle::printRequest2NDLMap: " + request2NDLMap);
	}
	
	protected Logger logger(){
		return LIBNDL.logger();
	}
	
	abstract public String getRequest();
	
	abstract public String getName(ModelResource modelResource);
	abstract public void setName(ModelResource modelResource);
	
	abstract public String getNodeType(ComputeNode computeNode);
	abstract public void setNodeType(ComputeNode computeNode, String nodeType);
	
	abstract public void setPostBootScript(ComputeNode computeNode, String postBootScript);
	abstract public String getPostBootScript(ComputeNode computeNode);
	
	abstract public String getDomain(RequestResource requestResource);
    abstract public void setDomain(RequestResource requestResource, String d);
	
	
	
}
