package adbs.connection;

import adbs.channel.AdbChannel;
import adbs.channel.AdbChannelAddress;
import adbs.channel.AdbChannelInitializer;
import adbs.constant.Command;
import adbs.device.SocketAdbDevice;
import adbs.entity.AdbPacket;
import adbs.util.ChannelUtil;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandler;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.ReferenceCountUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class AdbChannelProcessor extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(AdbChannelProcessor.class);

    private final SocketAdbDevice device;

    private final AtomicInteger channelIdGen;

    private final Map<CharSequence, AdbChannelInitializer> reverseMap;

    public AdbChannelProcessor(
            SocketAdbDevice device,
            AtomicInteger channelIdGen,
            Map<CharSequence, AdbChannelInitializer> reverseMap
            ) {
        this.device = device;
        this.channelIdGen = channelIdGen;
        this.reverseMap = reverseMap;
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Uncaught exception: {}", cause.getMessage(), cause);
        ctx.fireExceptionCaught(cause);
    }

    private boolean fireChannelMessage(ChannelHandlerContext ctx, AdbPacket message) throws Exception {
        String handlerName = ChannelUtil.getChannelName(message.arg1);
        ChannelHandlerContext channelContext = ctx.pipeline().context(handlerName);
        if (channelContext != null) {
            ChannelInboundHandler handler = (ChannelInboundHandler) channelContext.handler();
            handler.channelRead(channelContext, message);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (!(msg instanceof AdbPacket)) {
            ReferenceCountUtil.release(msg);
            return;
        }
        AdbPacket message = (AdbPacket) msg;
        switch (message.command) {

            case A_OPEN:
                try {
                    int remoteId = message.arg0;
                    int localId = channelIdGen.getAndIncrement();
                    //-1是因为最后有一个\0
                    byte[] payload = new byte[message.size - 1];
                    message.payload.readBytes(payload);
                    String destination = new String(payload, StandardCharsets.UTF_8);
                    AdbChannelInitializer initializer = reverseMap.get(destination);
                    String channelName = ChannelUtil.getChannelName(localId);
                    AdbChannel channel = new AdbChannel(ctx.channel(), 0, remoteId);
                    channel.bind(new AdbChannelAddress(destination, localId)).addListener(f -> {
                        if (f.cause() == null) {
                            initializer.initChannel(channel);
                            ctx.pipeline().addLast(channelName, channel);
                            channel.eventLoop().register(channel);
                        } else {
                            ctx.writeAndFlush(new AdbPacket(Command.A_CLSE, 0, message.arg0));
                        }
                    });
                } catch (Throwable cause) {
                    ctx.writeAndFlush(new AdbPacket(Command.A_CLSE, 0, message.arg0));
                } finally {
                    ReferenceCountUtil.safeRelease(message);
                }
                break;

            case A_OKAY:
                fireChannelMessage(ctx, message);
                break;

            case A_WRTE:
                ctx.writeAndFlush(new AdbPacket(Command.A_OKAY, message.arg1, message.arg0));
                fireChannelMessage(ctx, message);
                break;

            case A_CLSE:
                ctx.writeAndFlush(new AdbPacket(Command.A_CLSE, message.arg1, message.arg0));
                fireChannelMessage(ctx, message);
                break;

            default:
                ctx.fireExceptionCaught(new Exception("Unexpected channel command:" + message.command));
                break;

        }
    }
}