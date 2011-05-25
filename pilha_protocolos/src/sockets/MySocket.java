/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package sockets;

import java.util.Random;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import pdu.Segment;
import pilha_protocolos.Utilities;
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
    private ArrayBlockingQueue<Segment> acceptQueue; //Segmentos usados  SOMENTE para ACCEPT

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
        acceptQueue = new ArrayBlockingQueue<Segment>(2);

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
     * Recupera dados recebidos pela conexao se estiverem disponiveis.
     * Se nao houverem dados disponiveis, aguarda ate receber
     *
     * @return byte[] Dados recebidos da conexao
     */
    public byte[] recieve() {
        return dequeueData().getData();
    }

    /*
     *
     */
    public void accept(){
        Segment s = null;
        try {
            s = acceptQueue.take();
        } catch (InterruptedException ex) {
            Utilities.logException(ex);
        }finally{
            Utilities.log("no_filter", "Segmentos na fila accept:%d", acceptQueue.size());

        }
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
     * Adiciona um segmento sinlizando um accept no socket
     * @param s Segmento a ser inserido no fim da fila
     *
     * @return true se o segmento foi adicionado na fila
     */
    public boolean enqueueAcceptData(Segment s){
        return acceptQueue.offer(s);
    }



    /**
     * Retorna o primeiro segmento da fila
     * Se a fila estiver vazia, a thread e bloqueada ate que algum dado
     * fique disponivel
     *
     * @return Primeiro segmento da fila
     */
    private Segment dequeueData(){
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
