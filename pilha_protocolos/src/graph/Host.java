/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package graph;


import java.io.Serializable;
import java.util.HashMap;

/**
 *
 * @author belitos
 */
public class Host implements Serializable {

    private String logicalID; // Our IP
    private MAC mac;

    // Neighbours <LogicalId, Connection>
    private HashMap<String, Connection> neighbours;

    public Host(String lID, String ip, String p) {
        logicalID = lID;
        mac = new MAC(ip,p);

        neighbours = new HashMap<String, Connection>();
    }

    public MAC getMAC()
    {
        return mac;
    }

    public String getLogicalID() {
        return logicalID;
    }


    public void addNeighbour(Host host, int mtu) {
        neighbours.put(host.getLogicalID(), new Connection(host, mtu));
    }

    public boolean isNeighbour(String hostId) {
        if(neighbours.containsKey(hostId)) {
            return true;
        }
        return false;
    }

    public HashMap<String,Connection> getNeighbour()
    {
        return neighbours;
    }

    public int getLinkMtu(String dst) {
        return neighbours.get(dst).getLinkMtu();
    }

    @Override
    public boolean equals(Object o) {
        if(o instanceof Host) {
            Host n = (Host) o;

            if(this.getMAC().getIP().equals(n.getMAC().getIP()) && this.getLogicalID().equals(n.getLogicalID())
                    && this.getMAC().getPort().equals(n.getMAC().getPort())) {
                return true;
            }
        }

        return false;
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 97 * hash + (this.logicalID != null ? this.logicalID.hashCode() : 0);
        hash = 97 * hash + (this.mac != null ? this.mac.hashCode() : 0);
        hash = 97 * hash + (this.neighbours != null ? this.neighbours.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return getLogicalID();
    }

    public class Connection {
        private Host host;
        private int mtu;


        public Connection(Host h, int m) {
            host = h;
            mtu = m;
        }

        public Host getHost() {
            return host;
        }

        public int getLinkMtu() {
            return mtu;
        }

        @Override
        public String toString()
        {
            return host.toString() + " - " + mtu;
        }
    }
}
