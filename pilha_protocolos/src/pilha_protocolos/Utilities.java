/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pilha_protocolos;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import pdu.Datagram;

/**
 *
 * @author tiago
 */
public class Utilities {
    
    // ativar logs (true)
    private static final boolean LOG    = true;
    
    // ativar prints (true)
    private static final boolean PRINT  = true;

    // ativar logs de exceptions (true)
    private static final boolean LOG_EX = true;

    // LOG tags
    public static final String LINK_TAG = "LinkLayer";
    public static final String NETWORK_TAG = "NetworkLayer";
    public static final String TRANSPORT_TAG = "TransportLayer";
    public static final String PROTOCOL_STACK_TAG = "ProtocolStack";

    // FILTER (Mude o filtro antes da compilacao)
    // NO_FILTER imprime todos os logs (se LOG == true)
    private static final String NO_FILTER = "no_filter";
    private static final String FILTER = NETWORK_TAG;

    public static int getObjectSize(Serializable o){
        byte[] bytes = null;
        ObjectOutput out;
        ByteArrayOutputStream bos;

        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            bytes = bos.toByteArray();
        } catch(IOException ex) {
            ex.printStackTrace();
            return -1;
        }

        return bytes.length;
    }

    public static byte[] toByteArray(Serializable o) {
        byte[] bytes = null;
        ObjectOutput out;
        ByteArrayOutputStream bos;

        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            bytes = bos.toByteArray();
        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        }

        return bytes;
    }

    public static Object toObject(byte[] byteArray) {
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        ObjectInput in;
        Object o = null;
        try {
            in = new ObjectInputStream(bis);
            o = in.readObject();
        } catch(IOException ex) {
            ex.printStackTrace();
            return null;
        } catch(ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        return o;
    }

    public static void print(String s, Object... args) {
        if(PRINT) {
            System.out.printf("PRINT: " + s, args);
            System.out.println();
        }
    }

    public static void printError(String s, Object... args) {
        System.err.printf("ERROR: " + s, args);
        System.out.println();
    }

    public static void log(String tag, String message, Object... args) {
        if(LOG) {
            if(FILTER.equals(NO_FILTER) || FILTER.equals(tag)) {
                System.out.printf("LOG: " + tag + ": " + message, args);
                System.out.println();
            }
        }
    }

    public static void logException(Exception e) {
        if(LOG_EX) {
            System.out.printf("EXCEPTION: " + e.getLocalizedMessage());
            System.out.printf("\nMESSAGE: " + e.getMessage());
            System.out.printf("\nSTACK TRACE: ");
            e.printStackTrace();
        }
    }
}
