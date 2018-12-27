package org.renci.ahab.libndl.resources.manifest;

import org.apache.commons.collections15.Factory;
import org.renci.ahab.libndl.Slice;


public class CrossConnect extends ManifestResource{
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

	protected long bandwidth;
	protected long latency;
	protected String label = null;
	protected String realName = null;


	public CrossConnect(Slice slice, String name) {
		super(slice);
		this.name = name;
	}

	interface ILinkCreator {
		public LinkConnection create(String prefix);
		public LinkConnection create(String nm, long bw);
		public void reset();
	}

	public void setBandwidth(long bw) {
		bandwidth = bw;
	}

	public void setLatency(long l) {
		latency = l;
	}

	public void setLabel(String l) {
		if ((l != null) && l.length() > 0)
			label = l;
		else
			label = null;
	}

	public String getLabel() {
		return label;
	}

	public long getBandwidth() {
		return bandwidth;
	}

	public long getLatency() {
		return latency;
	}


	public void setRealName(String n) {
		this.realName = n;
	}

	@Override
	public String toString() {
		return name;
	}



	public static class OrcaLinkFactory implements Factory<LinkConnection> {
	   private ILinkCreator inc = null;

		public OrcaLinkFactory(ILinkCreator i) {
			inc = i;
		}

		public LinkConnection create() {
			if (inc == null)
				return null;
			synchronized(inc) {
				return inc.create(null);
			}
		}
	}

	// link to broadcast?
	public boolean linkToBroadcast() {
		return false;
	}

	// link to shared storage?
	public boolean linkToSharedStorage() {
		return false;
	}

	@Override
	public String getPrintText() {
		// TODO Auto-generated method stub
		return null;
	}
}
