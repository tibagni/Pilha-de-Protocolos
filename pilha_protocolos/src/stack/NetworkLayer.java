/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import graph.Host;
import java.util.Collections;
import java.util.HashMap;

/**
 *
 * @author tiago
 */
public class NetworkLayer implements Runnable {

    private HashMap<String, NextHost> routingTable = new HashMap<String, NextHost>();

    private static NetworkLayer networkLayer = null;

    private NetworkLayer() { }

    public static NetworkLayer getInstance() {
        if(networkLayer == null)
            networkLayer = new NetworkLayer();
        return networkLayer;
    }

    public Host getHostInRoutingTable(String ip) {
        HashMap<String, NextHost> m = (HashMap<String, NextHost>)
                Collections.synchronizedMap(routingTable);

        return m.get(ip).getHost();
    }

    /**
     * Set a host in routing table
     * @param hd Destination  host (final)
     * @param nh Next host to get to destination
     * @param hops Number of hops to hd
     */
    public void setHostInRoutingTable(Host hd, Host nh, int hops) {
        HashMap<String, NextHost> m = (HashMap<String, NextHost>)
                Collections.synchronizedMap(routingTable);
        m.put(hd.getLogicalID(), new NextHost(nh, hops));
    }

    /**
     * Execute the routing algorithm every 3 minutes
     */
    public void run() {
        //throw new UnsupportedOperationException("Not supported yet.");
    }

    private class NextHost {
        private Host host;
        private int hops;

        public NextHost(Host h, int hp) {
            host = h;
            hops = hp;
        }

        public Host getHost() {
            return host;
        }

        public int getHops() {
            return hops;
        }

        public void setHost(Host h) {
            host = h;
        }

        public void setHops(int h) {
            hops = h;
        }
    }

    private static class Routing {

    }
}
