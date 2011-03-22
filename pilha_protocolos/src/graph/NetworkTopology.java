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
    private HashMap<String, String> connections;

    public NetworkTopology() {
        hosts = new HashMap<String, Host>();
        connections = new HashMap<String, String>();
    }

    public void addHost(Host n) {
        hosts.put(n.getLogicalID(), n);

    }

    public Host getHost(String key) {
        return hosts.get(key);
    }

    public void addConnection(Host n1, Host n2, int mtu) {
        Host localhost = ProtocolStack.getLocalhost();

        // At first we only need to store the neighbours
        if(n1.getLogicalID().equals(localhost.getLogicalID())) {
            localhost.addNeighbour(n2, mtu);
            System.out.printf("Oi:%s\t%s\t%s\t%d\n", n1, n2, mtu, hosts.size());
        }
    }
}
