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
import java.util.logging.Level;
import java.util.logging.Logger;
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
    public final static int ADLER_LIMIT = 9;




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

    public synchronized boolean sendFrame(Host h,Frame f)
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
         ObjectOutput out;
         ByteArrayOutputStream bos;
         byte[] toSend = null;
         byte[] longSum;
         byte[] aux;

     
        

        try {
           
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(f);
            frameBytes = bos.toByteArray();



            aux = new byte[ProtocolStack.MAX_MTU_SIZE-ADLER_LIMIT];
            
            for(int i = 0; i < frameBytes.length; i++)
                aux[i] = frameBytes[i];
            
            for(int i = frameBytes.length; i < ProtocolStack.MAX_MTU_SIZE-ADLER_LIMIT; i ++)
                aux[i] = 0;
                
            

            Checksum checksumEngine = new Adler32();
            checksumEngine.update(aux, 0, aux.length);


            
            toSend = new byte[ProtocolStack.MAX_MTU_SIZE];
            
            
            longSum = new Long(checksumEngine.getValue()).toString().getBytes();

            
            checksumEngine.reset();




            for(int i = 0; i < aux.length ; i++)
                toSend[i] = aux[i];

            for(int i = aux.length, j = 0; j < longSum.length; i++, j++)
            {
                toSend[i] = longSum[j];
               
            }

        } catch (IOException ex) {
            System.err.printf("IOException - checksum!\n\n");
            System.exit(1);
        }

         return toSend;
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
                Checksum checksumEngine = new Adler32();
                byte[] data;
                boolean isCorrupted;


                try {
                     
                    socket.receive(receivedFrame);
                    frame = receivedFrame.getData();


                   

                    
                    checksumEngine.update(frame, 0, ProtocolStack.MAX_MTU_SIZE-ADLER_LIMIT);
                    

                    byte[] result = new Long(checksumEngine.getValue()).toString().getBytes();
                    

                    isCorrupted = false;

                    for(int i = 0,j = ProtocolStack.MAX_MTU_SIZE-ADLER_LIMIT ; i < result.length-1; i++, j++)
                        if(result[i] != frame[j])
                            isCorrupted = true;


                    if(isCorrupted)
                    {
                        System.err.printf("Corrupted!\n");
                        continue;
                    }

                    data =  Arrays.copyOfRange(frame, 0, ProtocolStack.MAX_MTU_SIZE-ADLER_LIMIT);

                    // Getting the Object from the byte[]
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = new ObjectInputStream(bis);



                    // Now we have to cast the Object to a Frame
                    f = (Frame) in.readObject();

                    deliverToNetworkLayer(f);

                    checksumEngine.reset();

                   

                } catch(ClassNotFoundException ex) {
                    // The link layer should not fail because some frame is broken
                    System.err.printf("Corrupted packet!\n");
                    continue;
                } catch(IOException ex) {
                    // The link layer should not fail because some frame is broken
                    System.err.printf("Corrupted packet!\n");
                    continue;
                }

                 
            }

        
    }

    private void deliverToNetworkLayer(Frame f) {
        // TODO deliver datagram to network layer

        System.out.printf("\n\nMessage received:%s\n\n",f.getS());
    }

}
