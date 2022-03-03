package adbs.device;

import adbs.util.ChannelFactory;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.interfaces.RSAPrivateCrtKey;
import java.util.concurrent.TimeUnit;

@SuppressWarnings({"rawtypes", "unchecked"})
public class SocketAdbDevice extends AbstractAdbDevice {

    private static final Logger logger = LoggerFactory.getLogger(SocketAdbDevice.class);

    private final String host;

    private final Integer port;

    public SocketAdbDevice(String host, Integer port, RSAPrivateCrtKey privateKey, byte[] publicKey) throws Exception {
        super(host + ":" + port, privateKey, publicKey, new SocketChannelFactory(host, port));
        this.host = host;
        this.port = port;
    }

    public String host() {
        return host;
    }

    public Integer port() {
        return port;
    }

    @Override
    public void close() throws Exception {
        try {
            super.close();
        } finally {
            SocketChannelFactory factory = (SocketChannelFactory) factory();
            try {
                factory.eventLoop.shutdownGracefully().get(30, TimeUnit.SECONDS);
            } catch (Exception e) {
                logger.error("shutdown event loop failed", e);
            }
        }
    }

    private static class SocketChannelFactory implements ChannelFactory {

        private final String host;

        private final int port;

        private final EventLoopGroup eventLoop;

        public SocketChannelFactory(String host, int port) {
            this.host = host;
            this.port = port;
            this.eventLoop = new NioEventLoopGroup(1, r -> {
                return new Thread(r, "AdbThread-" + host + ":" + port);
            });
        }

        @Override
        public ChannelFuture newChannel(ChannelInitializer<Channel> initializer) {
            Bootstrap bootstrap = new Bootstrap();
            return bootstrap.group(eventLoop)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 30000)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .option(ChannelOption.SO_REUSEADDR, true)
                    .option(ChannelOption.AUTO_CLOSE, true)
                    .handler(initializer)
                    .connect(host, port);
        }
    }
}
