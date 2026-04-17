package io.github.jqssun.displayextend.job;

import android.hardware.display.DisplayManager;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.display.IDisplayManager;
import android.hardware.display.IVirtualDisplayCallback;
import android.hardware.display.VirtualDisplay;
import android.hardware.display.VirtualDisplayConfig;
import android.media.projection.IMediaProjection;
import android.media.projection.MediaProjectionHidden;
import android.os.Build;
import android.view.Display;
import android.view.DisplayInfo;
import android.view.Surface;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.lang.reflect.Constructor;

import dev.rikka.tools.refine.Refine;

public class CreateVirtualDisplay {

    // Internal fields copied from android.hardware.display.DisplayManager
    private static final int VIRTUAL_DISPLAY_FLAG_PUBLIC = android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC;
    private static final int VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY = android.hardware.display.DisplayManager.VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
    private static final int VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH = 1 << 6;
    private static final int VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT = 1 << 7;
    private static final int VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL = 1 << 8;
    private static final int VIRTUAL_DISPLAY_FLAG_SHOULD_SHOW_SYSTEM_DECORATIONS = 1 << 9;
    private static final int VIRTUAL_DISPLAY_FLAG_TRUSTED = 1 << 10;
    private static final int VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP = 1 << 11;
    private static final int VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED = 1 << 12;
    private static final int VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED = 1 << 13;
    private static final int VIRTUAL_DISPLAY_FLAG_OWN_FOCUS = 1 << 14;
    private static final int VIRTUAL_DISPLAY_FLAG_DEVICE_DISPLAY_GROUP = 1 << 15;

    public static VirtualDisplay createVirtualDisplay(VirtualDisplayArgs virtualDisplayArgs, Surface surface) {
        if (ShizukuUtils.hasPermission()) {
            try {
                return createByShizuku(virtualDisplayArgs, surface, false);
            } catch(RuntimeException e) {
                try {
                    return createByShizuku(virtualDisplayArgs, surface, true);
                } catch(RuntimeException e2) {
                    if (State.getMediaProjection() != null) {
                        return createByMediaProjection(virtualDisplayArgs, surface);
                    }
                    throw e2;
                }
            }
        } else {
            return createByMediaProjection(virtualDisplayArgs, surface);
        }
    }

    private static VirtualDisplay createByMediaProjection(VirtualDisplayArgs virtualDisplayArgs, Surface surface) {
        VirtualDisplay virtualDisplay = State.getMediaProjection().createVirtualDisplay(virtualDisplayArgs.virtualDisplayName,
                virtualDisplayArgs.width, virtualDisplayArgs.height, virtualDisplayArgs.dpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_PUBLIC,
                surface, null, null);
        State.setMediaProjection(null);
        return virtualDisplay;
    }

    private static @NonNull VirtualDisplay createByShizuku(VirtualDisplayArgs virtualDisplayArgs, Surface surface, boolean ownContentOnly) {
        int virtualDisplayWidth = virtualDisplayArgs.width;
        IDisplayManager displayManager = ServiceUtils.getDisplayManager();
        int flags = getFlags(virtualDisplayArgs, ownContentOnly);
        VirtualDisplayConfig config = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            config = buildVirtualDisplayConfigApi34(virtualDisplayArgs, surface, flags, virtualDisplayWidth);
        } else {
            // config = null
        }
        IVirtualDisplayCallback callback = new VirtualDisplayCallback();
        IMediaProjection projection = null;
        if (State.getMediaProjection() != null) {
            MediaProjectionHidden mediaProjectionHidden = Refine.unsafeCast(State.getMediaProjection());
            projection = mediaProjectionHidden.getProjection();
        }
        int displayId = -1;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            displayId = displayManager.createVirtualDisplay(config, callback, projection, "com.android.shell");
        } else {
            displayId = displayManager.createVirtualDisplay(callback, projection, "com.android.shell", virtualDisplayArgs.virtualDisplayName, virtualDisplayWidth, virtualDisplayArgs.height, virtualDisplayArgs.dpi, surface, flags, virtualDisplayArgs.virtualDisplayName);
        }
        DisplayInfo displayInfo = ServiceUtils.getDisplayManager().getDisplayInfo(displayId);
        State.log("virtual display created, displayId: " + displayId + ", uniqueId: " + displayInfo.uniqueId);
        VirtualDisplay virtualDisplay = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            virtualDisplay = DisplayManagerGlobal.getInstance().createVirtualDisplayWrapper(config, callback, displayId);
        } else {
            try {
                DisplayManagerGlobal displayManagerGlobal = DisplayManagerGlobal.getInstance();
                Class<?> virtualDisplayClass = VirtualDisplay.class;
                Constructor<?> constructor = virtualDisplayClass.getDeclaredConstructor(
                        DisplayManagerGlobal.class,
                        Display.class,
                        IVirtualDisplayCallback.class,
                        Surface.class
                );
                constructor.setAccessible(true);
                Display display = displayManagerGlobal.getRealDisplay(displayId);
                virtualDisplay = (VirtualDisplay) constructor.newInstance(
                        displayManagerGlobal,
                        display,
                        callback,
                        surface
                );
            } catch(Throwable e) {
                throw new RuntimeException(e);
            }
        }
        State.setMediaProjection(null);
        return virtualDisplay;
    }

    private static int getFlags(VirtualDisplayArgs virtualDisplayArgs, boolean ownContentOnly) {
        int flags = VIRTUAL_DISPLAY_FLAG_PUBLIC
                | VIRTUAL_DISPLAY_FLAG_SUPPORTS_TOUCH;
        //    | VIRTUAL_DISPLAY_FLAG_DESTROY_CONTENT_ON_REMOVAL;
        if (ownContentOnly) {
            flags |= VIRTUAL_DISPLAY_FLAG_OWN_CONTENT_ONLY;
        }
        if (virtualDisplayArgs.followAppRotation) {
            flags |= VIRTUAL_DISPLAY_FLAG_ROTATES_WITH_CONTENT;
        }
        if (Build.VERSION.SDK_INT >= AndroidVersions.API_33_ANDROID_13) {
            flags |= VIRTUAL_DISPLAY_FLAG_TRUSTED
                    | VIRTUAL_DISPLAY_FLAG_OWN_DISPLAY_GROUP
                    | VIRTUAL_DISPLAY_FLAG_ALWAYS_UNLOCKED
                    | VIRTUAL_DISPLAY_FLAG_TOUCH_FEEDBACK_DISABLED;
            if (Build.VERSION.SDK_INT >= AndroidVersions.API_34_ANDROID_14) {
                flags |= VIRTUAL_DISPLAY_FLAG_DEVICE_DISPLAY_GROUP;
                //    flags |= VIRTUAL_DISPLAY_FLAG_OWN_FOCUS
                //            | VIRTUAL_DISPLAY_FLAG_DEVICE_DISPLAY_GROUP;
            }
        }
        return flags;
    }

    public static class VirtualDisplayCallback extends IVirtualDisplayCallback.Stub {
        public void onPaused() {
        }
        public void onResumed() {
        }
        public void onStopped() {
        }
    }

    @RequiresApi(Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
    private static VirtualDisplayConfig buildVirtualDisplayConfigApi34(
            VirtualDisplayArgs virtualDisplayArgs, Surface surface, int flags, int width) {
        return new VirtualDisplayConfig.Builder(
                virtualDisplayArgs.virtualDisplayName,
                width, virtualDisplayArgs.height, virtualDisplayArgs.dpi)
                .setSurface(surface)
                .setFlags(flags)
                .setRequestedRefreshRate(virtualDisplayArgs.refreshRate)
                .build();
    }
}
