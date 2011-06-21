/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
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
    private int step;

    static final int STEP_WAITING_TO_SEND      = 0; // Valido somente para clientes (senders)
    static final int STEP_WAITING_TO_RECEIVE   = 1; // Valido somente para servers (receivers)
    static final int STEP_TRANSFERRING         = 2;
    static final int STEP_PENDING_REQUEST      = 3; // Valido somente para servers (enviar confirmacao)
    static final int STEP_WAITING_CONFIRMATION = 4; // Valido somente para clientes

    public FileTransfer(int myPort){ //Receiver
        socket = new MySocket(ProtocolStack.getLocalhost().getLogicalID(),myPort);
        socket.bindServer();
        socket.accept();
        step = STEP_WAITING_TO_RECEIVE;
    }

    public FileTransfer(String logicaldDst, int portDst, int myPort){ //Sender
        socket = new MySocket(ProtocolStack.getLocalhost().getLogicalID(),myPort, logicaldDst, portDst);
        socket.bind();
        step = STEP_WAITING_TO_SEND;
    }

    public void sendRequestToSend(String fileName){
        File f = new File(fileName);
        actualFile = new FileInfo("cu cum taxo", f.length(), fileName);

        if (socket.send(Utilities.toByteArray(actualFile)) > 0) {
            step = STEP_WAITING_CONFIRMATION;
        }
    }

    /*package*/ FileInfo getFileInfo() {
        return actualFile;
    }

    public boolean sendResponse(String resp) {
        boolean result = false;
        if (socket.send(resp.getBytes()) > 0) {
            result = true;
            step = STEP_TRANSFERRING;
        }
        if ("deny".equals(resp)) {
            step = STEP_WAITING_TO_RECEIVE;
        }
        return result;
    }
    public boolean sendFile(String fileName){
        File f = new File(fileName);
        FileInputStream file = null;
        byte[] data = new byte[(int)f.length()];

        try {
            file = new FileInputStream(fileName);
            file.read(data);
        } catch (Exception ex) {
            Utilities.logException(ex);
            return false;
        } finally {
            try {
                file.close();
            } catch (IOException ex) {
                Utilities.logException(ex);
            }
        }
        if (socket.send(data) > 0) {
            // Enviado com sucesso
            step = STEP_WAITING_TO_SEND;
        }
        
        // Mesmo que a transferencia nao ocorra com sucesso
        // a maquina de estados deve voltar ao inicio
        step = STEP_WAITING_TO_SEND;
        return true;
    }
    public boolean receive(){
        FileInfo info = null;
        FileOutputStream file = null;
        boolean result = false;
        
        if(step == STEP_WAITING_TO_RECEIVE){
           step = STEP_PENDING_REQUEST;
           info = (FileInfo) Utilities.toObject(socket.recieve());
           actualFile = info;
           result = true; // FileInfo disponivel
        } else if (step == STEP_WAITING_CONFIRMATION) {
            // Pacote de confirmacao
            String confirmation = new String(socket.recieve());
            if ("accept".equals(confirmation)) {
                result = true;
                step = STEP_TRANSFERRING;
            } else {
                step = STEP_WAITING_TO_SEND;
                result = false;
            }
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
                file = new FileOutputStream("rec"+actualFile.getName());
                file.write(receivedFinal);
                result = true;
            } catch (Exception ex) {
                Utilities.logException(ex);
            } finally {
                // Volta ao estado inicial
                step = STEP_WAITING_TO_RECEIVE;
                try {
                    file.close();
                } catch (IOException ex) {
                    Utilities.logException(ex);
                }
            }
        }
        return result;
    }

    public int whichStep() {
        return step;
    }

    private byte[] concat(byte[] a, byte[] b){
        byte [] c = new byte[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);

        return c;
    }
}
