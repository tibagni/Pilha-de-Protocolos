/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pilha_protocolos;


import stack.ProtocolStack;

/**
 *
 * @author belitos
 */
public class Main {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        // TODO store the Logical ID from command-line
        ProtocolStack.setLocalhost("1");

        /*
         * Dont confuse with java.util.Stack!
         */
        ProtocolStack s = new ProtocolStack("top.cnf");
    }

}
