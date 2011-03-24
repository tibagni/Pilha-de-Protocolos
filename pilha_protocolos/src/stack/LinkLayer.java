/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package stack;

import graph.Host;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.logging.Level;
import java.util.logging.Logger;
import pdu.Frame;
import pilha_protocolos.GarbledDatagramSocket;




/**
 *
 * @author belitos
 */
public class LinkLayer implements Runnable{

    private static LinkLayer linkLayer = null;
    private GarbledDatagramSocket socket;



    /*
     * My little friends, dont forget to synchronize the socket! (the same to send and receive!)
     * I hope we dont shit bricks, lol.
     */

    private LinkLayer()
    {
       // Singleton class. Can't be instantiated outside.

        try {
            socket = new GarbledDatagramSocket(Integer.parseInt(ProtocolStack.getLocalhost().getMAC().getPort()),0,0,0);
        } catch(SocketException ex) {
            System.err.printf("Error creating DatagramSocket!(SocketException)\n\n");
            System.exit(1);
        } catch(IOException ioEx)
        {
             System.err.printf("Error creating DatagramSocket!(IOException)\n\n");
             System.exit(1);
        }

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

    @Override
    public void finalize()
    {
        
        socket.close();
    }

    public boolean sendFrame(Host h,Frame f)
    {
        Host localhost = ProtocolStack.getLocalhost();

        if(h == null)
            return false;

        if(!localhost.isNeighbour(h.getLogicalID()))
            return false;

        try {

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutput out = new ObjectOutputStream(bos);
            out.writeObject(f);
            byte[] frameBytes = bos.toByteArray();
            DatagramPacket packet;
            
            packet = new DatagramPacket(frameBytes, frameBytes.length, InetAddress.getByName(h.getMAC().getIP()), Integer.parseInt(h.getMAC().getPort()));
            socket.send(packet);

        } catch (UnknownHostException ex) {
            Logger.getLogger(LinkLayer.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
                Logger.getLogger(LinkLayer.class.getName()).log(Level.SEVERE, null, ex);
        }





        return true;
    }

    /**
     * Create a server datagram socket to listento the link
     */
    private void receiveFrames() {
        
        

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

                    System.out.printf("%s\n\n",f.getS());

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
