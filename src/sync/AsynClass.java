/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sync;

import com.google.common.collect.Lists;
import java.util.List;
import sync.Enum.AckProtocolCmd;
import sync.Enum.ProtocolCmd;

/**
 *
 * @author Tareq
 */
public class AsynClass {
    private final List<AckCmdProtocolInterface> vListener = Lists.newLinkedList();
    private final SyncProtocol syncProtocol;

    public AsynClass() {
        syncProtocol = new SyncProtocol();
    }
    public void ini(){
        syncProtocol.setAsynClass(this);
         syncProtocol.addCommandListener((ProtocolCmd newValue) -> {
             switch(newValue){
                 case doNothing:
                     doNothing();
                     break;
                 case doHandShake:
                     doHandShake();
                     break;             
             }
         });
    }
    public AckCmdProtocolInterface addAckCmdListener(AckCmdProtocolInterface listener){
        vListener.add(listener);
        return listener;
    }
    public void removeAckCmdListener(AckCmdProtocolInterface listener){
        vListener.remove(listener);
    }    
    public void updateAckCmd(AckProtocolCmd newValue){
        vListener.forEach((listner) -> {
            listner.ackCommand(newValue);
        });        
    }    
    private void doHandShake(){
        
    }
    private void doNothing(){
        updateAckCmd(AckProtocolCmd.done);
    }    
}
