/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client.ext;

/**
 *
 * @author madfooatcom
 */
public class States {
    public static enum ConnectioState{
        nil,
        connected,
        disconected
    }
    public static enum ItemState{
        inuse,
        free,
        notready
    }
}
