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

/**
 *
 * @author tiago
 */
public class Utilities {

    public static int getObjectSize(Object o){
        byte[] bytes = null;
        ObjectOutput out;
        ByteArrayOutputStream bos;

        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            bytes = bos.toByteArray();
        } catch(IOException ex) {
            // TODO sei la
        }

        return bytes.length;
    }

    public static byte[] toByteArray(Object o) {
        byte[] bytes = null;
        ObjectOutput out;
        ByteArrayOutputStream bos;

        try {
            bos = new ByteArrayOutputStream();
            out = new ObjectOutputStream(bos);
            out.writeObject(o);
            bytes = bos.toByteArray();
        } catch(IOException ex) {
            // TODO sei la
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

        } catch(ClassNotFoundException e) { }
        return o;
    }
}
