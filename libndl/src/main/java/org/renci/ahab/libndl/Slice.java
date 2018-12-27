package org.renci.ahab.libndl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import org.apache.log4j.Logger;
import org.renci.ahab.libndl.ndl.RequestGenerator;
import org.renci.ahab.libndl.resources.common.ModelResource;
import org.renci.ahab.libndl.resources.request.*;
import org.renci.ahab.libndl.util.IP4Subnet;
import org.renci.ahab.libtransport.AccessToken;
import org.renci.ahab.libtransport.ISliceTransportAPIv1;
import org.renci.ahab.libtransport.SliceAccessContext;
import org.renci.ahab.libtransport.util.ContextTransportException;
import org.renci.ahab.libtransport.util.TransportException;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCTransportException;
import edu.uci.ics.jung.graph.SparseMultigraph;

public class Slice {
	
	private SliceGraph sliceGraph;
	private ISliceTransportAPIv1 sliceProxy;
	private String name;
	private SliceAccessContext<? extends AccessToken> sliceContext;
	
	private Slice(){
		LIBNDL.setLogger();
		sliceGraph = new SliceGraph(this);
	}
	
	public static Slice create(ISliceTransportAPIv1 sliceProxy, SliceAccessContext<? extends AccessToken> sctx, String name){
		Slice s = new Slice();
		s.sliceGraph.loadNewRequest();
		s.setName(name);
		s.setSliceProxy(sliceProxy);
		s.setSliceContext(sctx);
		return s;
	}
	
	public static Slice loadRequestFile(String fileName){
		return Slice.loadRequest(readRDFFile(new File(fileName)));
	}
	
	public static Slice loadRequest(String requestRDFString){
		Slice s = new Slice();
		s.sliceGraph.loadRequestRDF(requestRDFString);
		return s;

	}
	
	public static Slice loadManifestFile(ISliceTransportAPIv1 sliceProxy, String sliceName) throws ContextTransportException, TransportException{
		Slice s = Slice.loadManifest(sliceProxy.sliceStatus(sliceName));
		
		s.setName(sliceName);
		s.setSliceProxy(sliceProxy);
		return s;
	}

	public static Slice loadManifestFile(String fileName){
		return Slice.loadManifest(readRDFFile(new File(fileName)));
	}
	
	public static Slice loadManifest(String manifestRDFString){
		Slice s = new Slice();
		s.sliceGraph.loadManifestRDF(manifestRDFString);
		
		return s; 
	}
	
	//refresh the slice by pulling a new manifest.  Note: this resets any pending modifications
	public void refresh(){
		this.sliceGraph = new SliceGraph(this);
		try {
			this.sliceGraph.loadManifestRDF(sliceProxy.sliceStatus(this.name));
		} catch (TransportException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			LIBNDL.logger().error("Handling exception in refresh()");
		}
	}
	
	private static String readRDFFile(File f){
		BufferedReader bin = null; 
		String rawRDF = null;
		try {
			FileInputStream is = new FileInputStream(f);
			bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

			StringBuilder sb = new StringBuilder();

			String line = null;
			while((line = bin.readLine()) != null) {
				sb.append(line);
				// re-add line separator
				sb.append(System.getProperty("line.separator"));
			}

			bin.close();

			rawRDF = sb.toString();

		} catch (Exception e) {
			LIBNDL.logger().debug("error reading file " + f.toString());
			e.printStackTrace();
			return null;
		} 

		return rawRDF;
	}
	
	public ComputeNode addComputeNode(String name){
		return sliceGraph.addComputeNode(name);
	}

	public StorageNode addStorageNode(String name, long capacity, String mntPoint){
		return sliceGraph.addStorageNode(name, capacity, mntPoint);
	}

	public StitchPort addStitchPort(String name, String label, String port, long bandwidth){
		return sliceGraph.addStitchPort(name, label, port, bandwidth);	 
	}
	SparseMultigraph<RequestResource, Interface> g = new SparseMultigraph<RequestResource, Interface>();
	
	public BroadcastNetwork addBroadcastLink(String name, long bandwidth){
		return sliceGraph.addBroadcastLink(name, bandwidth);
	}

	public BroadcastNetwork addBroadcastLink(String name){
		return this.addBroadcastLink(name,10000000l);
	}

	public LinkNetwork addLinkNetwork(String name, long bandwidth){
		return sliceGraph.addLinkNetwork(name, bandwidth);
	}

	public LinkNetwork addLinkNetwork(String name){
		return this.addLinkNetwork(name,10000000l);
	}
	
	public RequestResource getResourceByName(String nm){
		return sliceGraph.getResourceByName(nm);
	}
	
	public RequestResource getResouceByURI(String uri){
		return sliceGraph.getResourceByURI(uri);
	}
	
	public Interface stitch(RequestResource r1, RequestResource r2){
		LIBNDL.logger().error("slice.stitch is unimplemented");
		return null;
	}
	
	public void autoIP(){
		for (Network n : sliceGraph.getLinks()){
			n.autoIP();
		}
	}
	
	public void setName(String sliceName) {
		this.name = sliceName;
	}
	public String getName() {
		return this.name;
	}
	
	public boolean isNewSlice(){
		return sliceGraph.getNDLModel().isNewSlice();
	}
	
	
	public void commit(int count, int sleepInterval) throws XMLRPCTransportException{
		boolean done = false;
		int i = 0;
		do{
			i++;
			try{
				this.commit();
				done = true;
			} catch (XMLRPCTransportException e){
				System.out.print("Slice commit failed: sleeping for " + sleepInterval + " seconds. ");
				if(i >= count) throw e;
			} catch (Exception e){
				System.out.print("Slice commit failed: sleeping for " + sleepInterval + " seconds. ");				
			}
			
			try {
			    Thread.sleep(sleepInterval*1000); //1000 milliseconds is one second.
			} catch(InterruptedException ex) {  
			    Thread.currentThread().interrupt();
			}
		}while (!done && i < count);
	}
	
	public void commit() throws XMLRPCTransportException{
		try{
			LIBNDL.logger().debug("Name: " + this.getName());
			LIBNDL.logger().debug("Req: " + this.getRequest());
			LIBNDL.logger().debug("sliceProxy: " + sliceProxy);
			if(isNewSlice()){
				LIBNDL.logger().debug("commit new slice");
				sliceProxy.createSlice(this.getName(), this.getRequest(),this.getSliceContext());
			} else {
				LIBNDL.logger().debug("commit modify slice");
				sliceProxy.modifySlice(this.getName(), this.getRequest());
			}
		} catch (XMLRPCTransportException e){
			throw e;
		} catch (Exception e){
			this.logger().debug("Failed to commit changes");
			e.printStackTrace();
			return;
		}
	}
	
	public void delete(){
		try{
			LIBNDL.logger().debug("Name: " + this.getName());
			LIBNDL.logger().debug("sliceProxy: " + sliceProxy);
			if(!isNewSlice()){
				sliceProxy.deleteSlice(this.getName());
			}
		} catch (Exception e){
			this.logger().debug("Failed to delete slice");
			e.printStackTrace();
			return;
		}
	}
	
	public String enableSliceStitching(RequestResource r, String secret){
		String stitching_GUID = r.getStitchingGUID();
		try {
			sliceProxy.permitSliceStitch(this.name, stitching_GUID, secret);
		} catch (ContextTransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransportException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stitching_GUID;
	}
	
	public Collection<ModelResource> getAllResources(){
		return sliceGraph.getResources();
	}
	
	public Collection<Interface> getInterfaces(){
		return sliceGraph.getInterfaces();
	}

	public Collection<Network> getLinks(){
		return sliceGraph.getLinks();
	}
		
	public Collection<BroadcastNetwork> getBroadcastLinks(){
		return sliceGraph.getBroadcastLinks();
	}
	
	public Collection<Node> getNodes(){
		return sliceGraph.getNodes();
	}
	
	public Collection<ComputeNode> getComputeNodes(){
		return sliceGraph.getComputeNodes();
	}
	
	public Collection<StorageNode> getStorageNodes(){
		return sliceGraph.getStorageNodes();
	}	

	public Collection<StitchPort> getStitchPorts(){
		return sliceGraph.getStitchPorts();
	}	
	
	public String getState(){
		return "getState unimplimented";
	}
	
	
	public static Collection<String> getDomains(){
		return RequestGenerator.domainMap.keySet();
	}
	
	public void setSliceProxy(ISliceTransportAPIv1 sliceProxy) {
		this.sliceProxy = sliceProxy;
	}

	private void save(String file){
		sliceGraph.save(file);
	}
	
	public String getRequest(){
		return sliceGraph.getRDFString();
	}

	public Logger logger(){
		return LIBNDL.logger();
	}
	
	public String getDebugString(){
		return sliceGraph.getDebugString();
	}
	public String getSliceGraphString(){
		return sliceGraph.getSliceGraphString();
	}
	public Collection<Interface> getInterfaces(RequestResource requestResource) {
		// TODO Auto-generated method stub
		return null;
	}

	public boolean isNewRequest() {
		// TODO Auto-generated method stub
		return false;
	}

	public void increaseComputeNodeCount(ComputeNode computeNode, int i) {
		// TODO Auto-generated method stub
		
	}

	public void deleteComputeNode(ComputeNode computeNode, String uri) {
		// TODO Auto-generated method stub
		
	}

	public void addStitch(ComputeNode computeNode, RequestResource r, Interface stitch) {
		// TODO Auto-generated method stub
		
	}

	public IP4Subnet setSubnet(String ip, int mask) {
		// TODO Auto-generated method stub
		return null;
	}

	public void addStitch(StorageNode storageNode, RequestResource r, Interface stitch) {
		// TODO Auto-generated method stub
		
	}

	public void addStitch(StitchPort stitchPort, RequestResource r, Interface stitch) {
		// TODO Auto-generated method stub
		
	}

	public IP4Subnet allocateSubnet(int dEFAULT_SIZE) {
		// TODO Auto-generated method stub
		return null;
	}

	public SliceAccessContext<? extends AccessToken> getSliceContext() {
		return sliceContext;
	}

	public void setSliceContext(SliceAccessContext<? extends AccessToken> sliceContext) {
		this.sliceContext = sliceContext;
	}
	
	public void renew(Date newDate) {
	    try {
	        sliceProxy.renewSlice(name, newDate);
	    } catch(ContextTransportException e) {
	        e.printStackTrace();
	    } catch(TransportException e) {
	        e.printStackTrace();
	    }
	}
}
