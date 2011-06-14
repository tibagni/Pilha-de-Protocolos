/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import pdu.Segment;
import sockets.MySocket;
import pilha_protocolos.Utilities;

/**
 * Comentarios:
 * Como so tem um protocolo de transporte nao tem problema deixar os metodos aqui mesmo
 * mas se precisar de mais protocolos, ou se no futuro poderao ser adicionados outros
 * o ideal seria criar uma classe pra cada protocolo com uma interface comum e usar composicao
 * aqui para os protocolos, assim esta classe nao precisaria ser alterada mais
 * se surgir um novo protocolo e so criar uma classe pra ele com a interface comum!
 */

/**
 *
 * @author tiago
 */
public class TransportLayer {
    private HashMap<Integer, SocketWrapper> sockets;
    private MySocket server = null;
    private boolean startingConnection;


    private static TransportLayer transportLayer = null;

    private TransportLayer() {
        sockets = new HashMap<Integer, SocketWrapper>();
        startingConnection = false;
    }

    public static TransportLayer getInstance() {
        if(transportLayer == null) {
            transportLayer = new TransportLayer();
        }
        return transportLayer;
    }

    /**
     * Utilizar SEMPRE este metodo ao manipular o Map sockets
     * NAO manipular sockets diretamente
     *
     * @return HashMap de sockets seguro (Thread-safe)
     */
    private Map<Integer, SocketWrapper> synchronizedSockets() {
        return Collections.synchronizedMap(sockets);
    }

    /**
     * Estabelece uma conexao TCP baseado nos dados do socket fornecido
     * e, em caso de sucesso, registra o socket
     *
     * @param socket Socket para estabelecer a conexao
     * @return resultado da operacao
     */
    public synchronized boolean connect(MySocket socket) {
        if(synchronizedSockets().containsKey(socket.getLocalPort())) {
            //Nao pode haver dois sockets registrados e escutando a mesma porta!!
            return false;
        }
        //Rgister socket
        synchronizedSockets().put(socket.getLocalPort(),
                new SocketWrapper(socket));
        
        //Comeca o 3-way handshake
        return connectRDT(socket);
    }

    /**
     * three-way handshake
     */
    private boolean connectRDT(MySocket socket) {
        Segment connectSegment = new Segment(socket.getLocalPort(),
                                             socket.getRemotePort(),
                                             socket.getSeqNumber(),
                                             0, // O primeiro ack e zero
                                             new byte[1], //manda 1 byte
                                             socket.getWindowSize(), //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);

        // Seta a flag SYN para o estabelecimento da conexao
        connectSegment.setSYN(true);
        // O valor no campo ack e valido
        connectSegment.setAckValid(true);
        // Envia pacote para requisitar conexao
        startingConnection = true;
        send(socket.getLocalPort(), connectSegment);
        startingConnection = false;

        return true;
    }

    private void closeRDTConnection(int portMap,MySocket socket) {
        // TODO encerra a conexao TCP
        Segment s = new Segment(socket.getLocalPort(),
                                             socket.getRemotePort(),
                                             socket.getSeqNumber(),
                                             0, // O primeiro ack e zero
                                             new byte[1], //manda 1 byte
                                             server.getWindowSize(), //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);
        s.setFIN(true);

        send(portMap,s);
    }

    public void disconnect(int portMap) {
        closeRDTConnection(portMap,synchronizedSockets().get(portMap).socket);
        //Remove o socket da lista...
        synchronizedSockets().remove(portMap);
        //Encerra a conexao RDT (TCP)
        
    }
    public void registerSocket(MySocket socket){
        synchronizedSockets().put(socket.getLocalPort(), new SocketWrapper(socket));
    }

    /**
     * Envia um segmento pelo socket mapeado na porta portMap
     *
     * @param portMap porta na qual o socket esta mapeado
     * @param segment Segmento a ser enviado
     * @return numero de bytes enviados, -1 em caso de falha
     */
    public synchronized int send(int portMap, Segment segment) {
        int retval;
        if(!synchronizedSockets().containsKey(portMap)) return -1;
        
        MySocket socket = (MySocket) synchronizedSockets().get(portMap).socket;
        if(segment.getSeqNumber() == -1){
            segment.setSeqNumber(socket.getSeqNumber());
        }
        segment.setWindowSize(socket.getWindowSize());

        retval = sendRDT(segment);
        Utilities.log(Utilities.TRANSPORT_TAG,"SEND:%d-%d-%d\n\n\n\n",socket.getSeqNumber(),segment.getSeqNumber(),segment.getData().length);
        if(socket.getSeqNumber() < segment.getSeqNumber() + segment.getData().length){ //Timers cant upgrade the seq number
            socket.setSeqNumber(socket.getSeqNumber() + segment.getData().length);
        }
        return retval;
    }

    public boolean registerServerSocket(MySocket s){
        if(server == null){
            server = s;
            this.synchronizedSockets().put(s.getLocalPort(), new SocketWrapper(server));
            return true;
        } else {
            return false;
        }
    }

    private int sendRDT(Segment seg) {
        SocketWrapper sw = synchronizedSockets().get(seg.getSourcePort());
        // Adiciona na lista de segmentos a serem enviados
        
        if (sw.getReceiversWindowSize() < 1 && seg.getData().length > 1) {
            //Envia segmentos de 1 byte para consultar tamanho da janela do receptor

            //Controle de fluxo!!! Nao envia se a janela do receptor estiver
            //cheia, segmento fica armazenado em toSend!

            // inicia uma thread que fica verificando o tamanho da janela
            // a cada instante de tempo e envia os segmentos pendentes quando der
            if(!sw.synchronizedToSendSegments().contains(seg)){
                sw.synchronizedToSendSegments().add(seg);
            }
            sw.startCheckWindowSize();
            return 0;
        }

        if(!seg.isAckValid() || startingConnection == true){
            // Inicia o timer relacionado ao segmento
            sw.startTimer(seg);
        }
        //Agora o segmento pode ser retirado da fila de pendentes (toSend)
        if(sw.synchronizedToSendSegments().contains(seg)){
            sw.synchronizedToSendSegments().remove(seg);
        }

        //Envia segmento para a camada de rede
        Utilities.log(Utilities.TRANSPORT_TAG, seg.toString());
        byte[] segmentBytes = Utilities.toByteArray(seg);
        NetworkLayer.getInstance().send(segmentBytes, sw.socket.getRemoteAddress(),
                ProtocolStack.TRASNPORT_PROTOCOL_RDT);

        
        
        return segmentBytes.length;
    }

    private void deliverToUpperLayer(Segment s) {
        this.synchronizedSockets().get(s.getDestPort()).socket.enqueueData(s);
    }

    public void receive(final Segment segment, String fromAddr) {
        Utilities.log(Utilities.TRANSPORT_TAG,"Recebeu:%s\n\n\n",segment);
        if(segment.getSYN()) {
            //Estabelecimento de conexao
            if(segment.getAck() == 0 && server != null) {
                //Requisicao de conexao, enviar ACK
                Utilities.print("recebeu syn, envia ack\n");
                int ackNum = segment.getSeqNumber() + 1;

                // Estabelecer conexao deste lado (servidor)
                synchronizedSockets().get(segment.getDestPort()).connected = true;
                //Enviar SYNACK (flag SYN continua ativada)
                Segment s = new Segment(segment.getDestPort(),
                                             segment.getSourcePort(),
                                             server.getSeqNumber(),
                                             ackNum, // O primeiro ack e zero
                                             new byte[1], //manda 1 byte
                                             server.getWindowSize(), //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);
                s.setAckNum(ackNum);
                s.setAckValid(true);
                s.setSYN(true);
                SegmentWrapper sw = new SegmentWrapper(segment, fromAddr);
                //Coloca requisicao de conexao na fila do accept
                server.enqueueAcceptData(sw);
                send(segment.getDestPort(), s);
            } else if(segment.getAck() == 
                 synchronizedSockets().get(segment.getDestPort()).socket.getSeqNumber()) {
                //Ack de pedido de conexao, conexao estabelecida
                Utilities.print("recebeu ack, conexao estabelecida\n");
               SocketWrapper sw =  synchronizedSockets().get(segment.getDestPort());
               sw.connected = true;
                
                //enviar ultimo ACK (flag SYN desativada)
                int ackNum = segment.getSeqNumber() + 1;

                Segment s = new Segment(segment.getDestPort(),
                                             segment.getSourcePort(),
                                             sw.socket.getSeqNumber(),
                                             ackNum,
                                             new byte[1], //manda 1 byte
                                             sw.socket.getWindowSize(), //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);
                s.setAckValid(true);

                send(segment.getDestPort(),s);

            } else {
                //fodeu total
                Utilities.printError("3-way handshake - Estado invalido");
            }
        } else {
            if(segment.getData().length == 1) {
                if(segment.isAckValid()){
                    /*ignora, somente ack*/
                Utilities.log(Utilities.TRANSPORT_TAG, "recebi ack!!!!!");
                } else {
                    SocketWrapper sw =  synchronizedSockets().get(segment.getDestPort());
                    int ackNum = segment.getSeqNumber() + 1;
                    //sw.socket.setSeqNumber(ackNum);
                    Segment s = new Segment(segment.getDestPort(),
                                             segment.getSourcePort(),
                                             sw.socket.getSeqNumber(),
                                             ackNum, // O primeiro ack e zero
                                             new byte[1], //manda 1 byte
                                             sw.socket.getWindowSize(), //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);
                    //Seta valor de ack valido para o receptor ignorar o pacote
                    s.setAckValid(true);
                    send(segment.getDestPort(), s);
                }
            } else {
               //

                Utilities.log(Utilities.TRANSPORT_TAG, "else porra");
                //deliver to upper layer na camada de transporte
                SocketWrapper sw =  synchronizedSockets().get(segment.getDestPort());
                if(segment.getSeqNumber() > sw.getLastRecSeqNumber() ){
                    deliverToUpperLayer(segment);
                    // Envia ack do segmento
                       // SocketWrapper sw =  synchronizedSockets().get(segment.getDestPort());
                        int ackNum = segment.getSeqNumber() + segment.getData().length;
                        //sw.socket.setSeqNumber(ackNum);
                        Segment s = new Segment(segment.getDestPort(),
                                                 segment.getSourcePort(),
                                                 sw.socket.getSeqNumber(),
                                                 ackNum, // O primeiro ack e zero
                                                 new byte[1], //manda 1 byte
                                                 sw.socket.getWindowSize(), //window size
                                                 ProtocolStack.TRASNPORT_PROTOCOL_RDT);
                        //Seta valor de ack valido para o receptor ignorar o pacote
                        s.setAckValid(true);
                        s.setSYN(false);
                        sw.setLastRecSeqNumber(segment.getSeqNumber());
                        send(segment.getDestPort(),s);
                }
            }
        }
        SocketWrapper sw = synchronizedSockets().get(segment.getDestPort());
        //Atualiza tamanho da janela do receptor
        sw.setReceiversWindowSize(segment.getWindowSize());

        if(segment.getWindowSize() > 0 && sw.synchronizedToSendSegments().size() > 0){
            sw.stopCheckWindowSize();

            for(Segment s : sw.synchronizedToSendSegments()){
                send(s.getDestPort(),s);
            }
        }
        Segment segToRemove = null;
        if(segment.isAckValid()){
           // Utilities.log(Utilities.TRANSPORT_TAG, "ack valido");
            synchronized(sw.synchronizedSentSegments()){
                Iterator<Segment> it = sw.synchronizedSentSegments().iterator();
                while(it.hasNext()){
                    Segment s = it.next();
                    //System.out.printf("%d-%d-%d-%d\n\n",s.getSeqNumber(),s.getData().length,segment.getAck(),segment.getData().length);
                    if(s.getSeqNumber() + s.getData().length == segment.getAck()){
                        segToRemove = s;
                        Utilities.log(Utilities.TRANSPORT_TAG, "removendo segmento " + s.toString());
                        break;
                    }
                }
            }
        }
        if(sw != null){
            //System.out.printf("AQUI PORRA\n\n\n\n\n");
            sw.stopTimer(segToRemove);
        }

    }

    public static class SegmentWrapper {
        public final Segment segment;
        public final String fromAddr;

        public SegmentWrapper(Segment s, String fA) {
            segment = s;
            fromAddr = fA;
        }
    }

    private class SocketWrapper {
        private static final int TIMEOUT = 4000;
        private static final int TIMEOUT_WINDOW = 500;
        MySocket socket;
        boolean connected = false;
        int receiversWindowSize = MySocket.QUEUE_CAPACITY;
        private ArrayList<Segment> toSend;
        private ArrayList<Segment> sentSegments;
        private ArrayList<Timer> segTimers;
        private Timer timerCheckWindowSize = null;
        private int lastRecSeqNumber;

        

        SocketWrapper(MySocket socket) {
            sentSegments = new ArrayList<Segment>();
            toSend = new ArrayList<Segment>();
            segTimers = new ArrayList<Timer>();
            this.socket = socket;
            lastRecSeqNumber = 0;

        }
        public int getLastRecSeqNumber(){
            return lastRecSeqNumber;
        }
        public void setLastRecSeqNumber(int l){
            lastRecSeqNumber = l;
        }
        public void startCheckWindowSize(){
            if(timerCheckWindowSize != null) return;

            timerCheckWindowSize = new Timer();

            timerCheckWindowSize.schedule(new TimerTask(){
                public void run(){

                    Segment s = new Segment(socket.getLocalPort(),
                                             socket.getRemotePort(),
                                             socket.getSeqNumber(),
                                             0,
                                             new byte[1], //manda 1 byte
                                             socket.getWindowSize(), //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);
                    send(socket.getRemotePort(),s);
                }
            }, TIMEOUT_WINDOW, TIMEOUT_WINDOW + 1);
        }
        public void stopCheckWindowSize(){
            timerCheckWindowSize.cancel();
            timerCheckWindowSize = null;
        }

        public int getReceiversWindowSize() {
            synchronized(this){
                return receiversWindowSize;
            }
        }

        public void setReceiversWindowSize(int newSize) {
            synchronized(this){
                receiversWindowSize = newSize;
            }
        }

        public void startTimer(Segment s){
         //   int index = synchronizedSentSegments().indexOf(s);

          //  if(index == -1){ //So cria um timer se o segmento e novo ( nao e retransmissao)
                Timer t = new Timer();
                SegmentTask task = new SegmentTask(s);

                t.schedule(task,TIMEOUT,TIMEOUT+100);

                synchronizedTimers().add(t);
                synchronizedSentSegments().add(s);
           // }

        }

        public List<Segment> synchronizedToSendSegments(){
            return Collections.synchronizedList(toSend);
        }

        public List<Segment> synchronizedSentSegments(){
            return Collections.synchronizedList(sentSegments);
        }
        public List<Timer> synchronizedTimers(){
            return Collections.synchronizedList(segTimers);
        }
        
        public void stopTimer(Segment s){
            int index = synchronizedSentSegments().indexOf(s);

            if(index != -1){
                synchronizedSentSegments().remove(index);
               // synchronizedSentSegments().remove(index);
                synchronizedTimers().remove(index).cancel(); //para timer

            }
        }

        private class SegmentTask extends TimerTask{
            private int times = 0; //quantas vezes foi enviado
            private Segment seg;
            private final static int STOP_SEND = 10;

            public SegmentTask(Segment s){
                seg = s;
                times = 0;
                
            }

            @Override
            public void run() {
                
                if(times < STOP_SEND){
                    TransportLayer.getInstance().send(socket.getLocalPort(), seg);

                }else if (times >= STOP_SEND) {
                    this.cancel();
                }
                times++;
            }

            public Segment getSegment(){
                return seg;
            }
        }
    }
}
