/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp;

import classes.Reply;
import classes.UniqueId;
import com.google.common.collect.Maps;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;
import com.nurkiewicz.asyncretry.RetryExecutor;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 *
 * @author Tareq
 */
public class Client {
    private final String remotehost;
    private final int port; 
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private Channel channel;
    private final UniqueId uniqueId;
    private final Map<Integer, Reply> replies;
    private final ScheduledExecutorService scheduler;
    private final RetryExecutor executor;
    //private int tryCount = 0;
    //private final int maxTryCount = 99;
    private final ConnectionFeedBack connectionFeedBack;

    public Client(String remotehost, int port, ConnectionFeedBack connectionFeedBack) {
        this.connectionFeedBack = connectionFeedBack;
        uniqueId = new UniqueId();
        this.remotehost = remotehost;
        this.port = port;        
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();     
        replies = Maps.newConcurrentMap();
        scheduler = Executors.newScheduledThreadPool(2);
        executor = new AsyncRetryExecutor(scheduler).
                    retryOn(Exception.class).
                    withExponentialBackoff(500, 2).     //500ms times 2 after each retry
                    withMaxDelay(20000).               //10 seconds
                    withUniformJitter().                //add between +/- 100 ms randomly
                    withMaxRetries(10);        
        ini(this.connectionFeedBack);
    }
    private void ini(ConnectionFeedBack connectionFeedBack){
        bootstrap.group(group)
            .channel(NioSocketChannel.class)
            .handler(new NettyClientInitializer(replies, connectionFeedBack));   
    }
    private Channel connect() throws InterruptedException{
        System.out.println("Try to Connect on : " +remotehost+":"+port );
        return bootstrap.connect(remotehost, port).sync().channel();
    }
    public void tryToConnect(){
        final CompletableFuture<Channel> tryTask = executor.getWithRetry(() ->{
           return connect(); 
        });
        tryTask.thenAccept(ch ->{
            channel = ch;
            System.out.println("Connected! " + ch.toString());
        });        
        CompletableFuture<Channel> exceptionally = tryTask.exceptionally(throwable ->{
            channel = null;
            System.out.println("[Error] "+throwable.getMessage());
            return null;
        });        
        //channel = exceptionally.get();
    }
    public int send(String msg){
        String filter = msg.replace("\n", "").replace("\r", "");  
        final int id = uniqueId.getUniqueId();
        replies.put(id, new Reply());
        String m = filter.replaceAll(">\\s*<", "><");//encryption.encrypt(id+msg);//
        channel.writeAndFlush( id+m + "\r\n");
        return id;
    }
    public String sendwithRply(String msg, int timeout, final TimeUnit tu){
        String filter = msg.replace("\n", "").replace("\r", "");  
        final int id = uniqueId.getUniqueId();
        final Reply r = new Reply();
        replies.put(id, r);
        String m = filter.replaceAll(">\\s*<", "><");//encryption.encrypt(id+msg);//
        channel.writeAndFlush( id+m + "\r\n");
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            try {
                r.waitForReply(timeout, tu);
                System.out.println(r.getMessage());
                return r.getMessage();
            } catch (InterruptedException | TimeoutException ex) {
                System.err.println("[Error] "+ex.getMessage());
                throw new RuntimeException(ex.getCause());
            }
            
        });
        final CompletableFuture<String> recovered = future.exceptionally(throwable ->{
            return null;
        });                
        return future.join();
    }
    public void shutdown(){
        group.shutdownGracefully();
    }
    
    public void close(){
        channel.close();
    }

    public String getHost() {
        return remotehost;
    }

    public int getPort() {
        return port;
    }
    public boolean isConnected(){
        return channel.isOpen();
    }

    public Channel getChannel() {
        return channel;
    }

    public Map<Integer, Reply> getReplies() {
        return replies;
    }

}

