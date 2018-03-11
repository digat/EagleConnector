/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;

import classes.Reply;
import com.fasterxml.uuid.Generators;
import interfaces.ConnectionFeedBack;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import object.client.ext.States;
import object.client.ext.TryConnectException;

/**
 *
 * @author Tareq
 */
public class Client {

    private final String remotehost;
    private final int port;
    private final static EventLoopGroup GROUP = new NioEventLoopGroup();
    private final Bootstrap bootstrap;
    private final AtomicReference<Channel> channel = new AtomicReference<>(null);
    private final Map<String, Reply> replies;
    //private int tryCount = 0;
    //private final int maxTryCount = 99;
    private final ConnectionFeedBack connectionFeedBack;
    private States.ConnectioState connectioState = States.ConnectioState.nil;
    private final ExecutorService EXECUTOR = Executors.newFixedThreadPool(1);
    private final AtomicBoolean reTryFlag = new AtomicBoolean(true);

    public static class Monitor extends Thread {

        private final BlockingQueue<States.ConnectioState> queue = new LinkedBlockingQueue();

        public Monitor() {
            super("Monitor Thraed");
        }

        @Override
        public void run() {
            while (!this.isInterrupted()) {
                try {
                    States.ConnectioState cs = queue.take();
                    System.out.println("now it is : " + cs.name());
                } catch (InterruptedException ex) {
                    System.out.println("[Error] " + ex.getMessage());
                }
            }
        }

        public void state(States.ConnectioState connectioState) {
            queue.add(connectioState);
        }
    }

    public void stopRetry() {
        reTryFlag.set(false);
    }

    public Client(String remotehost, int port, ConnectionFeedBack connectionFeedBack) {
        //int processors = Runtime.getRuntime().availableProcessors();
        //pool = Executors.newFixedThreadPool(processors);
        this.connectionFeedBack = connectionFeedBack;
        //uniqueId = new UniqueId();
        this.remotehost = remotehost;
        this.port = port;

        bootstrap = new Bootstrap();
        bootstrap.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2000);
        replies = new ConcurrentHashMap();
        ini();
    }

    private void ini() {
        bootstrap.group(GROUP)
                .channel(NioSocketChannel.class)
                .handler(new NettyClientInitializer(replies, setupConnectionFeedBack()));
    }

    public void setConnectioState(States.ConnectioState connectioState) {
        this.connectioState = connectioState;
    }

    public void tryToConnect() {
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

    private ConnectionFeedBack setupConnectionFeedBack() {
        ConnectionFeedBack cfb = new ConnectionFeedBack() {
            @Override
            public void connectionActive() {
                setConnectioState(States.ConnectioState.connected);
                connectionFeedBack.connectionActive();
            }

            @Override
            public void connectionClosed() {
                setConnectioState(States.ConnectioState.disconected);
                connectionFeedBack.connectionClosed();
                try {
                    TimeUnit.SECONDS.sleep(5);
                } catch (InterruptedException ex) {
                }
                if (reTryFlag.get()) {
                    connectionFeedBack.onTryToConnect();
                    tryToConnect();
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

    public String send(String msg) {
        String filter = msg.replace("\n", "").replace("\r", "");
        //final int id = uniqueId.getUniqueId();
        final String id = Generators.randomBasedGenerator().generate().toString();
        replies.put(id, new Reply());
        String m = filter.replaceAll(">\\s*<", "><");//encryption.encrypt(id+msg);//
        channel.get().writeAndFlush(id + m + "\r\n");
        return id;
    }

    public CompletableFuture<Boolean> sendwithoutRply(final String msg, final int timeout, final TimeUnit tu) {
        //CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
        final CompletableFuture<Boolean> statefuture = new CompletableFuture<>();
        String filter = msg.replace("\n", "").replace("\r", "");
        //final int id = uniqueId.getUniqueId();
        final String id = Generators.randomBasedGenerator().generate().toString();
        final Reply r = new Reply();
        //System.err.println(msg);
        replies.put(id, r);
        String m = filter.replaceAll(">\\s*<", "><");//encryption.encrypt(id+msg);//
        m = m.replaceAll("(&(?!amp;))", "&amp;");
        m = m.trim();
        final String finalMsg = id + m + "\r\n";
        channel.get().writeAndFlush(finalMsg).addListener((ChannelFutureListener) (ChannelFuture future) -> {
            if (future.isSuccess()) {
                statefuture.complete(true);
            } else {
                System.err.println("[Error][unable to send msg] " + finalMsg);
                statefuture.complete(false);
            }
        });
        return statefuture;
    }

    public CompletableFuture<String> sendwithRplyInFuture(final String msg, final int timeout, final TimeUnit tu) {
        CompletableFuture<String> result = new CompletableFuture<>();
        String filter = msg.replace("\n", "").replace("\r", "");
        //final int id = uniqueId.getUniqueId();
        final String id = Generators.randomBasedGenerator().generate().toString();
        final Reply r = new Reply();
        r.setResult(result);
        //System.err.println(msg);
        replies.put(id, r);
        String m = filter.replaceAll(">\\s*<", "><");//encryption.encrypt(id+msg);//
        m = m.replaceAll("(&(?!amp;))", "&amp;");
        m = m.trim();
        channel.get().writeAndFlush(id + m + "\r\n");
        return result;
    }

    public CompletableFuture<String> sendwithRply(final String msg, final int timeout, final TimeUnit tu) {
        CompletableFuture<String> result = new CompletableFuture<>();
        //CompletableFuture<String> future = new CompletableFuture<>();
        String filter = msg.replace("\n", "").replace("\r", "");
        //final int id = uniqueId.getUniqueId();
        final String id = Generators.randomBasedGenerator().generate().toString();
        final Reply r = new Reply();
        r.setResult(result);
        //System.err.println(msg);
        replies.put(id, r);
        String m = filter.replaceAll(">\\s*<", "><");//encryption.encrypt(id+msg);//
        m = m.replaceAll("(&(?!amp;))", "&amp;");
        m = m.trim();
        channel.get().writeAndFlush(id + m + "\r\n");
        return result;
    }

    public void shutdown() {
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

    public Map<String, Reply> getReplies() {
        return replies;
    }

}
