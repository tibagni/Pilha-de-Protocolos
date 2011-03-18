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

}
