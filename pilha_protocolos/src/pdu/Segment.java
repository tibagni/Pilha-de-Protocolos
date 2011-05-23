/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pdu;

import java.io.Serializable;

/**
 *
 * @author tiago
 */
public class Segment implements Serializable {

    private int sourcePort;
    private int destPort;
    private int seqNumber;
    private int ackNum;
    private byte[] data;
    private int windowSize;
    private int protocol;
    
    // Flags
    private boolean syn;
    private boolean fin;

    public Segment(int srcPort, int dstPort, int seq, int ack, byte[] data, 
            int windowSze, int proto) {
        sourcePort = srcPort;
        destPort = dstPort;
        seqNumber = seq;
        ackNum = ack;
        this.data = data;
        windowSize = windowSze;
        protocol = proto;

    }
    public Segment(int srcPort, int dstPort, byte[] data, int proto) {
        this(srcPort, dstPort, 0, 0, data, 0, proto);

    }
    
    public int getProtocol(){
        return protocol;
    }
    public int getWindowSize(){
        return windowSize;
    }
    public int getSourcePort(){
        return sourcePort;
    }
    public int getDestPort(){
        return destPort;
    }
    public int getSeqNumber(){
        return seqNumber;
    }
    public int getAck(){
        return ackNum;
    }
    public byte[] getData(){
        return data;
    }

    public void setAckNum(int ackNum) {
        this.ackNum = ackNum;
    }

    public void setSeqNumber(int seqNumber) {
        this.seqNumber = seqNumber;
    }

    public void setWindowSize(int windowSize) {
        this.windowSize = windowSize;
    }

    /**
     * Seta o valor da flag SYN - usada no estabelecimento da conexao
     * No momento da conexao (3-way handshake) e somente no momento da conexao
     * esta flag DEVE ser true
     *
     * @param s valor booleano para a flag
     */
    public void setSYN(boolean s) {
        syn = s;
    }

    /**
     * Seta o valor da flag FIN - usada no encerramento da conexao
     * No momento do encerramento da conxao e somente no encerramento esta flag
     * DEVE ser true
     *
     * @param f valor booleano para a flag
     */
    public void setFIN(boolean f) {
        fin = f;
    }

    public boolean getSYN() {
        return syn;
    }

    public boolean getFIN() {
        return fin;
    }

}
