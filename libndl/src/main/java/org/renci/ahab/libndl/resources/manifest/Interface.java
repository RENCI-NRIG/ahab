/**
 * 
 */
package org.renci.ahab.libndl.resources.manifest;

/**
 * @author geni-orca
 *
 */
public abstract class Interface {
	ManifestResource a;
	ManifestResource b;
		
	public Interface(ManifestResource a, ManifestResource b){
		this.a = a;
		this.b = b;
	}
}
