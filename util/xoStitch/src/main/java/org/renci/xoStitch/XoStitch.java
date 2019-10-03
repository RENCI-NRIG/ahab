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
import org.apache.commons.cli.HelpFormatter;
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
  static int bw;

  static Map<String, String> domains;


  static ISliceTransportAPIv1 sliceProxy;
  static SliceAccessContext<SSHAccessToken> sctx;


  static void checkPrintList(String[] args){
    Option option_list= Option.builder("list").required(false).hasArg().argName("list type").desc("'stitchports': Prints a list of shortnames for known stitchports. 'hopsites': Prints a list of ExoGENI sites that can be used to deploy a hop in a circute").build();

    Options options = new Options();
    CommandLineParser parser = new DefaultParser();

    CommandLine commandLine = null;
    options.addOption(option_list);
    try
      {
        commandLine = parser.parse(options, args);

        //print splist
        if (commandLine.hasOption("list")){
          String listType = commandLine.getOptionValue("list");

          if (listType.equals("stitchports")){

            System.out.println("Known ExoGENI Stitchports:");
            for (String key: KNOWN_STITCHPORTS.keySet()){
              String value = KNOWN_STITCHPORTS.get(key).toString();
              System.out.println(key + " " + value);
            }
          } else if(listType.equals("hopsites")){
            System.out.println("ExoGENI Hop Sites:");
            for (String key: domains.keySet()){
              String value = domains.get(key).toString();
              System.out.println(key + " : " + value);
            }
          } else {
              System.out.println("Invalid list types. Accepted values 'stitchports' or 'hopsites'");
          }
          System.exit(0);
        }
      }catch (ParseException exception){}
  }

  public static void main( String[] args ) throws Exception
  {
    //List of known stitchport that have short names
    KNOWN_STITCHPORTS = new HashMap<String, String>();
    KNOWN_STITCHPORTS.put("uc", "http://geni-orca.renci.org/owl/ion.rdf#AL2S/Chameleon/Cisco/6509/GigabitEthernet/1/1");
    KNOWN_STITCHPORTS.put("tacc", "http://geni-orca.renci.org/owl/ion.rdf#AL2S/TACC/Cisco/6509/TenGigabitEthernet/1/1");

    //List of all ExoGENI racks that can support a hop node
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
    domains.put("ciena","CIENA (Ottawa,  CA) XO Rack");
    domains.put("ciena2","CIENA2 (Hanover, MD) XO Rack");

    checkPrintList(args);

    //Process commandline
    CommandLine commandLine = null;

    //common options
    Option option_certLocation = Option.builder("c").required(true).argName("GENI Certificate").hasArg().desc("Path to your GENI Certificate. Usually geni-username.pem").build();
    Option option_controllerUrl= Option.builder("u").required(false).argName("ExoGENI Controller URL").hasArg().desc("Optional. Defaults to ExoSM: " + DEFAULT_CONTROLLER_URL).build();
    Option option_sliceNane= Option.builder("n").required(false).argName("ExoGENI slice name").hasArg().desc("Name of your slice on ExoGENI. Optional. A default name will be constructed based on the requested circuit.").build();
    Option option_passphrase= Option.builder("p").required(false).argName("ExoGENI Certificat Passphrase").hasArg().desc("Optional. Defaults to empty string.").build();

    Option option_sp1 = Option.builder("sp1").required(true).argName("ExoGENI Stitchport URL for the first circuit endpoint").hasArgs().desc("Common stitchports can be a abbreviated with shortname. For example, the Chameleon stitchports can be referenced as 'uc' and 'tacc'. A list of ExoGENI stitchports is availible at https://wiki.exogeni.net/doku.php?id=public:experimenters:resource_types:start.").build();
    Option option_sp2 = Option.builder("sp2").required(true).argName("ExoGENI Stitchport URL for the second circuit endpoint").hasArg().desc("Common stitchports can be a abbreviated with shortname. For example, the Chameleon stitchports can be referenced as 'uc' and 'tacc'. A list of ExoGENI stitchports is availible at https://wiki.exogeni.net/doku.php?id=public:experimenters:resource_types:start.").build();
    Option option_vlan1 = Option.builder("vlan1").required(true).argName("VLAN 1").hasArg().desc("VLAN to use for ExoGENI Stitchport 1").build();
    Option option_vlan2 = Option.builder("vlan2").required(true).argName("VALN 2").hasArg().desc("LAN to use for ExoGENI Stitchport 1").build();

    Option option_hop= Option.builder("hop").required(false).argName("ExoGENI hop site").hasArg().desc("Specify the ExoGENI site to use as an intermediate hop for your circuit").build();
    Option option_bw= Option.builder("bw").required(false).argName("bandwidth").hasArg().desc("Bandwidth rate limiting for a circuit that includes a hop").build();

    Option option_list= Option.builder("list").required(false).argName("[stitchports|hopsites]").hasArg().desc("'stitchports': Prints a list of shortnames for known stitchports. 'hopsites': Prints a list of ExoGENI sites that can be used to deploy a hop in a circute").build();

    Options options = new Options();
    CommandLineParser parser = new DefaultParser();

    options.addOption(option_sp1);
    options.addOption(option_sp2);
    options.addOption(option_vlan1);
    options.addOption(option_vlan2);
    options.addOption(option_hop);
    options.addOption(option_bw);
    options.addOption(option_certLocation);
    options.addOption(option_controllerUrl);
    options.addOption(option_sliceNane);
    options.addOption(option_passphrase);
    options.addOption(option_list);



    try
    {
      commandLine = parser.parse(options, args);

      //Set controller url
      if (commandLine.hasOption("u")){
        controllerUrl = commandLine.getOptionValue("u");
      } else {
        controllerUrl = DEFAULT_CONTROLLER_URL;
      }

      //Set certificate location
      certLocation = commandLine.getOptionValue("c");
      keyLocation = certLocation;

      //Set cert passphrase (optional: defaults to "")
      if (commandLine.hasOption("p")){
        passphrase = commandLine.getOptionValue("p");
      } else {
        passphrase = "";
      }

      //Set optional hop site
      String hopStr = "";
      if (commandLine.hasOption("hop")){
        hopStr = commandLine.getOptionValue("hop");
        hop = domains.get(hopStr);
      }

      //Set optional bandwidth. (only works with hop)
      if (commandLine.hasOption("bw")){
        String bwStr = commandLine.getOptionValue("bw");
        bw = Integer.parseInt(bwStr);
      } else {
        bw = 10000000; //10 mbps
      }



      String url1_str = commandLine.getOptionValue("sp1");
      if (KNOWN_STITCHPORTS.containsKey(url1_str)) {
        sp1_url = KNOWN_STITCHPORTS.get(url1_str);
      } else {
        sp1_url = url1_str;
      }
      sp1_label = commandLine.getOptionValue("vlan1");

      String url2_str = commandLine.getOptionValue("sp2");
      if (KNOWN_STITCHPORTS.containsKey(url2_str)) {
        sp2_url = KNOWN_STITCHPORTS.get(url2_str);
      } else {
        sp2_url = url2_str;
      }
      sp2_label = commandLine.getOptionValue("vlan2");

      //optionally set slice name.
      if (commandLine.hasOption("n")){
        sliceName = commandLine.getOptionValue("n");
      } else {
        sliceName = "";
        if (KNOWN_STITCHPORTS.containsKey(url1_str)) {
          sliceName += url1_str + "-"+ sp1_label;
        } else {
          sliceName += "customUrl-"+ sp1_label;
        }
        if (KNOWN_STITCHPORTS.containsKey(url2_str)) {
          sliceName += "-" + url2_str + "-"+ sp2_label;
        } else {
          sliceName += "-customUrl-"+ sp2_label;
        }
      }

      String[] remainder = commandLine.getArgs();
      if (remainder.length >= 1){
        command = remainder[0];
      } else {
        System.out.print("Invalid stitch type or missing arguments");
      }

    }
    catch (ParseException exception)
    {
      //System.out.print("Parse error: ");
      //System.out.println(exception.getMessage());

      HelpFormatter formatter = new HelpFormatter();
      formatter.setWidth(150);
      formatter.printHelp("xoStitch [create|delete|status] -sp1 <stitchport> -vlan1 <vlan> -sp2 <stitchport> -vlan2 <vlan> -c <geni.pem> <options>", options );
      System.out.println("\n\n Examples: ");
      System.out.println("xoStitch create -sp1 uc -vlan1 3290 -sp2 tacc -vlan2 3500 -c geni.pem");
      System.out.println("xoStitch status -sp1 uc -vlan1 3290 -sp2 tacc -vlan2 3500 -c geni.pem");
      System.out.println("xoStitch delete -sp1 uc -vlan1 3290 -sp2 tacc -vlan2 3500 -c geni.pem");
      System.out.println("xoStitch create -sp1 http://geni-orca.renci.org/owl/ion.rdf#AL2S/Chameleon/Cisco/6509/GigabitEthernet/1/1 -vlan1 3290 -sp2 http://geni-orca.renci.org/owl/ion.rdf#AL2S/TACC/Cisco/6509/TenGigabitEthernet/1/1 -vlan2 3500 -c geni.pem");
      System.exit(1);
    }

    //buid the exogeni transport context
    buildContext(certLocation, keyLocation);

    //Call command
    if (command.equals("create")){
      if (hop != null) {
        XoStitch.createHopSlice(hop,bw,certLocation,keyLocation,controllerUrl,sliceName);
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

  public static void createHopSlice(String hopSite, int bw, String certLocation, String keyLocation, String controllerUrl, String sliceName ) throws Exception{
    //System.out.println("createHopSlice: " + hopSite + ", " + certLocation + ", " + keyLocation + ", " + controllerUrl + ", " + sliceName);
    //System.out.println("sp1: " + sp1_label + ", " + sp1_url);
    //System.out.println("sp2: " + sp2_label + ", " + sp2_url);

    Slice slice = Slice.create(sliceProxy,sctx,sliceName);

    String imageShortname="centos7-v1.0.4-openvswitch";
    String imageURL ="http://geni-images.renci.org/images/standard/centos/centos7.4-v1.0.4-openvswitch/centos7.4-v1.0.4-openvswitch.xml";
    String imageHash ="77094c373a787c0d2ff5594b26088a3a716f5c2f";
    String domain=hopSite;
    String nodeType="XO Extra large";
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

    StitchPort sp1 = slice.addStitchPort("sp1",sp1_label,sp1_url,bw);
    StitchPort sp2 = slice.addStitchPort("sp2",sp2_label,sp2_url,bw);

    hopNode.stitch(sp1);
    hopNode.stitch(sp2);

    slice.commit();
  }


  public static void createSlice(String certLocation, String keyLocation, String controllerUrl, String sliceName ) throws Exception{
    //System.out.println("createSlice: " + certLocation + ", " + keyLocation + ", " + controllerUrl + ", " + sliceName);
    //System.out.println("sp1: " + sp1_label + ", " + sp1_url);
    //System.out.println("sp2: " + sp2_label + ", " + sp2_url);

    Slice slice = Slice.create(sliceProxy,sctx,sliceName);

    StitchPort sp1 = slice.addStitchPort("sp1",sp1_label,sp1_url,100000000);
    StitchPort sp2 = slice.addStitchPort("sp2",sp2_label,sp2_url,100000000);

    sp1.stitch(sp2);

    slice.commit();
  }
}
