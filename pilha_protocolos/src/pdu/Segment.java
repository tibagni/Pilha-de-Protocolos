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

    public Segment(){

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

}
