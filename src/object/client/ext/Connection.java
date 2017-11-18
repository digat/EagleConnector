/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client.ext;

import java.io.Serializable;
import object.client.ext.States.ConnectioState;

/**
 *
 * @author madfooatcom
 */
public interface Connection extends Serializable {
    public void setState(ConnectioState state);
}
