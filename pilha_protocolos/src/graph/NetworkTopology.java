/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graph;

import java.util.HashMap;
import stack.ProtocolStack;

/**
 *
 *
 * @author belitos
 */
public class NetworkTopology {

    private HashMap<String, Host> hosts;

    private static NetworkTopology networkTopology = null;

    public static NetworkTopology getInstance() {
        if(networkTopology == null)
            networkTopology = new NetworkTopology();
        return networkTopology;
    }

    private NetworkTopology() {
        hosts = new HashMap<String, Host>();
       
    }

    public void addHost(Host n) {
        hosts.put(n.getLogicalID(), n);

    }

    public Host getHost(String key) {
        return hosts.get(key);
    }

    public void addConnection(Host n1, Host n2, int mtu) {
        Host localhost = ProtocolStack.getLocalhost();

        // No comeco, so armazena os vizinhos
        if(n1.getLogicalID().equals(localhost.getLogicalID())) {
            localhost.addNeighbour(n2, mtu);
            
        }
    }
}
