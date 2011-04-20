/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pilha_protocolos;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
}
