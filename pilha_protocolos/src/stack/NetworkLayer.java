/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import graph.Host;
import graph.Host.Connection;
import graph.NetworkTopology;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import pdu.Datagram;
import pdu.Datagram.TTLException;
import pilha_protocolos.Utilities;

/**
 *
 * @author tiago
 */
public class NetworkLayer implements Runnable, Serializable{

    private HashMap<String, NextHost> routingTable;
    private HashMap<Integer, ArrayList<Datagram>> datagramFragments;
    private ArrayList<Host> alive;

    private static NetworkLayer networkLayer = null;

    public static final int FREEZE_TIME = 60000;
    public static final int PING_TIME = 500;
    public static final int HOP_LIMIT = 15;

    private static final Object lock = new Object();
    private Routing routing;

    private NetworkLayer() {
        routingTable = new HashMap<String, NextHost>();
        routing = new Routing();
        datagramFragments = new HashMap<Integer, ArrayList<Datagram>>();
        alive = new ArrayList<Host>();
    }

    /**
     * Method called by tranport layer to send a message with random datagram id
     * @param data Transport layer message
     * @param to IP address of destination
     * @param protocol Upper layer protocol
     */
    public synchronized void send(byte[] data, String to, byte protocol) {
        // Set source address as localhost
        String from = ProtocolStack.getLocalhost().getLogicalID();
        send(data, to, protocol, Datagram.NONE, from);
    }

    /**
     * Method called by tranport layer to send a message with specified datagram id
     * @param data Transport layer message
     * @param to IP address of destination
     * @param protocol Upper layer protocol
     * @param datagramId Datagram id, if NONE will be random
     * @param from Source ip (usually localhost)
     */
    public synchronized void send(byte[] data, String to, byte protocol,
            int datagramId, String from) {

        if(datagramId == Datagram.NONE) {
            Random r = new Random();
            Random r2 = new Random();
            datagramId = r.nextInt() + r2.nextInt();
        }

        // Get next hop from routing table.
        Host nextHost = getHostInRoutingTable(to);
        if(nextHost == null) return;
        int limit = 5000; //nextHost.getLinkMtu();
        Datagram d = new Datagram(from, to, protocol, Datagram.TTL, datagramId, data);

        // Check to see if datagram is bigger than the limit (MTU)
        if(pilha_protocolos.Utilities.getObjectSize(d) > limit - LinkLayer.ADLER_LIMIT) {
            List<Datagram> fragments = new ArrayList<Datagram>();
            byte[] byteData = data;
            int dataSize = limit - Datagram.MAX_HEADER_SIZE - LinkLayer.ADLER_LIMIT;
            int fragId = 0;
            while(byteData.length > dataSize) {
                fragId++;
                byte[] auxBytes = Arrays.copyOfRange(byteData, 0, dataSize);
                byteData = Arrays.copyOfRange(byteData, dataSize, byteData.length);
                fragments.add(new Datagram(from, to, protocol, Datagram.TTL, datagramId,
                       auxBytes, fragId, false));
            }
            // Send last fragment with flag true
            fragments.add(new Datagram(from, to, protocol, Datagram.TTL, datagramId,
                        byteData, fragId, true));

            for(Datagram fragment : fragments) {
                // Send to linkLayer
                sendToLinkLayer(fragment, nextHost);
            }
        } else {
            // No fragmentation needed
            sendToLinkLayer(d, nextHost);
        }
    }

    private synchronized void sendToLinkLayer(Datagram d, Host nextHost) {
        byte[] datagram = Utilities.toByteArray(d);
        LinkLayer.getInstance().send(datagram, ProtocolStack.NETWORK_PROTOCOL_NP, nextHost);
    }

    /*package*/ void receive(byte[] datagramBytes) {
        try {
            Datagram datagram = (Datagram) Utilities.toObject(datagramBytes);
            // forward the packet if it's destination address isn't localhost
            if(!datagram.getDestination().equals(ProtocolStack.getLocalhost().getLogicalID())) {
                forward(datagram);
                return;
            }
            // Not fragmented, just deliver to upper layer
            if(datagram.isLastDatagramFragment() && datagram.getDatagramFragmentId() == 1) {
                deliverToUpperLayer(datagram);
            } else {
                List<Datagram> fragments = addToFragmentsList(datagram);
                Collections.sort(fragments);
                
                // Check to see if list is complete (all fragments)
                int fragId = 1;
                int dataSize = 0;
                boolean completed = false;
                for(Datagram d : fragments) {
                    if(d.getDatagramFragmentId() != fragId)
                        return;
                    if(d.isLastDatagramFragment()) completed = true;
                    dataSize += d.getData().length;
                    fragId++;
                }
                byte[] data = new byte[dataSize];
                if(completed) {
                    int j = 0;
                    for(Datagram d : fragments) {
                        byte[] tempData = d.getData();
                        for(int i = 0; i < tempData.length; i++, j++)
                            data[j] = tempData[i];
                    }
                    try {
                        datagram = (Datagram) Utilities.toObject(data);
                        deliverToUpperLayer(datagram);
                        datagramFragments.remove(datagram.getDatagramId());
                        fragments = null;
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }

        } catch(Exception ex) {
            //Fodeu!!!
            ex.printStackTrace();
        }
    }

    private List<Datagram> addToFragmentsList(Datagram datagram) {
        ArrayList<Datagram> fragments = datagramFragments.get(datagram.getDatagramId());
        if(fragments == null) {
            fragments = new ArrayList<Datagram>();
            datagramFragments.put(datagram.getDatagramId(), fragments);
        }
        // Discard duplicated datagrams
        if(!fragments.contains(datagram))
            fragments.add(datagram);

        return fragments;
    }

    private void forward(Datagram d) {
        try {
            d.decrementTTL();
            // Just forward the datagram mantaining the DatagramId and source IP
            // (just the end host should handle the fragmentation)
            send(d.getData(), d.getDestination(), d.getUpperLayerProtocol(), 
                    d.getDatagramId(), d.getSource());
        } catch(TTLException ex) {
            // TTL is 0, the packet must be discarded now!
        }
    }

    private void deliverToUpperLayer(Datagram datagram) {
        if(datagram == null) return;

        switch(datagram.getUpperLayerProtocol()) {
            case ProtocolStack.TRASNPORT_PROTOCOL_RDT:
                System.out.println("recebi filho da poata");
                break;
            case ProtocolStack.TRASNPORT_PROTOCOL_UDT:
                break;
            case ProtocolStack.ROUTING_ALGORITHM:
                ArrayList<DistanceVector> distanceVector =
                        (ArrayList<DistanceVector>)Utilities.toObject(datagram.getData());
                
                routing.recalculate(distanceVector, datagram.getSource());
                break;
            case ProtocolStack.ICMP_REQUEST:
                // Send replay
                System.out.println("received request");
                Host h = NetworkTopology.getInstance().getHost(datagram.getSource());
                setHostInRoutingTable(h, h, 1);
                send(new String("icmp replay").getBytes(), datagram.getSource(), ProtocolStack.ICMP_REPLAY);
                break;
            case ProtocolStack.ICMP_REPLAY:
                // Set sender host as alive
                System.out.println("received reply");
                alive.add(NetworkTopology.getInstance().getHost(datagram.getSource()));
                break;
        }
    }

    public static NetworkLayer getInstance() {
        if(networkLayer == null)
            networkLayer = new NetworkLayer();
        return networkLayer;
    }

    private Host getHostInRoutingTable(String ip) {
        Map<String, NextHost> m = 
                Collections.synchronizedMap(routingTable);

        NextHost nh = m.get(ip);
        if(nh != null)
            return nh.getHost();

        return null;
    }

    private boolean isShorter(String ip, NextHost next)
    {
        Map<String, NextHost> m = 
                Collections.synchronizedMap(routingTable);

        if(m.get(ip).getHops() > next.getHops())
            return true;

        return false;
    }

    /**
     * Set a host in routing table
     * @param hd Destination  host (final)
     * @param nh Next host to get to destination
     * @param hops Number of hops to hd
     * @return if the operation was completed
     */
    public boolean setHostInRoutingTable(Host hd, Host nh, int hops)
    {
        Map<String, NextHost> m = 
                Collections.synchronizedMap(routingTable);


        NextHost nextHost = new NextHost(nh, hops);

        if(hops > HOP_LIMIT)
            return false;

        if(((!m.containsKey(hd.getLogicalID()))
                || isShorter(hd.getLogicalID(),nextHost)) &&
                (!ProtocolStack.getLocalhost().getLogicalID().equals(hd.getLogicalID())) )
        {
            m.put(hd.getLogicalID(), nextHost);
            return true;
        }

        return false;
    }

    /**
     * Execute the routing algorithm every 3 minutes
     */
    public void run() {

        while(true)
        {
            try {
                routing.checkAlive();
                Thread.sleep(PING_TIME);
                routing.start();
                System.out.printf("depois do start %s\n %s\n",routingTable, ProtocolStack.getLocalhost());
                Thread.sleep(FREEZE_TIME);

            } catch (InterruptedException ex) {
                Logger.getLogger(NetworkLayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }

    private class NextHost implements Serializable {
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

        @Override
        public String toString() {
            return "host: " + host + " hopes: " + hops;
        }
    }

    private class Routing implements Serializable
    {

        public void checkAlive() {
            synchronized(lock) {
                HashMap<String, Connection> neighbours = ProtocolStack.getLocalhost().getNeighbour();
                Iterator<String> it = neighbours.keySet().iterator();
                while(it.hasNext()) {
                    Host h = neighbours.get(it.next()).getHost();

                    setHostInRoutingTable(h, h, 1);
                    send(new String("icmp request").getBytes(), h.getLogicalID(),
                            ProtocolStack.ICMP_REQUEST);
                }
            }
        }
        
        public void start()
        {
                synchronized(lock)
                {
                    // Reset Routing table
                    routingTable.clear();
                    ArrayList<DistanceVector> distanceVector = new ArrayList<DistanceVector>();

                    // Percorre apenas vizinhos ativos
                    for(Host h : alive) {
                        setHostInRoutingTable(h,h,1);
                        distanceVector.add(new DistanceVector(h.getLogicalID(), 1));
                    }

                    //PROPAGAR
                    sendDistanceVector(distanceVector);
                    alive.clear();
              }
        }

        public void recalculate(ArrayList<DistanceVector> array, String source)
        {
            synchronized(lock)
            {
                
                ArrayList<DistanceVector> myArray = new ArrayList<DistanceVector>();
                for(int i = 0; i < array.size(); i++)
                {
                    DistanceVector d = array.get(i);
                    Host hd = NetworkTopology.getInstance().getHost(d.getDestination());
                    Host nh = NetworkTopology.getInstance().getHost(source);

                    if(setHostInRoutingTable(hd,nh,d.getHops()+1))
                        myArray.add(new DistanceVector(hd.getLogicalID(),d.getHops()+1));


                }

                sendDistanceVector(myArray);

                System.out.printf("%s\n",routingTable);
            }
        }
    }
    
    private synchronized void sendDistanceVector(ArrayList<DistanceVector> distance) {
        if(distance.size() > 0) {
            byte[] distanceVector = Utilities.toByteArray(distance);
            if (distanceVector == null) return;

            HashMap<String, Connection> neighbours = ProtocolStack.getLocalhost().getNeighbour();
            Iterator<String> it = ProtocolStack.getLocalhost().getNeighbour().keySet().iterator();
            while(it.hasNext()) {
                send(distanceVector, neighbours.get(it.next()).getHost().getLogicalID(),
                        ProtocolStack.ROUTING_ALGORITHM);
            }
        }
    }

    public class DistanceVector implements Serializable
    {

        private Integer hops;
        private String destinationIP;


        public DistanceVector(String d, int h)
        {
            destinationIP = new String(d);
            hops = h;
        }

        public String getDestination()
        {
            return destinationIP;
        }

        public int getHops()
        {
            return hops;
        }

    }
}
