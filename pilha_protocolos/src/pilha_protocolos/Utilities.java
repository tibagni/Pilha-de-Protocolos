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
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import stack.ProtocolStack;

/**
 *
 * @author tiago
 */
public class Utilities {
    private static final Logger logger = Logger.getLogger(Utilities.class.getName());
    private static  FileHandler handler;

    static {
        try {
            String logName = ProtocolStack.getLocalhost().getLogicalID() +
                    "-log.txt";
            handler = new FileHandler(logName, true);
        } catch (Exception ex) {
            // Exception while creating log file
            handler = null;
            ex.printStackTrace();
        } finally {
            if (handler != null) {
                logger.addHandler(handler);
                handler.setFormatter(new SimpleFormatter());
            }
            logger.setLevel(Level.ALL);
            logger.setUseParentHandlers(false); // Nao mostrar no console
        }
    }

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
    private static final String FILTER = NO_FILTER;

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
            logException(ex);
            return -1;
        }

        return bytes.length;
    }

    public static byte[] toByteArray(Serializable o) {
        byte[] bytes = null;
        ObjectOutput out = null;
        ByteArrayOutputStream bos = null;

        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            bytes = bos.toByteArray();
        } catch(IOException ex) {
            logException(ex);
            return null;
        } finally {
            try {
                out.close();
                bos.close();
            } catch (IOException ex) {
                logException(ex);
            } catch (NullPointerException ex) {
                Utilities.logException(ex);
            }
        }

        return bytes;
    }

    public static Object toObject(byte[] byteArray) {
        ByteArrayInputStream bis = new ByteArrayInputStream(byteArray);
        ObjectInput in = null;
        Object o = null;
        try {
            in = new ObjectInputStream(bis);
            o = in.readObject();
        } catch(IOException ex) {
            logException(ex);
            return null;
        } catch(ClassNotFoundException e) {
            logException(e);
            return null;
        } finally {
            try {
                in.close();
                bis.close();
            } catch (IOException ex) {
                logException(ex);
            } catch (NullPointerException ex) {
                Utilities.logException(ex);
            }
        }
        return o;
    }

    public static void print(String s, Object... args) {
        if(PRINT) {
            System.out.printf("PRINT=> " + s, args + "\n");
        }
    }

    public static void printError(String s, Object... args) {
        System.err.printf("ERROR=> " + s, args);
        System.err.println();
    }

    public synchronized static void log(String tag, String message, Object... args) {
        if(LOG) {
            if(FILTER.equals(NO_FILTER) || FILTER.equals(tag)) {
                String s = String.format("LOG [" + tag + "]=> " + message, args);
                logger.log(Level.INFO, "{0}\n", s);
            }
        }
    }

    public static void logException(Exception e) {
        if(LOG_EX) {
            logger.log(Level.SEVERE, null, e);
        }
    }

    public static void logMethod() {
        StackTraceElement[] st = Thread.getAllStackTraces().get(Thread.currentThread());
        StackTraceElement element = st[3];
        logger.log(Level.INFO, "LOG [METHOD]=> {0}.{1}",
                new Object[]{element.getClassName(), element.getMethodName()});
    }
}
