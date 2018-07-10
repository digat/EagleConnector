/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sync;

import com.google.common.collect.Lists;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @author Tareq
 */
public class SyncProtocol {
    
    private final TimerTask task;
    private Timer timer = new Timer();
    private final AtomicBoolean stop = new AtomicBoolean(false);
    private final AtomicBoolean sendCommand = new AtomicBoolean(false);
    private final List<CmdProtocolInterface> vListener = Lists.newLinkedList();
    private AsynClass asynClass;
    private long timeCyclePerios =  1000L;
    public SyncProtocol(long timeCyclePerios) {
        this.timeCyclePerios = timeCyclePerios;
        task = init();
    }
    public SyncProtocol() {
        task = init();
    }
    private void setup(){
        timer.scheduleAtFixedRate(task, 500L, timeCyclePerios);
        
    }
    public void pause() {
        this.timer.cancel();
    }    
    
    public void resume() {
        this.timer = new Timer();
        this.timer.schedule( task, 0, timeCyclePerios);
    }    
    private TimerTask init(){
        return  new TimerTask() {
            @Override
            public void run() {
                System.out.println("Task performed on " + new Date());
                if(sendCommand.get()){
                    sendCommand.set(true);
                }
                if(stop.get()){
                    pause();
                    System.err.println("Exist performed on " + new Date());
                }
            }
        };        
    }

    public AtomicBoolean getStop() {
        return stop;
    }
    public CmdProtocolInterface addCommandListener(CmdProtocolInterface listener){
        vListener.add(listener);
        return listener;
    }
    public void removeCommandListener(CmdProtocolInterface listener){
        vListener.remove(listener);
    }    
    private void updateCommand(Enum.ProtocolCmd newValue){
        vListener.forEach((listner) -> {
            listner.command(newValue);
        });        
    }    

    public AsynClass getAsynClass() {
        return asynClass;
    }

    public void setAsynClass(AsynClass asynClass) {
        this.asynClass = asynClass;
        this.asynClass.addAckCmdListener(new AckCmdProtocolInterface(){
            @Override
            public void ackCommand(Enum.AckProtocolCmd newValue) {
                
            }
        
        });
    }
    
    public static void main(String[] args) throws InterruptedException {
        SyncProtocol sp = new SyncProtocol(2000L);
        sp.setup();
        Thread.sleep(5000);
        sp.getStop().set(true);
    }
}
