/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pdu;


import graph.MAC;
import java.io.Serializable;
import stack.ProtocolStack;

/**
 *
 * @author tiago
 */
public class Frame implements Serializable {

    private String st;
    private Long checksum;
    private int type;
    private MAC sender;
    private MAC dest;

    public Frame()
    {
        checksum = null;
        st = "Message sent from host:"+ProtocolStack.getLocalhost().getLogicalID();
    }
    
    public Frame(int t, Long c, MAC s, MAC d)
    {
        checksum = c;
        type = t;
        st = "testando";
        sender = new MAC(s.getIP(), s.getPort());
        dest  = new MAC(d.getIP(), d.getPort());
        
    }

    public Long getCheckSum()
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

    public void setCheckSum(Long c)
    {
        checksum = c;
    }

}
