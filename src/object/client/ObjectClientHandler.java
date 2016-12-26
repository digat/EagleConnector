/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import interfaces.ConnectionFeedBack;

/**
 *
 * @author Tareq
 */
public class ObjectClientHandler extends SimpleChannelInboundHandler<Object> {
    private final ConnectionFeedBack connectionFeedBack;

    public ObjectClientHandler(ConnectionFeedBack connectionFeedBack) {
        super();
        this.connectionFeedBack = connectionFeedBack;     
    }
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
        System.out.println("we did it :)");
        if(msg instanceof Object ){
            //System.out.println("we did it :)");
            //print((RequestAddUserMsg) msg);
        }else{
            System.err.println("we did not :( ");
        }
        connectionFeedBack.onRecivedData(msg, ctx.channel());
    }
    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        //System.out.println("[Server] connection alive");
    }
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        //System.out.println("[Server] connection added");
        connectionFeedBack.connectionActive();
    }
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        connectionFeedBack.connectionClosed();
        //System.out.println("[Server] connection removed");
        //System.out.println("[Server] : "+ctx.channel().remoteAddress()+" has disconnect me.");
    }    
    /*
    private void print(UserClass user){
        if(user!=null){
            System.out.println("[UserId] \t"+user.getUserId());
            System.out.println("[PlanId] \t"+user.getPlanId());
            System.out.println("[PublishKey] \t"+user.getPublishKey());
            System.out.println("[SubscribeKey] \t"+user.getSubscribeKey());
            System.out.println("[SecretKey] \t"+user.getSecretKey());        
        }
    }*/    
}
