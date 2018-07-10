/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sync;

import com.google.common.collect.Lists;
import java.util.List;
import sync.Enum.ConnectionState;

/**
 *
 * @author Tareq
 */
public class FsmClass {
    private ConnectionState connectionState = ConnectionState.init;
    private final List<FsmInterface> vListener = Lists.newLinkedList();

    public FsmClass() {
    }
    public void setState(ConnectionState connectionState){
        this.updateFsm(this.connectionState, connectionState);
        this.connectionState = connectionState;
    }
    public FsmInterface addFsmListener(FsmInterface listener){
        vListener.add(listener);
        return listener;
    }
    public void removeFsmListener(FsmInterface listener){
        vListener.remove(listener);
    }    
    private void updateFsm(ConnectionState newValue, ConnectionState oldValue){
        vListener.forEach((listner) -> {
            listner.fsmUpdate(newValue, oldValue);
        });        
    }
    
}
