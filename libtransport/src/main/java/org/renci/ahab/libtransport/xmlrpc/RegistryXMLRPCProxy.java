package org.renci.ahab.libtransport.xmlrpc;

import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;

import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.XmlRpcClient;
import org.apache.xmlrpc.client.XmlRpcClientConfigImpl;
import org.renci.ahab.libtransport.IActorRegistryAPIv1;

/**
 * XMLRPC Proxy singleton for ORCA Actor Registry (partial)
 * @author ibaldin
 *
 */
public class RegistryXMLRPCProxy implements IActorRegistryAPIv1 {
	private static final String GET_AMS = "registryService.getAMs";
	private static final String GET_IMAGES = "registryService.getAllImages";
	private static final String GET_CONTROLLERS = "registryService.getAllControllers";
	private static final String GET_DEFAULT_IMAGE = "registryService.getDefaultImage";
	
	// fields returned by the registry for actors
	public enum Field {
		DESCRIPTION("DESC"),
		FULLRDF("FULLRDF");
		
		private final String name;
		
		private Field (String n) {
			name = n;
		}
		
		public String getName() {
			return name;
		}
	}
	
	private byte[] registryCertDigest;
	
	/**
	 * Create a new proxy with an expected fingerprint of the registry cert
	 * @param regFingerprint
	 * @param url
	 * @throws XMLRPCTransportException
	 */
	public RegistryXMLRPCProxy(String regFingerprint, final URL url) throws XMLRPCTransportException {
		// singleton
        // get registry cert fingerprint
    	String[] fingerPrintBytes = regFingerprint.split(":");
    	
    	registryCertDigest = new byte[16];
    	
    	for (int i = 0; i < 16; i++ )
    		registryCertDigest[i] = (byte)(Integer.parseInt(fingerPrintBytes[i], 16) & 0xFF);
        
    	// Create a trust manager that does not validate certificate chains
        // so we can speak to the registry
    	TrustManager[] trustAllCerts = new TrustManager[] {
    			new X509TrustManager() {
    				boolean initState = false;
    				X509TrustManager defaultTrustManager = null;
    				
    				private void init() {
    					if (initState) 
    						return;
    					initState = true;
    					try {
    						TrustManagerFactory trustMgrFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
    						trustMgrFactory.init((KeyStore)null);
    						TrustManager trustManagers[] = trustMgrFactory.getTrustManagers();
    						for (int i = 0; i < trustManagers.length; i++) {
    		                    if (trustManagers[i] instanceof X509TrustManager) {
    		                        defaultTrustManager = (X509TrustManager) trustManagers[i];
    		                        return;
    		                    }
    		                }
    					} catch(NoSuchAlgorithmException nsae) {
    						;
    					} catch(KeyStoreException kse) {
    						;
    					}
    				}
    				
    				public X509Certificate[] getAcceptedIssuers() {
    					return null;
    				}

    				public void checkClientTrusted(X509Certificate[] certs, String authType) {
    					// Trust always
    				}

    				public void checkServerTrusted(X509Certificate[] certs, String authType) {

    					init();
    					
    					MessageDigest md = null;
    					try {
    						md = MessageDigest.getInstance("MD5");

    						if (certs.length == 0) 
    							throw new CertificateException();

    						byte[] certDigest = md.digest(certs[0].getEncoded());
    						// note that this TM is used to validate different certs:
    						// 1. The registry cert (whose fingerprint we want to match)
    						// 2. https://geni-orca.renci.org/owl
    						// 3. https://api.twitter.com/ 
    						// and so on. 
    						if (!Arrays.equals(certDigest, registryCertDigest)) {
    							if (defaultTrustManager != null)
    								defaultTrustManager.checkServerTrusted(certs, authType);
    						}
    					} catch (NoSuchAlgorithmException e) {
    						;
    					} catch (Exception e) {
    						e.printStackTrace();
    					}
    				}
    			}
    	};
     
        // Install the all-trusting trust manager
        try {
        	SSLContext sc = SSLContext.getInstance("TLS");
        	// Create empty HostnameVerifier
        	HostnameVerifier hv = new HostnameVerifier() {
        		public boolean verify(String arg0, SSLSession arg1) {
        			return true;
        		}
        	};

        	sc.init(null, trustAllCerts, new java.security.SecureRandom());
        	HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
        	HttpsURLConnection.setDefaultHostnameVerifier(hv);
        } catch (NoSuchAlgorithmException e1) {

        } catch (KeyManagementException e2) {

        }
	}
	
	/* (non-Javadoc)
	 * @see orca.ahab.libtransport.xmlrpc.IActorRegistryAPIv1#getAMs(boolean, java.net.URL)
	 */
	@Override
	public Map<String, Map<String, String>> getAMs(boolean verbose, URL url) throws Exception {
        // call the actor registry
        Map<String, Map<String, String>> amData = null;
        try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(url);
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
			
			// get verbose list of the AMs
			amData = (Map<String, Map<String, String>>)client.execute(GET_AMS, new Object[]{!verbose});
        } catch (XmlRpcException e) {
        	throw new Exception("Unable to contact registry " + url + " due to " + e);
        }
		return amData;
	}
	
	/**
	 * Get specific field from the map (field names guaranteed to be typo free, use values from RegistrXMLRPCProxy.Field enum)
	 * @param k
	 * @param m
	 * @param f
	 * @return
	 */
	public static String getField(String k, Map<String, Map< String, String>> m, Field f) {
		if (!m.containsKey(k))
			return null;
		return m.get(k).get(f.getName());
	}
	
	/* (non-Javadoc)
	 * @see orca.ahab.libtransport.xmlrpc.IActorRegistryAPIv1#getImages(java.net.URL)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getImages(URL url) throws XMLRPCTransportException {
		List<Map<String, String>> ret = null;
		
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(url);
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
		
			// get verbose list of the AMs
			Object[] rret = (Object[])client.execute(GET_IMAGES, new Object[]{});
			ret = new ArrayList<Map<String, String>>();
			for (Object n: rret) {
				if (n instanceof HashMap<?, ?>) {
					ret.add((HashMap<String, String>)n);
				}
			}
		} catch (XmlRpcException e) {
			throw new XMLRPCTransportException("Unable to contact registry " + url + " due to " + e);
		}
		return ret;
	}
	
	/* (non-Javadoc)
	 * @see orca.ahab.libtransport.xmlrpc.IActorRegistryAPIv1#getControllers(java.net.URL)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public List<Map<String, String>> getControllers(URL url) throws XMLRPCTransportException {
		List<Map<String, String>> ret = null;
		
		try {
			XmlRpcClientConfigImpl config = new XmlRpcClientConfigImpl();
			config.setServerURL(url);
			XmlRpcClient client = new XmlRpcClient();
			client.setConfig(config);
		
			// get verbose list of the AMs
			Object[] rret = (Object[])client.execute(GET_CONTROLLERS, new Object[]{});
			ret = new ArrayList<Map<String, String>>();
			for (Object n: rret) {
				if (n instanceof HashMap<?, ?>) {
					ret.add((HashMap<String, String>)n);
				}
			}
		} catch (XmlRpcException e) {
			throw new XMLRPCTransportException("Unable to contact registry " + url + " due to " + e);
		}
		return ret;
	}
}
