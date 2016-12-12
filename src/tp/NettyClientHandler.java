/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;

import classes.Reply;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import java.util.Map;

/**
 *
 * @author Tareq
 */
public class NettyClientHandler extends SimpleChannelInboundHandler<String> {
    
    private final Map<Integer, Reply> replies;
    private final ConnectionFeedBack connectionFeedBack;
    //private final EventBus eventBus;
    
    public NettyClientHandler(Map<Integer, Reply> replies, ConnectionFeedBack connectionFeedBack) {
        super();
        this.replies = replies;
        this.connectionFeedBack = connectionFeedBack;
        //this.eventBus = eventBus;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext chc, String i) throws Exception {
        int id = getId(i, "<?");
        if(id >-1 ){
            //System.out.println("[From serverX] : "+i);
            Reply r = replies.get(id);
            //if(!r.isReady()){
                r.setMessage(getXml(i, "<?"));
                r.unlock();
            //}            
            //DataChangedHandler.fireDataChange(new DataChangeEvent(i, 0));
            //eventBus.post(r.getMessage());
        }
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
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        System.err.println("[Client][exceptionCaught] : "+ctx.channel().remoteAddress()+" Error : "+cause.getMessage());
        connectionFeedBack.connectionException(cause);
        ctx.close();
    }    
    private int getId(String xml, String match){
        int pos = xml.indexOf(match); 
        if(pos>-1){
            try{
                int id = Integer.parseInt(xml.substring(0, pos));
                return id;
            }catch(NumberFormatException ex){
                return -1;
            }
        }else{
            return pos;
        }
    }    
    private String getXml(String xml, String match){
        int pos = xml.indexOf(match); 
        if(pos>-1){
                return xml.substring(pos);
        }else{
            return null;
        }
    }     
     
}
