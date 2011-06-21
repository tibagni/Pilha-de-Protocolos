/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package application;

import java.io.Serializable;

/**
 *
 * @author belitos
 */
public class FileInfo implements Serializable {

    private String owner;
    private long size;
    private String name;

    public FileInfo(String o, long s, String n){
        owner = o;
        size = s;
        name = n;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOwner() {
        return owner;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }
}
