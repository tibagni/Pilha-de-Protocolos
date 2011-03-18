/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import graph.Graph;
import graph.Node;
import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.StringTokenizer;

/**
 *
 * @author belitos
 */
public class Stack {

    private Link link;
    public static final String LINK_STRING = "Enlaces";
    public static final String END_STRING = "Fim";
    private Graph graph;

    public Stack(String filePath)
    {
        graph = new Graph();

        readFile(filePath);


    }

    private void readFile(String path)
    {
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

        while(in.hasNext())
        {
            line = in.nextLine().trim();

            if(line.equals("")) //Blanck line
                continue;
            else if(line.equals(END_STRING))
                break;
            else if(line.equals(LINK_STRING))
            {
                isConnection = true;
                continue;
            }

            line = replaceCharacters(line);
           
            token = new StringTokenizer(line," ");
            


           if(!isConnection) //Adding nodes
               graph.addNode(new Node(token.nextToken(), token.nextToken(), token.nextToken()));
           else
               graph.addConnection(token.nextToken(), token.nextToken(), token.nextToken());
        }

        if(in != null)
            in.close();
    }

    private String replaceCharacters(String line)
    {

        line = line.replace(":"," ");
        line = line.replace("->"," ");
        line = line.replace("MTU = "," ");
        line = line.replace(","," ");
        line = line.replace("Porta = "," ");
        line = line.replace("IP = "," ");
        line = line.replace(";","");
        return line;
    }

}
