/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client.ext;

import interfaces.ConnectionFeedBack;
import io.netty.channel.Channel;
import oms.Message;
import oms.OmsHandShake;
import org.squirrelframework.foundation.fsm.StateMachineBuilderFactory;
import org.squirrelframework.foundation.fsm.UntypedStateMachine;
import org.squirrelframework.foundation.fsm.UntypedStateMachineBuilder;
import org.squirrelframework.foundation.fsm.annotation.StateMachineParameters;
import org.squirrelframework.foundation.fsm.impl.AbstractUntypedStateMachine;

/**
 *
 * @author madfooatcom
 */
public class TestClient {
    enum FSMEvent {
        ToDisconnect, ToConnect, ToTryToConnect
    }
    @StateMachineParameters(stateType=String.class, eventType=FSMEvent.class, contextType=Integer.class)
    static class StateMachineSample extends AbstractUntypedStateMachine {
        protected void fromDisconnectToConnect(String from, String to, FSMEvent event, Integer context) {
            System.out.println("Transition from '"+from+"' to '"+to+"' on event '"+event+
                "' with context '"+context+"'.");
        }
        protected void fromConnectToDisconnect(String from, String to, FSMEvent event, Integer context) {
            System.out.println("Transition from '"+from+"' to '"+to+"' on event '"+event+
                "' with context '"+context+"'.");
        }
        protected void fromConnectToTryToConnect(String from, String to, FSMEvent event, Integer context) {
            System.out.println("Transition from '"+from+"' to '"+to+"' on event '"+event+
                "' with context '"+context+"'.");
        }     
        protected void fromDisconnectToTryToConnect(String from, String to, FSMEvent event, Integer context) {
            System.out.println("Transition from '"+from+"' to '"+to+"' on event '"+event+
                "' with context '"+context+"'.");
        }            
        protected void ontoConnect(String from, String to, FSMEvent event, Integer context) {
            System.out.println("Entry State \'"+to+"\'.");
            if(objectClient!=null){
                Channel ch = objectClient.getChannel();
                if(ch!=null){
                    OmsHandShake hs = new OmsHandShake("hs");
                    ch.writeAndFlush(hs);
                    System.err.println("send OmsHandShake");
                }
            }
        }
        private static ObjectClient objectClient;

        public static void setObjectClient(ObjectClient objectClient) {
            StateMachineSample.objectClient = objectClient;
        }
        
    }    
    public static void main(String[] args) throws InterruptedException {
        UntypedStateMachineBuilder builder = StateMachineBuilderFactory.create(StateMachineSample.class);
        builder.externalTransition().from("C").to("T").on(FSMEvent.ToTryToConnect).callMethod("fromConnectToTryToConnect");
        builder.externalTransition().from("D").to("T").on(FSMEvent.ToTryToConnect).callMethod("fromDisconnectToTryToConnect");
        builder.externalTransition().from("T").to("C").on(FSMEvent.ToConnect).callMethod("fromDisconnectToConnect");
        builder.externalTransition().from("C").to("D").on(FSMEvent.ToDisconnect).callMethod("fromConnectToDisconnect");
        builder.onEntry("C").callMethod("ontoConnect");

        // 4. Use State Machine
        final UntypedStateMachine fsm = builder.newStateMachine("D");
        

        System.out.println("Current state is "+fsm.getCurrentState());     
        ObjectClient oc = new ObjectClient(-1, "127.0.0.1", 7755, Message.class.getClassLoader());
        StateMachineSample.setObjectClient(oc);
        oc.setup();
        oc.setListenConnectionFeedBack(new ConnectionFeedBack(){
            @Override
            public void connectionActive() {
                //System.out.println("Current state is "+fsm.getCurrentState());
                fsm.fire(FSMEvent.ToConnect, 1);
            }

            @Override
            public void connectionClosed() {
                //System.out.println("Current state is "+fsm.getCurrentState());
                fsm.fire(FSMEvent.ToDisconnect, -1);
            }

            @Override
            public void onTryToConnect() {
                //System.out.println("Current state is "+fsm.getCurrentState());
                fsm.fire(FSMEvent.ToTryToConnect, 0);
            }
            
        });
        
        oc.start();
        /*
        ObjectClientConnectionPool ocp = new ObjectClientConnectionPool(1,"127.0.0.1", 7755, Message.class.getClassLoader());
        ocp.withMonitor();
        //ObjectClient oc = new ObjectClient("127.0.0.1", 7876, Message.class.getClassLoader());
        ocp.setup();
        Thread.sleep(2000);
        /*
        PoolItem pi = ocp.acquire();
        System.err.println(ocp.getFreeItemsCount());
        PoolItem pi1 = ocp.acquire();
        System.err.println(ocp.getFreeItemsCount());
        PoolItem pi2 = ocp.acquire();
        System.err.println(ocp.getFreeItemsCount());
        */
    }
    
}
