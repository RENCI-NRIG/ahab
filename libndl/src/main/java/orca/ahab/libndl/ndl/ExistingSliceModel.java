package orca.ahab.libndl.ndl;

import java.util.HashMap;
import java.util.Map;

import com.hp.hpl.jena.ontology.Individual;
import com.hp.hpl.jena.rdf.model.Resource;

import edu.uci.ics.jung.graph.SparseMultigraph;
import orca.ahab.libndl.LIBNDL;
import orca.ahab.libndl.SliceGraph;
import orca.ahab.libndl.resources.manifest.ManifestResource;
import orca.ahab.libndl.resources.request.BroadcastNetwork;
import orca.ahab.libndl.resources.request.ComputeNode;
import orca.ahab.libndl.resources.request.Interface;
import orca.ahab.libndl.resources.request.InterfaceNode2Net;
import orca.ahab.libndl.resources.request.RequestResource;
import orca.ahab.libndl.resources.request.StitchPort;
import orca.ahab.libndl.resources.request.StorageNode;
import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;
import orca.ndl.NdlManifestParser;

public class ExistingSliceModel extends NDLModel{
	protected NdlManifestParser sliceModel;
	
	/* map of RequestResource in original slice or request to ndl Resource */
	protected Map<RequestResource,Resource> slice2NDLMap;
	
	
	public ExistingSliceModel(SliceGraph sliceGraph, String rdf) {
		super();
		
		slice2NDLMap = new HashMap<RequestResource,Resource>();
		
		ManifestLoader loader = new ManifestLoader(sliceGraph,this);
		sliceModel = loader.load(rdf);
		
		try {
			String nsGuid = "1111111111"; //hack for now
			ngen = new NdlGenerator(nsGuid, LIBNDL.logger(), true);
			String nm = (nsGuid == null ? "my-modify" : nsGuid + "/my-modify");
			reservation = ngen.declareModifyReservation(nm);
		} catch (Exception e) {
			LIBNDL.logger().debug("Fail: createModifyRequest");
			return;
		} 
	}

	
	protected void mapSliceResource2ModelResource(RequestResource r, Resource i){
		slice2NDLMap.put(r,i);
	}
	
	
	protected Resource getModelResource(RequestResource r){
		Resource modelResource = null;	
		if((modelResource = request2NDLMap.get(r)) != null) {
			return modelResource;
		} 
		return slice2NDLMap.get(r);
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
			Individual ni;
			if (cn.getNodeCount() > 0){
				if (cn.getSplittable())
					ni = ngen.declareServerCloud(name, cn.getSplittable());
				else
					ni = ngen.declareServerCloud(name);
			} else {
				ni = ngen.declareComputeElement(name);
				ngen.addVMDomainProperty(ni);
			}
			
			mapRequestResource2ModelResource(cn, ni);
			ngen.addResourceToReservation(reservation, ni);
		} catch (NdlException e) {
			logger().error("NewSliceModel:add(ComputeNode):" + e.getStackTrace());
		}	
		
	}

	@Override
	public void add(BroadcastNetwork bn) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void add(StitchPort sp) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void add(InterfaceNode2Net i) {
		// TODO Auto-generated method stub
		
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
	public String getName(RequestResource cn) {
		return this.getModelResource(cn).getLocalName();
	}

	@Override
	public void setName(RequestResource cn) {
		// TODO Auto-generated method stub
		
	}



}