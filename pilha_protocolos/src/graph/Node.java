/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package graph;

/**
 *
 * @author belitos
 */
public class Node {

    private String logicalID;
    private String IP;
    private String port;


    public Node(String lID,String ip,String p)
    {
        logicalID = lID;
        IP = ip;
        port = p;
    }

    public String getPort()
    {
        return port;
    }
    public String getIP()
    {
        return IP;
    }
    public String getLogicalID()
    {
        return logicalID;
    }

    @Override
    public boolean equals(Object o)
    {
        Node n = (Node) o;

        if(this.getIP().equals(n.getIP()) && this.getLogicalID().equals(n.getLogicalID()) && this.getPort().equals(n.getPort()))
            return true;

        return false;
    }

}
