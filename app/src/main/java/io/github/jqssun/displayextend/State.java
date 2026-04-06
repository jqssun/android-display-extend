package io.github.jqssun.displayextend;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.hardware.display.VirtualDisplay;
import android.media.projection.MediaProjection;
import android.os.IBinder;
import android.util.Log;

import io.github.jqssun.displayextend.job.Job;
import io.github.jqssun.displayextend.job.YieldException;
import io.github.jqssun.displayextend.shizuku.IUserService;
import io.github.jqssun.displayextend.shizuku.UserService;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import rikka.shizuku.Shizuku;

public class State {
    public static WeakReference<Activity> currentActivity = new WeakReference<>(null);
    public static BreadcrumbManager breadcrumbManager;
    private static Job currentJob;
    public static List<String> logs = new ArrayList<>();
    private static MediaProjection mediaProjection;
    public static MediaProjection mediaProjectionInUse;
    public static int lastSingleAppDisplay;
    public static volatile IUserService userService;
    public static VirtualDisplay bridgeVirtualDisplay;
    public static int bridgeDisplayId = -1;
    public static VirtualDisplay mirrorVirtualDisplay;
    public static int mirrorDisplayId = -1;
    public static FloatingButtonService floatingButtonService;
    public static Activity isInPureBlackActivity = null;


    private static final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    public static final ServiceConnection userServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder binder) {
            State.log("user service connected");
            State.userService = IUserService.Stub.asInterface(binder);
            if (State.currentActivity.get() != null) {
                State.currentActivity.get().runOnUiThread(() -> {
                    State.resumeJob();
                });
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            State.log("user service disconnected");
        }
    };

    public static Shizuku.UserServiceArgs userServiceArgs = new Shizuku.UserServiceArgs(new ComponentName(BuildConfig.APPLICATION_ID, UserService.class.getName()))
            .daemon(true)
                .tag("temp7")
                .processNameSuffix("connect-screen")
                .debuggable(false)
                .version(BuildConfig.VERSION_CODE);

    public static boolean isJobRunning() {
        return currentJob != null;
    }   

    public static void startNewJob(Job job) {
        if (currentJob != null) {
            if (currentActivity != null && currentActivity.get() != null) {
                State.log("job already running: " + currentJob.getClass().getSimpleName());
            }
            return;
        }
        currentJob = job;
        try {
            State.log("starting job: " + job.getClass().getSimpleName());
            currentJob.start();
            State.log("job completed: " + job.getClass().getSimpleName());
            currentJob = null;
        } catch (YieldException e) {
            State.log("job yielded: " + job.getClass().getSimpleName() + ", " + e.getMessage());
        } catch (RuntimeException e) {
            State.log("job failed: " + job.getClass().getSimpleName());
            String stackTrace = android.util.Log.getStackTraceString(e);
            State.log("stacktrace: " + stackTrace);
            currentJob = null;
        }
        breadcrumbManager.refreshCurrentFragment();
    }

    public static void resumeJob() {
        if (currentJob == null) {
            breadcrumbManager.refreshCurrentFragment();
            return;
        }
        try {
            State.log("resuming job: " + currentJob.getClass().getSimpleName());
            currentJob.start();
            State.log("job completed: " + currentJob.getClass().getSimpleName());
            currentJob = null;
        } catch (YieldException e) {
            State.log("job yielded: " + currentJob.getClass().getSimpleName() + ", " + e.getMessage());
        } catch (RuntimeException e) {
            State.log("job resume failed: " + currentJob.getClass().getSimpleName());
            String stackTrace = android.util.Log.getStackTraceString(e);
            State.log("stacktrace: " + stackTrace);
            currentJob = null;
        }
        breadcrumbManager.refreshCurrentFragment();
    }

    public static void resumeJobLater(long delayMillis) {
        if (currentActivity.get() != null) {
            mainHandler.postDelayed(() -> {
                resumeJob();
            }, delayMillis);
        }
    }

    public static void log(String message) {
        logs.add(message);
        Log.i("ConnectScreen", message);
        if (currentActivity != null && currentActivity.get() != null) {
            IMainActivity  mainActivity = (IMainActivity) currentActivity.get();
            mainActivity.updateLogs();
        }
    }

    public static void unbindUserService() {
        if (userService == null) {
            Shizuku.unbindUserService(State.userServiceArgs, userServiceConnection, true);
        }
    }

    public static MediaProjection getMediaProjection() {
        return mediaProjection;
    }

    public static void setMediaProjection(MediaProjection newMediaProjection) {
        if (newMediaProjection == null) {
            mediaProjection = null;
        } else {
            mediaProjection = newMediaProjection;
            mediaProjectionInUse = newMediaProjection;
        }
    }

    public static int getBridgeVirtualDisplayId() {
        if (bridgeVirtualDisplay == null) {
            return -1;
        }
        return bridgeVirtualDisplay.getDisplay().getDisplayId();
    }

    public static int getMirrorVirtualDisplayId() {
        if (mirrorVirtualDisplay == null) {
            return -1;
        }
        return mirrorVirtualDisplay.getDisplay().getDisplayId();
    }
}
