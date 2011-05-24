/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pdu;

import java.io.Serializable;
import java.util.Arrays;

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

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }
        if(getClass() != obj.getClass()) {
            return false;
        }
        final Segment other = (Segment) obj;
        if(this.sourcePort != other.sourcePort) {
            return false;
        }
        if(this.destPort != other.destPort) {
            return false;
        }
        if(this.seqNumber != other.seqNumber) {
            return false;
        }
        if(this.ackNum != other.ackNum) {
            return false;
        }
        if(!Arrays.equals(this.data, other.data)) {
            return false;
        }
        if(this.windowSize != other.windowSize) {
            return false;
        }
        if(this.protocol != other.protocol) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 79 * hash + this.sourcePort;
        hash = 79 * hash + this.destPort;
        hash = 79 * hash + this.seqNumber;
        hash = 79 * hash + this.ackNum;
        hash = 79 * hash + Arrays.hashCode(this.data);
        hash = 79 * hash + this.windowSize;
        hash = 79 * hash + this.protocol;
        return hash;
    }
}
