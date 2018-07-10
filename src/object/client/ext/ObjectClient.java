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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import object.client.ext.States.ConnectioState;
import oms.AcknowledgeReport;
import oms.TestRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author madfooatcom
 */
public class ObjectClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(ObjectClient.class);
    public static class Monitor extends Thread {

        private final BlockingQueue<ConnectioState> queue = new LinkedBlockingQueue();

        public Monitor() {
            super("Monitor Thraed");
        }

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                try {
                    ConnectioState cs = queue.take();
                    System.out.println("now it is : " + cs.name());
                } catch (InterruptedException ex) {
                    System.out.println("[Error] " + ex.getMessage());
                }
            }
        }

        public void state(ConnectioState connectioState) {
            queue.add(connectioState);
        }
    }
    private final String remotehost;
    private final int port;
    private final static EventLoopGroup GROUP = new NioEventLoopGroup();
    private final Bootstrap bootstrap;
    private ConnectionFeedBack connectionFeedBack;
    private ConnectioState connectioState = ConnectioState.nil;
    //private final ReentrantLock lock = new ReentrantLock();
    private CompletableFuture<Channel> futureChannel;
    private final AtomicReference<Channel> channel = new AtomicReference<>(null);
    //private final Monitor monitor; 
    private final ClassLoader classLoader;
    private final ConcurrentArrayList<Connection> listeners = new ConcurrentArrayList<>();
    private final int id;
    private final static ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);
    private ReConnectManager<ObjectClient> reConnectManager;
    private final AtomicBoolean reTryFlag = new AtomicBoolean(true);
    private ConnectionFeedBack listenConnectionFeedBack;


    public ObjectClient(int id, String remotehost, int port, ClassLoader classLoader) {
        this.id = id;
        this.remotehost = remotehost;
        this.port = port;
        this.classLoader = classLoader;
        //GROUP = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 6000);
        // At client side option is tcpNoDelay and at server child.tcpNoDelay
        //monitor= new Monitor ();

    }

    public void setup() {
        //monitor.start();
        setupConnectionFeedBack();
        init(classLoader);
        reConnectManager = new ReConnectManager(this, 5000L);
    }

    public void addConnectioStateListener(Connection connection) {
        listeners.add(connection);
    }

    public void removeConnectioStateListener(Connection connection) {
        listeners.remove(listeners.indexOf(connection));
    }

    private void fire(final ConnectioState connectioState) {
        listeners.iterator().forEachRemaining(cons -> {
            cons.setState(connectioState);
        });
    }
    public void start(){
        reConnectManager.resume();
    }

    protected void connectMe() {
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

    public ConnectioState getConnectioState() {
        return connectioState;
    }

    public void setConnectioState(ConnectioState connectioState) {
        this.connectioState = connectioState;
        fire(connectioState);
        //System.out.println("now it is : "+connectioState.name());
        //monitor.state(connectioState);
    }

    private void setupConnectionFeedBack() {
        connectionFeedBack = new ConnectionFeedBack() {
            @Override
            public void connectionActive() {
                //LOGGER.info("[ExeuteFeedBackService] connectionActive");
                setConnectioState(ConnectioState.connected);
                reConnectManager.setConnected(true);
                if(listenConnectionFeedBack!=null){
                    listenConnectionFeedBack.connectionActive();
                }
            }

            @Override
            public void connectionClosed() {
                LOGGER.info("[connectionClosed] connectionClosed");
                setConnectioState(ConnectioState.disconected);
                reConnectManager.setConnected(false);
                if (reTryFlag.get()) {
                    connectionFeedBack.onTryToConnect();
                }                    
                if(listenConnectionFeedBack!=null){
                    listenConnectionFeedBack.connectionClosed();
                }
                //start();
            }

            @Override
            public void connectionException(Throwable cause) {
                setConnectioState(ConnectioState.disconected);
                LOGGER.error("[connectionException] onRecivedError {}", cause);
                if(listenConnectionFeedBack!=null){
                    listenConnectionFeedBack.connectionException(cause);
                }
            }
            

            @Override
            public void onRecivedError(Throwable cause) {
                LOGGER.error("[onRecivedError] onRecivedError {}", cause);
               if(listenConnectionFeedBack!=null){
                    listenConnectionFeedBack.onRecivedError(cause);
                }                
            }

            @Override
            public void onRecivedData(Object message, Channel channel) {
                if (message instanceof AcknowledgeReport) {
                    //AcknowledgeReport ack = (AcknowledgeReport) message;
                    //LOGGER.info("[AcknowledgeReport] "+ack.getMsgKey());
                }
               if(listenConnectionFeedBack!=null){
                    listenConnectionFeedBack.onRecivedData(message, channel);
                }                  
            }
        };

    }

    public void setListenConnectionFeedBack(ConnectionFeedBack listenConnectionFeedBack) {
        this.listenConnectionFeedBack = listenConnectionFeedBack;
        reConnectManager.setListenConnectionFeedBack(this.listenConnectionFeedBack);
    }

    public ConnectionFeedBack getListenConnectionFeedBack() {
        return listenConnectionFeedBack;
    }
    

    private void init(ClassLoader classLoader) {
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
    // Handler should handle the IdleStateEvent triggered by IdleStateHandler.

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

    public CompletableFuture<Channel> getFutureChannel() {
        return futureChannel;
    }

    public Channel getChannel() {
        return channel.get();
    }

    public int getId() {
        return id;
    }
    public void stopRetry() {
        reTryFlag.set(false);
    }
}
