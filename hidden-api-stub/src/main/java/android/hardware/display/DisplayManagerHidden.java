package android.hardware.display;

import dev.rikka.tools.refine.RefineAs;

@RefineAs(DisplayManager.class)
public class DisplayManagerHidden {
    public void setRefreshRateSwitchingType(int newValue) {
        throw new RuntimeException("stub!");
    }

    public int getMatchContentFrameRateUserPreference() {
        throw new RuntimeException("stub!");
    }
}
