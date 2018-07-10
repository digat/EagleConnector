/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sync;

import sync.Enum.ProtocolCmd;

/**
 *
 * @author Tareq
 */
public interface CmdProtocolInterface {
    public void command(ProtocolCmd newValue);
}
