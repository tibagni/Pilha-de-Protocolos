/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pdu;

/**
 *
 * @author tiago
 */
public class Datagram implements Comparable{
    public static final int MAX_HEADER_SIZE = 20;
    public static final int TTL = 15;

    private String source; // source IP (logical Id)
    private String destination; // destination IP (logical Id)
    private byte upperLayerProtocol; // Witch protocol to deliver
    private int ttl; // Time to live
    private int datagramId; // Id of datagram

    // Fields related to fragmentation
    private int fragmentId; // id of the datagram fragment
    private boolean isLastFragment; // Flag indicating whether is the last fragment or not

    private byte[] data;

    public Datagram(String src, String dst, byte ulp, int ttl, int dId, byte[] data) {
        this(src, dst, ulp, ttl, dId, data, 1, true);
    }

    public Datagram(String src, String dst, byte ulp, int ttl, int dId, byte[] data,
            int fragId, boolean isLastFrag) {
        source = src;
        destination = dst;
        upperLayerProtocol = ulp;
        this.ttl = ttl;
        datagramId = dId;
        this.data = data;
        fragmentId = fragId;
        isLastFragment = isLastFrag;

    }

    /**
     * Decrement TTL or throw an exception indicating you must discard the packet
     * @throws pdu.Datagram.TTLException
     */
    public void decrementTTL() throws TTLException{
        if(ttl > 0) {
            ttl--;
        } else throw new TTLException();
    }

    /**
     * Get TTL value
     * @return current ttl
     */
    public int getTTL() {
        return ttl;
    }

    /**
     * retrieves witch protocol to deliver the data
     * @return upperLayerProtocol
     */
    public byte getUpperLayerProtocol() {
        return upperLayerProtocol;
    }

    public String getSource() {
        return source;
    }

    public String getDestination() {
        return destination;
    }

    public int getDatagramFragmentId() {
        return fragmentId;
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
        fragmentId = id;
    }

    public void setLastDatagramFragment(boolean b) {
        isLastFragment = b;
    }

    @Override
    public boolean equals(Object o) {
        return compareTo(o) == 0 ? true : false;
    }
    @Override
    public int compareTo(Object t) {
        if(!(t instanceof Datagram)) {
            return -1;
        }
        Datagram d = (Datagram) t;
        if(this.fragmentId < d.getDatagramFragmentId()) return - 1;

        if(this.fragmentId > d.getDatagramFragmentId()) return  1;

        return 0;
    }

    /**
     * Exception throwed when the packet should be discarded
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
