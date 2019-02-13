package org.renci.ahab.libndl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;

import orca.ndl.NdlGenerator;
import org.apache.log4j.Logger;
import org.renci.ahab.libndl.ndl.NDLGenerator;
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

import javax.annotation.Resource;

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

	// class to simplify the binning of
	// reservation states
	private static class StateBins {
		public static final int MaxBins = 16;
		private int[] bins = new int[MaxBins];

		public void add(int s) {
			if ((s >= 0) && (s < MaxBins))
				bins[s]++;
		}

		/**
		 * Does the specified state appear in the bin?
		 *
		 * @param s
		 * @return
		 */
		public boolean hasState(int s) {
			if ((s >= 0) && (s < MaxBins)) {
				if (bins[s] > 0)
					return true;
			}
			return false;
		}

		/**
		 * Do any other states, other than s appear in the bin?
		 *
		 * @param s
		 * @return
		 */
		public boolean hasStatesOtherThan(int... s) {
			int count = 0;
			for (int i = 0; i < MaxBins; i++) {
				if (bins[i] > 0)
					count++;
			}

			int count1 = 0;
			for (int i = 0; i < s.length; i++) {
				if (bins[s[i]] > 0)
					count1++;
			}

			if ((count1 == count) && (count > 0))
				return false;
			return true;
		}
	};



    public boolean isSliceDead() {
        StateBins b = new StateBins();
        for (ModelResource r : getAllResources()) {
            RequestResource requestResource = (RequestResource) r;
            b.add(NDLGenerator.reservationStates.get(requestResource.getState()));
        }
        if (!b.hasStatesOtherThan(NDLGenerator.ReservationState.ReservationStateClosed.value,
                NDLGenerator.ReservationState.ReservationStateCloseWait.value,
                NDLGenerator.ReservationState.ReservationStateFailed.value))
            return true;
        return false;
    }

    public NDLGenerator.SliceState getState() {
        StateBins b = new StateBins();

        for (ModelResource r : getAllResources()) {
            RequestResource requestResource = (RequestResource) r;
            System.out.println("Resource=" + requestResource.getName() + " State=" + requestResource.getState());
            b.add(NDLGenerator.reservationStates.get(requestResource.getState()));
        }

        // has only Active, ActiveTicketed, Closed or Ticketed reservations
        if (!b.hasStatesOtherThan(NDLGenerator.ReservationState.ReservationStateActiveTicketed.value,
                NDLGenerator.ReservationState.ReservationStateNascent.value,
                NDLGenerator.ReservationState.ReservationStateActive.value,
                NDLGenerator.ReservationState.ReservationStateTicketed.value) &&
                (b.hasState(NDLGenerator.ReservationState.ReservationStateNascent.value) ||
                b.hasState(NDLGenerator.ReservationState.ReservationStateTicketed.value) ||
                b.hasState(NDLGenerator.ReservationState.ReservationStateActiveTicketed.value)))
            return NDLGenerator.SliceState.CONFIGURING;

        // has only Closed, Closing, Pending Close or Failed reservations
        if (!b.hasStatesOtherThan(NDLGenerator.ReservationState.ReservationStateClosed.value,
                NDLGenerator.ReservationState.ReservationStateCloseWait.value,
                NDLGenerator.ReservationState.ReservationPendingStateClosing.value,
                NDLGenerator.ReservationState.ReservationStateFailed.value))
            return NDLGenerator.SliceState.CLOSING_DEAD;

        // has only Active or Failed or Closed reservations
        if ((!b.hasStatesOtherThan(NDLGenerator.ReservationState.ReservationStateActive.value,
                NDLGenerator.ReservationState.ReservationStateFailed.value,
                NDLGenerator.ReservationState.ReservationStateClosed.value)) &&
                (b.hasState(NDLGenerator.ReservationState.ReservationStateFailed.value)))
            return NDLGenerator.SliceState.STABLE_ERROR;

        // has only Active or Closed reservartions
        if (!b.hasStatesOtherThan(NDLGenerator.ReservationState.ReservationStateActive.value,
                NDLGenerator.ReservationState.ReservationStateClosed.value))
            return NDLGenerator.SliceState.STABLE_OK;

        return NDLGenerator.SliceState.NULL;
    }
}
