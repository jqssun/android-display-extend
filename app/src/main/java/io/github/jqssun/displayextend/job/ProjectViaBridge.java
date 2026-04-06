package io.github.jqssun.displayextend.job;

import android.app.ActivityOptions;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjectionConfig;
import android.media.projection.MediaProjectionManager;
import android.view.Display;
import android.view.DisplayHidden;

import io.github.jqssun.displayextend.BridgeActivity;
import io.github.jqssun.displayextend.BridgePref;
import io.github.jqssun.displayextend.MainActivity;
import io.github.jqssun.displayextend.MediaProjectionService;
import io.github.jqssun.displayextend.State;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import dev.rikka.tools.refine.Refine;

public class ProjectViaBridge implements Job {
    private static int TYPE_WIFI = 3;
    private final AcquireShizuku acquireShizuku = new AcquireShizuku();
    private final Display bridgeDisplay;
    private final VirtualDisplayArgs virtualDisplayArgs;
    private boolean mediaProjectionRequested;

    public ProjectViaBridge(Display bridgeDisplay, VirtualDisplayArgs virtualDisplayArgs) {
        this.bridgeDisplay = bridgeDisplay;
        this.virtualDisplayArgs = virtualDisplayArgs;
    }

    @Override
    public void start() throws YieldException {
        if (!ShizukuUtils.hasPermission()) {
            acquireShizuku.start();
            if (!acquireShizuku.acquired) {
                return;
            }
        }
        DisplayHidden displayHidden = Refine.unsafeCast(bridgeDisplay);
        if (requestMediaProjectionPermission(State.currentActivity.get(), displayHidden.getType() == TYPE_WIFI)) {
            Context context = State.currentActivity.get();
            Intent intent = new Intent(context, BridgeActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.putExtra("virtualDisplayArgs", virtualDisplayArgs);
            ActivityOptions options = ActivityOptions.makeBasic();
            options.setLaunchDisplayId(bridgeDisplay.getDisplayId());
            context.startActivity(intent, options.toBundle());
        }
        State.bridgeDisplayId = bridgeDisplay.getDisplayId();
    }

    private boolean requestMediaProjectionPermission(Context context, boolean isWifiDisplay) throws YieldException {
        if (State.bridgeVirtualDisplay != null) {
            return true;
        }
        if (BridgePref.skipMediaProjectionPermission || isWifiDisplay) {
            // no media projection needed for wifi displays
            return true;
        }
        if (State.getMediaProjection() != null) {
            State.log("MediaProjection already exists, skipping");
            return true;
        }
        if (mediaProjectionRequested) {
            if (MediaProjectionService.isStarting && MediaProjectionService.instance == null) {
                throw new YieldException("waiting for service to start");
            }
            State.log("projection permission denied, skipping");
            return false;
        }
        MediaProjectionService.isStarting = true;
        mediaProjectionRequested = true;
        MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
        if (mediaProjectionManager != null) {
            Intent captureIntent = null;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                captureIntent = mediaProjectionManager.createScreenCaptureIntent(MediaProjectionConfig.createConfigForDefaultDisplay());
            } else {
                captureIntent = mediaProjectionManager.createScreenCaptureIntent();
            }
            State.currentActivity.get().startActivityForResult(captureIntent, MainActivity.REQUEST_CODE_MEDIA_PROJECTION);
            throw new YieldException("waiting for projection permission");
        } else {
            throw new RuntimeException("cannot get MediaProjectionManager");
        }
    }

}
