/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package stack;

import graph.NetworkTopology;
import graph.Host;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import pdu.Frame;

/**
 *
 * @author belitos
 */
public class ProtocolStack {

    
    public static final int MAX_MTU_SIZE = 5000;

   
    public static final String LINK_STRING = "Enlaces";
    public static final String END_STRING = "Fim";
    private NetworkTopology graph;

    // Stores the loopback node of this host
    private static Host localhost = null;

    private ExecutorService pool;

    public ProtocolStack(String filePath,String logicalID) {
        graph = new NetworkTopology();

        readFile(filePath,logicalID);

        pool = Executors.newFixedThreadPool(1);

        pool.execute(LinkLayer.getInstance());

        





       
    }

    public void send()
    {
        Scanner s = new Scanner(System.in);
        int i = 1;
        Host h;

        while(true)
        {
            System.out.printf("--Current topology--\n");

            while((h = graph.getHost(Integer.toString(i))) != null)
            {
                System.out.printf("ID:%s\tIP:%s\tPort:%s\n\n",h.getLogicalID(),h.getMAC().getIP(),h.getMAC().getPort());
                i++;
            }
            i = 1;


            System.out.printf("What host you want to send a msg?");

        
            if(LinkLayer.getInstance().sendFrame(graph.getHost(Integer.toString(s.nextInt())),new Frame()))
                System.out.printf("Packet sent!\n\n");
            else
                System.out.printf("You are not connected to this host or MTU exceeded!\n\n");
        }
       

        //pool.shutdownNow();

        //System.exit(1);
        
    }

    public static void setLocalHost(Host h)
    {
        if(localhost == null)
            localhost = h;

    }
   

    public static Host getLocalhost() {
        return localhost;
    }

    private void readFile(String path,String logicalID) {
        Scanner in = null;
        String line;
        StringTokenizer token;
        boolean isConnection = false; //Indicates that from now on we will read links between nodes

        try {
            in = new Scanner(new File(path));
        } catch (FileNotFoundException ex) {
            System.err.printf("File not found!\n\n");
            System.exit(1);

        }

        in.nextLine(); //Get rid of the "Nos" line

        while (in.hasNext()) {
            line = in.nextLine().trim();

            if (line.equals("")) //Blank line
            {
                continue;
            } else if (line.equals(END_STRING)) {
                break;
            } else if (line.equals(LINK_STRING)) {
                isConnection = true;
                ProtocolStack.setLocalHost(graph.getHost(logicalID));
                continue;
            }

            line = replaceCharacters(line);

            token = new StringTokenizer(line, " ");


            if (!isConnection) //Adding nodes
            {
                graph.addHost(new Host(token.nextToken(), token.nextToken(), token.nextToken()));
            } else {
                graph.addConnection(graph.getHost(token.nextToken()),
                        graph.getHost(token.nextToken()), Integer.parseInt(token.nextToken()));
            }
        }

        if (in != null) {
            in.close();
        }

    }

    private String replaceCharacters(String line) {

        line = line.replace(":", " ");
        line = line.replace("->", " ");
        line = line.replace("MTU = ", " ");
        line = line.replace(",", " ");
        line = line.replace("Porta = ", " ");
        line = line.replace("IP = ", " ");
        line = line.replace(";", "");
        return line;
    }
}