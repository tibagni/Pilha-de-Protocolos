/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pdu;

/**
 *
 * @author tiago
 */
public class Datagram {
    private String source; // source IP (logical Id)
    private String destination; // destination IP (logical Id)
    private byte upperLayerProtocol; // Witch protocol to deliver
    private int ttl; // Time to live

    // Fields related to fragmentation
    private int fragmentId; // id of the datagram fragment
    private boolean isLastFragment; // Flag indicating whether is the last fragment or not

    private Object data;

    public Datagram(String src, String dst, byte ulp, int ttl, Object data) {
        this(src, dst, ulp, ttl, data, 1, true);
    }

    public Datagram(String src, String dst, byte ulp, int ttl, Object data,
            int fragId, boolean isLastFrag) {
        source = src;
        destination = dst;
        upperLayerProtocol = ulp;
        this.ttl = ttl;
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

    public boolean isLastDatagramFragment() {
        return isLastFragment;
    }

    public void setDatagramFragmentId(int id) {
        fragmentId = id;
    }

    public void setLastDatagramFragment(boolean b) {
        isLastFragment = b;
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
