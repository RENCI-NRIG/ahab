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
package org.renci.ahab.libndl.resources.request;

import java.util.HashSet;
import java.util.Set;

import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.SliceGraph;

public abstract class Node extends RequestResource {

	
	public String toString() {
		return getName();
	}
		
//basic constructor
	public Node(SliceGraph sliceGraph, String name) {
		super(sliceGraph);
		//this.name = name; //name should be unique... i think
		//this.domain = null;
		this.dependencies = null;
	}

	

	public void addDependency(Node n) {
		if (n != null) 
			dependencies.add(n);
	}
	
	public void removeDependency(Node n) {
		if (n != null)
			dependencies.remove(n);
	}
	
	public void clearDependencies() {
		dependencies = new HashSet<RequestResource>();
	}
	
	public boolean isDependency(Node n) {
		if (n == null)
			return false;
		return dependencies.contains(n);
	}
	
	/**
	 * returns empty set if no dependencies
	 * @return
	 */
	public Set<String> getDependencyNames() { 
		Set<String> ret = new HashSet<String>();
		for(RequestResource n: dependencies) 
			ret.add(n.getName());
		return ret;
	}
	
	public Set<RequestResource> getDependencies() {
		return dependencies;
	}
	 
}