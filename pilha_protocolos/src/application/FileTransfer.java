/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import pilha_protocolos.Utilities;
import sockets.MySocket;
import stack.ProtocolStack;

/**
 *
 * @author belitos
 */
public class FileTransfer{

    private FileInfo actualFile;
    private MySocket socket;
    private boolean firstTime;

    public FileTransfer(int myPort){ //Receiver
        socket = new MySocket(ProtocolStack.getLocalhost().getLogicalID(),myPort);
        socket.bindServer();
        socket.accept();
        firstTime = true;
    }

    public FileTransfer(String logicaldDst, int portDst, int myPort){ //Sender
        socket = new MySocket(ProtocolStack.getLocalhost().getLogicalID(),myPort, logicaldDst, portDst);
        socket.bind();
        firstTime = true;
    }

    public void prepareSentMahNizzle(String fileFuckingName){
        File f = new File(fileFuckingName);
        actualFile = new FileInfo("cu com taxo",f.length(),fileFuckingName);

        socket.send(Utilities.toByteArray(actualFile));
    }

    public void send(String fileName){
        File f = new File(fileName);
        byte[] data = new byte[(int)f.length()];

        try {
            FileInputStream file = new FileInputStream(fileName);
            file.read(data);
        } catch (Exception ex) {
            Logger.getLogger(FileTransfer.class.getName()).log(Level.SEVERE, null, ex);
        }

        socket.send(data);
    }
    public void receive(){
        Object obj;
        if(firstTime){

           firstTime = false;
           obj = Utilities.toObject(socket.recieve());
           actualFile = (FileInfo) obj;

        } else {
        

            int actualSize = 0;
            byte[] receivedAux;
            byte[] receivedFinal = new byte[0];

            while(actualSize < actualFile.getSize()){

                receivedAux = socket.recieve();
                actualSize += receivedAux.length;
                receivedFinal = concat(receivedFinal, receivedAux);
            }

            try {

                FileOutputStream file = new FileOutputStream("rec"+actualFile.getName());
                file.write(receivedFinal);

            } catch (Exception ex) {
                Logger.getLogger(FileTransfer.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
    }
    public byte[] concat(byte[] a, byte[] b){

        byte [] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }



}
