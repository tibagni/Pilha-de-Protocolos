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
public class Frame implements Serializable {

    private byte type;
    byte[] data;
    
    public Frame(byte t, byte[] data)
    {
        type = t;
        this.data = data;
    }

    public byte getType()
    {
        return type;
    }

    public byte[] getData() {
        return data;
    }
}
