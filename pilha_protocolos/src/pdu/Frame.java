/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package pdu;

import java.io.Serializable;

/**
 *
 * @author tiago
 */
public class Frame implements Serializable {

    private String s;

    public Frame()
    {

        s = "oi";
    }

    public String getS()
    {
        return s;
    }

}
