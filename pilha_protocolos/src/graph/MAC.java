/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package graph;

import java.io.Serializable;

/**
 *
 * @author belitos
 */
public class MAC implements Serializable {

    private String IP;
    private String port;


    public MAC(String i,String p)
    {
            IP = i;
            port = p;

    }

        public String getIP()
        {
            return IP;
        }

        public String getPort()
        {
            return port;
        }

}
