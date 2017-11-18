/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tp.com;

import classes.Reply;
import classes.UniqueId;
import com.fasterxml.uuid.Generators;
import com.nurkiewicz.asyncretry.AsyncRetryExecutor;
import com.nurkiewicz.asyncretry.RetryExecutor;
import interfaces.ConnectionFeedBack;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import tp.NettyClientInitializer;

/**
 *
 * @author madfooatcom
 */
public class Client {
    private final String remotehost;
    private final int port; 
    private final EventLoopGroup group;
    private final Bootstrap bootstrap;
    private Channel channel;
    private final UniqueId uniqueId;
    private final Map<String, Reply> replies;
    private final ScheduledExecutorService scheduler;
    private final RetryExecutor executor;
    //private int tryCount = 0;
    //private final int maxTryCount = 99;
    private final ConnectionFeedBack connectionFeedBack;
    private final ExecutorService pool;
    public Client(String remotehost, int port, ConnectionFeedBack connectionFeedBack) {
        int processors = Runtime.getRuntime().availableProcessors();
        pool = Executors.newFixedThreadPool(processors);
        this.connectionFeedBack = connectionFeedBack;
        uniqueId = new UniqueId();
        this.remotehost = remotehost;
        this.port = port;        
        group = new NioEventLoopGroup();
        bootstrap = new Bootstrap();     
        replies = new ConcurrentHashMap();
        scheduler = Executors.newScheduledThreadPool(1);
        executor = new AsyncRetryExecutor(scheduler).
                    retryOn(Exception.class).
                    withExponentialBackoff(1000, 2).     //500ms times 2 after each retry
                    withMaxDelay(10000).               //10 seconds
                    withUniformJitter().                //add between +/- 100 ms randomly
                    withMaxRetries(10);        
        ini(connectionFeedBack);
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
    public CompletableFuture<Boolean> tryToConnect(){
        CompletableFuture<Boolean> futureResult  =  new CompletableFuture();
        final CompletableFuture<Channel> tryTask = executor.getWithRetry(() ->{
           return connect(); 
        });
        tryTask.thenAccept(ch ->{
            channel = ch;
            System.out.println("Connected! " + ch.toString());
            futureResult.complete(Boolean.TRUE);
        });        
        CompletableFuture<Channel> exceptionally = tryTask.exceptionally(throwable ->{
            channel = null;
            System.out.println("[Error] "+throwable.getMessage());
            futureResult.complete(Boolean.FALSE);
            return null;
        });        
        //channel = exceptionally.get();
        return futureResult;
    }
    public String send(String msg){
        String filter = msg.replace("\n", "").replace("\r", "");  
        //final int id = uniqueId.getUniqueId();
        final String id = Generators.randomBasedGenerator().generate().toString();
        replies.put(id, new Reply());
        String m = filter.replaceAll(">\\s*<", "><");//encryption.encrypt(id+msg);//
        channel.writeAndFlush( id+m + "\r\n");
        return id;
    }
    public CompletableFuture<Boolean> sendwithoutRply(final String msg, final int timeout, final TimeUnit tu){
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
            if(!channel.isOpen()){
                tryToConnect();
            }
            final String finalMsg = id+m + "\r\n";
            channel.writeAndFlush( finalMsg).addListener((ChannelFutureListener) (ChannelFuture future) -> {
                if (future.isSuccess()) {
                    statefuture.complete(true);
                }else{
                    System.err.println("[Error][unable to send msg] "+finalMsg);
                    statefuture.complete(false);
                }                
            });
            return statefuture;
           // return "";
        //}, pool);
        //return future.join();                   
    }
    public CompletableFuture<String> sendwithRplyInFuture(final String msg, final int timeout, final TimeUnit tu){
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
            if(channel==null){
                tryToConnect().join();
            }
            if(!channel.isOpen()){
                tryToConnect().join();
            }
            channel.writeAndFlush( id+m + "\r\n");
        return result;
    }
    public CompletableFuture<String> sendwithRply(final String msg, final int timeout, final TimeUnit tu){
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
            if(channel==null){
                tryToConnect().join();
            }
            if(!channel.isOpen()){
                tryToConnect();
            }
            channel.writeAndFlush( id+m + "\r\n");
        return result;            
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

    public Map<String, Reply> getReplies() {
        return replies;
    }

}    

