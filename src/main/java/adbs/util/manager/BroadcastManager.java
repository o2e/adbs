package adbs.util.manager;

import adbs.device.AdbDevice;
import adbs.exception.RemoteException;
import org.apache.commons.lang3.StringUtils;

public class BroadcastManager {

    private final AdbDevice device;

    private final ShellManager shell;

    public BroadcastManager(AdbDevice device) {
        this.device = device;
        this.shell = new ShellManager(device);
    }

    public void broadcast(String action, String data) throws Exception {
        broadcast(action, "-d", data);
    }

    public void broadcast(String action, String... args) throws Exception {
        String[] buildArgs = new String[args.length + 3];
        buildArgs[0] = "broadcast";
        buildArgs[1] = "-a";
        buildArgs[2] = action;
        System.arraycopy(args, 0, buildArgs, 3, args.length);
        String result = shell.shell("am", buildArgs);
        result = StringUtils.trim(result);
        if (!result.contains("Broadcast completed")) {
            throw new RemoteException(result);
        }
    }

}
