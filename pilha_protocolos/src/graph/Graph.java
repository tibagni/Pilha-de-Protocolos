/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package graph;

/**
 *
 * @author belitos
 */
public class Graph {


    public Graph()
    {

    }


    public void addNode(Node n)
    {
        System.out.printf("%s\t%s\t%s\n",n.getLogicalID(),n.getIP(),n.getPort());

    }

    public void addConnection(String n1, String n2, String MTU)
    {
        System.out.printf("Oi:%s\t%s\t%s\n",n1,n2,MTU);

    }

}
