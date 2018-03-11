/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package interfaces;

import io.netty.channel.Channel;

/**
 *
 * @author Tareq
 */
public interface ConnectionFeedBack {
    default public void connectionException (Throwable cause){};
    public void connectionActive();
    public void connectionClosed();
    default public void onTryToConnect(){};
    default public void onRecivedData(String message){};
    default public void onRecivedData(Object message, Channel channel){};
    default public void onRecivedError(Throwable cause){};
}
