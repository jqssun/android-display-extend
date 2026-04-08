package io.github.jqssun.displayextend.job;

import android.content.Context;
import android.hardware.display.DisplayManager;
import android.media.AudioAttributes;
import android.media.AudioDeviceAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.IAudioService;
import android.os.Build;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.view.Display;

import io.github.jqssun.displayextend.Pref;
import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.List;

public class DisplayMonitor {
    private static boolean registered = false;
    public static void init(DisplayManager displayManager) {
        if (registered) {
            return;
        }
        registered = true;
        for (Display display : displayManager.getDisplays()) {
            handleNewDisplay(display);
        }
        displayManager.registerDisplayListener(new DisplayManager.DisplayListener() {
            @Override
            public void onDisplayAdded(int displayId) {
                State.log("display added, displayId: " + displayId);
                Display display = displayManager.getDisplay(displayId);
                if (display != null) {
                    handleNewDisplay(display);
                }
            }

            @Override
            public void onDisplayRemoved(int displayId) {
                State.log("display removed, displayId: " + displayId);
                if (State.floatingButtonService != null) {
                    State.floatingButtonService.onDisplayRemoved(displayId);
                }
            }

            @Override
            public void onDisplayChanged(int displayId) {
                if (State.floatingButtonService != null) {
                    State.floatingButtonService.onDisplayChanged(displayId);
                }
            }
        }, null);
    }

    private static void handleNewDisplay(Display display) {
        if (display.getDisplayId() == Display.DEFAULT_DISPLAY) {
            return;
        }
        Context context = State.currentActivity.get();
        if (context == null) {
            return;
        }
        handleAutoOpenLastApp(context, display);
        handleDisableUsbAudio(context);
    }

    private static void handleDisableUsbAudio(Context context) {
        if (!ShizukuUtils.hasPermission()) {
            return;
        }
        boolean isDisabled = Pref.getUsbAudioDisabled();
        if (!isDisabled) {
            return;
        }
        IAudioService audioManager = ServiceUtils.getAudioManager();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            List<AudioDeviceAttributes> devices = audioManager.getDevicesForAttributes(new AudioAttributes.Builder().build());
            for (AudioDeviceAttributes device : devices) {
                if (device.getType() == AudioDeviceInfo.TYPE_HDMI) {
                    try {
                        audioManager.setWiredDeviceConnectionState(device, 0, "com.android.shell");
                        State.log("disabled audio output: " + device);
                    } catch(Throwable e) {
                        State.log("failed to disable audio output: " + e);
                    }
                }
            }
        } else {
            AudioManager audioManager2 = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            for (AudioDeviceInfo device : audioManager2.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                if (device.getType() == AudioDeviceInfo.TYPE_HDMI) {
                    try {
                        audioManager.setWiredDeviceConnectionState(device.getType(), 0, device.getAddress(), "", "com.android.shell");
                        State.log("disabled audio output: " + device.getType() + ", " + device.getProductName());
                    } catch(Throwable e) {
                        State.log("failed to disable audio output: " + e);
                    }
                } else if (device.getType() == AudioDeviceInfo.TYPE_BUILTIN_SPEAKER) {
                    try {
                        audioManager.setWiredDeviceConnectionState(device.getType(), 1, device.getAddress(), "", "com.android.shell");
                        State.log("enabled audio output: " + device.getType() + ", " + device.getProductName());
                    } catch(Throwable e) {
                        State.log("failed to enable audio output: " + e);
                    }
                }
            }
        }
    }

    private static void handleAutoOpenLastApp(Context context, Display display) {
        boolean autoBridge = Pref.getAutoBridge(display.getName());
        if (ShizukuUtils.hasPermission() && (autoBridge || display.getDisplayId() == State.bridgeDisplayId)) {
            new Handler().postDelayed(() -> {
                DisplayMetrics metrics = new DisplayMetrics();
                display.getMetrics(metrics);
                State.startNewJob(new ProjectViaBridge(display, new VirtualDisplayArgs(context.getString(R.string.bridge_display), display.getWidth(), display.getHeight(), (int) display.getRefreshRate(), metrics.densityDpi, Pref.getRotatesWithContent())));
            }, 500);
            return;
        }
        boolean autoOpen = Pref.getAutoOpenLastApp(display.getName());
        if (!autoOpen) {
            return;
        }
        String lastPackageName = Pref.getLastPackageName();
        if (lastPackageName == null) {
            return;
        }
        State.log("auto-launching " + lastPackageName + " on display " + display.getName());
        new Handler().postDelayed(() -> {
            ServiceUtils.launchPackage(context, lastPackageName, display.getDisplayId());
        }, 500);
    }
}
