package io.github.jqssun.displayextend;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.provider.Settings;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import io.github.jqssun.displayextend.job.StartFloatingButton;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

public class FloatingButtonService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private int displayId;
    private float initialX, initialY;
    private float initialTouchX, initialTouchY;
    private static final int FADE_DELAY = 5000;
    private Runnable fadeOutRunnable;
    private android.os.Handler handler;
    private boolean isReady = true;
    private static final long DOUBLE_CLICK_TIME_DELTA = 300;
    private long lastClickTime = 0;

    public static boolean startFloating(Context context, int displayId, boolean dryRun) {
        if (!Settings.canDrawOverlays(context)) {
            if (dryRun) {
                return false;
            }
            Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName())
            );
            context.startActivity(intent);
            return false;
        }

        if (ShizukuUtils.hasShizukuStarted()) {
            if (!dryRun) {
                State.startNewJob(new StartFloatingButton(displayId, context));
            }
            return true;
        }

        if (!TouchpadAccessibilityService.isAccessibilityServiceEnabled(context)) {
            if (!dryRun) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                context.startActivity(intent);
            }
            return false;
        }

        if (dryRun) {
            return true;
        }
        Intent serviceIntent = new Intent(context, TouchpadAccessibilityService.class);
        context.startService(serviceIntent);


        new Handler().postDelayed(() -> {
            if (TouchpadAccessibilityService.getInstance() == null) {
                Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                context.startActivity(intent);
            } else {
                Intent serviceIntent2 = new Intent(context, FloatingButtonService.class);
                serviceIntent2.putExtra("display_id", displayId);
                context.startService(serviceIntent2);
            }
        }, 1000);
        return true;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        State.floatingButtonService = this;
        if (intent != null) {
            displayId = intent.getIntExtra("display_id", -1);
            if (displayId != -1) {
                if (floatingView != null) {
                    this.onDestroy();
                }
                _createFloatingButton();
            }
        }
        return START_STICKY;
    }

    public void onDisplayRemoved(int displayId) {
        if (this.displayId == displayId) {
            stopSelf();
        }
    }

    public void onSingleAppLaunched() {
        _resetButtonVisibility();
    }

    private void _createFloatingButton() {
        handler = new android.os.Handler();
        fadeOutRunnable = () -> _fadeOutButton();

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_back_button, null);
        ImageView backButton = floatingView.findViewById(R.id.floating_back_image);
        backButton.setImageResource(R.drawable.ic_back);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.TOP | Gravity.START;
        
        if (Pref.getFloatingButtonForceLandscape()) {
            params.screenOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE;
        }
        params.x = Pref.getButtonX();
        params.y = Pref.getButtonY();
        if (params.x < 0) {
            params.x = 0;
        }
        if (params.y < 0) {
            params.y = 0;
        }

        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display display = displayManager.getDisplay(displayId);
        if (display == null) {
            stopSelf();
            return;
        }

        Context displayContext = createDisplayContext(display);
        windowManager = (WindowManager) displayContext.getSystemService(Context.WINDOW_SERVICE);

        try {
            windowManager.addView(floatingView, params);
        } catch (Exception e) {
            State.log("failed to add floating window: " + e.getMessage());
            stopSelf();
            return;
        }

        floatingView.setOnTouchListener((v, event) -> {
            _resetButtonVisibility();

            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    initialX = params.x;
                    initialY = params.y;
                    initialTouchX = event.getRawX();
                    initialTouchY = event.getRawY();
                    return true;

                case MotionEvent.ACTION_MOVE:
                    params.x = (int) (initialX + (event.getRawX() - initialTouchX));
                    params.y = (int) (initialY + (event.getRawY() - initialTouchY));
                    windowManager.updateViewLayout(floatingView, params);
                    return true;

                case MotionEvent.ACTION_UP:
                    if (Math.abs(event.getRawX() - initialTouchX) < 10
                            && Math.abs(event.getRawY() - initialTouchY) < 10) {
                        long clickTime = System.currentTimeMillis();
                        if (clickTime - lastClickTime < DOUBLE_CLICK_TIME_DELTA) {
                            TouchpadActivity.launchLastPackage(this, displayId);
                            lastClickTime = 0;
                        } else {
                            if (!isReady) {
                                _resetButtonVisibility();
                            } else {
                                TouchpadActivity.performBackGesture(ServiceUtils.getInputManager(), displayId);
                            }
                            lastClickTime = clickTime;
                        }
                    } else {
                        Pref.setButtonPosition(params.x, params.y);
                    }
                    return true;
            }
            return false;
        });

        _startFadeOutTimer();
    }

    private void _startFadeOutTimer() {
        handler.removeCallbacks(fadeOutRunnable);
        handler.postDelayed(fadeOutRunnable, FADE_DELAY);
    }

    private void _fadeOutButton() {
        floatingView.animate()
                .alpha(0.0f)
                .setDuration(500)
                .withEndAction(() -> isReady = false)
                .start();
    }

    private void _resetButtonVisibility() {
        handler.removeCallbacks(fadeOutRunnable);
        floatingView.animate()
                .alpha(1.0f)
                .setDuration(200)
                .withEndAction(() -> isReady = true)
                .start();
        _startFadeOutTimer();
    }

    @Override
    public void onDestroy() {
        if (handler != null) {
            handler.removeCallbacks(fadeOutRunnable);
        }
        super.onDestroy();
        if (floatingView != null && windowManager != null) {
            windowManager.removeView(floatingView);
            floatingView = null;
        }
        State.floatingButtonService = null;
    }

    public void onDisplayChanged(int displayId) {
        if (this.displayId == displayId) {
            _resetButtonVisibility();
        }
    }
}