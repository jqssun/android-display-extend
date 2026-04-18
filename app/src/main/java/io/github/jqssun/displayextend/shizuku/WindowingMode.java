package io.github.jqssun.displayextend.shizuku;

public class WindowingMode {
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
