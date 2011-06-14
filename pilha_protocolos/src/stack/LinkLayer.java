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
import pilha_protocolos.Utilities;

/**
 *
 * @author belitos
 */
public class LinkLayer implements Runnable{

    private static LinkLayer linkLayer = null;
    private GarbledDatagramSocket socket;
    public final static int ADLER_SIZE = 8;

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
            Utilities.printError("Error creating DatagramSocket!(SocketException)\n\n");
            System.exit(1);
        } catch(IOException ioEx) {
             Utilities.printError("Error creating DatagramSocket!(IOException)\n\n");
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
    public void finalize() throws Throwable {
        super.finalize();
        socket.close();
    }

    /*package*/ synchronized boolean send(byte[] data, byte protocol, Host h) {
        Frame frame = new Frame(protocol, data);
        return sendFrame(h, frame);
    }

    private boolean sendFrame(Host h, Frame f)
    {
        byte[] sendPacket;
        DatagramPacket packet;
        Host localhost = ProtocolStack.getLocalhost();

        if(h == null)
            return false;

        if(!localhost.isNeighbour(h.getLogicalID()))
            return false;

        try {

            sendPacket = calculateCheckSum(f);

             if(sendPacket.length > ProtocolStack.MAX_MTU_SIZE )
                return false;
            
            packet = new DatagramPacket(sendPacket, sendPacket.length,
                    InetAddress.getByName(h.getMAC().getIP()), Integer.parseInt(h.getMAC().getPort()));
            socket.send(packet);

        } catch (UnknownHostException ex) {
            Utilities.printError("UnkownHostException - send packet!\n\n");
            System.exit(1);
        } catch (IOException ex) {
            Utilities.printError("IOException - send packet!\n\n");
            System.exit(1);
        }
        return true;
    }

    private byte[] calculateCheckSum(Frame f) {
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

            aux = new byte[ProtocolStack.MAX_MTU_SIZE-ADLER_SIZE];
            System.arraycopy(frameBytes, 0, aux, 0, frameBytes.length);
            
            for(int i = frameBytes.length; i < ProtocolStack.MAX_MTU_SIZE - ADLER_SIZE; i ++)
                aux[i] = 0;          

            Checksum checksumEngine = new Adler32();
            checksumEngine.update(aux, 0, aux.length);
       
            toSend = new byte[ProtocolStack.MAX_MTU_SIZE];
            
            byte[] chkSum = new Long(checksumEngine.getValue()).toString().getBytes();
            // Deixa o tamanho do checksum sempre em 8 bytes
            longSum = new byte[ADLER_SIZE];
            if (chkSum.length > ADLER_SIZE) {
                System.arraycopy(chkSum, 0, longSum, 0, ADLER_SIZE);
            } else if (chkSum.length < ADLER_SIZE) {
                for (int i = chkSum.length; i < ADLER_SIZE; i++) {
                    longSum[i] = 0;
                }
            }
            
            checksumEngine.reset();
            // Coloca o checksum nos 8 primeiros bytes, depois coloca os dados
            System.arraycopy(longSum, 0, toSend, 0, ADLER_SIZE);
            System.arraycopy(aux, 0, toSend, ADLER_SIZE, aux.length);

        } catch (IOException ex) {
            Utilities.printError("IOException - checksum!\n\n");
            return null;
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

                    data =  Arrays.copyOfRange(frame, ADLER_SIZE, ProtocolStack.MAX_MTU_SIZE);
                    checksumEngine.update(data, 0, data.length);

                    byte[] result = new Long(checksumEngine.getValue()).toString().getBytes();
                    isCorrupted = false;

                    for(int i = 0; i < ADLER_SIZE; i++)
                        if(result[i] != frame[i]) {
                            isCorrupted = true;
                            System.err.print("merdaaaaaaaaaaaaaaaaa\n");
                        }

                    //isCorrupted = false; // TODO fix CRC

                    if(isCorrupted) {
                        Utilities.log(Utilities.LINK_TAG, "Corrupted packet arrived!\n");
                        continue;
                    }

                    // Getting the Object from the byte[]
                    ByteArrayInputStream bis = new ByteArrayInputStream(data);
                    ObjectInput in = new ObjectInputStream(bis);

                    // Now we have to cast the Object to a Frame
                    f = (Frame) in.readObject();

                    deliverToNetworkLayer(f);

                    checksumEngine.reset();
                } catch(ClassNotFoundException ex) {
                    // The link layer should not fail because some frame is broken
                    Utilities.log(Utilities.LINK_TAG, "Corrupted packet arrived!\n");
                    continue;
                } catch(IOException ex) {
                    // The link layer should not fail because some frame is broken
                    Utilities.log(Utilities.LINK_TAG, "Corrupted packet arrived!\n");
                    continue;
                }
                 
            }
    }

    private void deliverToNetworkLayer(Frame f) {
        switch(f.getType()) {
            case ProtocolStack.NETWORK_PROTOCOL_NP:
                NetworkLayer.getInstance().receive(f.getData());
                break;
        }
    }

}
