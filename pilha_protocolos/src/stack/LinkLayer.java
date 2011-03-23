/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import pdu.Frame;




/**
 *
 * @author belitos
 */
public class LinkLayer implements Runnable{

    private static LinkLayer linkLayer;

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
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(Integer.parseInt(ProtocolStack.PORT));
        } catch(SocketException ex) {
            // TODO handle socket fail
        }

        // Always waiting for a frame
        while(true) {

            byte[] frame = new byte[ProtocolStack.MAX_MTU_SIZE];
            DatagramPacket receivedFrame = new DatagramPacket(frame, frame.length);
            Frame f = null;

            try {
                socket.receive(receivedFrame);
                // Getting the Object from the byte[]
                ByteArrayInputStream bis = new ByteArrayInputStream(receivedFrame.getData());
                ObjectInput in = new ObjectInputStream(bis);
                
                // Now we have to cast the Object to a Frame
                f = (Frame) in.readObject();

            } catch(ClassNotFoundException ex) {
                // The link layer should not fail because some frame is broken
                continue;
            } catch(IOException ex) {
                // The link layer should not fail because some frame is broken
                continue;
            }

            deliverToNetworkLayer(f);

        }
    }

    private void deliverToNetworkLayer(Frame f) {
        // TODO deliver datagram to network layer
    }

}
