package adbs.util.manager;

import adbs.device.AdbDevice;
import adbs.exception.RemoteException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PackageManager {

    private static final String SUCCESS = "Success";

    private final AdbDevice device;

    private final FileManager fm;

    private final ShellManager shell;

    public PackageManager(AdbDevice device) {
        this.device = device;
        this.fm = new FileManager(device);
        this.shell = new ShellManager(device);
    }

    private static String[] expandArgs(String head, String tail, String... args) {
        String[] expandArgs = new String[args.length + 2];
        expandArgs[0] = head;
        System.arraycopy(args, 0, expandArgs, 1, args.length);
        expandArgs[args.length + 1] = tail;
        return expandArgs;
    }

    public List<String> list(Integer userId) throws Exception {
        String[] args;
        if (userId == null) {
            args = new String[]{"list", "packages"};
        } else {
            args = new String[]{"list", "packages", "--user", String.valueOf(userId)};
        }
        String result = shell.shell("pm", args);
        List<String> packages = new ArrayList<>();
        for(String pkg : result.split("\r|\n")) {
            pkg = StringUtils.trim(pkg);
            if (StringUtils.isEmpty(pkg)) {
                continue;
            }
            packages.add(StringUtils.removeStart(pkg, "package:"));
        }
        return packages;
    }

    public List<String> list() throws Exception {
        return list(null);
    }

    public String path(String pkg, Integer userId) throws Exception {
        String[] args;
        if (userId == null) {
            args = new String[]{"path", pkg};
        } else {
            args = new String[]{"path", "--user", String.valueOf(userId), pkg};
        }
        args = ArrayUtils.addAll(args, "|", "sed", "-e", "s/^package://");
        String result = shell.shell("pm", args);
        return StringUtils.trim(result);
    }

    public String path(String pkg) throws Exception {
        return path(pkg, null);
    }

    public void install(File apk, Integer userId, String... args) throws Exception {
        String remote = "/data/local/tmp/" + apk.getName();
        fm.push(apk, remote);
        install(remote, userId, args);
    }

    public void install(File apk, String... args) throws Exception {
        install(apk, null, args);
    }

    public void install(String remote, Integer userId, String... args) throws Exception {
        if (userId != null) {
            args = ArrayUtils.addAll(args, "--user", String.valueOf(userId), "-r");
        }
        String result = shell.shell("pm", expandArgs("install", remote, args));
        result = StringUtils.trim(result);
        fm.delete(remote);
        if (!result.startsWith(SUCCESS)) {
            throw new RemoteException(result);
        }
    }

    public void install(String remote, String... args) throws Exception {
        install(remote, null, args);
    }

    public void uninstall(String pkg, Integer userId, String... args) throws Exception {
        if (userId != null) {
            args = ArrayUtils.addAll(args, "--user", String.valueOf(userId));
        }
        String result = shell.shell("pm", expandArgs("uninstall", pkg, args));
        result = StringUtils.trim(result);
        if (!result.startsWith(SUCCESS)) {
            throw new RemoteException(result);
        }
    }

    public void uninstall(String pkg, String... args) throws Exception {
        uninstall(pkg, null, args);
    }

    public void launch(String pkg) throws Exception {
        String result = shell.shell("monkey", "-p", pkg, "-c", "android.intent.category.LAUNCHER", "1");
        result = StringUtils.trim(result);
        if (result.contains("elapsed time")) {
            throw new RemoteException(result);
        }
    }

    public void launch(String pkg, String activity) throws Exception {
        launch(pkg, activity, null);
    }

    public void launch(String pkg, String activity, Integer userId) throws Exception {
        launch(pkg, activity, null, userId);
    }

    public void launch(String pkg, String activity, String data, Integer userId, String... args) throws Exception {
        List<String> argList = new ArrayList<>();
        argList.add("start");
        argList.add("-n");
        argList.add(pkg + "/" + activity);
        if (data != null) {
            argList.add("-d");
            argList.add(data);
        }
        if (userId != null) {
            argList.add("--user");
            argList.add(String.valueOf(userId));
        }
        Collections.addAll(argList, args);
        String[] fullArgs = new String[argList.size()];
        argList.toArray(fullArgs);
        String result = shell.shell("am", fullArgs);
        result = StringUtils.trim(result);
        if (result.contains("Error type")) {
            throw new RemoteException(result);
        }
    }

    public void launch(Integer userId, String... args) throws Exception {
        String[] allArgs;
        if (userId == null) {
            allArgs = new String[]{"start"};
        } else {
            allArgs = new String[]{"start", "--user", String.valueOf(userId)};
        }
        allArgs = ArrayUtils.addAll(allArgs, args);
        String result = shell.shell("am", allArgs);
        result = StringUtils.trim(result);
        if (result.contains("Error type")) {
            throw new RemoteException(result);
        }
    }

    public void launch(String... args) throws Exception {
        launch(null, args);
    }

    public void clear(String pkg, Integer userId) throws Exception {
        String[] args;
        if (userId == null) {
            args = new String[]{"clear", pkg};
        } else {
            args = new String[]{"clear", "--user", String.valueOf(userId), pkg};
        }
        String result = shell.shell("am", args);
        result = StringUtils.trim(result);
        if (!SUCCESS.equals(result)) {
            throw new RemoteException(result);
        }
    }

    public void clear(String pkg) throws Exception {
        clear(pkg, null);
    }

    public void stop(String pkg, Integer userId) throws Exception {
        String[] args;
        if (userId == null) {
            args = new String[]{"force-stop", pkg};
        } else {
            args = new String[]{"force-stop", "--user", String.valueOf(userId), pkg};
        }
        String result = shell.shell("am", args);
        result = StringUtils.trim(result);
        if (StringUtils.isNotEmpty(result)) {
            throw new RemoteException(result);
        }
    }

    public void stop(String pkg) throws Exception {
        stop(pkg, null);
    }
}
