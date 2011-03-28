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
import java.util.Arrays;
import java.util.zip.Adler32;
import java.util.zip.Checksum;
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
        byte[] sendPacket;
        
        DatagramPacket packet;
        Host localhost = ProtocolStack.getLocalhost();

        if(h == null)
            return false;

        if(!localhost.isNeighbour(h.getLogicalID()))
            return false;



        try {

            



            sendPacket  = calculateCheckSum(f);

             if(sendPacket.length > ProtocolStack.MAX_MTU_SIZE )
                return false;
            
            
            
            packet = new DatagramPacket(sendPacket, sendPacket.length, InetAddress.getByName(h.getMAC().getIP()), Integer.parseInt(h.getMAC().getPort()));

           // System.out.printf("%d------\n",packet.getData().length);
            socket.send(packet);

        } catch (UnknownHostException ex) {
            System.err.printf("UnkownHostException - send packet!\n\n");
            System.exit(1);
        } catch (IOException ex) {
            System.err.printf("IOException - send packet!\n\n");
            System.exit(1);
        }





        return true;
    }

    private byte[] calculateCheckSum(Frame f)
    {
         byte[] frameBytes = null;
         byte[] newFrame = null;
         byte[] checkSum;
         ObjectOutput out;
         ByteArrayOutputStream bos;
         int i;

        try {
           
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(f);
            frameBytes = bos.toByteArray();

            f.setLength(frameBytes.length);

            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(f);
            frameBytes = bos.toByteArray();

            Checksum checksumEngine = new Adler32();
            checksumEngine.update(frameBytes, 0, frameBytes.length);
            checkSum = String.valueOf(checksumEngine.getValue()).getBytes();


            

           

            
           
           
            
            newFrame = new byte[frameBytes.length + checkSum.length];

            //System.out.printf("Check:%d\t%d\t%d\n\n",checksumEngine.getValue(),f.getLength(), checkSum.length);

            for(i = 0; i < frameBytes.length; i++)
                newFrame[i] = frameBytes[i];

            for(int k = 0; k < checkSum.length; i++, k++)
                newFrame[i] = checkSum[k];

            checksumEngine.reset();


           




        } catch (IOException ex) {
            System.err.printf("IOException - checksum!\n\n");
            System.exit(1);
        }

         return newFrame;
    }

    
    /**
     * Create a server datagram socket to listento the link
     */
    private void receiveFrames() {
        
        

        // Always waiting for a frame
        while(true) {


                byte[] frame = new byte[ProtocolStack.MAX_MTU_SIZE];
                byte[] checkSum,bytes,receivedCheckSum;
                DatagramPacket receivedFrame = new DatagramPacket(frame, frame.length);
                Frame f = null;
                


                try {
                     
                    socket.receive(receivedFrame);

                    bytes = receivedFrame.getData();
                     
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

                 //System.out.printf("%s\t%d\n\n",f.getS(),f.getLength());

                 
                 

                 checkSum = new byte[f.getLength()];

                 for(int i = 0; i < f.getLength(); i++)
                     checkSum[i] = bytes[i];

                 

                 Checksum checksumEngine = new Adler32();
                 checksumEngine.update(checkSum, 0, checkSum.length);

                // System.out.printf("%d-----%d\n\n",receivedFrame.getData().length,f.getLength());

                 

               //  System.out.printf("%d\n\n",checksumEngine.getValue());
                 

                 receivedCheckSum = new byte[String.valueOf(checksumEngine.getValue()).getBytes().length];



                 for(int i = f.getLength(), j = 0; j < receivedCheckSum.length; i++, j++)
                     receivedCheckSum[j] = bytes[i];
                     
                     
                 


               // System.out.printf("111-%d-%d-%s-%s\n",f.getLength(), receivedCheckSum.length,new String(receivedCheckSum),checksumEngine.getValue());


                 if(Long.toString(checksumEngine.getValue()).equals(new String(receivedCheckSum)))
                     deliverToNetworkLayer(f);
                 else
                     System.out.printf("Corrupted packet!\n\n");

                

                checksumEngine.reset();
            }

        
    }

    private void deliverToNetworkLayer(Frame f) {
        // TODO deliver datagram to network layer

        System.out.printf("Delivering to super layer\n\n");
    }

}
