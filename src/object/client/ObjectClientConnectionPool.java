/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.pool.ChannelHealthChecker;
import io.netty.channel.pool.ChannelPoolHandler;
import io.netty.channel.pool.FixedChannelPool;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.util.concurrent.Future;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import interfaces.ConnectionFeedBack;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 *
 * @author Tareq
 */
public class ObjectClientConnectionPool {
    private final  EventLoopGroup group;
    private final  Bootstrap bootstrap;
    private  FixedChannelPool channelPool; 
    private final AtomicBoolean closed = new AtomicBoolean(false);
    
    
    public ObjectClientConnectionPool(String HOST, int PORT, ConnectionFeedBack onnectionFeedBack, ClassLoader classLoader) {

        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        try {
            
            bootstrap.group(group).channel(NioSocketChannel.class).remoteAddress(HOST, PORT);

            channelPool = new FixedChannelPool(bootstrap,
                    new ChannelPoolHandler() {
                        @Override
                        public void channelReleased(Channel ch)
                                throws Exception {
                            System.out.println("-->channelReleased");
                        }
                        
                        @Override
                        public void channelCreated(Channel ch) throws Exception {
                            ChannelPipeline pipeline = ch.pipeline();
                            
                            pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
                            pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
                            pipeline.addLast("encoder", new ObjectEncoder());
                            pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.cacheDisabled(classLoader)));
                            pipeline.addLast("handler", new ObjectClientHandler(onnectionFeedBack));
                            

                        }
                        
                        @Override
                        public void channelAcquired(Channel ch)
                                throws Exception {
                            System.out.println("channelAcquired");
                        }
                    }, ChannelHealthChecker.ACTIVE,FixedChannelPool.AcquireTimeoutAction.FAIL, 6000, // TODO make this configurable
                    500, Integer.MAX_VALUE, true); 

        } finally {
            //group.shutdownGracefully();
        }
    }
    public CompletableFuture<Channel> acquire() {
        if (closed.get()) {
            throw new IllegalStateException("Pool was already closed. It can no longer be used.");
        }
        Future<Channel> acquire = channelPool.acquire();
        CompletableFuture<Channel> client = new CompletableFuture<>();
        try{
            acquire = channelPool.acquire();
        }catch(IllegalStateException ex){
             client.completeExceptionally(ex);
        }

        acquire.addListener((Future<Channel> future) -> {
            if (future.isSuccess()) {
                Channel channel = future.get();
                client.complete(channel);
            } else {
                client.completeExceptionally(future.cause());
            }
        });
        return client;
    }
    public CompletableFuture<Void> release(Channel client) {
        if (closed.get()) {
            throw new IllegalStateException("Pool was already closed. It can no longer be used.");
        }

        if (client instanceof Channel) {
            //BloomdClientImpl bloomdClient = (BloomdClientImpl) client;
            Channel ch = client;

            CompletableFuture<Void> releaseFuture = new CompletableFuture<>();

            channelPool.release(ch).addListener(future -> {
                if (future.isSuccess()) {
                    releaseFuture.complete(null);
                } else {
                    releaseFuture.completeExceptionally(future.cause());
                }
            });
            return releaseFuture;
        } else {
            throw new IllegalArgumentException("Unrecognized client instance: " + client);
        }
    }
    
    public Future<?> closeConnections() {
        closed.set(true);
        channelPool.close();
        return group.shutdownGracefully();
    }     

}
