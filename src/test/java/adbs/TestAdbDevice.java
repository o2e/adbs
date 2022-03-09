package adbs;

import adbs.device.AdbDevice;
import adbs.device.SocketAdbDevice;
import adbs.exception.RemoteException;
import adbs.util.AuthUtil;
import adbs.util.DeviceListener;
import adbs.util.manager.ActivityManager;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.util.concurrent.Promise;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPrivateCrtKey;
import java.util.concurrent.TimeUnit;

public class TestAdbDevice {

    private static final Logger logger = LoggerFactory.getLogger(TestAdbDevice.class);

    private static final RSAPrivateCrtKey privateKey;
    private static final byte[] publicKey;

    static {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("root").setLevel(Level.INFO);
        try {
            privateKey = AuthUtil.loadPrivateKey("adbkey");
            publicKey = AuthUtil.generatePublicKey(privateKey).getBytes(StandardCharsets.UTF_8);
        } catch (Throwable cause) {
            throw new RuntimeException("load private key failed:" + cause.getMessage(), cause);
        }
    }

    private static String[] expandArgs(String head, String tail, String... args) {
        String[] expandArgs = new String[args.length + 2];
        expandArgs[0] = head;
        System.arraycopy(args, 0, expandArgs, 1, args.length);
        expandArgs[args.length + 1] = tail;
        return expandArgs;
    }

    public static void install(AdbDevice device, File apk, String... args) throws Exception {
        String remote = "/data/local/tmp/" + apk.getName();
        device.push(apk, remote).await();
        String result = device.shell("pm", expandArgs("install", remote, args)).get();
        result = StringUtils.trim(result);
        device.shell("rm", "-f", remote).await();
        if (!result.startsWith("Success")) {
            throw new RemoteException(result);
        }
    }

    public static void main(String[] args) throws Exception {
        LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
        loggerContext.getLogger("root").setLevel(Level.DEBUG);
        AdbDevice device = new SocketAdbDevice("192.168.137.95", 5555, privateKey, publicKey);
//        ActivityManager am = new ActivityManager(device);
//        String currentActivity = am.getCurrentActivity();
//        System.out.println(currentActivity);
        Promise promise = device.eventLoop().newPromise();
        device.shell(
                "nohup",
                new String[] {
                        "/data/local/v2ray-android/v2ray",
                        "-c",
                        "/data/local/v2ray-android/config.json",
                        ">",
                        "/data/local/v2ray-android/run.log",
                        "2>&1",
                        "&",
                        "tail",
                        "-f",
                        "/data/local/v2ray-android/run.log"
                },
                true,
                new ChannelInboundHandlerAdapter() {
                    @Override
                    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                        String message = (String) msg;
                        logger.info("[{}] v2ray start, output={}", device.serial(), message);
                        if (message.contains("V2Ray") && message.contains("started")) {
                            //出现这个说明启动成功了
                            promise.trySuccess(null);
                            ctx.close();
                        }
                    }
                }
        );
        promise.get(30, TimeUnit.SECONDS);
        System.out.println("test");
    }
}
