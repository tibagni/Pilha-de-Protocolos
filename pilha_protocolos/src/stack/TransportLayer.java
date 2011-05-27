/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    private static TransportLayer transportLayer = null;

    private TransportLayer() {
        sockets = new HashMap<Integer, SocketWrapper>();

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
    private HashMap<Integer, SocketWrapper> synchronizedSockets() {
        return (HashMap<Integer, SocketWrapper>)
                Collections.synchronizedMap(sockets);
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
        SocketWrapper registeredSocket = synchronizedSockets().put(socket.getLocalPort(),
                new SocketWrapper(socket));
        if(registeredSocket == null) {
            //Nao foi possivel registrar o socket...
            // Fecha a conexao RDT (TCP)
            closeRDTConnection();
            return false;
        }
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
                                             0, //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);

        // Seta a flag SYN para o estabelecimento da conexao
        connectSegment.setSYN(true);
        // O valor no campo ack e valido
        connectSegment.setAckValid(true);
        // Envia pacote para requisitar conexao
        send(socket.getLocalPort(), connectSegment);

        return true;
    }

    private void closeRDTConnection() {
        // TODO encerra a conexao TCP
    }

    public void disconnect(int portMap) {
        //Remove o socket da lista...
        synchronizedSockets().remove(portMap);
        //Encerra a conexao RDT (TCP)
        closeRDTConnection();
    }

    /**
     * Envia um segmento pelo socket mapeado na porta portMap
     *
     * @param portMap porta na qual o socket esta mapeado
     * @param segment Segmento a ser enviado
     * @return numero de bytes enviados, -1 em caso de falha
     */
    public int send(int portMap, Segment segment) {
        if(!synchronizedSockets().containsKey(portMap)) return -1;

        MySocket socket = (MySocket) synchronizedSockets().get(portMap).socket;
        segment.setSeqNumber(socket.getSeqNumber());

        //TODO dividir o segmento para enviar

        // Loop enviando todos os segmentos
        return sendRDT(segment);
    }

    public boolean registerServerSocket(MySocket s){
        if(server == null){
            server = s;
            return true;
        } else {
            return false;
        }
    }

    private int sendRDT(Segment seg) {
        SocketWrapper sw = synchronizedSockets().get(seg.getSourcePort());
        // Adiciona na lista de segmentos a serem enviados
        sw.synchronizedToSendSegments().add(seg);
        if (sw.getReceiversWindowSize() < Utilities.getObjectSize(seg)) {
            //Controle de fluxo!!! Nao envia se a janela do receptor estiver
            //cheia, segmento fica armazenado em toSend!

            //TODO inicia uma thread que fica verificando o tamanho da janela
            // a cada instante de tempo e envia os segmentos pendentes quando der
            return 0;
        }
        // Inicia o timer relacionado ao segmento
        sw.startTimer(seg);
        //Agora o segmento pode ser retirado da fila de pendentes (toSend)
        sw.synchronizedToSendSegments().remove(seg);

        //Envia segmento para a camada de rede
        byte[] segmentBytes = Utilities.toByteArray(seg);
        NetworkLayer.getInstance().send(segmentBytes, sw.socket.getRemoteAddress(),
                ProtocolStack.TRASNPORT_PROTOCOL_RDT);
        
        return segmentBytes.length;
    }

    private void deliverToUpperLayer() {

    }

    public void receive(Segment segment, String fromAddr) {
        if(segment.getSYN()) {
            //Estabelecimento de conexao
            if(segment.getAck() == 0 && server != null) {
                //Requisicao de conexao, enviar ACK
                int ackNum = segment.getSeqNumber() + 1;
                // Estabelecer conexao deste lado (servidor)
                synchronizedSockets().get(segment.getDestPort()).connected = true;
                //Enviar SYNACK (flag SYN continua ativada)
                Segment s = new Segment(segment.getDestPort(),
                                             segment.getSourcePort(),
                                             segment.getDestPort(),
                                             0, // O primeiro ack e zero
                                             new byte[1], //manda 1 byte
                                             0, //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);
                s.setAckNum(ackNum);
                s.setAckValid(true);
                SegmentWrapper sw = new SegmentWrapper(segment, fromAddr);
                //Coloca requisicao de conexao na fila do accept
                server.enqueueAcceptData(sw);
                send(segment.getSourcePort(),s);


            } else if(segment.getAck() == segment.getSeqNumber() + 1) {
                //Ack de pedido de conexao, conexao estabelecida
                synchronizedSockets().get(segment.getDestPort()).connected = true;
                
                //enviar ultimo ACK (flag SYN desativada)
                int ackNum = segment.getSeqNumber() + 1;

                Segment s = new Segment(segment.getDestPort(),
                                             segment.getSourcePort(),
                                             segment.getDestPort(),
                                             ackNum, // O primeiro ack e zero
                                             new byte[1], //manda 1 byte
                                             0, //window size
                                             ProtocolStack.TRASNPORT_PROTOCOL_RDT);
                s.setAckValid(true);
                send(segment.getSourcePort(),s);
            } else {
                //fodeu total
                Utilities.printError("3-way handshake - Estado invalido");
            }
        } else {
            if(segment.getData().length == 1) {
                if(segment.isAckValid()){
                    /*ignora, somente ack*/
                } else {
                    // TODO envia o tamanho da janela de cong.
                }
            }
            //TODO deliver to upper layer na camada de transporte
            deliverToUpperLayer();
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
        MySocket socket;
        boolean connected = false;
        int receiversWindowSize;
        private ArrayList<Segment> toSend;
        private ArrayList<Segment> sentSegments;
        private ArrayList<Timer> segTimers;

        

        SocketWrapper(MySocket socket) {
            sentSegments = new ArrayList<Segment>();
            toSend = new ArrayList<Segment>();
            segTimers = new ArrayList<Timer>();
        }

        public int getReceiversWindowSize() {
            return receiversWindowSize;
        }

        public void setReceiversWindowSize(int newSize) {
            receiversWindowSize = newSize;
        }

        public void startTimer(Segment s){
            Timer t = new Timer();
            SegmentTask task = new SegmentTask(s);

            t.schedule(task,TIMEOUT,TIMEOUT+100);
            synchronizedTimers().add(t);
            synchronizedSentSegments().add(s);

        }

        public ArrayList<Segment> synchronizedToSendSegments(){
            return (ArrayList<Segment>)Collections.synchronizedList(toSend);
        }

        public ArrayList<Segment> synchronizedSentSegments(){
            return (ArrayList<Segment>)Collections.synchronizedList(sentSegments);
        }
        public ArrayList<Timer> synchronizedTimers(){
            return (ArrayList<Timer>)Collections.synchronizedList(segTimers);
        }

        public void stopTimer(Segment s){
            int index = synchronizedSentSegments().indexOf(s);

            if(index != -1){
                synchronizedSentSegments().remove(index); // remove segmento
                synchronizedTimers().remove(index).cancel(); //para timer
            }
        }

        private class SegmentTask extends TimerTask{
            private int times; //quantas vezes foi enviado
            private Segment seg;
            private final static int STOP_SEND = 3;

            public SegmentTask(Segment s){
                seg = s;
                times  = 0;
            }

            @Override
            public void run() {
                if(times < STOP_SEND)
                    TransportLayer.getInstance().send(socket.getLocalPort(), seg);

                times++;
            }

            public Segment getSegment(){
                return seg;
            }
        }
    }
}
