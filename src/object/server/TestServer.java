/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.server;

import interfaces.ConnectionFeedBack;
import io.netty.channel.Channel;
import java.net.BindException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLException;
import oms.Message;

/**
 *
 * @author Tareq
 */
public class TestServer {
    public static void main(String[] args) throws CertificateException, SSLException, InterruptedException, BindException {
        ConnectionFeedBack connectionFeedBack = getConnectionFeedBack();
        ObjectServer os = new ObjectServer(7755,  Message.class.getClassLoader());
        os.run(connectionFeedBack);
    }

    private static ConnectionFeedBack getConnectionFeedBack() {
        return new ConnectionFeedBack(){
            @Override
            public void connectionActive() {
                System.err.println("new client");
            }

            @Override
            public void connectionClosed() {
                System.err.println("client left");
            }

            @Override
            public void onRecivedData(Object message, Channel channel) {
               System.err.println(message.getClass());
            }
        };
    }
}
