/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sync;

/**
 *
 * @author Tareq
 */
public class Enum {
    public enum AckProtocolCmd{
        done,
        error;
    }
    public enum ProtocolCmd{
        doNothing,
        doHandShake,
    }
    public enum ConnectionState{
        init(0),
        connected(1),
        error(2),
        disconnected(3);
        int value;
        ConnectionState(int value){
            this.value = value;
        }
        public int toInteger(){
            return value;
        }
        public static ConnectionState fromInteger(int val){
            switch(val){
                case 0:
                    return ConnectionState.init;
                case 1:
                    return ConnectionState.connected;
                case 2:
                    return ConnectionState.error;
                default:
                    return ConnectionState.disconnected;      
            }
        }
    }
}
