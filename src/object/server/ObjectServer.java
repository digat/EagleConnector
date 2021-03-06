package object.server;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
import interfaces.ConnectionFeedBack;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.compression.ZlibCodecFactory;
import io.netty.handler.codec.compression.ZlibWrapper;
import io.netty.handler.codec.serialization.ClassResolvers;
import io.netty.handler.codec.serialization.ObjectDecoder;
import io.netty.handler.codec.serialization.ObjectEncoder;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import java.net.BindException;
import java.security.cert.CertificateException;
import javax.net.ssl.SSLException;
//import org.slf4j.LoggerFactory;
/**
 *
 * @author Tareq
 */
public class ObjectServer {
    //private static final org.slf4j.Logger ddsLogger = LoggerFactory.getLogger("dds");
    
     private final int PORT;
     private static final boolean SSL = System.getProperty("ssl") != null;
     private final ClassLoader classLoader;
     private EventLoopGroup bossGroup;
     private EventLoopGroup workerGroup;
     private ServerBootstrap server;

    public ObjectServer(int PORT, ClassLoader classLoader) {
        this.PORT = PORT;        
        this.classLoader = classLoader;
        
    }
    public void run(final ConnectionFeedBack connectionFeedBack) throws CertificateException, SSLException, InterruptedException, BindException {
        final SslContext sslCtx;
        if (SSL) {
            SelfSignedCertificate ssc = new SelfSignedCertificate();
            sslCtx = SslContextBuilder.forServer(ssc.certificate(), ssc.privateKey()).build();
        } else {
            sslCtx = null;
        }
        bossGroup = new NioEventLoopGroup(1);
        workerGroup = new NioEventLoopGroup();
        try {
            server = new ServerBootstrap();
            server.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new ChannelInitializer<SocketChannel>() {
                @Override
                public void initChannel(SocketChannel ch) throws Exception {
                    ChannelPipeline p = ch.pipeline();
                    p.addLast("deflater", ZlibCodecFactory.newZlibEncoder(ZlibWrapper.GZIP));
                    p.addLast("inflater", ZlibCodecFactory.newZlibDecoder(ZlibWrapper.GZIP));
                    if (sslCtx != null) {
                        p.addLast(sslCtx.newHandler(ch.alloc()));
                    }
                    p.addLast(
                            new ObjectEncoder(),
                            new ObjectDecoder(ClassResolvers.cacheDisabled(classLoader)),
                            new ObjectServerHandler(connectionFeedBack));
                }
             });

            // Bind and start to accept incoming connections.
            System.out.println("[ObjectServer]\t "+"Server listen to port "+PORT);
            server.bind(PORT).sync().channel().closeFuture().sync();
        }catch(InterruptedException ex){
                throw new RuntimeException("bind failed..");
       } 
        finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
        
    }
    public void stop(){
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();        
    }     
}
