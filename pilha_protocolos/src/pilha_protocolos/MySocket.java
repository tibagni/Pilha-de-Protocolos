/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pilha_protocolos;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import pdu.Segment;
import stack.ProtocolStack;
import stack.TransportLayer;


/**
 * Substitui a classe Socket para utilizar a pilha de protocolos
 * do projeto.
 * @author tiago
 */
public class MySocket {
    private String localAddress;
    private int localPort;

    private String remoteAddress;
    private int remotePort;

    private ArrayBlockingQueue<Segment> dataQueue;

    private int sequenceNumber;

    // Capacidade da fila de dados
    private static final int QUEUE_CAPACITY = 10;

    private static final int SEQ_LIMIT = 65000;

    public MySocket(String localAddr, int localPort, String remoteAddr, int remotePort) {
        localAddress = localAddr;
        this.localPort = localPort;
        remoteAddress = remoteAddr;
        this.remotePort = remotePort;

        dataQueue = new ArrayBlockingQueue<Segment>(QUEUE_CAPACITY);

        //Inicia o numero de sequencia...
        sequenceNumber = new Random().nextInt(SEQ_LIMIT);
    }

    /**
     * Envia bytes por uma conexao TCP pelo socket
     *
     * @param data Vetor de bytes contendo os dados a serem enviados
     * @return numero de bytes enviados
     */
    public int send(byte[] data) {
        Segment segment = new Segment(localPort, remotePort, data,
                ProtocolStack.TRASNPORT_PROTOCOL_RDT);

        // Manda para a camada de transporte
        return TransportLayer.getInstance().send(localPort, segment);
    }

    /**
     * Close socket
     */
    public void close() {
        TransportLayer.getInstance().disconnect(localPort);
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public int getLocalPort() {
        return localPort;
    }

    public String getRemoteAddress() {
        return remoteAddress;
    }

    public int getRemotePort() {
        return remotePort;
    }

    public int getSeqNumber() {
        return sequenceNumber;
    }

    public void setSeqNumber(int newSeq) {
        sequenceNumber = newSeq;
    }

    /**
     * coloca dados no fim da fila do socket
     * @param s Segmento a ser inserido no fim da fila
     * 
     * @return true se o segmento foi adicionado na fila
     */
    public boolean enqueueData(Segment s){
        return dataQueue.offer(s);
    }

    /**
     * Retorna o primeiro segmento da fila
     * Se a fila estiver vazia, a thread e bloqueada ate que algum dado
     * fique disponivel
     *
     * @return Primeiro segmento da fila
     */
    public Segment dequeueData(){
        Segment s = null;

        try {
            s = dataQueue.take();
        } catch (InterruptedException ex) {
            Utilities.logException(ex);
        } finally {
            return s;
        }
    }

 
}
