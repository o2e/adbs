package adbs.util.manager;

import adbs.device.AdbDevice;
import adbs.entity.sync.SyncDent;
import adbs.exception.RemoteException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;

public class FileManager {

    private final AdbDevice device;

    private final BroadcastManager bm;

    private final ShellManager shell;

    public FileManager(AdbDevice device) {
        this.device = device;
        this.bm = new BroadcastManager(device);
        this.shell = new ShellManager(device);
    }

    public void pull(String src, OutputStream dest) throws Exception {
        device.pull(src, dest).get();
    }

    public void push(InputStream src, String dest, int mode, int mtime) throws Exception {
        device.push(src, dest, mode, mtime).get();
    }

    public void pull(String src, File dest) throws Exception {
        device.pull(src, dest).get();
    }

    public void push(File src, String dest) throws Exception {
        device.push(src, dest).get();
    }

    public void delete(String path) throws Exception {
        String result = shell.shell("rm", "-f", path);
        result = StringUtils.trim(result);
        if (StringUtils.isNotEmpty(result)) {
            throw new RemoteException(result);
        }
    }

    public SyncDent[] list(String path) throws Exception {
        return device.list(path).get();
    }

    public void pushMedia(InputStream source, String remote, int mode, int lastModified) throws Exception {
        push(source, remote, mode, lastModified);
        bm.broadcast("android.intent.action.MEDIA_SCANNER_SCAN_FILE", "file://" + remote);
    }

    public void pushMedia(File local, String remote) throws Exception {
        push(local, remote);
        bm.broadcast("android.intent.action.MEDIA_SCANNER_SCAN_FILE", "file://" + remote);
    }

    public void deleteMedia(String path) throws Exception {
        delete(path);
        bm.broadcast("android.intent.action.MEDIA_SCANNER_SCAN_FILE", "file://" + path);
    }

    public void mkdir(String path) throws Exception {
        shell.shell("mkdir", path);
    }

    public void chmod(String path, String mod) throws Exception {
        shell.shell("chmod", mod, path);
    }
}
