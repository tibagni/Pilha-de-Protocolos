/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pdu;

import java.io.Serializable;
import java.util.Arrays;

/**
 *
 * @author tiago
 */
public class Datagram implements Comparable, Serializable {
    public static final int MAX_HEADER_SIZE = 300;
    public static final int TTL = 15;
    public static int NONE = -1;

    private String source; // IP origem (logical Id)
    private String destination; // IP destino (logical Id)
    private byte upperLayerProtocol; // Protocolo da camada superior
    private int ttl; // Time to live (tempo de vida, numero de hops)
    private int datagramId; // Id do datagrama

    // Fragmentacao
    private int offset; // offset do inicio do fragmento
    private boolean isLastFragment; // Flag indicando se e o utltimo

    private byte[] data;

    public Datagram(String src, String dst, byte ulp, int ttl, int dId, byte[] data) {
        this(src, dst, ulp, ttl, dId, data, 0, true);
    }

    public Datagram(String src, String dst, byte ulp, int ttl, int dId, byte[] data,
            int offset, boolean isLastFrag) {
        source = src;
        destination = dst;
        upperLayerProtocol = ulp;
        this.ttl = ttl;
        datagramId = dId;
        this.data = data;
        this.offset = offset;
        isLastFragment = isLastFrag;

    }

    /**
     * Decrementa TTL ou lanca uma execao indicando que o pacote deve ser descartado
     * @throws pdu.Datagram.TTLException
     */
    public void decrementTTL() throws TTLException{
        if(ttl > 0) {
            ttl--;
        } else throw new TTLException();
    }

    public int getTTL() {
        return ttl;
    }

    public byte getUpperLayerProtocol() {
        return upperLayerProtocol;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public int getOffset() {
        return offset;
    }

    public int getDatagramId() {
        return datagramId;
    }

    public byte[] getData() {
        return data;
    }

    public boolean isLastDatagramFragment() {
        return isLastFragment;
    }

    public void setDatagramFragmentId(int id) {
        offset = id;
    }

    public void setLastDatagramFragment(boolean b) {
        isLastFragment = b;
    }

    @Override
    public int hashCode()
    {
        int hash = 7;
        hash = 31 * hash + (this.source != null ? this.source.hashCode() : 0);
        hash = 31 * hash + (this.destination != null ? this.destination.hashCode() : 0);
        hash = 31 * hash + this.upperLayerProtocol;
        hash = 31 * hash + this.ttl;
        hash = 31 * hash + this.datagramId;
        hash = 31 * hash + this.offset;
        hash = 31 * hash + (this.isLastFragment ? 1 : 0);
        hash = 31 * hash + Arrays.hashCode(this.data);
        return hash;
    }

    @Override
    public String toString()
    {
        return "Datagram{" + "source=" + source + "destination=" + destination + "upperLayerProtocol=" + upperLayerProtocol + "ttl=" + ttl + "datagramId=" + datagramId + "offset=" + offset + "isLastFragment=" + isLastFragment + "data=" + data + '}';
    }

    // Compara apenas o id do fragmento (nao e preciso mais do que isto)
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Datagram)) return false;
        return compareTo(o) == 0 ? true : false;
    }

    // Compara apenas o id do fragmento (nao e preciso mais do que isto)
    @Override
    public int compareTo(Object t) {
        if(!(t instanceof Datagram)) {
            return -1;
        }
        Datagram d = (Datagram) t;
        if(this.offset < d.getOffset()) return - 1;

        if(this.offset > d.getOffset()) return  1;

        return 0;
    }

    /**
     * Execao que indica que o datagrama deve ser descartado
     */
    public static class TTLException extends Exception {
        public TTLException(String s) {
            super(s);
        }

        public TTLException() {
            super("TTL field is already zero\n" +
                    "you must discard this packet");
        }
    }
}
