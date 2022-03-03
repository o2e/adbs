package adbs.util.manager;

import adbs.device.AdbDevice;
import adbs.exception.RemoteException;

import java.util.concurrent.TimeUnit;

public class ShellManager {

    private final AdbDevice device;

    public ShellManager(AdbDevice device) {
        this.device = device;
    }

    public String shell(String cmd, String... args) throws Exception {
        try {
            return device.shell(cmd, args).get(30, TimeUnit.SECONDS);
        } catch (Throwable cause) {
            throw new RemoteException(cause.getMessage(), cause);
        }
    }

}
