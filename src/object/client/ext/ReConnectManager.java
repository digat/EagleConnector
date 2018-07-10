/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client.ext;

import interfaces.ConnectionFeedBack;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tp.Client;

/**
 *
 * @author Tareq
 * @param <T>
 */
public class ReConnectManager<T> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReConnectManager.class);
    private final AtomicBoolean connected = new AtomicBoolean(false);
    private final AtomicBoolean running = new AtomicBoolean(false);
    private long timeCyclePerios = 1000L;
    private Timer timer;
    private ConnectionFeedBack listenConnectionFeedBack;
    // private final TimerTask task;
    private final T client;

    public ReConnectManager(T client, long timeCyclePerios) {
        this.client = client;
        this.timeCyclePerios = timeCyclePerios;
        //task = init();
    }

    public void cancel() {
        if (timer != null) {
            timer.cancel();
            running.set(false);
        }
    }

    public void resume() {
        running.set(true);
        if(timer!=null)
            timer.cancel();
        this.timer = new Timer();
        this.timer.schedule(getTask(), 0, timeCyclePerios);
        
    }

    private TimerTask getTask() {
        return new TimerTask() {
            @Override
            public void run() {
                //System.out.println("Task performed on " + new Date());
                if (connected.get()) {
                    running.set(false);
                    cancel();
                    LOGGER.debug("Connected on {}", new Date());
                    
                    //System.err.println("Exist performed on " + new Date());
                } else {
                    if(listenConnectionFeedBack!=null){
                        listenConnectionFeedBack.onTryToConnect();
                    }
                    running.set(true);
                    LOGGER.debug("try to connect on {}",  new Date());
                    doConnect();
                    
                }
            }
        };
    }

    private void doConnect() {
        if (client instanceof ObjectClient) {
            ObjectClient oc = (ObjectClient) client;
            oc.connectMe();
        }else if (client instanceof Client) {
            Client oc = (Client) client;
            oc.connectMe();            
        }else if (client instanceof ObjectNewClient){
            ObjectNewClient onc = (ObjectNewClient) client;
            onc.connectMe();
        }
    }

    public void setConnected(boolean flage) {
        connected.set(flage);
        if(!flage){//not connected
            if(!running.get()){
                resume();
            }
            
        }
    }

    public void setListenConnectionFeedBack(ConnectionFeedBack listenConnectionFeedBack) {
        this.listenConnectionFeedBack = listenConnectionFeedBack;
    }
    
}
