package org.renci.ahab.libndl.resources.request;

import org.renci.ahab.libndl.SliceGraph;

/**
 * Orca storage node implementation
 * @author ibaldin
 *
 */
public class StorageNode extends Node {
	private static final String STORAGE = "Storage";
	protected long capacity = 0;
	// is this a storage on shared or dedicated network?
	protected boolean sharedNetworkStorage = true;
	protected boolean doFormat = true;
	protected String hasFSType = "ext4", hasFSParam = "-F -b 2048", hasMntPoint = "/mnt/target"; 
	
	public StorageNode(SliceGraph sliceGraph, String name) {
		super(sliceGraph, name);
	}
	
	public void setCapacity(long cap) {
		assert(cap >= 0);
		capacity = cap;
	}

	public void setMntPoint(String mntPoint) {
	    if(mntPoint != null && !mntPoint.isEmpty()) {
	        hasMntPoint = mntPoint;
        }
    }
	
	public long getCapacity() {
		return capacity;
	}
	
	public void setSharedNetwork() {
		sharedNetworkStorage = true;
	}
	
	public void setDedicatedNetwork() {
		sharedNetworkStorage = false;
	}
	
	public boolean getSharedNetwork() {
		return sharedNetworkStorage;
	}
	
	public void setDoFormat(boolean m) {
		doFormat = m;
	}
	
	public boolean getDoFormat() {
		return doFormat;
	}
	
	public void setFS(String t, String p, String m) {
		hasFSType = t;
		hasFSParam = p;
		hasMntPoint = m;
	}
	
	public String getFSType() {
		return hasFSType;
	}
	
	public String getFSParam() {
		return hasFSParam;
	}
	
	public String getMntPoint() {
		return hasMntPoint;
	}
	
	
	public Interface stitch(RequestResource r){
		Interface stitch = null;
		if (r instanceof Network){
			stitch = new InterfaceNode2Net(this,(Network)r,sliceGraph);		
		} else {
			System.out.println("Error: Cannot stitch OrcaStorageNode to " + r.getClass().getName());
			return null;
		}
		sliceGraph.addStitch(this,r,stitch);

		return stitch;
	}

	public boolean isAttachedTo(ComputeNode c) {
	    boolean retVal = false;
        for(Interface i: getInterfaces()) {
            InterfaceNode2Net ifc = (InterfaceNode2Net)i;
            if(c.getInterface((RequestResource)ifc.getLink()) != null) {
                retVal = true;
            }
        }
        return retVal;
    }

	@Override
	public String getPrintText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() {
		sliceGraph.deleteResource(this);
	}
}
