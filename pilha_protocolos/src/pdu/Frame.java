/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pdu;


import graph.MAC;
import java.io.Serializable;

/**
 *
 * @author tiago
 */
public class Frame implements Serializable {

    private String st;
    private int checksum;
    private int type;
    private MAC sender;
    private MAC dest;

    public Frame()
    {

        st = "Sou bom";
    }
    
    public Frame(int t, int c, MAC s, MAC d)
    {
        checksum = c;
        type = t;
        st = "testando";
        sender = new MAC(s.getIP(), s.getPort());
        dest  = new MAC(d.getIP(), d.getPort());
        
    }

    public int getLength()
    {
        return checksum;
    }

    public int getType()
    {
        return type;
    }

    public String getS()
    {
        return st;
    }

    public MAC getSender()
    {
        return sender;
    }

    public MAC getDestination()
    {
        return dest;
    }

    public void setLength(int c)
    {
        checksum = c;
    }

}
