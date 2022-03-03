package adbs.constant;

import org.apache.commons.lang3.StringUtils;

public enum DeviceMode {

    SYSTEM(StringUtils.EMPTY),
    BOOTLOADER("bootloader"),
    RECOVERY("recovery"),
    SIDELOAD("sideload"),
    SIDELOAD_AUTO_REBOOT("sideload-auto-reboot")
    ;

    private final String name;

    DeviceMode(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
