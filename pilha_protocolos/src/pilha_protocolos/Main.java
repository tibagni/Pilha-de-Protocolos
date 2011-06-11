/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pilha_protocolos;



import application.App;
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

        if(args.length == 0 || args.length > 2 || args.length == 1)
        {
            Utilities.printError("First argument is the location of cnf file, " +
                    "second the logical ID of the host!\n\n");
            System.exit(1);
        }
        /*
         * Dont confuse with java.util.Stack!
         */

        ProtocolStack s = new ProtocolStack(args[0],args[1]);
//        if(args[1].equals("1")){/*
//            MySocket socket = new MySocket(ProtocolStack.getLocalhost().getLogicalID(),8000);
//            socket.bindServer();
//            socket.accept();*/
//            FileTransfer tran = new FileTransfer(8000);
//            tran.receive();
//            tran.receive();
//            System.out.println("conectou com o server");
//        }else{/*
//            MySocket socket = new MySocket(ProtocolStack.getLocalhost().getLogicalID(),123,
//                    "1", 8000);
//            socket.bind();
//            socket.send("Sou bom, e ta funfando".getBytes());*/
//            FileTransfer tranCli = new FileTransfer("1",8000,123);
//            tranCli.prepareSentMahNizzle("vai.txt");
//            try {
//                Thread.sleep(1000);
//            } catch (InterruptedException ex) {
//                Logger.getLogger(Main.class.getName()).log(Level.SEVERE, null, ex);
//            }
//            tranCli.send("vai.txt");
//            System.out.println("cliente iniciou a conexao");
//        }

        App app = new App();
        app.startApp();
        //s.send();


    }

}
