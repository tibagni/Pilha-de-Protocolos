/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import graph.Host;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import pdu.Datagram;

/**
 *
 * @author tiago
 */
public class NetworkLayer implements Runnable {

    private HashMap<String, NextHost> routingTable;

    private static NetworkLayer networkLayer = null;

    private NetworkLayer() {
        routingTable = new HashMap<String, NextHost>();
    }

    /**
     * Method called by tranport layer to send a message
     * @param data Transport layer message
     * @param to IP address of destination
     * @param protocol Upper layer protocol
     */
    public void send(Object data, String to, byte protocol) {
        // Set source address as localhost
        String from = ProtocolStack.getLocalhost().getLogicalID();

        // Get next hop from routing table.
        Host nextHost = getHostInRoutingTable(to);
        int limit = nextHost.getLinkMtu();
        Datagram d = new Datagram(from, to, protocol, 4, data);

        // Check to see if datagram is bigger than the limit (MTU)
        if(pilha_protocolos.Utilities.getObjectSize(d) > limit - LinkLayer.ADLER_LIMIT) {
            // TODO fragmentation
            // Criar um array de datagramas e ir instanciando
            // de acordo com a necessidade passando id correto
            // depois chamar o método sendToLinkLayer n vezes
            //(1 vez para cada datagrama) - de boa...
            List<Datagram> fragments = new ArrayList<Datagram>();
            // TODO ...
            for(Datagram fragment : fragments) {
                // Send to linkLayer
                sendToLinkLayer(fragment);
            }
        } else {
            // No fragmentation needed
            sendToLinkLayer(d);
        }
    }

    private void sendToLinkLayer(Datagram d) {

    }

    public static NetworkLayer getInstance() {
        if(networkLayer == null)
            networkLayer = new NetworkLayer();
        return networkLayer;
    }

    private Host getHostInRoutingTable(String ip) {
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
    private void setHostInRoutingTable(Host hd, Host nh, int hops) {
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
