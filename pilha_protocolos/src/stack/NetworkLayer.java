/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import graph.Host;
import graph.Host.Connection;
import graph.NetworkTopology;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import pdu.Datagram;

/**
 *
 * @author tiago
 */
public class NetworkLayer implements Runnable {

    private HashMap<String, NextHost> routingTable;

    private static NetworkLayer networkLayer = null;

    public static final int FREEZE_TIME = 10;

    private static final Object lock = new Object();
    private Routing routing;

    private NetworkLayer() {
        routingTable = new HashMap<String, NextHost>();
        routing = new Routing();
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

    private synchronized void sendToLinkLayer(Datagram d) {

    }

    public static NetworkLayer getInstance() {
        if(networkLayer == null)
            networkLayer = new NetworkLayer();
        return networkLayer;
    }

    private   Host getHostInRoutingTable(String ip) {
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
        //throw new UnsupportedOperationException("Not supported yet.");

        


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
