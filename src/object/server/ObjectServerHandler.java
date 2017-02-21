/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.server;
import interfaces.ConnectionFeedBack;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Arrays;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
/**
 *
 * @author Tareq
 */
public class ObjectServerHandler extends SimpleChannelInboundHandler<Object> {
    private static final String API_KEY = "1436779c1fac45f1b6779c1fac35f103";
    //private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConnectionFeedBack connectionFeedBack;
    
    public ObjectServerHandler(ConnectionFeedBack connectionFeedBack) {
        super();
        this.connectionFeedBack = connectionFeedBack;
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        //cause.printStackTrace();
        //log.error("[API/ObjectServerHandler] Error {}", Arrays.toString(cause.getStackTrace()));
        ctx.close();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        if(msg!=null){
            connectionFeedBack.onRecivedData(msg, ctx.channel());  
        }

    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        String msg = "[client] : "+ctx.channel().remoteAddress()+" is Connected";
        //if(!dataProcessingThread.isAlive()){
        //    dataProcessingThread.start();
        //}
        //System.err.println(msg);
        //log.info("[API/ObjectServerHandler] INFO {}", msg);
        //EventManager.fireInMsgToAll(new InMsgEvent(msg));
         connectionFeedBack.connectionActive();
    }
    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        String msg = "[client] : "+ctx.channel().remoteAddress()+" Connection Closed.";
        //System.err.println(msg);
        //log.info("[API/ObjectServerHandler] INFO {}", msg);
        //EventManager.fireInMsgToAll(new InMsgEvent(msg));
        connectionFeedBack.connectionClosed();
    }
}

