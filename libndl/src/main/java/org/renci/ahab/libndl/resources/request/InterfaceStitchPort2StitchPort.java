/**
 *
 */
package org.renci.ahab.libndl.resources.request;

import org.renci.ahab.libndl.SliceGraph;

import com.hp.hpl.jena.sparql.function.library.namespace;

/**
 * @author geni-orca
 *
 */
public class InterfaceStitchPort2StitchPort extends Interface{
	private String macAddress;

	public InterfaceStitchPort2StitchPort(StitchPort sp1, StitchPort sp2, SliceGraph sliceGraph){
		super(sp1,sp2, sliceGraph);
	}

	public StitchPort getSp1() {
		return (StitchPort)a;
	}
	public void setSp1(StitchPort sp1) {
		this.a = sp1;
	}

	public StitchPort getSp2() {
		return (StitchPort)b;
	}
	public void setSp2(StitchPort sp2) {
		this.b = sp2;
	}

	public String toString(){
		String rtnStr = "";

		rtnStr += "Stitch ";
		if(a != null)
			rtnStr += a.getName() + " to ";
		else
			rtnStr += "null to ";

		if(b != null)
			rtnStr += b.getName();
		else
			rtnStr += "null to ";

		return rtnStr;
	}

	@Override
	public String getPrintText() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete() {
		// TODO Auto-generated method stub
		sliceGraph.deleteResource(this);

	}

}
