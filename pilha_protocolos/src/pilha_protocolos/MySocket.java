/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pilha_protocolos;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import pdu.Segment;
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

    // Capacidade da fila de dados
    private static final int QUEUE_CAPACITY = 10;

    public MySocket(String localAddr, int localPort, String remoteAddr, int remotePort) {
        localAddress = localAddr;
        this.localPort = localPort;
        remoteAddress = remoteAddr;
        this.remotePort = remotePort;

        dataQueue = new ArrayBlockingQueue<Segment>(QUEUE_CAPACITY);
    }

    /**
     * Envia bytes por uma conexao TCP pelo socket
     *
     * @param data Vetor de bytes contendo os dados a serem enviados
     * @return numero de bytes enviados
     */
    public int send(byte[] data) {
        Segment segment = new Segment();
        // TODO criar o segmento com os dados fornecidos

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

    public boolean insertSegment(Segment s){
        return dataQueue.offer(s);
    }

    public Segment getSegment(){
        Segment s = null;

        try {
            s = dataQueue.take();
        } catch (InterruptedException ex) {
            Logger.getLogger(MySocket.class.getName()).log(Level.SEVERE, null, ex);
        }finally{
            return s;
        }
    }

 
}
