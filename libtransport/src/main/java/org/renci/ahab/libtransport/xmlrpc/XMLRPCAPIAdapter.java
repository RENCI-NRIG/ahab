package org.renci.ahab.libtransport.xmlrpc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.renci.ahab.libtransport.ISliceAccessAPIAdapter;
import org.renci.ahab.libtransport.SSHAccessToken;

public class XMLRPCAPIAdapter implements ISliceAccessAPIAdapter<SSHAccessToken> {

	private static final String SUDO_FIELD = "sudo";
	private static final String KEYS_FIELD = "keys";
	private static final String LOGIN_FIELD = "login";

	/**
	 * For XMLRPC we expect a map of logins and SSH public key strings 
	 */
	@Override
	public List<Map<String, Object>> convertTokens(Map<String, Map<String, List<SSHAccessToken>>> m) {
		List<Map<String, Object>> ret = new ArrayList<>();
		// loop over ids
		for(Map.Entry<String, Map<String, List<SSHAccessToken>>> e: m.entrySet()) {
			String id = e.getKey();
			List<SSHAccessToken> sshTokens = new ArrayList<>();
			// loop over elements and collect keys
			for(Map.Entry<String, List<SSHAccessToken>> ee: e.getValue().entrySet()) {
				sshTokens.addAll(ee.getValue());
			}
			List<String> sshKeys = new ArrayList<>();
			boolean sudoFinal = false;
			for(SSHAccessToken t: sshTokens) {
				sshKeys.addAll(t.getKeys());
				sudoFinal = sudoFinal || t.getSudo();
			}
			Map<String, Object> userEntry = new HashMap<String, Object>();
			userEntry.put(LOGIN_FIELD, id);
			userEntry.put(KEYS_FIELD, sshKeys);
			if(sudoFinal) {
                userEntry.put(SUDO_FIELD, "yes");
            }
            else {
                userEntry.put(SUDO_FIELD, "no");
            }
			ret.add(userEntry);
        }
		return ret;
	}

}
