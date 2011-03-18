/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graph;

import java.util.HashMap;

/**
 *
 * @author belitos
 */
public class Host {

    private String logicalID; // Our IP

    // Ou MAC ADDRESS
    private String IP;
    private String port;

    // Neighbours
    private HashMap<String, String> neighbours;

    public Host(String lID, String ip, String p) {
        logicalID = lID;
        IP = ip;
        port = p;

        neighbours = new HashMap<String, String>();
    }

    public String getPort() {
        return port;
    }

    public String getIP() {
        return IP;
    }

    public String getLogicalID() {
        return logicalID;
    }


    public void addNeighbour(String hostId, String mtu) {
        neighbours.put(hostId, mtu);
    }

    public boolean iNeighbour(String hostId) {
        if(neighbours.containsKey(hostId)) {
            return true;
        }
        return false;
    }

    public String getLinkMtu(String hostId) {
        return neighbours.get(hostId);
    }

    @Override
    public boolean equals(Object o) {
        Host n = (Host) o;

        if (this.getIP().equals(n.getIP()) && this.getLogicalID().equals(n.getLogicalID())
                && this.getPort().equals(n.getPort())) {
            return true;
        }

        return false;
    }

}
