/**
 * 
 */
package org.renci.ahab.libndl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.*;

import org.renci.ahab.libndl.resources.request.*;
import org.renci.ahab.libtransport.ISliceTransportAPIv1;
import org.renci.ahab.libtransport.ITransportProxyFactory;
import org.renci.ahab.libtransport.PEMTransportContext;
import org.renci.ahab.libtransport.SSHAccessToken;
import org.renci.ahab.libtransport.SliceAccessContext;
import org.renci.ahab.libtransport.TransportContext;
import org.renci.ahab.libtransport.util.SSHAccessTokenFileFactory;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCProxyFactory;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCTransportException;
import org.renci.ahab.ndllib.transport.OrcaSMXMLRPCProxy;

/**computeElement to group: Workers, newNode: http://geni-orca.renci.org/owl/2ce709f3-bee2-46f0-97da-b6b944aed836#Workers/3

 * @author geni-orca
 *
 */
public class TestDriver {
    public static void sleep(int sec){
        try {
            Thread.sleep(sec*1000);                 //1000 milliseconds is one second.
        } catch(InterruptedException ex) {  
            Thread.currentThread().interrupt();
        }
    }

    public static void blockUntilUp(String pem, String sliceName, String nodeName){
        String SDNControllerIP = null; 
        while (SDNControllerIP == null){

            SDNControllerIP=TestDriver.getPublicIP(pem, sliceName, nodeName);
            System.out.println(nodeName + " SDNControllerIP: " + SDNControllerIP);

            if(SDNControllerIP != null) break;

            TestDriver.sleep(30);
        };
    }

    public static void buildSDX(String [] args, int count){
        String rdf;

        LIBNDL.setLogger();
        String sliceName = "pruth.sdx.1";
        TestDriver.startSDNSlice_Controller(args[0],sliceName,count);

        String SDNControllerIP = null; 
        do{
            TestDriver.sleep(30);
            SDNControllerIP=TestDriver.getPublicIP(args[0], sliceName, "SDNcontroller");
            System.out.println("SDNControllerIP: " + SDNControllerIP);
        } while (SDNControllerIP == null);

        TestDriver.addSDNSlice_VSDX_v2(args[0],sliceName,count,SDNControllerIP);
        System.out.println("SDNControllerIP: " + TestDriver.getPublicIP(args[0], sliceName, "SDNcontroller"));

        for (int i = 0; i < count; i++){
            System.out.println("Waiting for sw"+i);
            TestDriver.blockUntilUp(args[0], sliceName, "sw"+i);
        }    

        for (int i = 0; i < count; i++){
            TestDriver.testAddLocalBroadcastNetwork(args[0],sliceName,"sw"+i,"vlan-sw"+i);
        }
        TestDriver.sleep(20);
        for (int i = 0; i < count; i++){
            for (int j = 0; j < 2; j++){
                TestDriver.testAddComputeNode2Network(args[0], sliceName, "node"+i+"-"+j, "vlan-sw"+i, "172.16."+i+"."+(100+j), 0);
            }
        }
    }

    public static void deleteAllSDXNetworks(String pem, String sliceName){
        try{
            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));

            Slice s = Slice.loadManifestFile(sliceProxy, sliceName);

            for (BroadcastNetwork net : s.getBroadcastLinks()){
                if(net.getName().startsWith("SDX")){
                    System.out.println("deleting net: "  + net.getName());
                    net.delete();
                }
            }

            s.commit();
        } catch  (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static void setupSDXLinearNetwork(String pem, String sliceName){
        try{
            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));

            Slice s = Slice.loadManifestFile(sliceProxy, sliceName);
            List<ComputeNode> switches = new ArrayList<ComputeNode>();
            for(ComputeNode node : s.getComputeNodes()){
                System.out.println("node: "  + node);
                if(node.getName().startsWith("sw")){
                    switches.add(node);
                }
            }

            for (int i = 1; i < switches.size(); i++){
                int parentIndex = i-1;
                ComputeNode child = switches.get(i);
                ComputeNode parent = switches.get(parentIndex);
                System.out.println("Child: " + child.getName() + ", Parent: " + parent.getName());
                TestDriver.testAddNetwork(pem,sliceName,child.getName(),parent.getName(),"SDX-vlan-"+child.getName()+"-"+parent.getName());
            }
        } catch  (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static void setupSDXTreeNetwork(String pem, String sliceName){
        try{
            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));
            Slice s = Slice.loadManifestFile(sliceProxy, sliceName);

            List<ComputeNode> switches = new ArrayList<ComputeNode>();

            for(ComputeNode node : s.getComputeNodes()){
                System.out.println("node: "  + node);
                if(node.getName().startsWith("sw")){
                    switches.add(node);
                }
            }

            for (int i = 1; i < switches.size(); i++){
                int parentIndex = (i-1)/2;
                ComputeNode child = switches.get(i);
                ComputeNode parent = switches.get(parentIndex);
                System.out.println("Child: " + child.getName() + ", Parent: " + parent.getName());

                TestDriver.testAddNetwork(pem,sliceName,child.getName(),parent.getName(),"SDX-vlan-"+child.getName()+"-"+parent.getName()); 
            }
        } catch  (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static String geIP(String pem, String sliceName, String nodeName, String netName){
        try{
            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));

            Slice s = Slice.loadManifestFile(sliceProxy, sliceName);
            System.out.println("Slice: " + s.getSliceGraphString());
            ComputeNode  node = (ComputeNode) s.getResourceByName(nodeName);
            BroadcastNetwork net = (BroadcastNetwork)s.getResourceByName(netName);

            System.out.println("node: " + node);
            System.out.println("net: " + net);
            InterfaceNode2Net iface = (InterfaceNode2Net) node.getInterface(net);

            String ip = iface.getIpAddress();
            System.out.println("Get IP:  " + ip);

            System.out.println("Interface: " + iface);
        } catch  (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
        return "";
    }

    public static String getPublicIP(String pem, String sliceName, String nodeName){
        try{
            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));

            Slice s = Slice.loadManifestFile(sliceProxy, sliceName);
            ComputeNode  node = (ComputeNode) s.getResourceByName(nodeName);
            return node.getManagementIP();
        } catch  (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
        return "";
    }

    public static void startSDNSlice_Controller(String pem, String sliceName, int count){
        String controllerImageShortName="Centos7-SDN-controller.v0.4";
        String controllerImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos7-SDN-controller.v0.4/Centos7-SDN-controller.v0.4.xml";
        String controllerImageHash ="b71cbdbd8de5b2d187ae9a3efb0a19a170b92183";
        String controllerDomain="RENCI (Chapel Hill, NC USA) XO Rack";
        String contorllerNodeType="XO Medium";
        String controllerPostBootScript="#!/bin/bash\n echo hello, world > /tmp/bootscript.log";

        try{
            //SSH context
            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("/home/geni-orca/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();            
            sctx.addToken("root", "root", t);
            sctx.addToken("root", t);

            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));
            Slice s = Slice.create(sliceProxy, sctx, sliceName);

            ComputeNode   controllerNode = s.addComputeNode("SDNcontroller");
            controllerNode.setImage(controllerImageURL,controllerImageHash,controllerImageShortName);
            controllerNode.setNodeType(contorllerNodeType);
            controllerNode.setDomain(controllerDomain);
            controllerNode.setPostBootScript(controllerPostBootScript);


            String switchImageShortName="Centos6.7-SDN.v0.1";
            String switchImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos6.7-SDN.v0.1/Centos6.7-SDN.v0.1.xml";
            String switchImageHash ="77ec2959ff3333f7f7e89be9ad4320c600aa6d77";
            String switchDomain="RENCI (Chapel Hill, NC USA) XO Rack";
            String switchNodeType="XO Medium";
            String switchPostBootScript=getSDNControllerScript();

            s.commit();
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static void addSDNSlice_VSDX(String pem, String sliceName, int count, String SDNControllerIP){
        String switchImageShortName="Centos6.7-SDN.v0.1";
        String switchImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos6.7-SDN.v0.1/Centos6.7-SDN.v0.1.xml";
        String switchImageHash ="77ec2959ff3333f7f7e89be9ad4320c600aa6d77";
        String switchDomain="RENCI (Chapel Hill, NC USA) XO Rack";
        String switchNodeType="XO Medium";
        String switchPostBootScript=getSDNSwitchScript(SDNControllerIP);

        try{
            //SSH context
            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("/home/geni-orca/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();            
            sctx.addToken("pruth", "pruth", t);
            sctx.addToken("pruth", t);

            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));

            Slice s = Slice.loadManifestFile(sliceProxy, sliceName);

            ArrayList<ComputeNode> switches = new ArrayList<ComputeNode>();
            for (int i = 0; i < count; i++){
                ComputeNode  sw = s.addComputeNode("sw2-"+i);
                sw.setImage(switchImageURL,switchImageHash,switchImageShortName);
                sw.setNodeType(switchNodeType);
                sw.setDomain(domains.get(0));
                sw.setPostBootScript(switchPostBootScript);
                switches.add(i,sw);
            }

            System.out.println("REQUEST: \n" + s.getRequest());

            s.commit();
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static void addSDNSlice_VSDX_v2(String pem, String sliceName, int count, String SDNControllerIP){
        String switchImageShortName="Centos6.7-SDN.v0.1";
        String switchImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos6.7-SDN.v0.1/Centos6.7-SDN.v0.1.xml";
        String switchImageHash ="77ec2959ff3333f7f7e89be9ad4320c600aa6d77";
        String switchDomain="RENCI (Chapel Hill, NC USA) XO Rack";
        String switchNodeType="XO Medium";
        String switchPostBootScript=getSDNSwitchScript(SDNControllerIP);

        try{
            //SSH context
            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("/home/geni-orca/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();            
            sctx.addToken("pruth", "pruth", t);
            sctx.addToken("pruth", t);

            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));
            //setup the nodes
            try{
                Slice s = Slice.loadManifestFile(sliceProxy, sliceName);

                ArrayList<ComputeNode> switches = new ArrayList<ComputeNode>();
                for (int i = 0; i < count; i++){
                    ComputeNode  sw = s.addComputeNode("sw"+i);
                    sw.setImage(switchImageURL,switchImageHash,switchImageShortName);
                    sw.setNodeType(switchNodeType);
                    sw.setDomain(domains.get(0));
                    sw.setPostBootScript(switchPostBootScript);
                }
                System.out.println("Commiting: " + s.getDebugString());

                s.commit();

                TestDriver.sleep(10);
            } catch (Exception e){
                e.printStackTrace();
                System.err.println("Proxy factory test failed");
                assert(false);
            }
        } catch  (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static void testNewSlice1(String pem){
        try{
            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();

            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("~/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();

            sctx.addToken("pruth", "pruth", t);

            sctx.addToken("pruth", t);

            System.out.println(sctx);

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));
            Slice s = Slice.create(sliceProxy, sctx, "pruth.slice1");

            for (int i = 0; i < 16; i++){
                ComputeNode   newnode = s.addComputeNode("ComputeNode"+i);
                newnode.setImage("http://geni-images.renci.org/images/standard/centos/centos6.3-v1.0.11.xml","776f4874420266834c3e56c8092f5ca48a180eed","PRUTH-centos");
                newnode.setNodeType("XO Large");
                newnode.setDomain("RENCI (Chapel Hill, NC USA) XO Rack");
                newnode.setPostBootScript("master post boot script");

            }
            System.out.println("testNewSlice1: " + s.getDebugString());

            System.out.println("testNewSlice1: " + s.getRequest());

            s.commit();
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static void testNewSlice2(String pem, String sliceName){
        String controllerImageShortName="Centos7-SDN-controller.v0.4";
        String controllerImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos7-SDN-controller.v0.4/Centos7-SDN-controller.v0.4.xml";
        String controllerImageHash ="b71cbdbd8de5b2d187ae9a3efb0a19a170b92183";
        String contorllerNodeType="XO Medium";
        String controllerPostBootScript="#!/bin/bash\n echo hello, world > /tmp/bootscript.log";

        try{
            //SSH context
            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("/home/geni-orca/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();            
            sctx.addToken("root", "root", t);
            sctx.addToken("root", t);

            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));
            Slice s = Slice.create(sliceProxy, sctx, sliceName);

            ComputeNode   Node1 = s.addComputeNode("Node1");
            Node1.setImage(controllerImageURL,controllerImageHash,controllerImageShortName);
            Node1.setNodeType(contorllerNodeType);
            Node1.setDomain(domains.get(1));
            Node1.setPostBootScript(controllerPostBootScript);


            ComputeNode   Node2 = s.addComputeNode("Node2");
            Node2.setImage(controllerImageURL,controllerImageHash,controllerImageShortName);
            Node2.setNodeType(contorllerNodeType);
            Node2.setDomain(domains.get(2));
            Node2.setPostBootScript(controllerPostBootScript);
            BroadcastNetwork net = s.addBroadcastLink("VLAN0");
            InterfaceNode2Net i1 = (InterfaceNode2Net) net.stitch(Node1);
            InterfaceNode2Net i2 = (InterfaceNode2Net) net.stitch(Node2);

            i1.setIpAddress("172.16.1.1");
            i2.setIpAddress("172.16.1.2");

            s.commit();
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static void testLibtransport(String pem){
        try {
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));

            for (String str : sliceProxy.listMySlices()){
                System.out.println(str);
            }

            String manifest = sliceProxy.sliceStatus("pruth.101");
            Slice s = Slice.loadManifest(manifest);
            System.out.println("Slice pruth.101 = " + s.getDebugString());

            ComputeNode   newnode = s.addComputeNode("ComputeNode0");
            newnode.setImage("http://geni-images.renci.org/images/standard/centos/centos6.3-v1.0.11.xml","776f4874420266834c3e56c8092f5ca48a180eed","PRUTH-centos");
            newnode.setNodeType("XO Large");
            newnode.setDomain("RENCI (Chapel Hill, NC USA) XO Rack");
            newnode.setPostBootScript("master post bootnew Slice(); script");

            System.out.println("Slice pruth.101 = " + s.getDebugString());

            sliceProxy.modifySlice("pruth.101", s.getRequest());

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
        System.out.println("Proxy factory test succeeded");
    }


    public static void testLoad(String pem, String sliceName){
        Slice s = null;
        try{

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, sliceName);
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            return;
        }
        System.out.println("******************** START Slice Info " + s.getName() + " *********************");
        System.out.println(s.getSliceGraphString());
        System.out.println("******************** END PRINTING *********************");

    }

    public static void ComputeNode(String pem, String delNodeName){
        Slice s = null;
        try{
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, "pruth.1");
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            return;
        }
        s.logger().debug("******************** START Slice Info " + s.getName() + " *********************");
        s.logger().debug(s.getDebugString());
        s.logger().debug("******************** END PRINTING *********************");
        ComputeNode cn = (ComputeNode)s.getResourceByName(delNodeName);
        s.logger().debug("cn: " + cn);
        s.logger().debug("compute node: " + cn.getName());
        s.logger().debug("compute node: " + cn.getDomain());
        s.logger().debug("compute node: " + cn.getNodeType());
        s.logger().debug("compute node: " + cn.getImageShortName());
        s.logger().debug("compute node: " + cn.getImageHash());
        s.logger().debug("compute node: " + cn.getImageUrl());
        s.logger().debug("compute node: " + cn.getPostBootScript());

        cn.delete();

        try {
            s.commit();
        } catch (XMLRPCTransportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    public static void testDelete(String pem, String sliceName){
        Slice s = null;
        try{
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, sliceName);
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            return;
        }

        s.delete();
    }


    public static void testAddComputeNode2Network(String pem, String sliceName, String newNodeName, String netName, String ip, int domain){
        String switchImageShortName="Centos6.7-SDN.v0.1";
        String switchImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos6.7-SDN.v0.1/Centos6.7-SDN.v0.1.xml";
        String switchImageHash ="77ec2959ff3333f7f7e89be9ad4320c600aa6d77";
        String switchDomain=domains.get(domain);
        String switchNodeType="XO Medium";
        String switchPostBootScript="switch boot script";

        try{
            //SSH context
            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("/home/geni-orca/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();            
            sctx.addToken("pruth", "pruth", t);
            sctx.addToken("pruth", t);

            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));

            //setup the nodes
            try{
                Slice s = Slice.loadManifestFile(sliceProxy, sliceName);

                ComputeNode  node = s.addComputeNode(newNodeName);
                node.setImage(switchImageURL,switchImageHash,switchImageShortName);
                node.setNodeType(switchNodeType);
                node.setDomain(switchDomain);
                node.setPostBootScript(switchPostBootScript);

                BroadcastNetwork net = (BroadcastNetwork)s.getResourceByName(netName); 

                s.logger().debug("BroadcastNetwork net = " + net);

                Interface int1  = net.stitch(node);

                s.logger().debug("Interface int1 = " + int1);

                ((InterfaceNode2Net)int1).setIpAddress(ip);

                s.logger().debug("AddNode2Net request:  " + s.getRequest());

                s.commit();
            } catch (Exception e){
                e.printStackTrace();
                System.err.println("Proxy factory test failed");
                assert(false);
            }
        } catch  (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }


    public static String testAddLocalBroadcastNetwork(String pem, String sliceName, String nodeName, String networkName){
        Slice s = null;
        try{

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, sliceName);
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            return "error";
        }

        s.logger().debug("******************** START Before Slice Info " + s.getName() + " *********************");
        s.logger().debug("******************** END Before  *********************");

        ComputeNode node1 = (ComputeNode)s.getResourceByName(nodeName);
        BroadcastNetwork net = s.addBroadcastLink(networkName);
        Interface int1 = net.stitch(node1);
        s.logger().debug("******************** START After Slice Info " + s.getName() + " *********************");
        s.logger().debug("******************** END After  *********************");

        String rdfString = s.getRequest();

        try {
            s.commit();
        } catch (XMLRPCTransportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return rdfString;
    }

    public static void testAddComputeNode(String pem, String sliceName, String newNodeName, int domain){
        String switchImageShortName="Centos6.7-SDN.v0.1";
        String switchImageURL ="http://geni-images.renci.org/images/pruth/SDN/Centos6.7-SDN.v0.1/Centos6.7-SDN.v0.1.xml";
        String switchImageHash ="77ec2959ff3333f7f7e89be9ad4320c600aa6d77";
        String switchDomain=domains.get(domain);
        String switchNodeType="XO Medium";
        String switchPostBootScript="switch boot script";

        try{
            //SSH context
            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();
            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("/home/geni-orca/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();            
            sctx.addToken("pruth", "pruth", t);
            sctx.addToken("pruth", t);

            //ExoGENI controller context
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);
            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));
            //setup the nodes
            try{

                Slice s = Slice.loadManifestFile(sliceProxy, sliceName);

                ArrayList<ComputeNode> switches = new ArrayList<ComputeNode>();

                ComputeNode  sw = s.addComputeNode(newNodeName);
                sw.setImage(switchImageURL,switchImageHash,switchImageShortName);
                sw.setNodeType(switchNodeType);
                sw.setDomain(switchDomain);
                sw.setPostBootScript(switchPostBootScript);

                s.commit();
            } catch (Exception e){
                e.printStackTrace();
                System.err.println("Proxy factory test failed");
                assert(false);
            }

        } catch  (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }
    }

    public static String testAddNetwork(String pem, String sliceName, String node1Name, String node2Name, String networkName){
        Slice s = null;
        try{
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, sliceName);
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            return "error";
        }

        ComputeNode node1 = (ComputeNode)s.getResourceByName(node1Name);
        ComputeNode node2 = (ComputeNode)s.getResourceByName(node2Name);

        BroadcastNetwork net = s.addBroadcastLink(networkName);
        Interface int1 = net.stitch(node1);
        Interface int2 = net.stitch(node2);

        String rdfString = s.getRequest();

        try {
            s.commit();
        } catch (XMLRPCTransportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return rdfString;
    }

    public static void testDeleteNetwork(String pem, String networkName){
        Slice s = null;
        try{
            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, "pruth.slice1");
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            return;
        }
        BroadcastNetwork net = (BroadcastNetwork)s.getResourceByName(networkName);
        net.delete();

        try {
            s.commit();
        } catch (XMLRPCTransportException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public static void printRequest2Log(Slice s){
        s.logger().debug("******************** START printReqest2Log *********************");
        for (Node node : s.getNodes()){
            String printStr = "PRUTH:" + node;
            if (node instanceof ComputeNode){
                printStr += ", size: " + ((ComputeNode)node).getNodeCount();
                for (org.renci.ahab.libndl.resources.manifest.Node mn : ((ComputeNode)node).getManifestNodes()){
                    printStr += ", manifestNode: " + mn.getURI();
                }
            }
            s.logger().debug(printStr);
        }

        for (Network link : s.getLinks()){
            s.logger().debug("PRUTH:" + link);
        }
        s.logger().debug("******************** END printReqest2Log *********************");
    }

    private static final String GLOBAL_PREF_FILE = "/etc/rm/rm.properties";
    private static final String PREF_FILE = ".rm.properties";

    private static final String PUBSUB_PROP_PREFIX = "RM.pubsub";
    private static final String PUBSUB_SERVER_PROP = PUBSUB_PROP_PREFIX + ".server";
    private static final String PUBSUB_LOGIN_PROP = PUBSUB_PROP_PREFIX + ".login";
    private static final String PUBSUB_PASSWORD_PROP = PUBSUB_PROP_PREFIX + ".password";
    private static Properties rmProperties = null;

    /**
     * Read and process preferences file
     */
    protected static void processPreferences() {

        Properties p = System.getProperties();

        // properties can be under /etc/mm/mm.properties or under $HOME/.mm.properties
        // in that order of preference
        String prefFilePath = GLOBAL_PREF_FILE;

        try {
            System.err.println("loding properites from " + prefFilePath);
            rmProperties = loadPropertiesFromAnyFile(prefFilePath);
            System.err.println("rmProperties = " + rmProperties.toString());
            return;
        } catch (IOException ioe) {
            System.err.println("Unable to load global config file " + prefFilePath + ", trying local file");
        }

        prefFilePath = "" + p.getProperty("user.home") + p.getProperty("file.separator") + PREF_FILE;
        try {
            System.err.println("loding properites from " + prefFilePath);

            rmProperties = loadPropertiesFromAnyFile(prefFilePath);
            System.err.println("rmProperties = " + rmProperties.toString());
        } catch (IOException e) {
            System.err.println("Unable to load local config file " + prefFilePath + ", exiting.");
            System.exit(1);
        }
    }

    /**
     * loads properties from a file in the classpath
     * @param fileName
     * @return
     * @throws IOException
     */
    private static Properties loadProperties(String fileName) throws IOException {
        InputStream is = TestDriver.class.getClassLoader().getResourceAsStream(fileName);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        Properties p = new Properties();
        p.load(bin);
        bin.close();

        return p;
    }

    /**
     * loads properties from any file , given it's absolute path
     * @param fileName
     * @return
     * @throws IOException
     */
    private static Properties loadPropertiesFromAnyFile(String fileName) throws IOException {
        File prefs = new File(fileName);
        FileInputStream is = new FileInputStream(prefs);

        BufferedReader bin = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        Properties p = new Properties();
        p.load(bin);
        bin.close();

        return p;
    }

    // Send modify request to a specific ORCA controller
    private static void sendModifyRequestToORCA(String sliceId, String controllerUrl, String modifyReq){
        String modifyRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            modifyRes = orcaProxy.modifySlice(sliceId, modifyReq);
        } catch (Exception ex) {
            //logger.error("Exception while calling ORCA modifySlice" + ex);
            System.out.println("Exception while calling ORCA modifySlice" + ex);
            return;
        }
        return;
    }

    // Send create request to a specific ORCA controller
    private static void sendCreateRequestToORCA(String sliceId, String controllerUrl, String createReq){
        String createRes = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            createRes = orcaProxy.createSlice(sliceId, createReq);

        } catch (Exception ex) {
            System.out.println("Exception while calling ORCA createSlice" + ex);
            return;
        }
        return;
    }

    private static String getManifestFromORCA(String sliceId, String controllerUrl){

        String manifest = null;
        String sanitizedManifest = null;
        try {
            OrcaSMXMLRPCProxy orcaProxy = new OrcaSMXMLRPCProxy(rmProperties);
            orcaProxy.setControllerUrl(controllerUrl);
            manifest = orcaProxy.sliceStatus(sliceId);

            sanitizedManifest = sanitizeManifest(manifest);
        } catch (Exception ex) {
            System.out.println("Exception while calling ORCA sliceStatus" + ex);
            return null;
        }
        return sanitizedManifest;

    }

    private static String sanitizeManifest(String manifest) {

        if (manifest == null)
            return null;

        int ind = manifest.indexOf("<rdf:RDF");
        if (ind > 0)
            return manifest.substring(ind);
        else
            return null;


    }

    public static final ArrayList<String> domains;
    static {
        ArrayList<String> l = new ArrayList<String>();

        l.add("PSC (Pittsburgh, TX, USA) XO Rack");
        l.add("TAMU (College Station, TX, USA) XO Rack");

        l.add("UH (Houston, TX USA) XO Rack");
        l.add("WSU (Detroit, MI, USA) XO Rack");
        l.add("UFL (Gainesville, FL USA) XO Rack");
        l.add("OSF (Oakland, CA USA) XO Rack");
        l.add("SL (Chicago, IL USA) XO Rack");
        l.add("UMass (UMass Amherst, MA, USA) XO Rack");
        l.add("WVN (UCS-B series rack in Morgantown, WV, USA)");
        l.add("UAF (Fairbanks, AK, USA) XO Rack");
        l.add("BBN/GPO (Boston, MA USA) XO Rack");
        l.add("RENCI (Chapel Hill, NC USA) XO Rack");
        l.add("UvA (Amsterdam, The Netherlands) XO Rack");

        domains = l;
    }

    private static String  getSDNControllerScript(){
        return "#!/bin/bash \n" +
            "#script not build yet";
    }

    private static String getSDNSwitchScript(String SDNControllerIP){

        return "#!/bin/bash \n" +
            "{ \n " +
            "reset_bridge () { \n" +
            "   #$1 bridge name \n" +
            "  br_name=$1\n" +
            "  controller_ip=$2\n" +
            "\n" +
            " echo reset_bridge $br_name\n" +
            " ovs-vsctl del-br $br_name\n" +
            " ovs-vsctl add-br $br_name\n" +
            " ifconfig $br_name up\n" +
            "\n" +
            " echo \"setting ovs to use controller \" ${controller_ip} \n" +
            " ovs-vsctl set-controller br0 tcp:${controller_ip}:6633 \n" +
            "     ovs-vsctl set controller br0 connection-mode=out-of-band \n" +
            "              \n" +
            " } \n" +
            " \n" +
            " \n" +
            " is_bridge_consistant () { \n" +
            "     #checks to see if bridge has non-existant interfaces  \n" +
            "     br_name=$1 \n" +
            "     echo checking $br_name \n" +
            "     #1 bridge name                                           \n" +
            "     for iface in `ovs-vsctl list-ports ${br_name}`; do \n" +
            "        echo checkong $iface \n" +
            "         ip link show $iface 2>&1 > /dev/null \n" +
            "        if [ \"$?\" != \"0\" ]; then \n" +
            "            #error iface dne                             \n" +
            "            echo Found wedged iface $iface \n" +
            "            return 1 \n" +
            "        fi \n" +
            "    done \n" +
            "     \n" +
            "     return 0 \n" +
            " } \n" +
            " \n" +
            " /etc/init.d/openvswitch restart \n" +
            " sleep 10 \n" +
            " ovs-vsctl add-br br0 \n" +
            " ifconfig br0 up \n" +
            " controller_ip='" + SDNControllerIP + "' \n" +
            " echo \"setting ovs to use controller \" ${controller_ip} \n" +
            " ovs-vsctl set-controller br0 tcp:${controller_ip}:6633 \n" +
            " ovs-vsctl set controller br0 connection-mode=out-of-band \n" +
            " ovs-appctl fdb/show br0 \n" +
            " sleep 60 \n" +
            " while true; do \n" +
            "  #check to see if bridge is consistant \n" +
            "   is_bridge_consistant \"br0\" \n" +
            "           if [ \"$?\" != \"0\" ]; then \n" +
            "           echo resetting bridge br0 \n" +
            "           reset_bridge \"br0\" ${controller_ip} \n" +
            "   fi \n" +
            "     br0_ifaces=`ovs-vsctl list-ifaces br0` \n" +
            "     echo 'Ifaces on br0: '$br0_ifaces \n" +
            "     echo 'All ifaces' \n" +
            "     for i in `ip link show | grep '^[1-9]' | awk -F\": \"  '{print $2}'`; do  \n" +
            "        ip=`ip -f inet -o addr show $i` \n" +
            "      if [[ \"$ip\" != \"\" ]]; then \n" +
            "          echo skipping $i \n" +
            "      else \n" +
            "         echo checking ${i};   \n" +
            "          br_4_iface=`ovs-vsctl iface-to-br $i` \n" +
            "         if [ \"$?\" != \"0\" ]; then \n" +
            "             echo \"adding \"$i \n" +
            "             ifconfig $i promisc up \n" +
            "             ovs-vsctl add-port br0 $i \n" +
            "         else \n" +
            "            echo \"skipping already added iface: \"$i \n" +
            "          fi \n" +
            "       fi \n" +
            "   done \n" +
            "     echo sleeping 10 \n" +
            "     sleep 10  \n" +
            " done \n" +
            " echo Bootscript done. \n" +
            " } 2>&1 > /tmp/bootscript.log \n" +
            " " ;
    }

    public static void testCreateSliceWithStorage(String pem){
        try{

            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();

            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("~/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();

            sctx.addToken("kthare10", "kthare10", t);

            sctx.addToken("kthare10", t);

            System.out.println(sctx);

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://geni.renci.org:11443/orca/xmlrpc"));

            Slice s = Slice.create(sliceProxy, sctx, "kthare10.slice1");

            ComputeNode   n0 = s.addComputeNode("Node0");
            n0.setImage("http://geni-images.renci.org/images/standard/centos-comet/centos7.4-v1.0.3-comet/centos7.4-v1.0.3-comet.xml",
                    "3dd17be8e0c24dd34b4dbc0f0d75a0b3f398c520","centos7.4-v1.0.3-comet");
            n0.setNodeType("XO Extra large");
            n0.setDomain("UH (Houston, TX USA) XO Rack");

/*            ComputeNode   n1 = s.addComputeNode("Node1");
            n1.setImage("http://geni-images.renci.org/images/standard/centos/centos7.4-v1.0.3.xml",
                    "ebab47f2f1e9b7702d200cfa384ad86048bd29cd","centos7.4-v1.0.3");
            n1.setNodeType("XO Medium");
            n1.setDomain("UH (Houston, TX USA) XO Rack");

            BroadcastNetwork bn = s.addBroadcastLink("Network");
            bn.stitch(n0);
            bn.stitch(n1);
*/
            String storageName = "Storage0";
            StorageNode storage = s.addStorageNode(storageName, 1, "/mnt/target");
            storage.setDomain(n0.getDomain());
            storage.setDoFormat(true);
            System.out.println("after Adding storage=" + storage.toString());

            LinkNetwork conn = s.addLinkNetwork("C2S0");
            conn.stitch(storage);
            conn.stitch(n0, storage);
/*
            StitchPort stitchPort = s.addStitchPort("SP1", "3291", "http://geni-orca.renci.org/owl/ion.rdf#AL2S/Chameleon/Cisco/6509/GigabitEthernet/1/1",  10000000L);
            n0.stitch(stitchPort);
*/
            System.out.println("testCreateSliceWithStorage:\n" + s.getRequest());

            s.commit();

            System.out.println("successfully created slice");
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }

    }
    public static void testGetSliceState(String pem, String sliceName){
        Slice s = null;
        try{

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));
            //ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://ciena2-hn.exogeni.net:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, sliceName);
            System.out.println("Slice state=" + s.getState());

            ComputeNode c = (ComputeNode) s.getResourceByName("Node0");
            System.out.println("Compute= " + c);

            Collection<StorageNode> storageNodes = s.getStorageNodes();
            List<StorageNode> storageNodesToBeRenewed = new LinkedList<>();
            Collection<Interface> computeInterfaces  = c.getInterfaces();

            System.out.println("Compute interfaces " + computeInterfaces);

            for(StorageNode st: storageNodes) {
                for(Interface i: st.getInterfaces()) {
                    InterfaceNode2Net ifc = (InterfaceNode2Net)i;
                    if(c.getInterface((RequestResource)ifc.getLink()) != null) {
                        System.out.println("KOMAL");
                    }
                    else {
                        System.out.println("Not found " +i);
                    }
                }
            }


        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            e.printStackTrace();
            return;
        }
    }

    public static void testModifyWithStorage(String pem, String sliceName){
        Slice s = null;
        try{

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, sliceName);

            ComputeNode c = (ComputeNode) s.getResourceByName("Node0");
            String storageName = "Storage1";
            StorageNode storage = s.addStorageNode(storageName, 1, null);
            storage.setDomain(c.getDomain());
            System.out.println("after Adding storage=" + storage.toString());

            LinkNetwork conn = s.addLinkNetwork("C2S1");
            conn.stitch(storage);
            conn.stitch(c, storage);
            System.out.println(s.getRequest());
            s.commit();
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            e.printStackTrace();
            return;
        }
    }

    public static void testModifyDeleteStorage(String pem, String sliceName){
        Slice s = null;
        try{

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, sliceName);
            String storageName = "Storage1";
            StorageNode storage = (StorageNode) s.getResourceByName(storageName);
            storage.delete();
            System.out.println(s.getRequest());
            s.commit();
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            e.printStackTrace();
            return;
        }
    }

    public static void testSliceStatusWithStorage(String pem, String sliceName){
        Slice s = null;
        try{

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL    ("https://geni.renci.org:11443/orca/xmlrpc"));

            s = Slice.loadManifestFile(sliceProxy, sliceName);
        } catch (Exception e){
            s.logger().debug("Failed to fetch manifest");
            e.printStackTrace();
            return;
        }
    }


    public static void testCreateSliceWithStorageOnQueens(String pem){
        try{

            SliceAccessContext<SSHAccessToken> sctx = new SliceAccessContext<>();

            SSHAccessTokenFileFactory fac = new SSHAccessTokenFileFactory("~/.ssh/id_rsa.pub", true);
            SSHAccessToken t = fac.getPopulatedToken();

            sctx.addToken("kthare10", "kthare10", t);

            sctx.addToken("kthare10", t);

            System.out.println(sctx);

            ITransportProxyFactory ifac = new XMLRPCProxyFactory();
            System.out.println("Opening certificate " + pem + " and key " + pem);
            TransportContext ctx = new PEMTransportContext("", pem, pem);

            ISliceTransportAPIv1 sliceProxy = ifac.getSliceProxy(ctx, new URL("https://rocky-hn.exogeni.net:11443/orca/xmlrpc"));

            Slice s = Slice.create(sliceProxy, sctx, "kthare10-slice2");

            ComputeNode   n0 = s.addComputeNode("Node0");
            n0.setPostBootScript("touch /root/psb_was_here");

            //n0.setImage("http://geni-images.renci.org/images/standard/centos-comet/centos7.6.1810-comet/centos7.6.1810-comet.xml",
            //        "9a0538dc8b8631c2f16727044a501bb835ba40e7","centos7.6.1810-comet");

            //n0.setImage("http://geni-images.renci.org/images/standard/centos-comet/centos6.10-comet/centos6.10-comet.xml",
            //        "c21cce26d89e336695c64f94c3ccfebac88e856c","centos6.10-comet");


            //n0.setImage("http://geni-images.renci.org/images/standard/debian-comet/debian-9.9.0-comet/debian-9.9.0-comet.xml",
            //        "932d7a6ca3c6e37fdc334c8330d2e8380e67f0d4","debian-9.9.0-comet");

            //n0.setImage("http://geni-images.renci.org/images/standard/fedora-comet/fedora30-v1.2-comet/fedora30-v1.2-comet.xml",
            //        "8ed6c2d1e69b30f42b2deb02eb6b7404679c212d","fedora30-v1.2-comet");

            n0.setImage("http://geni-images.renci.org/images/standard/ubuntu-comet/ubuntu-16.04-comet/ubuntu-16.04-comet.xml",
                    "cd51e0f0399b54b3c6b48917ec819ffe75d8c200","ubuntu-16.04-comet");


            n0.setNodeType("XO Medium");
            n0.setDomain("ROCKY XO Rack");

            String storageName = "Storage0";
            StorageNode storage = s.addStorageNode(storageName, 1, "/mnt/target");
            storage.setDomain(n0.getDomain());
            System.out.println("after Adding storage=" + storage.toString());

            LinkNetwork conn = s.addLinkNetwork("C2S0");
            conn.stitch(storage);
            conn.stitch(n0, storage);

            System.out.println("testCreateSliceWithStorage:\n" + s.getRequest());

            s.commit();

            System.out.println("successfully created slice");
        } catch (Exception e){
            e.printStackTrace();
            System.err.println("Proxy factory test failed");
            assert(false);
        }

    }
    public static void main(String [] args){

    LIBNDL.setLogger();

    System.out.println("ndllib TestDriver: START");

    //TestDriver.deleteAllSDXNetworks(args[0], "pruth.sdx.1");
    //TestDriver.setupSDXLinearNetwork(args[0],"pruth.sdx.1" );
    //TestDriver.testCreateSliceWithStorage(args[0]);
    //TestDriver.testGetSliceState(args[0],"kthare10.slice1");
    //TestDriver.testModifyWithStorage(args[0],"kthare10.slice1");
    //TestDriver.testModifyDeleteStorage(args[0],"kthare10.slice1");
    //TestDriver.testSliceStatusWithStorage(args[0],"kthare10.slice1");
    //TestDriver.testDelete(args[0],"kthare10.slice1");
    //TestDriver.testNewSlice1(args[0]);
    //TestDriver.testGetSliceState(args[0],"Mobius-Exogeni-kthare10-7fa8e8a8-99c8-4b1b-a96e-aca4b61c8873");
    TestDriver.testCreateSliceWithStorageOnQueens(args[0]);
    System.out.println("ndllib TestDriver: END");
    }
}
