package io.github.jqssun.displayextend;

import android.media.AudioDeviceInfo;
import android.os.Build;
import android.view.InputDevice;
import android.view.accessibility.AccessibilityWindowInfo;

import java.lang.reflect.Method;

public final class PlatformCompat {
    private static Method inputDeviceIsExternalMethod;
    private static boolean inputDeviceIsExternalMethodInitialized;

    private PlatformCompat() {
    }

    public static String getAudioDeviceAddress(AudioDeviceInfo device) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return device.getAddress();
        }
        return "";
    }

    public static boolean isExternalInputDevice(InputDevice device) {
        if (device == null) {
            return false;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return device.isExternal();
        }
        try {
            Method method = getInputDeviceIsExternalMethod();
            if (method != null) {
                Object result = method.invoke(device);
                if (result instanceof Boolean) {
                    return (Boolean) result;
                }
            }
        } catch (Throwable ignored) {
        }

        int sources = device.getSources();
        if ((sources & InputDevice.SOURCE_TOUCHSCREEN) != 0) {
            return false;
        }
        return (sources & (InputDevice.SOURCE_MOUSE
                | InputDevice.SOURCE_KEYBOARD
                | InputDevice.SOURCE_STYLUS
                | InputDevice.SOURCE_TOUCHPAD
                | InputDevice.SOURCE_TRACKBALL
                | InputDevice.SOURCE_DPAD
                | InputDevice.SOURCE_GAMEPAD
                | InputDevice.SOURCE_JOYSTICK)) != 0;
    }

    public static boolean windowMatchesDisplay(AccessibilityWindowInfo window, int displayId) {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.R || window.getDisplayId() == displayId;
    }

    private static Method getInputDeviceIsExternalMethod() {
        if (!inputDeviceIsExternalMethodInitialized) {
            try {
                inputDeviceIsExternalMethod = InputDevice.class.getMethod("isExternal");
            } catch (Throwable ignored) {
                inputDeviceIsExternalMethod = null;
            }
            inputDeviceIsExternalMethodInitialized = true;
        }
        return inputDeviceIsExternalMethod;
    }
}
