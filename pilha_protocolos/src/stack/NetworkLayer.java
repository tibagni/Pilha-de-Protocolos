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
import java.util.Set;
import pdu.Datagram;
import pdu.Datagram.TTLException;
import pdu.Segment;
import pilha_protocolos.Utilities;

/**
 *
 * @author tiago
 */
public class NetworkLayer implements Runnable, Serializable {

    private HashMap<String, NextHost> routingTable;
    private HashMap<Integer, ArrayList<Datagram>> datagramFragments;

    private static NetworkLayer networkLayer = null;

    public static final int FREEZE_TIME = 30000;
    public static final int HOP_LIMIT = 15;

    private static final Object lock = new Object();
    private Routing routing;

    private NetworkLayer() {
        routingTable = new HashMap<String, NextHost>();
        routing = new Routing();
        datagramFragments = new HashMap<Integer, ArrayList<Datagram>>();

        // Inicializacao da tabela de roteamento - Tabela de rotas comeca conhecendo os vizinhos
        HashMap<String, Connection> neighbours = ProtocolStack.getLocalhost().getNeighbour();
        Iterator<String> it = neighbours.keySet().iterator();
        while(it.hasNext()) {
            Host h = neighbours.get(it.next()).getHost();
            setHostInRoutingTable(h, h, 1);
        }
    }

    /**
     * Chamado pela camada de transporte, cria um datagrama (fragmenta)
     * e envia para camada de enlace
     *
     * @param data Transport layer message
     * @param to IP address of destination
     * @param protocol Upper layer protocol
     */
    public synchronized void send(byte[] data, String to, byte protocol) {
        // Set source address as localhost
        String from = ProtocolStack.getLocalhost().getLogicalID();
        send(data, to, protocol, Datagram.NONE, from, 0);
    }

    /**
     * Cria e envia um datagrama com um id de datagrama já especificado
     *
     * @param data Transport layer message
     * @param to IP address of destination
     * @param protocol Upper layer protocol
     * @param datagramId Datagram id, if NONE will be random
     * @param from Source ip (usually localhost)
     */
    private synchronized void send(byte[] data, String to, byte protocol,
            int datagramId, String from, int offset) {

        if(datagramId == Datagram.NONE) {
            Random r = new Random();
            Random r2 = new Random();
            datagramId = r.nextInt() + r2.nextInt();
        }

        // Pega next hop da routing table.
        Host nextHost = getHostInRoutingTable(to);
        if(nextHost == null) return;
        int limit = ProtocolStack.getLocalhost().getLinkMtu(nextHost.getLogicalID());
        Datagram d = new Datagram(from, to, protocol, Datagram.TTL, datagramId, data);

        // Verifica se datagrama e maior que limit (MTU)
        if(pilha_protocolos.Utilities.getObjectSize(d) > limit - LinkLayer.ADLER_SIZE) {
            List<Datagram> fragments = new ArrayList<Datagram>();
            byte[] byteData = data;
            int dataSize = limit - Datagram.MAX_HEADER_SIZE - LinkLayer.ADLER_SIZE;
            int fragOffset = offset;
            while(byteData.length > dataSize) {
                Utilities.log(Utilities.NETWORK_TAG, "Fragmento criado: %d", fragOffset);
                byte[] auxBytes = Arrays.copyOfRange(byteData, 0, dataSize);
                byteData = Arrays.copyOfRange(byteData, dataSize, byteData.length);
                fragments.add(new Datagram(from, to, protocol, Datagram.TTL, datagramId,
                       auxBytes, fragOffset, false));
                fragOffset += auxBytes.length;
            }
            // Adiciona ultimo fragmento na lista com a flag isLast setada para true
            fragments.add(new Datagram(from, to, protocol, Datagram.TTL, datagramId,
                        byteData, (fragOffset), true));

            for(Datagram fragment : fragments) {
                Utilities.log(Utilities.NETWORK_TAG, "Enviando fragmento %d para camada de enlace",
                        fragment.getOffset());
                // Send to linkLayer - envia para camada de enlace
                sendToLinkLayer(fragment, nextHost);
            }
        } else {
            // nao e necessario fragmentar datagrama
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
            // Se ip destino nao e o memso que localhost (pacote não é para este host), roteia
            if(!datagram.getDestination().equals(ProtocolStack.getLocalhost().getLogicalID())) {
                forward(datagram);
                return;
            }
            // Datagrama nao fragmentado, entrega pra camada superior
            if(datagram.isLastDatagramFragment() && datagram.getOffset() == 0) {
                deliverToUpperLayer(datagram);
            } else {
                List<Datagram> fragments = addToFragmentsList(datagram);
                Collections.sort(fragments);
                
                // Verifica se lista de fragmentos esta completa (all fragments)
                int nextFragOffset = 0;
                int dataSize = 0;
                boolean completed = false;
                for(Datagram d : fragments) {
                    if(d.getOffset() != nextFragOffset)
                        return;
                    if(d.isLastDatagramFragment()) completed = true;
                    dataSize += d.getData().length;
                    nextFragOffset += d.getData().length;
                }
                if(completed) {
                    byte[] data = new byte[dataSize];
                    int j = 0;
                    for(Datagram d : fragments) {
                        byte[] tempData = d.getData();
                        for(int i = 0; i < tempData.length; i++, j++)
                            data[j] = tempData[i];
                    }
                    try {
                        // Cria um datagrama completo
                        datagram = new Datagram(datagram.getSource(), datagram.getDestination(),
                                datagram.getUpperLayerProtocol(), datagram.getTTL(), datagram.getDatagramId(),
                                data);
                        deliverToUpperLayer(datagram);
                        datagramFragments.remove(datagram.getDatagramId());
                        fragments = null;
                    } catch(Exception ex) {
                        Utilities.logException(ex);
                    }
                }
            }

        } catch(Exception ex) {
            Utilities.logException(ex);
        }
    }

    private List<Datagram> addToFragmentsList(Datagram datagram) {
        ArrayList<Datagram> fragments = datagramFragments.get(datagram.getDatagramId());
        if(fragments == null) {
            fragments = new ArrayList<Datagram>();
            datagramFragments.put(datagram.getDatagramId(), fragments);
        }
        // Ignora datagramas duplicados
        if(!fragments.contains(datagram))
            fragments.add(datagram);

        return fragments;
    }

    private void forward(Datagram d) {
        try {
            d.decrementTTL();
            // Repassa o datagrama mantendo ID e IP de origem (age como roteador)
            // (o host final deve lidar com a fragmentacao)
            send(d.getData(), d.getDestination(), d.getUpperLayerProtocol(), 
                    d.getDatagramId(), d.getSource(), d.getOffset());
        } catch(TTLException ex) {
            // TTL 0, o pacote sera descartado
            Utilities.log(Utilities.NETWORK_TAG, "TTL = 0, descartando pacote");
        }
    }

    private void deliverToUpperLayer(Datagram datagram) {
        if(datagram == null) return;

        switch(datagram.getUpperLayerProtocol()) {
            case ProtocolStack.TRASNPORT_PROTOCOL_RDT:
                Segment seg = (Segment) Utilities.toObject(datagram.getData());
                TransportLayer.getInstance().receive(seg, datagram.getSource());
                break;
            case ProtocolStack.TRASNPORT_PROTOCOL_UDT:
                Utilities.log(Utilities.NETWORK_TAG, "Mensagem UDT recebida");
                break;
            case ProtocolStack.ROUTING_ALGORITHM:
                Utilities.log(Utilities.NETWORK_TAG, "Vetor de distancia recebido");
                ArrayList<DistanceVector> distanceVector =
                        (ArrayList<DistanceVector>)Utilities.toObject(datagram.getData());
                routing.recalculate(distanceVector, datagram.getSource());
                break;
            case ProtocolStack.ICMP_REQUEST:
                // Manda ICMP replay
                Utilities.log(Utilities.NETWORK_TAG, "Mensagem ICMP request recebida, enviando replay...");
                send(new String("icmp replay").getBytes(), datagram.getSource(), ProtocolStack.ICMP_REPLAY);
                break;
            case ProtocolStack.ICMP_REPLAY:
                // Ping respondido com sucesso.
                Utilities.log(Utilities.NETWORK_TAG, "Mensagem ICMP replay recebida");
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
     * Adiciona host na routing table
     * @param hd Destination  host (final)
     * @param nh Next hop
     * @param hops Number of hops to hd
     * @return Se foi inserido com sucesso
     */
    public boolean setHostInRoutingTable(Host hd, Host nh, int hops)
    {
        Map<String, NextHost> m = 
                Collections.synchronizedMap(routingTable);

        NextHost nextHost = new NextHost(hd, hops);

        if(hops > HOP_LIMIT) return false;

        // Se o vizinho que enviou o pacote estiver na tabela, reinicie seu timestap
        if(m.containsKey(nh.getLogicalID())) {
            m.get(nh.getLogicalID()).startTimestamp();
            // Numero de hops de um no vizinho e 1 (menor numero de hops possivel)
            m.get(nh.getLogicalID()).setHops(1);
        } else {
            // Se o vizinho nao estiver na tabela, adicione-o
            m.put(nh.getLogicalID(), new NextHost(nh, 1));
        }

        // A entrada é atualizada caso o caminho apresentado seja menor ou o NextHost seja o mesmo da tabela
        // se nao houver entrada, cria
        // nao adiciona entrada para localhost
        if(((!m.containsKey(hd.getLogicalID())) || isShorter(hd.getLogicalID(), nextHost)
                || m.get(hd.getLogicalID()).getHost().equals(nh))
                && (!ProtocolStack.getLocalhost().getLogicalID().equals(hd.getLogicalID())) ) {
            // TODO arrumar atualizacao da tabela quando uma rota do mesmo NextHost e recebida
            m.put(hd.getLogicalID(), nextHost);
            return true;
        }
        return false;
    }

    /**
     * Roda o algoritmo de roteamento
     */
    public void run() {

        while(true)
        {
            try {
                routing.start();
                // loga a routing table
                Utilities.log(Utilities.NETWORK_TAG, "%s\n %s\n", routingTable, ProtocolStack.getLocalhost());
                
                Thread.sleep(FREEZE_TIME);

            } catch (InterruptedException ex) {
                Utilities.logException(ex);
            }
        }
    }

    private class NextHost implements Serializable {
        public static final int TIMESTAMP = 3;

        private Host host;
        private int hops;
        private int timestamp;


        public NextHost(Host h, int hp) {
            host = h;
            hops = hp;
            timestamp = TIMESTAMP;
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

        public int decTimestamp() {
            return --timestamp;
        }

        public int startTimestamp() {
            timestamp = TIMESTAMP;
            return timestamp;
        }

        @Override
        public String toString() {
            return "<host: " + host + " hopes: " + hops + " timestamp: " + timestamp + ">";
        }
    }

    private class Routing implements Serializable
    {        
        public void start()
        {
                synchronized(lock)
                {
                    // Recupera tabela de roteamento sincronizada
                    Map<String, NextHost> routes =
                            Collections.synchronizedMap(routingTable);
                    ArrayList<DistanceVector> distanceVector = new ArrayList<DistanceVector>();
                    // Decrementa timestamp de todos as entradas da tabela
                    // e limpa as entradas que nao estao mais ligadas

                    Set s = Collections.synchronizedSet(routes.keySet());
                    ArrayList<String> toRemove = new ArrayList<String>();
                    synchronized(s) {   // Evita que duas threads iterem por este set ao mesmo tempo!
                        Iterator<String> it = s.iterator();
                        while(it.hasNext()) {
                            String key = it.next();
                            if(routes.get(key).decTimestamp() < 0) {
                                // Remove entrada com timestamp negativo da tabela (parte 2 da POG)
                                // Do modo mais filho da puta eu acabei descobrindo que nao se pode
                                // remover um elemento de uma collection dentro de um loop de um iterator
                                // sincronizado.
                                // Para resolver isso, a gambiarra e: salva todos os keys que devem ser removidos
                                // e remove fora do loop, fora do bloco sincronizado (mas usando a tabela sincronizada)
                                toRemove.add(key);
                            } else {
                                // Cria uma entrada para cada rota na tabela para publicar.
                                NextHost nh = routes.get(key);
                                if(nh != null) {
                                    distanceVector.add(new DistanceVector(key, nh.getHops()));
                                }
                            }
                        }
                        // Segunda parte da gambiarra! Infelizmente tera que ter outro loop =/
                        // remove da tabela de roteamento tdos os hosts que estao em toRemove
                        for(String removeHost : toRemove) {
                            routes.remove(removeHost);
                        }
                    }
                    //PROPAGAR
                    sendDistanceVector(distanceVector);
              }
        }

        public void recalculate(ArrayList<DistanceVector> array, String source)
        {
            synchronized(lock)
            {
                for(int i = 0; i < array.size(); i++)
                {
                    DistanceVector d = array.get(i);
                    Host hd = NetworkTopology.getInstance().getHost(d.getDestination());
                    Host nh = NetworkTopology.getInstance().getHost(source);

                    setHostInRoutingTable(hd, nh, d.getHops() + 1);
                }
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
