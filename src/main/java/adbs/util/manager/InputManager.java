package adbs.util.manager;

import adbs.device.AdbDevice;

public class InputManager {

    private final AdbDevice device;

    private final ShellManager shell;

    public InputManager(AdbDevice device) {
        this.device = device;
        this.shell = new ShellManager(device);
    }

    public void input(String type, String... args) throws Exception {
        String[] inputArgs = new String[args.length + 1];
        inputArgs[0] = type;
        System.arraycopy(args, 0, inputArgs, 1, args.length);
        shell.shell("input", inputArgs);
    }

    public void tap(int x, int y) throws Exception {
        input("tap", String.valueOf(x), String.valueOf(y));
    }

    public void home() throws Exception {
        input("keyevent", "3");
    }

    public void menu() throws Exception {
        input("keyevent", "1");
    }

    public void back() throws Exception {
        input("keyevent", "4");
    }

    public void swipe(int xFrom, int yFrom, int xTo, int yTo, int duration) throws Exception {
        input(
                "swipe",
                String.valueOf(xFrom), String.valueOf(yFrom),
                String.valueOf(xTo), String.valueOf(yTo),
                String.valueOf(duration)
        );
    }

    public void input(String text) throws Exception {
        input("text", text);
    }

}
