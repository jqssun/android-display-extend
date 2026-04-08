package io.github.jqssun.displayextend;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

public class Pref {
    private static final String PREF_NAME = "settings";
    private static final String APP_PREF_NAME = "app_preferences";
    private static final String BRIDGE_PREF_NAME = "bridge_settings";
    private static final String FLOATING_PREF_NAME = "FloatingButtonPrefs";

    // settings keys
    public static final String KEY_USB_AUDIO_DISABLED = "usb_audio_disabled";
    public static final String KEY_USE_REAL_SCREEN_OFF = "use_real_screen_off";

    // bridge keys
    public static final String KEY_ROTATES_WITH_CONTENT = "rotates_with_content";
    public static final String KEY_SKIP_MEDIA_PROJECTION_PERMISSION = "skip_media_projection_permission";

    // app_preferences keys
    public static final String KEY_SHOW_ALL_APPS = "show_all_apps";
    public static final String KEY_LAST_PACKAGE_NAME = "LAST_PACKAGE_NAME";
    public static final String KEY_FLOATING_BUTTON_FORCE_LANDSCAPE = "FLOATING_BUTTON_FORCE_LANDSCAPE";
    public static final String KEY_FORCE_DPI = "force_dpi";
    private static final String PREFIX_FLOATING_BUTTON = "FLOATING_BUTTON_";
    private static final String PREFIX_AUTO_BRIDGE = "AUTO_BRIDGE_";
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

    // bridge prefs

    public static boolean getRotatesWithContent() {
        return _bridge().getBoolean(KEY_ROTATES_WITH_CONTENT, true);
    }

    public static void setRotatesWithContent(boolean v) {
        _bridge().edit().putBoolean(KEY_ROTATES_WITH_CONTENT, v).apply();
    }

    public static boolean getSkipMediaProjectionPermission() {
        return _bridge().getBoolean(KEY_SKIP_MEDIA_PROJECTION_PERMISSION, false);
    }

    public static void setSkipMediaProjectionPermission(boolean v) {
        _bridge().edit().putBoolean(KEY_SKIP_MEDIA_PROJECTION_PERMISSION, v).apply();
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

    public static int getForceDpi() {
        return _app().getInt(KEY_FORCE_DPI, 0);
    }

    public static void setForceDpi(int v) {
        _app().edit().putInt(KEY_FORCE_DPI, v).apply();
    }

    public static boolean getForceLandscape() {
        return _app().getBoolean(KEY_FLOATING_BUTTON_FORCE_LANDSCAPE, false);
    }

    public static void setForceLandscape(boolean v) {
        _app().edit().putBoolean(KEY_FLOATING_BUTTON_FORCE_LANDSCAPE, v).apply();
    }

    public static boolean getAutoBridge(String displayName) {
        return _app().getBoolean(PREFIX_AUTO_BRIDGE + displayName, false);
    }

    public static void setAutoBridge(String displayName, boolean v) {
        _app().edit().putBoolean(PREFIX_AUTO_BRIDGE + displayName, v).apply();
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

    // internal

    private static SharedPreferences _prefs() {
        return _ctx().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences _app() {
        return _ctx().getSharedPreferences(APP_PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences _bridge() {
        return _ctx().getSharedPreferences(BRIDGE_PREF_NAME, Context.MODE_PRIVATE);
    }

    private static SharedPreferences _floating() {
        return _ctx().getSharedPreferences(FLOATING_PREF_NAME, Context.MODE_PRIVATE);
    }

    private static Context _ctx() {
        Activity a = State.currentActivity != null ? State.currentActivity.get() : null;
        if (a != null) return a;
        throw new IllegalStateException("No context available");
    }
}
