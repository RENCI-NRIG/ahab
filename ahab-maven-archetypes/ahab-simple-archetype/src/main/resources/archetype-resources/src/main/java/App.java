package ${package};

import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.resources.request.ComputeNode;
import org.renci.ahab.libndl.resources.request.InterfaceNode2Net;
import org.renci.ahab.libndl.resources.request.Network;
import org.renci.ahab.libtransport.ISliceTransportAPIv1;
import org.renci.ahab.libtransport.ITransportProxyFactory;
import org.renci.ahab.libtransport.PEMTransportContext;
import org.renci.ahab.libtransport.SSHAccessToken;
import org.renci.ahab.libtransport.SliceAccessContext;
import org.renci.ahab.libtransport.TransportContext;
import org.renci.ahab.libtransport.util.SSHAccessTokenFileFactory;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCProxyFactory;

import java.net.URL;

/**
 * `mvn clean package`
 * `java -cp ./target/${artifactId}-${version}-jar-with-dependencies.jar ${package}.App certLocation keyLocation controllerURL sliceName`
 * Verify slice creation in Flukes
 */
public class App 
{
    public static void main( String[] args ) throws Exception
    {
        System.out.println( "Creating a simple Slice!" );

        // Need some command line options -- use your GENI cert
        // http://www.exogeni.net/2015/09/exogeni-getting-started-tutorial/
        String certLocation = args[0];
        String keyLocation = args[1];
        String controllerUrl = args[2]; // "https://geni.renci.org:11443/orca/xmlrpc";
        String sliceName = args[3]; // "your_name";

        /*
         * Get Slice Proxy
         */
        ISliceTransportAPIv1 sliceProxy;

        //ExoGENI controller context
        ITransportProxyFactory ifac = new XMLRPCProxyFactory();
        System.out.println("Opening certificate " + certLocation + " and key " + keyLocation);
        char [] pwd = System.console().readPassword("Enter password for key: ");
        TransportContext ctx = new PEMTransportContext(String.valueOf(pwd), certLocation, keyLocation);
        sliceProxy = ifac.getSliceProxy(ctx, new URL(controllerUrl));

        /*
         * SSH Context
         */
        SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();

        SSHAccessTokenFileFactory fac;
        fac = new SSHAccessTokenFileFactory("~/.ssh/id_rsa.pub", false);

        SSHAccessToken t = fac.getPopulatedToken();
        sctx.addToken("root", "root", t);
        sctx.addToken("root", t);

        /*
         * Main Example Code
         */
        Slice slice = Slice.create(sliceProxy, sctx, sliceName);

        String nodeImageShortName = "Centos 6.8 v1.1.0";
        String nodeImageURL = "http://geni-images.renci.org/images/standard/centos/centos6.8-v1.1.0/centos6.8-v1.1.0.xml";
        String nodeImageHash = "88e40764a31b2ddc8410302c81cf3916f6c140b5";
        String nodeNodeType = "XO Medium";
        String nodePostBootScript = "#node boot script";
        String nodeDomain = "RENCI (Chapel Hill, NC USA) XO Rack";

        String newNodeName = sliceName + "0";
        ComputeNode node0 = slice.addComputeNode(newNodeName);
        node0.setImage(nodeImageURL,nodeImageHash,nodeImageShortName);
        node0.setNodeType(nodeNodeType);
        node0.setDomain(nodeDomain);
        node0.setPostBootScript(nodePostBootScript);

        newNodeName = sliceName + "1";
        ComputeNode node1 = slice.addComputeNode(newNodeName);
        node1.setImage(nodeImageURL,nodeImageHash,nodeImageShortName);
        node1.setNodeType(nodeNodeType);
        node1.setDomain(nodeDomain);
        node1.setPostBootScript(nodePostBootScript);

        final String netName = "Net1";
        Network net1 = slice.addBroadcastLink(netName);
        InterfaceNode2Net ifaceNode0 = (InterfaceNode2Net) net1.stitch(node0);
        ifaceNode0.setIpAddress("172.16.0.100");
        ifaceNode0.setMacAddress("255.255.255.0");

        InterfaceNode2Net ifaceNode1 = (InterfaceNode2Net) net1.stitch(node1);
        System.out.println(slice.getRequest());
        ifaceNode1.setIpAddress("172.16.0.101");
        ifaceNode1.setMacAddress("255.255.255.0");

        slice.commit();

        while (true){

            slice.refresh();

            System.out.println("");
            System.out.println("Slice: " + slice.getAllResources());
            net1 = (Network) slice.getResourceByName(netName);
            System.out.println("Network State: "  + net1.getState());

            if(net1.getState() == "Active") break;

            try {
                Thread.sleep(10 * 1000);
            } catch(InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("Done, Network State: "  + net1.getState());
    }
}
