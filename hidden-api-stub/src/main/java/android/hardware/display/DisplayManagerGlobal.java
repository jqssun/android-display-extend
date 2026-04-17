package android.hardware.display;

import android.content.Context;
import android.view.Display;

public class DisplayManagerGlobal {
    public static DisplayManagerGlobal getInstance() {
        throw new RuntimeException();
    }

    public Display getRealDisplay(int displayId) {
        throw new RuntimeException("stub!");
    }

    public VirtualDisplay createVirtualDisplayWrapper(VirtualDisplayConfig virtualDisplayConfig,
                                                      IVirtualDisplayCallback callbackWrapper, int displayId) {
        throw new RuntimeException("stub!");
    }

    public VirtualDisplay createVirtualDisplayWrapper(VirtualDisplayConfig virtualDisplayConfig, Context windowContext,
                                                      IVirtualDisplayCallback callbackWrapper, int displayId) {
        throw new RuntimeException("stub!");
    }

    public void setUserPreferredDisplayMode(int displayId, Display.Mode mode) {
        throw new RuntimeException("stub!");
    }

    public void setUserPreferredDisplayMode(int displayId, Display.Mode mode, boolean storeMode) {
        throw new RuntimeException("stub!");
    }

    public void resetUserPreferredDisplayMode(int displayId) {
        throw new RuntimeException("stub!");
    }

    public Display.Mode getUserPreferredDisplayMode(int displayId) {
        throw new RuntimeException("stub!");
    }

    public Display.Mode getSystemPreferredDisplayMode(int displayId) {
        throw new RuntimeException("stub!");
    }

    public void setRefreshRateSwitchingType(int newValue) {
        throw new RuntimeException("stub!");
    }

    public int getRefreshRateSwitchingType() {
        throw new RuntimeException("stub!");
    }
}
