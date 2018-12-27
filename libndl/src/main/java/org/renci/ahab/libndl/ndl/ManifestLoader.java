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
import org.apache.commons.lang.StringUtils;
import org.renci.ahab.libndl.LIBNDL;
import org.renci.ahab.libndl.SliceGraph;
import org.renci.ahab.libndl.resources.request.ComputeNode;
import org.renci.ahab.libndl.resources.request.StitchPort;
import org.renci.ahab.libndl.resources.request.StorageNode;
import orca.ndl.INdlManifestModelListener;
import orca.ndl.NdlCommons;
import orca.ndl.NdlManifestParser;
import com.hp.hpl.jena.ontology.OntModel;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Resource;

/**
 * Class for loading manifests
 * @author ibaldin
 *
 */
public class ManifestLoader extends NDLLoader implements INdlManifestModelListener{
	private SliceGraph sliceGraph;
	private ExistingSliceModel ndlModel;
	
	public ManifestLoader(SliceGraph sliceGraph, ExistingSliceModel ndlModel){
		LIBNDL.logger().debug("new ManifestLoader");
		this.sliceGraph = sliceGraph;
		this.ndlModel = ndlModel;
	}
	
	public NdlManifestParser load(String rdf) {
		NdlManifestParser nmp = null;
		try {
			LIBNDL.logger().debug("About to parse manifest");
			
			// parse as manifest
			nmp = new NdlManifestParser(rdf, this);
			
			nmp.processManifest();	
			//nmp.freeModel();			
			//manifest.setManifestTerm(creationTime, expirationTime);
			
		} catch (Exception e) {
			LIBNDL.logger().debug("Excpetion: parsing request part of manifest" + e);
		} 
		return nmp;
	}
	
	/* Callbacks for Manifest mode */
	public void ndlInterface(Resource l, OntModel om, Resource conn,
			Resource node, String ip, String mask) {
		// TODO Auto-generated method stub
		String printStr =  "ndlManifest_Interface: \n\tName: " + l;
		printStr += "\n\tconn: " + conn;
		printStr += "\n\tnode: " + node;
		LIBNDL.logger().debug(printStr);
	}
	public void ndlNetworkConnection(Resource l, OntModel om,
			long bandwidth, long latency, List<Resource> interfaces) {
		String printStr = "ndlManifest_NetworkConnection: \n\tName: " + l.toString() + " (" + l.getLocalName() + ")";
		printStr += "\n\tInterfaces:";
		for (Resource r : interfaces){
			printStr += "\n\t\t " + r;
		}			

		LIBNDL.logger().debug(printStr);
	}

	public void ndlParseComplete() {		
		String printStr = "ndlManifest_ParseComplete";
		LIBNDL.logger().debug(printStr);
	}
	public void ndlReservation(Resource i, OntModel m) {
		// TODO Auto-generated method stub
		String printStr = "ndlManifest_Reservation: \n\tName: " + i;
		printStr += ", sliceState(Manifest:ndlReservation) = " + NdlCommons.getGeniSliceStateName(i);
		LIBNDL.logger().debug(printStr);
		
		
	}
	public void ndlReservationTermDuration(Resource d, OntModel m,
			int years, int months, int days, int hours, int minutes, int seconds) {
		// TODO Auto-generated method stub
		String printStr = "ndlManifest_ReservationTermDuration: \n\tName: " + d;
		LIBNDL.logger().debug(printStr);
	}
	public void ndlReservationResources(List<Resource> r, OntModel m) {
		// TODO Auto-generated method stub
		String printStr = "ndlManifest_ReservationResources: \n\tName: " + r;
		LIBNDL.logger().debug(printStr);
	}
	public void ndlReservationStart(Literal s, OntModel m, Date start) {
		// TODO Auto generated method stub
		String printStr = "ndlManifest_ReservationStart: \n\tName: " + s ;
		LIBNDL.logger().debug(printStr);
	}
	public void ndlReservationEnd(Literal e, OntModel m, Date end) {
		// TODO Auto-generated method stub
		String printStr = "ndlManifest_ReservationEnd: \n\tNameg.: " + e;
		LIBNDL.logger().debug(printStr);
	}// parse as request
	
	public void ndlNodeDependencies(Resource ni, OntModel m,
			Set<Resource> dependencies) {
		// TODO Auto-generated method stub
		String printStr = "ndlManifest_NodeDependencies: \n\tName: " + ni;
		LIBNDL.logger().debug(printStr);
	}
	public void ndlSlice(Resource sl, OntModel m) {
		// TODO Auto-generated method stub
		String printStr = "ndlManifest_Slice: \n\tName: " + sl;
		printStr += ", sliceState(manifest) = " + NdlCommons.getGeniSliceStateName(sl);
		LIBNDL.logger().debug(printStr);
	}
	public void ndlBroadcastConnection(Resource bl, OntModel om,
			long bandwidth, List<Resource> interfaces) {
		// TODO Auto-generated method stub
		String printStr = "ndlManifest_BroadcastConnection: ************* SHOULD NEVER HAPPEN ******************* \n\tName: " + bl; 
		printStr += "\n\tInterfaces:";
		for (Resource r : interfaces){
			printStr += "\n\t\t " + r;
		}
		LIBNDL.logger().debug(printStr);
	}
	public void ndlManifest(Resource i, OntModel m) {		
		String printStr = "ndlManifest_Manifest: \n\tName: " + i;
		printStr += ", sliceState = " + NdlCommons.getGeniSliceStateName(i);
		LIBNDL.logger().debug(printStr);
		
	}
	public void ndlLinkConnection(Resource l, OntModel m,
			List<Resource> interfaces, Resource parent) {// parse as request
		
		String printStr = "ndlManifest_LinkConnection: \n\tNameg.: " + l +  "\n\tparent: " + parent; 
		printStr += "\n\tInterfaces:";
		for (Resource r : interfaces){
			printStr += "\n\t\t " + r;
		}
		LIBNDL.logger().debug(printStr);
	}	
	public void ndlCrossConnect(Resource c, OntModel m, long bw,
			String label, List<Resource> interfaces, Resource parent) {
		String printStr = "ndlManifest_CrossConnect: \n\tName: " + c +  "\n\tparent: " + parent; 
		printStr += "\n\tInterfaces:";
		for (Resource r : interfaces){
			printStr += "\n\t\t " + r;
		}
		LIBNDL.logger().debug(printStr);
	}	
	public void ndlNetworkConnectionPath(Resource c, OntModel m,
			List<List<Resource>> path, List<Resource> roots) {
		// TODO Auto-generated method stub
		String printStr = "ndlManifest_NetworkConnectionPath: \n\tName: " + c;
		LIBNDL.logger().debug(printStr);
	
	}
	public void ndlNode(Resource ce, OntModel om, Resource ceClass,
			List<Resource> interfaces) {
		LIBNDL.logger().debug("\n\n\n #################################### Processing Node ############################################## \n\n\n");
		if (ce == null)
			return;
		String printStr = "ndlManifest_Node: ("+ ceClass  + ")\n\tName: " + ce + " (" + ce.getLocalName() + ")"; 
		printStr += ", state = " + NdlCommons.getResourceStateAsString(ce);
		printStr += "\n\tInterfaces:";
		for (Resource r : interfaces){
			printStr += "\n\t\t " + r;
		}
		LIBNDL.logger().debug(printStr);
	
		if (NdlCommons.isStitchingNodeInManifest(ce)) {
			LIBNDL.logger().debug("\n\n\n ************************************** FOUND STITCHPORT NODE *************************************** \n\n\n");
			LIBNDL.logger().debug("Found a stitchport");
			String label = NdlCommons.getLayerLabelLiteral(interfaces.get(0));
			String port = NdlCommons.getLinkTo(interfaces.get(0)).toString();
			long bandwidth = 10000000;
			StitchPort newStitchport = this.sliceGraph.buildStitchPort(getPrettyName(ce),label,port,bandwidth);
			
			ndlModel.mapSliceResource2ModelResource(newStitchport, ce);
			
			return;
		}
		if(NdlCommons.isNetworkStorage(ce)){
			LIBNDL.logger().debug("\n\n\n ************************************** FOUND STORAGE NODE *************************************** \n\n\n");
			LIBNDL.logger().debug("Found a storage node, returning");
			StorageNode newStorageNode = this.sliceGraph.buildStorageNode(getPrettyName(ce));
			ndlModel.mapSliceResource2ModelResource(newStorageNode, ce);
			
			return;
		}

		//Only handle compute nodes for now.
		if ((ceClass.equals(NdlCommons.computeElementClass) || ceClass.equals(NdlCommons.serverCloudClass))){
			LIBNDL.logger().debug("\n\n\n ************************************** FOUND COMPUTE NODE *************************************** \n\n\n");
			ComputeNode newNode = this.sliceGraph.buildComputeNode(getPrettyName(ce));
			ndlModel.mapSliceResource2ModelResource(newNode, ce);
			newNode.setPostBootScript(NdlCommons.getPostBootScript(ce));
			return;
		}
		
	}

	// sometimes getLocalName is not good enough
	// so we strip off orca name space and call it a day
	private String getTrueName(Resource r) {
		if (r == null)
			return null;
		
		return StringUtils.removeStart(r.getURI(), NdlCommons.ORCA_NS);
	}
	
	private String getPrettyName(Resource r) {
		String rname = getTrueName(r);
		int ind = rname.indexOf('#');
		if (ind > 0) {
			rname = rname.substring(ind + 1);
		}
		return rname;
	}
}
