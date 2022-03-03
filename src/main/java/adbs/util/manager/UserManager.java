package adbs.util.manager;

import adbs.device.AdbDevice;
import adbs.entity.User;
import adbs.exception.RemoteException;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class UserManager {

    private static final Pattern USER_INFO_PATTERN = Pattern.compile("^UserInfo\\{(.+):(.+):(.+)\\}(.*)$");

    private static final Pattern CREATE_USER_PATTERN = Pattern.compile("^Success: created user id (.d+)$");

    private final AdbDevice device;

    private final ShellManager shell;

    public UserManager(AdbDevice device) {
        this.device = device;
        this.shell = new ShellManager(device);
    }

    public List<User> listUsers() throws Exception {
        String result = shell.shell("pm", "list", "users");
        List<User> users = new ArrayList<>();
        for(String userInfo : result.split("\n")) {
            userInfo = StringUtils.trim(userInfo);
            if (StringUtils.isEmpty(userInfo)) {
                continue;
            }
            Matcher matcher = USER_INFO_PATTERN.matcher(userInfo);
            if (!matcher.matches()) {
                continue;
            }
            boolean isRunning = "running".equals(StringUtils.trim(matcher.group(4)));
            User user = new User(
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2),
                    isRunning
            );
            users.add(user);
        }
        return users;
    }

    public int createUser(String name) throws Exception {
        String result = shell.shell("pm", "create-user", name);
        result = StringUtils.trim(result);
        Matcher matcher = CREATE_USER_PATTERN.matcher(result);
        if (!matcher.matches()) {
            throw new RemoteException();
        }
        String userIdStr = StringUtils.trim(matcher.group(1));
        return Integer.parseInt(userIdStr);
    }

    public void removeUser(int userId) throws Exception {
        String result = shell.shell("pm", "remove-user", String.valueOf(userId));
        result = StringUtils.trim(result);
        if (!result.startsWith("Success:")) {
            throw new RemoteException(result);
        }
    }

    public void switchUser(int userId) throws Exception {
        String result = shell.shell("am", "switch-user", String.valueOf(userId));
        result = StringUtils.trim(result);
        if (StringUtils.isNotEmpty(result)) {
            throw new RemoteException(result);
        }
    }

    public int getCurrentUser() throws Exception {
        String result = shell.shell("am", "get-current-user");
        result = StringUtils.trim(result);
        return Integer.parseInt(result);
    }

    public void startUser(int userId) throws Exception {
        String result = shell.shell("am", "start-user", "-w", String.valueOf(userId));
        result = StringUtils.trim(result);
        if (!result.startsWith("Success:")) {
            throw new RemoteException(result);
        }
    }

    public void stopUser(int userId) throws Exception {
        String result = shell.shell("am", "stop-user", "-w", "-f", String.valueOf(userId));
        result = StringUtils.trim(result);
        if (StringUtils.isNotEmpty(result)) {
            throw new RemoteException(result);
        }
    }

    public boolean isUserStopped(int userId) throws Exception {
        String result = shell.shell("am", "is-user-stopped", String.valueOf(userId));
        result = StringUtils.trim(result);
        return Boolean.parseBoolean(result);
    }
}
