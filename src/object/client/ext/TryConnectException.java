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
public class TryConnectException extends Exception {

    public TryConnectException(String ip, int port) {
        super("Fail to connect ]"+ip+":"+port+"]");
    }
    
}