/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;

import interfaces.ConnectionFeedBack;
import classes.Reply;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.DelimiterBasedFrameDecoder;
import io.netty.handler.codec.Delimiters;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import java.util.Map;

/**
 *
 * @author Tareq
 */
public class NettyClientInitializer extends ChannelInitializer<SocketChannel>{
     private final Map<Integer, Reply> replies;
     //private final EventBus eventBus;
    private final ConnectionFeedBack connectionFeedBack;
    public NettyClientInitializer(Map<Integer, Reply> replies, ConnectionFeedBack connectionFeedBack) {
        super();
        this.replies = replies;
        this.connectionFeedBack = connectionFeedBack;
        //this.eventBus = eventBus;
    }

    @Override
    protected void initChannel(SocketChannel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        // Enable stream compression (you can remove these two if unnecessary)
        pipeline.addLast("deflater", new MyJZLibEncoder(ZlibWrapper.GZIP));
        pipeline.addLast("inflater", new MyJZLibDecoder(ZlibWrapper.GZIP));
        //pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));

        pipeline.addLast("framer", new DelimiterBasedFrameDecoder(
                ((1024*1024)*10), Delimiters.lineDelimiter()));


        pipeline.addLast("decoder", new StringDecoder());
        pipeline.addLast("encoder", new StringEncoder());

        // and then business logic.
        pipeline.addLast("handler", new NettyClientHandler(replies, connectionFeedBack));        
    }
    
}
