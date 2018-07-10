/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sync;

import sync.Enum.ConnectionState;

/**
 *
 * @author Tareq
 */
public interface FsmInterface {
    public void fsmUpdate(ConnectionState newValue, ConnectionState oldValue);
}
