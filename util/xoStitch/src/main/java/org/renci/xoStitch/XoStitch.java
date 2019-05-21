package org.renci.xostitch;

import org.renci.ahab.libndl.Slice;
import org.renci.ahab.libndl.resources.request.ComputeNode;
import org.renci.ahab.libndl.resources.request.InterfaceNode2Net;
import org.renci.ahab.libndl.resources.request.Network;
import org.renci.ahab.libndl.resources.request.StitchPort;

import org.renci.ahab.libtransport.ISliceTransportAPIv1;
import org.renci.ahab.libtransport.ITransportProxyFactory;
import org.renci.ahab.libtransport.PEMTransportContext;
import org.renci.ahab.libtransport.SSHAccessToken;
import org.renci.ahab.libtransport.SliceAccessContext;
import org.renci.ahab.libtransport.TransportContext;
import org.renci.ahab.libtransport.util.SSHAccessTokenFileFactory;
import org.renci.ahab.libtransport.xmlrpc.XMLRPCProxyFactory;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.Option.Builder;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
/**
 * `mvn clean package`
 * `java -cp ./target/stitch-1.0-SNAPSHOT-jar-with-dependencies.jar org.renci.chameleon.jupyter.stitch.App certLocation keyLocation controllerURL sliceName`
 * Verify slice creation in Flukes
 */
public class XoStitch
{
  static final String DEFAULT_CONTROLLER_URL = "https://geni.renci.org:11443/orca/xmlrpc"; //exosm
  static HashMap<String, String> KNOWN_STITCHPORTS;



//  enum StitchType
//  {
//    GENERIC_STITCH, CHAMELEON_STITCH;
//  }

//  static StitchType type = StitchType.GENERIC_STITCH;

  static String certLocation = null;
  static String keyLocation = null;
  static String controllerUrl = DEFAULT_CONTROLLER_URL;
  static String passphrase = null;

  static String command = null;
  static String sliceName = null;

  static String sp1_url = null;
  static String sp2_url = null;
  static String sp1_label = null;
  static String sp2_label = null;

  static String hop = null;

  static Map<String, String> domains;


  static ISliceTransportAPIv1 sliceProxy;
  static SliceAccessContext<SSHAccessToken> sctx;

    public static void main( String[] args ) throws Exception
    {
      KNOWN_STITCHPORTS = new HashMap<String, String>();
      KNOWN_STITCHPORTS.put("uc", "http://geni-orca.renci.org/owl/ion.rdf#AL2S/Chameleon/Cisco/6509/GigabitEthernet/1/1");
      KNOWN_STITCHPORTS.put("tacc", "http://geni-orca.renci.org/owl/ion.rdf#AL2S/TACC/Cisco/6509/TenGigabitEthernet/1/1");


    		domains = new HashMap<String, String>();
    		domains.put("rci", "RENCI (Chapel Hill, NC USA) XO Rack");
    		domains.put("bbn", "BBN/GPO (Boston, MA USA) XO Rack");
    		domains.put("fiu","FIU (Miami, FL USA) XO Rack");
    		domains.put("uh","UH (Houston, TX USA) XO Rack");
    		domains.put("uva","UvA (Amsterdam, The Netherlands) XO Rack");
    		domains.put("ufl","UFL (Gainesville, FL USA) XO Rack");
    		domains.put("uaf","UAF (Fairbanks, AK, USA) XO Rack");
    		domains.put("sl","SL (Chicago, IL USA) XO Rack");
    		domains.put("wvn","WVN (UCS-B series rack in Morgantown, WV, USA)");
    		domains.put("tamu","TAMU (College Station, TX, USA) XO Rack");
    		domains.put("umass","UMass (UMass Amherst, MA, USA) XO Rack");
    		domains.put("wsu","WSU (Detroit, MI, USA) XO Rack");
    		domains.put("psc","PSC (Pittsburgh, TX, USA) XO Rack");
    		domains.put("unf","UNF (Jacksonville, FL) XO Rack");
    		domains.put("geu","GWU (Washington DC,  USA) XO Rack");
    		domains.put("ciena","CIENA (Ottawa,  CA) XO Rack");
    		domains.put("ciena2","CIENA2 (Hanover, MD) XO Rack");




        System.out.println( "Creating a simple Slice!" );




        // command line ideas:
        // Chameleon:   ./stitch create -chameleon -tacc 3501 -uc 2391 -cert /path/to/geni/cert
        // Other:       ./stitch create -sp1 <url> -l1 <label> -sp2 <url> -l2 <label> -cert /path/to/cert -controllerURL <url>

        CommandLine commandLine;
        //chameleon options
        //Option option_chameleon = Option.builder("chameleon").required(false).argName("chameleon").desc("chameleon").build();
        //Option option_chameleon_uc = Option.builder("uc").required(false).argName("uc").hasArg().desc("uc").build();
        //Option option_chameleon_tacc = Option.builder("tacc").required(false).argName("tacc").hasArg().desc("tacc").build();

        //non-Chameleon options
        //Option option_sp1 = Option.builder("sp1").required(false).argName("stitchport1").hasArg().desc("stitchport1").build();
        //Option option_sp2 = Option.builder("sp2").required(false).argName("stitchport2").hasArg().desc("stitchport2").build();
        Option option_url1 = Option.builder("url1").required(true).argName("url1").hasArg().desc("url1").build();
        Option option_url2 = Option.builder("url2").required(true).argName("url2").hasArg().desc("url2").build();
        Option option_vlan1 = Option.builder("vlan1").required(true).argName("vlan1").hasArg().desc("vlan1").build();
        Option option_vlan2 = Option.builder("vlan2").required(true).argName("vlan2").hasArg().desc("vlan2").build();
        //Option option_sp1_label = Option.builder("l1").required(false).argName("label1").hasArg().desc("label1").build();
        //Option option_sp2_label = Option.builder("l2").required(false).argName("label2").hasArg().desc("label2").build();

        //common options
        Option option_certLocation = Option.builder("c").required(true).argName("certLocation").hasArg().desc("certLocation").build();
        //Option option_keyLocation = Option.builder("k").required(false).argName("keyLocation").hasArg().desc("keyLocation").build();

        Option option_controllerUrl= Option.builder("u").required(false).argName("controllerUrl").hasArg().desc("controllerUrl").build();
        Option option_hop= Option.builder("hop").required(false).argName("hop").hasArg().desc("hop").build();

        Option option_sliceNane= Option.builder("n").required(false).argName("sliceNane").hasArg().desc("sliceName").build();
        Option option_passphrase= Option.builder("p").required(false).argName("passphrase").hasArg().desc("passphrase").build();

        Options options = new Options();
        CommandLineParser parser = new DefaultParser();

        //options.addOption(option_chameleon);
        //options.addOption(option_chameleon_uc);
        //options.addOption(option_chameleon_tacc);

        //options.addOption(option_sp1);
        //options.addOption(option_sp2);
        //options.addOption(option_sp1_label);
        ///options.addOption(option_sp2_label);
        //options.addOption(option_endpoints);
        options.addOption(option_url1);
        options.addOption(option_url2);
        options.addOption(option_vlan1);
        options.addOption(option_vlan2);
        options.addOption(option_hop);

        options.addOption(option_certLocation);
        //options.addOption(option_keyLocation);
        options.addOption(option_controllerUrl);
        options.addOption(option_sliceNane);
        options.addOption(option_passphrase);

        try
        {
          commandLine = parser.parse(options, args);

          //Set controller url
          if (commandLine.hasOption("u")){
            controllerUrl = commandLine.getOptionValue("u");
          } else {
            controllerUrl = DEFAULT_CONTROLLER_URL;
          }
          System.out.println("controller url: " + controllerUrl);

          //Set certificate location
          if (commandLine.hasOption("c")){
              certLocation = commandLine.getOptionValue("c");
              keyLocation = certLocation;
          } else {
              System.out.println("GENI certificate required (-c <certificate_file>)");
              System.exit(1);
          }



          //Set cert passphrase
          if (commandLine.hasOption("p")){
              passphrase = commandLine.getOptionValue("p");
          } else {
              passphrase = "";
          }

          //Set hop
          String hopStr = "";
          if (commandLine.hasOption("hop")){
              hopStr = commandLine.getOptionValue("hop");
              hop = domains.get(hopStr);
          }

          //optionally set slice name
          if (commandLine.hasOption("n")){
              sliceName = commandLine.getOptionValue("n");
          }

          //Set endpoint1
          String url1_str = "";
          if (commandLine.hasOption("url1"))
          {
            url1_str = commandLine.getOptionValue("url1");
            System.out.println("url1_str = " + url1_str);
            if (KNOWN_STITCHPORTS.containsKey(url1_str)) {
              sp1_url = KNOWN_STITCHPORTS.get(url1_str);
            } else {
              sp1_url = url1_str;
            }
          }
          if (commandLine.hasOption("vlan1"))
          {
            sp1_label = commandLine.getOptionValue("vlan1");
          }

          System.out.println("sp1_url = " + sp1_url);
          System.out.println("sp1_label = " + sp1_label);

          //Set endpoint2
          String url2_str = "";
          if (commandLine.hasOption("url2"))
          {
            url2_str = commandLine.getOptionValue("url2");
            System.out.println("url2_str = " + url2_str);
            if (KNOWN_STITCHPORTS.containsKey(url2_str)) {
              sp2_url = KNOWN_STITCHPORTS.get(url2_str);
            } else {
              sp2_url = url2_str;
            }
          }
          if (commandLine.hasOption("vlan2"))
          {
            sp2_label = commandLine.getOptionValue("vlan2");
          }

          System.out.println("sp2_url = " + sp2_url);
          System.out.println("sp2_label = " + sp2_label);

          //replace short stitchport names and build slice name
          if(sliceName == null){
            sliceName = "";
            if (KNOWN_STITCHPORTS.containsKey(url1_str)) {
              sliceName += url1_str + "_"+ sp1_label;
            } else {
              sliceName += "customUrl_"+ sp1_label;
            }
            if (KNOWN_STITCHPORTS.containsKey(url2_str)) {
              sliceName += "-" + url2_str + "_"+ sp2_label;
            } else {
              sliceName += "-customUrl_"+ sp2_label;
            }
          }

          System.out.println("Revised Endpoints:");
          System.out.println("sliceName: " + sliceName);
          System.out.println("sp1_url: " + sp1_url);
          System.out.println("sp1_label: " + sp1_label);
          System.out.println("sp2_url: " + sp2_url);
          System.out.println("sp2_label: " + sp2_label);


          String[] remainder = commandLine.getArgs();
          System.out.print("Remaining arguments: ");
          for (String argument : remainder)
          {
            System.out.print(argument);
            System.out.print(" ");
          }

          if (remainder.length >= 1){
            command = remainder[0];
          } else {
            System.out.print("Invalid stitch type or missing arguments");
          }

        }
        catch (ParseException exception)
        {
          System.out.print("Parse error: ");
          System.out.println(exception.getMessage());
        }

        //buid the exogeni transport context
        buildContext(certLocation, keyLocation);

        //Call command
        if (command.equals("create")){
          if (hop != null) {
            XoStitch.createHopSlice(hop,certLocation,keyLocation,controllerUrl,sliceName);
          } else {
            XoStitch.createSlice(certLocation,keyLocation,controllerUrl,sliceName);
          }
        } else if (command.equals("delete")){
          XoStitch.deleteSlice(sliceName);
        }else if (command.equals("status")){
          XoStitch.statusSlice(sliceName);
        } else {
          System.out.print("Invalid command: " + command);
        }
      }

    public static void buildContext(String certLocation, String keyLocation) throws Exception{
      /*
       * Get Slice Proxy
       */

      //ExoGENI controller context
      ITransportProxyFactory ifac = new XMLRPCProxyFactory();
      System.out.println("Opening certificate " + certLocation + " and key " + keyLocation);
      //passphrase = String.valueOf(System.console().readPassword("Enter password for key: "));
      TransportContext ctx = new PEMTransportContext(passphrase, certLocation, keyLocation);
      sliceProxy = ifac.getSliceProxy(ctx, new URL(controllerUrl));

      /*
       * SSH Context
       */
      sctx = new SliceAccessContext<>();

      SSHAccessTokenFileFactory fac;
      fac = new SSHAccessTokenFileFactory("~/.ssh/id_rsa.pub", false);

      SSHAccessToken t = fac.getPopulatedToken();
      sctx.addToken("root", "root", t);
      sctx.addToken("root", t);

    }

    public static void deleteSlice(String name) throws Exception{
      System.out.print("deleteSlice: " + name);
      Slice.loadManifestFile(sliceProxy, sliceName).delete();;
    }

    public static void statusSlice(String name){
      System.out.println("statusSlice: " + name);
      try{
        Slice s = Slice.loadManifestFile(sliceProxy, sliceName);

        System.out.println("state: " + s.getState());
      } catch (Exception e){
        System.out.println(e);
      }
      return;
    }

    public static void createHopSlice(String hopSite, String certLocation, String keyLocation, String controllerUrl, String sliceName ) throws Exception{
      System.out.println("createHopSlice: " + hopSite + ", " + certLocation + ", " + keyLocation + ", " + controllerUrl + ", " + sliceName);
      System.out.println("sp1: " + sp1_label + ", " + sp1_url);
      System.out.println("sp2: " + sp2_label + ", " + sp2_url);

      Slice slice = Slice.create(sliceProxy,sctx,sliceName);

      String imageShortname="centos7-v1.0.4-openvswitch";
      String imageURL ="http://geni-images.renci.org/images/standard/centos/centos7.4-v1.0.4-openvswitch/centos7.4-v1.0.4-openvswitch.xml";
      String imageHash ="77094c373a787c0d2ff5594b26088a3a716f5c2f";
      String domain=hopSite;
      String nodeType="XO Medium";
      String postBootScript=" #!/bin/bash \n " +
                              " BRIDGE=br0 \n " +
                              " ovs-vsctl add-br br0   \n " +
                              " while true; do \n " +
                              "  echo looping  \n " +
                              "  for iface in `ifconfig  | grep flags | awk -F\":\"  '{print $1}'`; do  \n " +
                              "     echo $iface  \n " +
                              "     ip address show dev $iface  | grep inet  | grep $iface  \n " +
                              "     if [ $? -eq 0 ]; then  \n " +
                              "        echo Skipping $iface     \n " +
                              "     else  \n " +
                              "        echo adding $iface  \n " +
                              "        ovs-vsctl add-port br0 $iface  \n " +
                              "     fi  \n " +
                              "  done  \n " +
                              "  sleep 5  \n " +
                              " done  \n ";





      ComputeNode   hopNode = slice.addComputeNode("hop");
      hopNode.setImage(imageURL,imageHash,imageShortname);
      hopNode.setNodeType(nodeType);
      hopNode.setDomain(domain);
      hopNode.setPostBootScript(postBootScript);

      StitchPort sp1 = slice.addStitchPort("sp1",sp1_label,sp1_url,100000000);
      StitchPort sp2 = slice.addStitchPort("sp2",sp2_label,sp2_url,100000000);

      hopNode.stitch(sp1);
      hopNode.stitch(sp2);
      //System.out.println(slice.getRequest());
      slice.commit();
    }


    public static void createSlice(String certLocation, String keyLocation, String controllerUrl, String sliceName ) throws Exception{
      System.out.println("createSlice: " + certLocation + ", " + keyLocation + ", " + controllerUrl + ", " + sliceName);
      System.out.println("sp1: " + sp1_label + ", " + sp1_url);
      System.out.println("sp2: " + sp2_label + ", " + sp2_url);

      Slice slice = Slice.create(sliceProxy,sctx,sliceName);

      StitchPort sp1 = slice.addStitchPort("sp1",sp1_label,sp1_url,10000000);
      StitchPort sp2 = slice.addStitchPort("sp2",sp2_label,sp2_url,10000000);

      sp1.stitch(sp2);
      //System.out.println(slice.getRequest());
      slice.commit();
    }
}
