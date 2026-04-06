package io.github.jqssun.displayextend.shizuku;

public class WindowingMode {
    /** Windowing mode is currently not defined. */
    public static final int WINDOWING_MODE_UNDEFINED = 0;
    /** Occupies the full area of the screen or the parent container. */
    public static final int WINDOWING_MODE_FULLSCREEN = 1;
    /** Always on-top (always visible). of other siblings in its parent container. */
    public static final int WINDOWING_MODE_PINNED = 2;
    /** Can be freely resized within its parent container. */
    // TODO: Remove once freeform is migrated to wm-shell.
    public static final int WINDOWING_MODE_FREEFORM = 5;
    /** Generic multi-window with no presentation attribution from the window manager. */
    public static final int WINDOWING_MODE_MULTI_WINDOW = 6;
    public static String getWindowingMode(int displayId) {
        int windowingMode = ServiceUtils.getWindowManager().getWindowingMode(displayId);
        switch (windowingMode) {
            case 0:
                return "UNDEFINED";
            case 1:
                return "FULLSCREEN";
            case 2:
                return "PINNED";
            case 5:
                return "FREEFORM";
            case 6:
                return "MULTI_WINDOW ";
            default:
                return "UNKNOWN";
        }
    }
}
