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
public class Graph {

    private HashMap<String,Node> topology;


    public Graph()
    {
        topology = new HashMap<String,Node>();
    }


    public void addNode(Node n)
    {
        topology.put(n.getLogicalID(), n);

    }
    public Node getNode(String key)
    {
        return topology.get(key);
    }

    public void addConnection(String n1, String n2, String MTU)
    {
        System.out.printf("Oi:%s\t%s\t%s\t%d\n",n1,n2,MTU,topology.size());
    }

}
