/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.logging.Level;
import java.util.logging.Logger;




/**
 *
 * @author belitos
 */
public class LinkLayer implements Runnable{

    private static LinkLayer linkLayer = null;

    private LinkLayer()
    {
       // Singleton class. Can't be instantiated outside.
    }

    /**
     * Singleton method getInstance
     * @return the singleton intance of LinkLayer
     */
    public static LinkLayer getInstance() {
        if(linkLayer == null) {
            linkLayer = new LinkLayer();
        }
        return linkLayer;
    }

    public void run() {
        receiveFrames();
    }

    /**
     * Create a server datagram socket to listento the link
     */
    private void receiveFrames() {
        try {
            DatagramSocket socket = new DatagramSocket(Integer.parseInt(ProtocolStack.PORT));
        } catch(SocketException ex) {
            // TODO
        }

        // Always waiting for a frame
        while(true) {

        }
    }

}
