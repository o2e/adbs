package adbs.util.manager;

import adbs.device.AdbDevice;
import adbs.exception.RemoteException;
import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ActivityManager {

    private static final Pattern CURRENT_ACTIVITY_PATTERN = Pattern.compile("^mResumedActivity: ActivityRecord\\{.+\\s+.+\\s+(.+)\\s+.+\\}$");

    private final AdbDevice device;

    private final ShellManager shell;

    public ActivityManager(AdbDevice device) {
        this.device = device;
        this.shell = new ShellManager(device);
    }

    public String getCurrentActivity() throws Exception {
        String result = shell.shell("dumpsys", "activity", "activities", "|", "grep", "mResumedActivity");
        result = StringUtils.trim(result);
        Matcher matcher = CURRENT_ACTIVITY_PATTERN.matcher(result);
        if (!matcher.matches()) {
            throw new RemoteException(result);
        }
        return matcher.group(1);
    }

}
