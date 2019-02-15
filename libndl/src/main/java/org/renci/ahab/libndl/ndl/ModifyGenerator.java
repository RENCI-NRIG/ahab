 package org.renci.ahab.libndl.ndl;

import orca.ndl.NdlCommons;
import orca.ndl.NdlException;
import orca.ndl.NdlGenerator;
import org.renci.ahab.libndl.LIBNDL;
import org.renci.ahab.libndl.resources.request.ComputeNode;
import com.hp.hpl.jena.ontology.Individual;

/**
 * Generate a modify request
 * @author ibaldin
 *
 */
public class ModifyGenerator extends NDLGenerator{
	
	private NdlGenerator ngen = null;
	private Individual modRes = null;
	private String outputFormat = null;
	
	public ModifyGenerator(NdlGenerator ngen) {
		this.ngen = ngen;
	}
	
	public void setOutputFormat(String of) {
		outputFormat = of;
	}
	
	private String getFormattedOutput(String oFormat) {
		if (ngen == null)
			return null;
		if (oFormat == null)
			return getFormattedOutput(RequestGenerator.defaultFormat);
		if (oFormat.equals(RequestGenerator.RDF_XML_FORMAT)) 
			return ngen.toXMLString();
		else if (oFormat.equals(RequestGenerator.N3_FORMAT))
			return ngen.toN3String();
		else if (oFormat.equals(RequestGenerator.DOT_FORMAT)) {
			return ngen.getGVOutput();
		}
		else
			return getFormattedOutput(RequestGenerator.defaultFormat);
	}

	/**
	 * Create a modify request in a specific namespace (null is allowed - NDLLIBD will be used)
	 * This call is optional. Calling addNodesToGroup and removeNodeFromGroup will automatically
	 * make this call if it has not been made.
	 * @param nsGuid
	 */
	public void createModifyRequest(String nsGuid) {
		// this should never run in parallel anyway
			try {
				ngen = new NdlGenerator(nsGuid, LIBNDL.logger(), true);
				String nm = (nsGuid == null ? "my-modify" : nsGuid + "/my-modify");
				modRes = ngen.declareModifyReservation(nm);
			} catch (Exception e) {
				LIBNDL.logger().debug("Fail: createModifyRequest");
				return;
			} 
	
	}
	/**
	 * Remove a  nodes 
	 * 
	 */
	public void removeComputeNode(ComputeNode cn, Individual ni) {
		try {
			ngen.declareModifyElementRemoveNode(modRes, ni.getURI(), null);
		} catch (NdlException e) {
			LIBNDL.logger().debug("ModifyGenerator::removeComputeNode,  Failed to remove compute node");
			e.printStackTrace();
		}
	}
	
	/**
	 * Add a  nodes 
	 * 
	 */
	public void addComputeNode(ComputeNode cn) {
		if (ngen == null)
			createModifyRequest(null);
		try {
			Individual ni = ngen.declareServerCloud(cn.getName());
			ngen.declareModifyElementAddElement(modRes, ni);
			
			// for clusters, add number of nodes, declare as cluster (VM domain)
			if (cn.getNodeCount() > 0){
				ngen.addNumCEsToCluster(cn.getNodeCount(), ni);
				ngen.addVMDomainProperty(ni);
			}

			// node type 
			setNodeTypeOnInstance(cn.getNodeType(), ni);

			// check if image is set in this node
			if (cn.getImageUrl() != null) {
				Individual imI = ngen.declareDiskImage(cn.getImageUrl().toString(), cn.getImageHash(), cn.getImageShortName());
				ngen.addDiskImageToIndividual(imI, ni);
			} else {
				// only bare-metal can specify no image
				if (!NdlCommons.isBareMetal(ni))
					throw new NdlException("Node " + cn.getName() + " is not bare-metal and does not specify an image");

			}

			LIBNDL.logger().debug("About to add domain " + cn.getDomain());
			// if no global domain domain is set, declare a domain and add inDomain property
			//if (!globalDomain && (cn.getDomain() != null)) {
			if (cn.getDomain() != null) {
				LIBNDL.logger().debug("adding domain " + cn.getDomain());
				Individual domI = ngen.declareDomain(domainMap.get(cn.getDomain()));
				ngen.addNodeToDomain(domI, ni);
			}

			// post boot script
			if ((cn.getPostBootScript() != null) && (cn.getPostBootScript().length() > 0)) {
				ngen.addPostBootScriptToCE(cn.getPostBootScript(), ni);
			}
		} catch (NdlException e) {
			return;
		}
	}
	
	/**
	 * Add a count of nodes to a group
	 * @param groupUrl
	 * @param count
	 */
	public void addNodesToGroup(String groupUrl, Integer count) {
		if (ngen == null)
			createModifyRequest(null);
		try {
			LIBNDL.logger().debug("ngen: " + ngen + ", modRes: " + modRes +", groupUrl: " + groupUrl + ", count: " + count);
			ngen.declareModifyElementNGIncreaseBy(modRes, groupUrl, count);
		} catch (NdlException e) {
			LIBNDL.logger().debug("addNodesToGroup FAIL: ngen: " + ngen + ", modRes: " + modRes +", groupUrl: " + groupUrl + ", count: " + count);

			return;
		}
	}
	
	/**
	 * Remove a specific node from a specific group
	 * @param groupUrl
	 * @param nodeUrl
	 */
	public void removeNodeFromGroup(String groupUrl, String nodeUrl) {
		LIBNDL.logger().debug("removeNodeFromGroup: ngen: " + ngen + ", modRes: " + modRes +", groupUrl: " + groupUrl + ", nodeUrl: " + nodeUrl);
		if (ngen == null)
			createModifyRequest(null);
		try {
			ngen.declareModifyElementNGDeleteNode(modRes, groupUrl, nodeUrl);
		} catch (NdlException e) {
			return;
		}
	}
	
	/**
	 * Return modify request in specified format
	 * @return
	 */
	public String getModifyRequest() {
		LIBNDL.logger().debug("ModifyGenerator::getModifyRequest");
		return getFormattedOutput(outputFormat);
	}
	
	/**
	 * clear up the modify saver
	 */
	public void clear() {
		if (ngen != null) {
			ngen.done();
			ngen = null;
		}
	}

	@Override
	void generate(String rdf) {
		// TODO Auto-generated method stub
		
	}
}
