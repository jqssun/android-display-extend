package io.github.jqssun.displayextend;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class Pref {
    private static final String PREF_NAME = "settings";
    private static final String APP_PREF_NAME = "app_preferences";
    private static final String MANAGED_VIRTUAL_DISPLAY_PREF_NAME = "bridge_settings";
    private static final String FLOATING_PREF_NAME = "FloatingButtonPrefs";

    // settings keys
    public static final String KEY_USB_AUDIO_DISABLED = "usb_audio_disabled";
    public static final String KEY_USE_REAL_SCREEN_OFF = "use_real_screen_off";
    public static final String KEY_AUTO_BIND_INPUT = "auto_bind_input";
    public static final String KEY_AUTO_SCREEN_OFF = "auto_screen_off";
    public static final String KEY_SHOW_SYSTEM_SETTING_NAMES = "show_system_setting_names";
    public static final String KEY_TOUCHPAD_ACCESSIBILITY_OVERLAY = "touchpad_accessibility_overlay";
    public static final String KEY_TOUCHPAD_TAP_HOLD_DRAG = "touchpad_tap_hold_drag";

    // managed virtual display keys
    public static final String KEY_FOLLOW_APP_ROTATION = "rotates_with_content";
    public static final String KEY_SKIP_SCREEN_CAPTURE_PERMISSION = "skip_screen_capture_permission";

    // app_preferences keys
    public static final String KEY_SHOW_ALL_APPS = "show_all_apps";
    public static final String KEY_LAST_PACKAGE_NAME = "LAST_PACKAGE_NAME";
    public static final String KEY_FLOATING_BUTTON_FORCE_LANDSCAPE = "FLOATING_BUTTON_FORCE_LANDSCAPE";
    public static final String KEY_TOUCHPAD_SENSITIVITY = "touchpad_sensitivity";
    private static final String PREFIX_FLOATING_BUTTON = "FLOATING_BUTTON_";
    private static final String PREFIX_AUTO_MANAGED_VIRTUAL_DISPLAY = "AUTO_BRIDGE_";
    private static final String PREFIX_AUTO_OPEN_LAST_APP = "AUTO_OPEN_LAST_APP_";
    private static final String PREFIX_LAUNCH_TIME = "launch_time_";

    // floating button position keys
    public static final String KEY_BUTTON_X = "button_x";
    public static final String KEY_BUTTON_Y = "button_y";

    // settings prefs

    public static boolean getUsbAudioDisabled() {
        return _prefs().getBoolean(KEY_USB_AUDIO_DISABLED, false);
    }

    public static void setUsbAudioDisabled(boolean v) {
        _prefs().edit().putBoolean(KEY_USB_AUDIO_DISABLED, v).apply();
    }

    public static boolean getUseRealScreenOff() {
        return _prefs().getBoolean(KEY_USE_REAL_SCREEN_OFF, false);
    }

    public static void setUseRealScreenOff(boolean v) {
        _prefs().edit().putBoolean(KEY_USE_REAL_SCREEN_OFF, v).apply();
    }

    public static boolean getAutoBindInput() {
        return _prefs().getBoolean(KEY_AUTO_BIND_INPUT, true);
    }

    public static void setAutoBindInput(boolean v) {
        _prefs().edit().putBoolean(KEY_AUTO_BIND_INPUT, v).apply();
    }

    public static boolean getAutoScreenOff() {
        return _prefs().getBoolean(KEY_AUTO_SCREEN_OFF, false);
    }

    public static void setAutoScreenOff(boolean v) {
        _prefs().edit().putBoolean(KEY_AUTO_SCREEN_OFF, v).apply();
    }

    public static boolean getShowSystemSettingNames() {
        return _prefs().getBoolean(KEY_SHOW_SYSTEM_SETTING_NAMES, false);
    }

    public static void setShowSystemSettingNames(boolean v) {
        _prefs().edit().putBoolean(KEY_SHOW_SYSTEM_SETTING_NAMES, v).apply();
    }

    public static boolean getTouchpadAccessibilityOverlay() {
        SharedPreferences prefs = _prefs();
        if (prefs.contains(KEY_TOUCHPAD_ACCESSIBILITY_OVERLAY)) {
            return prefs.getBoolean(KEY_TOUCHPAD_ACCESSIBILITY_OVERLAY, false);
        }
        return TouchpadAccessibilityService.isAccessibilityServiceEnabled(_ctx());
    }

    public static void setTouchpadAccessibilityOverlay(boolean v) {
        _prefs().edit().putBoolean(KEY_TOUCHPAD_ACCESSIBILITY_OVERLAY, v).apply();
    }

    public static boolean getTouchpadTapHoldDrag() {
        return _prefs().getBoolean(KEY_TOUCHPAD_TAP_HOLD_DRAG, true);
    }

    public static void setTouchpadTapHoldDrag(boolean v) {
        _prefs().edit().putBoolean(KEY_TOUCHPAD_TAP_HOLD_DRAG, v).apply();
    }

    // managed virtual display prefs

    public static boolean getFollowAppRotation() {
        return _managedVirtualDisplay().getBoolean(KEY_FOLLOW_APP_ROTATION, true);
    }

    public static void setFollowAppRotation(boolean v) {
        _managedVirtualDisplay().edit().putBoolean(KEY_FOLLOW_APP_ROTATION, v).apply();
    }

    public static boolean getSkipScreenCapturePermission() {
        return _managedVirtualDisplay().getBoolean(KEY_SKIP_SCREEN_CAPTURE_PERMISSION, false);
    }

    public static void setSkipScreenCapturePermission(boolean v) {
        _managedVirtualDisplay().edit().putBoolean(KEY_SKIP_SCREEN_CAPTURE_PERMISSION, v).apply();
    }

    // app prefs

    public static boolean getShowAllApps() {
        return _app().getBoolean(KEY_SHOW_ALL_APPS, false);
    }

    public static void setShowAllApps(boolean v) {
        _app().edit().putBoolean(KEY_SHOW_ALL_APPS, v).apply();
    }

    public static String getLastPackageName() {
        return _app().getString(KEY_LAST_PACKAGE_NAME, null);
    }

    public static void setLastPackageName(String v) {
        _app().edit().putString(KEY_LAST_PACKAGE_NAME, v).apply();
    }

    public static boolean getFloatingButton(String displayName) {
        return _app().getBoolean(PREFIX_FLOATING_BUTTON + displayName, false);
    }

    public static void setFloatingButton(String displayName, boolean v) {
        _app().edit().putBoolean(PREFIX_FLOATING_BUTTON + displayName, v).apply();
    }

    public static float getTouchpadSensitivity() {
        return _app().getFloat(KEY_TOUCHPAD_SENSITIVITY, 3.0f);
    }

    public static void setTouchpadSensitivity(float v) {
        _app().edit().putFloat(KEY_TOUCHPAD_SENSITIVITY, v).apply();
    }

    public static boolean getFloatingButtonForceLandscape() {
        return _app().getBoolean(KEY_FLOATING_BUTTON_FORCE_LANDSCAPE, false);
    }

    public static void setFloatingButtonForceLandscape(boolean v) {
        _app().edit().putBoolean(KEY_FLOATING_BUTTON_FORCE_LANDSCAPE, v).apply();
    }

    public static boolean getAutoManagedVirtualDisplay(String displayName) {
        return _app().getBoolean(PREFIX_AUTO_MANAGED_VIRTUAL_DISPLAY + displayName, false);
    }

    public static void setAutoManagedVirtualDisplay(String displayName, boolean v) {
        _app().edit().putBoolean(PREFIX_AUTO_MANAGED_VIRTUAL_DISPLAY + displayName, v).apply();
    }

    public static boolean getAutoOpenLastApp(String displayName) {
        return _app().getBoolean(PREFIX_AUTO_OPEN_LAST_APP + displayName, false);
    }

    public static void setAutoOpenLastApp(String displayName, boolean v) {
        _app().edit().putBoolean(PREFIX_AUTO_OPEN_LAST_APP + displayName, v).apply();
    }

    public static long getLaunchTime(String packageName) {
        return _app().getLong(PREFIX_LAUNCH_TIME + packageName, 0L);
    }

    public static void setLaunchTime(String packageName, long time) {
        _app().edit().putLong(PREFIX_LAUNCH_TIME + packageName, time).apply();
    }

    // floating button position

    public static int getButtonX() {
        return _floating().getInt(KEY_BUTTON_X, 0);
    }

    public static int getButtonY() {
        return _floating().getInt(KEY_BUTTON_Y, 100);
    }

    public static void setButtonPosition(int x, int y) {
        _floating().edit().putInt(KEY_BUTTON_X, x).putInt(KEY_BUTTON_Y, y).apply();
    }

    public static void clearAll() {
        _prefs().edit().clear().apply();
        _app().edit().clear().apply();
        _managedVirtualDisplay().edit().clear().apply();
        _floating().edit().clear().apply();
    }

    // internal

    private static SharedPreferences _prefs() {
        return _ctx().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences _app() {
        return _ctx().getSharedPreferences(APP_PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences _managedVirtualDisplay() {
        return _ctx().getSharedPreferences(MANAGED_VIRTUAL_DISPLAY_PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences _floating() {
        return _ctx().getSharedPreferences(FLOATING_PREF_NAME, Context.MODE_PRIVATE);
    }

    private static Context _ctx() {
        Activity a = State.currentActivity.get();
        if (a != null) return a;
        throw new IllegalStateException("No context available");
    }
}
