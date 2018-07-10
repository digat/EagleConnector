/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package object.client.ext;

import interfaces.ConnectionFeedBack;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import oms.TestRequest;

/**
 *
 * @author Tareq
 */
public class ObjectNewClient {

    private final String remotehost;
    private final int port;
    private final static EventLoopGroup GROUP = new NioEventLoopGroup();
    private final Bootstrap bootstrap;
    private final AtomicReference<Channel> channel = new AtomicReference<>(null);
    private final ConnectionFeedBack connectionFeedBack;
    private States.ConnectioState connectioState = States.ConnectioState.nil;
    private final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);
    private final AtomicBoolean reTryFlag = new AtomicBoolean(true);
    private ReConnectManager<ObjectNewClient> reConnectManager;
    private final ClassLoader classLoader;

    public void stopRetry() {
        reTryFlag.set(false);
    }

    public ObjectNewClient(String remotehost, int port, ClassLoader classLoader, ConnectionFeedBack connectionFeedBack) {
        this.classLoader = classLoader;
        this.connectionFeedBack = connectionFeedBack;
        this.remotehost = remotehost;
        this.port = port;

        bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);

    }

    private void init() {
        bootstrap.group(GROUP)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) throws Exception {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast("idleStateHandler", new IdleStateHandler(40, 20, 0));
                        pipeline.addLast("DuplexHandler", new DuplexHandler());
                        pipeline.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
                        pipeline.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
                        pipeline.addLast("encoder", new ObjectEncoder());
                        pipeline.addLast("decoder", new ObjectDecoder(ClassResolvers.cacheDisabled(classLoader)));
                        pipeline.addLast("handler", new ObjectClientHandler(connectionFeedBack));
                    }
                });
    
    }

    public void setConnectioState(States.ConnectioState connectioState) {
        this.connectioState = connectioState;
    }
    public void setup() {
        init();
        reConnectManager = new ReConnectManager(this, 5000L);
    }    

    
    public void connectMe() {
        CompletableFuture<Channel> future = CompletableFuture.supplyAsync(() -> {
            return connect();
        }, EXECUTOR).whenComplete((result, ex) -> {
            if (result != null) {
                channel.set(result);
            } else {
                channel.set(null);
            }
        });
        future.join();
    }
    public void start(){
        reConnectManager.resume();
    }    

    private Channel connect() {
        try {
            //System.out.println("[connectioState] " + connectioState.name());
            //System.out.println("Try to Connect on : " + remotehost + ":" + port);
            CompletableFuture<Channel> futureResult = new CompletableFuture<>();
            ChannelFuture future = bootstrap.connect(remotehost, port);
            future.addListener((ChannelFutureListener) (ChannelFuture future1) -> {
                if (future1.isSuccess()) {
                    futureResult.complete(future1.channel());
                } else {
                    futureResult.completeExceptionally(new TryConnectException(remotehost, port));
                }
            });
            future.awaitUninterruptibly();
            return futureResult.join();
        } catch (Exception ex) {
            return null;
        }
    }

    private ConnectionFeedBack setupConnectionFeedBack() {
        ConnectionFeedBack cfb = new ConnectionFeedBack() {
            @Override
            public void connectionActive() {
                setConnectioState(States.ConnectioState.connected);
                reConnectManager.setConnected(true);
                connectionFeedBack.connectionActive();
                
            }

            @Override
            public void connectionClosed() {
                setConnectioState(States.ConnectioState.disconected);
                reConnectManager.setConnected(false);
                connectionFeedBack.connectionClosed();
                if (reTryFlag.get()) {
                    connectionFeedBack.onTryToConnect();
                }                
            }

            @Override
            public void connectionException(Throwable cause) {
                setConnectioState(States.ConnectioState.disconected);
                connectionFeedBack.connectionException(cause);
            }

            @Override
            public void onRecivedError(Throwable cause) {
                connectionFeedBack.onRecivedError(cause);
            }

            @Override
            public void onRecivedData(Object message, Channel channel) {
                connectionFeedBack.onRecivedData(message, channel);
            }
        };
        return cfb;
    }

    public States.ConnectioState getConnectioState() {
        return connectioState;
    }




    public void shutdown() {
        reConnectManager.cancel();
        
        GROUP.shutdownGracefully();
    }

    public void close() {
        channel.get().close();
    }

    public String getHost() {
        return remotehost;
    }

    public int getPort() {
        return port;
    }

    public boolean isConnected() {
        return channel.get().isOpen();
    }

    public Channel getChannel() {
        return channel.get();
    }

    public class DuplexHandler extends ChannelDuplexHandler {

        @Override
        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
            if (evt instanceof IdleStateEvent) {
                IdleStateEvent e = (IdleStateEvent) evt;
                if (e.state() == IdleState.READER_IDLE) {
                    //ctx.close();
                } else if (e.state() == IdleState.WRITER_IDLE) {
                    //System.out.println("[Ping] [userEventTriggered]");
                    ctx.writeAndFlush(new TestRequest("p"));
                }
            }
        }
    }

}
