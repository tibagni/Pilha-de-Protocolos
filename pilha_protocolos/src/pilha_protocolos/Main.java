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
        

        if(args.length == 0 || args.length > 2 || args.length == 1)
        {
            System.err.printf("First argument is the location of cnf file, second the logical ID of the host!\n\n");
            System.exit(1);
        }
        /*
         * Dont confuse with java.util.Stack!
         */
        ProtocolStack s = new ProtocolStack(args[0],args[1]);

        s.send();


    }

}
