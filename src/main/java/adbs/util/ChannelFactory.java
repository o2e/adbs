package adbs.util;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;

public interface ChannelFactory {

    ChannelFuture newChannel(ChannelInitializer<Channel> initializer);

}
