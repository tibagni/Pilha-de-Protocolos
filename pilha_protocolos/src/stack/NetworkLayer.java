/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import graph.Host;
import graph.Host.Connection;
import graph.NetworkTopology;
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
public class NetworkLayer implements Runnable {

    private HashMap<String, NextHost> routingTable;
    private HashMap<Integer, ArrayList<Datagram>> datagramFragments;

    private static NetworkLayer networkLayer = null;

    public static final int FREEZE_TIME = 10;

    private static final Object lock = new Object();
    private Routing routing;

    private NetworkLayer() {
        routingTable = new HashMap<String, NextHost>();
        routing = new Routing();
        datagramFragments = new HashMap<Integer, ArrayList<Datagram>>();
    }

    /**
     * Method called by tranport layer to send a message
     * @param data Transport layer message
     * @param to IP address of destination
     * @param protocol Upper layer protocol
     */
    public synchronized void send(byte[] data, String to, byte protocol) {
        // Set source address as localhost
        String from = ProtocolStack.getLocalhost().getLogicalID();

        Random r = new Random();
        Random r2 = new Random(r.nextInt());
        int datagramId = r.nextInt(r2.nextInt(200)) + r2.nextInt(r.nextInt(100));

        // Get next hop from routing table.
        Host nextHost = getHostInRoutingTable(to);
        int limit = nextHost.getLinkMtu();
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
                sendToLinkLayer(fragment);
            }
        } else {
            // No fragmentation needed
            sendToLinkLayer(d);
        }
    }

    private synchronized void sendToLinkLayer(Datagram d) {
        byte[] datagram = Utilities.toByteArray(d);
        Host to =  getHostInRoutingTable(d.getDestination());
        LinkLayer.getInstance().send(datagram, ProtocolStack.NETWORK_PROTOCOL_NP, to);
    }

    /*package*/ void receive(byte[] datagramBytes) {
        try {
            Datagram datagram = (Datagram) Utilities.toObject(datagramBytes);
            // Se o destino não for localhost, repassa
            if(!datagram.getDestination().equals(ProtocolStack.getLocalhost().getLogicalID())) {
                forward(datagram);
                return;
            }
            // Not fragmented
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
                        deliverToUpperLayer(data);
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
        //TODO
        try {
            d.decrementTTL();
            sendToLinkLayer(d);
        } catch(TTLException ex) {

        }
    }

    private void deliverToUpperLayer(Datagram d) {
        deliverToUpperLayer(d.getData());
    }

    private void deliverToUpperLayer(byte[] data) {

    }

    public static NetworkLayer getInstance() {
        if(networkLayer == null)
            networkLayer = new NetworkLayer();
        return networkLayer;
    }

    private Host getHostInRoutingTable(String ip) {
        Map<String, NextHost> m = 
                Collections.synchronizedMap(routingTable);

        return m.get(ip).getHost();
    }

    private boolean isShorter(String ip,NextHost next)
    {
        Map<String, NextHost> m = 
                Collections.synchronizedMap(routingTable);

        if(m.get(ip).getHops() < next.getHops())
            return true;

        return false;
    }

    /**
     * Set a host in routing table
     * @param hd Destination  host (final)
     * @param nh Next host to get to destination
     * @param hops Number of hops to hd
     */
    public boolean setHostInRoutingTable(Host hd, Host nh, int hops)
    {
        Map<String, NextHost> m = 
                Collections.synchronizedMap(routingTable);


        NextHost nextHost = new NextHost(nh, hops+1);

        if((!m.containsKey(hd.getLogicalID()))
                || isShorter(hd.getLogicalID(),nextHost))
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
                Thread.sleep(FREEZE_TIME);

               // System.err.printf("Routing start\n\n");

                System.err.printf("%s\n",routingTable);

                routing.start();

            } catch (InterruptedException ex) {
                Logger.getLogger(NetworkLayer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
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

    private  class Routing 
    {

        public void start()
        {
                synchronized(lock)
                {


                    HashMap<String,Connection> neighbour = ProtocolStack.getLocalhost().getNeighbour();
                    ArrayList<DistanceVector> distanceVector = new ArrayList<DistanceVector>();


                    Iterator<String> it = neighbour.keySet().iterator();

                    while(it.hasNext())
                    {
                        Host h = neighbour.get(it.next()).getHost();

                        setHostInRoutingTable(h,h,1);
                        distanceVector.add(new DistanceVector(h.getLogicalID(),1));
                    }

                    //PROPAGAR
                    sendDistanceVector(distanceVector);

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

                    if(setHostInRoutingTable(hd,nh,d.getHops()))
                        myArray.add(new DistanceVector(hd.getLogicalID(),d.getHops()+1));


                }

                sendDistanceVector(myArray);
            }
        }



    }
    
    private synchronized void sendDistanceVector(ArrayList<DistanceVector> distance)
    {
        if(distance.size() > 0)
        {
            //envia Cheira picas
            
            //sendToLinkLayer(new Datagram());
            
        }
        
    }

    public class DistanceVector
    {

        private int hops;
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
