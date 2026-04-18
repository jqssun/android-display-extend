package io.github.jqssun.displayextend;

import android.accessibilityservice.GestureDescription;
import android.app.ActivityTaskManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.content.res.ColorStateList;
import android.hardware.display.DisplayManager;
import android.hardware.input.IInputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.InputDevice;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.view.KeyEventHidden;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.MotionEventHidden;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import io.github.jqssun.displayextend.job.StartTouchPad;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.google.android.material.button.MaterialButton;

import dev.rikka.tools.refine.Refine;

public class TouchpadActivity extends AppCompatActivity {
    
    public static final int INJECT_INPUT_EVENT_MODE_ASYNC = 0;
    private View touchpadArea;
    private TextView gestureHint;
    private View touchpadHintContainer;
    private View touchpadRoot;
    private View topBar;
    private View bottomButtons;
    private View touchpadOverlay;
    private ImageView cursorView;
    private int displayId;
    private static final String TAG = "TouchpadActivity";
    private float cursorX = 0;
    private float cursorY = 0;
    private WindowManager.LayoutParams cursorParams;
    private float halfWidth;
    private float halfHeight;
    private IInputManager inputManager;
    private GestureState gestureState = new GestureState();
    private final ExecutorService ipcExecutor = Executors.newSingleThreadExecutor();
    private boolean isCursorLocked = false;
    private boolean useAccessibilityCursor = false;
    private boolean useAccessibilityTouchOverlay = false;
    private float sensitivity = 3.0f;
    private Spinner modeSpinner;
    private static final int MODE_NORMAL = 0;
    private static final int MODE_CURSOR_LOCKED = 1;
    private int rotation = 0; // 0=0°, 1=90°CW, 2=180°, 3=270°CW
    private boolean isNightModeEnabled = false;
    private MaterialButton nightModeButton;
    private View scrollStrip;
    private final Map<MaterialButton, ColorStateList> nightModeButtonTints = new LinkedHashMap<>();
    private final Map<MaterialButton, ColorStateList> nightModeButtonIconTints = new LinkedHashMap<>();
    private final Map<MaterialButton, Drawable> nightModeTextButtonBackgrounds = new LinkedHashMap<>();
    private Drawable defaultRootBackground;
    private Drawable defaultTopBarBackground;
    private Drawable defaultBottomButtonsBackground;

    private static class GestureState {
        List<MotionEvent> allMotionEvents = new ArrayList<>();
        int lastReplayed = 0;
        boolean isSingleFinger;
        float initialTouchX = 0;
        float initialTouchY = 0;
    }

    private static class StrokePoint {
        float x;
        float y;
        long time;

        StrokePoint(float x, float y, long time) {
            this.x = x;
            this.y = y;
            this.time = time;
        }
    }

    public static boolean startTouchpad(Context context,int displayId, boolean dryRun) {
        if (android.os.Build.VERSION.SDK_INT <= android.os.Build.VERSION_CODES.Q && !ShizukuUtils.hasPermission()) {
            return false;
        }
        if (displayId == Display.DEFAULT_DISPLAY) {
            return false;
        }
        if (!Settings.canDrawOverlays(context)) {
            if (!dryRun) {
                Intent intent = new Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:" + context.getPackageName())
                );
                context.startActivity(intent);
            }
            return false;
        }
        
        if (ShizukuUtils.hasShizukuStarted()) {
            if (!dryRun && Pref.getTouchpadAccessibilityOverlay()) {
                TouchpadAccessibilityService.ensureServiceAvailable(context, false);
            }
            if (!dryRun) {
                State.startNewJob(new StartTouchPad(displayId, context));
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
        
        if (!dryRun) {
            if(TouchpadAccessibilityService.getInstance() != null) {
                Intent touchpadIntent = new Intent(context, TouchpadActivity.class);
                touchpadIntent.putExtra("display_id", displayId);
                context.startActivity(touchpadIntent);
                return true;
            }
            Intent serviceIntent = new Intent(context, TouchpadAccessibilityService.class);
            context.startService(serviceIntent);

            new Handler().postDelayed(() -> {
                if (TouchpadAccessibilityService.getInstance() == null) {
                    Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
                    context.startActivity(intent);
                } else {
                    Intent touchpadIntent = new Intent(context, TouchpadActivity.class);
                    touchpadIntent.putExtra("display_id", displayId);
                    context.startActivity(touchpadIntent);
                }
            }, 1000);
            
        }
        return true;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_touchpad);

        touchpadRoot = findViewById(R.id.touchpadRoot);
        topBar = findViewById(R.id.topBar);
        bottomButtons = findViewById(R.id.bottomButtons);
        modeSpinner = findViewById(R.id.modeSpinner);
        touchpadArea = findViewById(R.id.touchpad_area);
        gestureHint = findViewById(R.id.touchpad_gesture_hint);
        touchpadHintContainer = findViewById(R.id.touchpad_button_hints);
        defaultRootBackground = touchpadRoot.getBackground();
        defaultTopBarBackground = topBar.getBackground();
        defaultBottomButtonsBackground = bottomButtons.getBackground();
        _updateHelp();
        _bindHintRow(R.id.hint_back, R.drawable.ic_back, R.string.touchpad_hint_back);
        _bindHintRow(R.id.hint_home, R.drawable.ic_home, R.string.touchpad_hint_home);
        _bindHintRow(R.id.hint_night_mode, R.drawable.ic_night_mode, R.string.touchpad_hint_night_mode);
        _bindHintRow(R.id.hint_rotate_ccw, R.drawable.ic_rotate, R.string.touchpad_hint_rotate_ccw);
        _bindHintRow(R.id.hint_rotate_cw, R.drawable.ic_rotate_cw, R.string.touchpad_hint_rotate_cw);

        if (savedInstanceState != null) {
            rotation = savedInstanceState.getInt("rotation", 0);
            isNightModeEnabled = savedInstanceState.getBoolean("night_mode_enabled", false);
        }

        displayId = getIntent().getIntExtra("display_id", Display.DEFAULT_DISPLAY);

        if (ShizukuUtils.hasPermission()) {
            inputManager = ServiceUtils.getInputManager();
        }
        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display targetDisplay = displayManager.getDisplay(displayId);
        if (targetDisplay == null) {
            finish();
            return;
        }

        halfWidth = targetDisplay.getWidth() / 2.0f;
        halfHeight = targetDisplay.getHeight() / 2.0f;

        _showMouseCursor(targetDisplay);

        touchpadArea.addOnLayoutChangeListener(
                (v, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom) ->
                        _syncTouchpadOverlay());

        nightModeButton = findViewById(R.id.nightModeButton);
        _registerNightModeButton((MaterialButton) findViewById(R.id.backButton));
        _registerNightModeButton((MaterialButton) findViewById(R.id.homeButton));
        _registerNightModeButton(nightModeButton);
        _registerNightModeButton((MaterialButton) findViewById(R.id.rotateCcwButton));
        _registerNightModeButton((MaterialButton) findViewById(R.id.rotateCwButton));
        _registerNightModeButton((MaterialButton) findViewById(R.id.switchModeButton));
        _registerNightModeButton((MaterialButton) findViewById(R.id.exitButton));
        nightModeButton.setOnClickListener(v -> _toggleNightMode());

        findViewById(R.id.backButton).setOnClickListener(v -> {
            performBackGesture(inputManager, displayId);
        });

        findViewById(R.id.homeButton).setOnClickListener(v -> {
            launchLastPackage(this, displayId);
        });

        _setupModeSpinner();

        findViewById(R.id.rotateCcwButton).setOnClickListener(v -> {
            rotation = (rotation + 1) % 4; // CCW
            cursorX = 0;
            cursorY = 0;
            _updateCursorPosition(0, 0);
            _applyRotation();
        });

        findViewById(R.id.rotateCwButton).setOnClickListener(v -> {
            rotation = (rotation + 3) % 4; // CW
            cursorX = 0;
            cursorY = 0;
            _updateCursorPosition(0, 0);
            _applyRotation();
        });

        _setupScrollStrip();
        sensitivity = Pref.getTouchpadSensitivity();
        _applyNightMode();

        findViewById(R.id.exitButton).setOnClickListener(v -> finish());

        findViewById(R.id.switchModeButton).setOnClickListener(v -> _switchMode());
    }

    private void _setupTouchListenerForAccessibility() {
        touchpadOverlay.setOnTouchListener((v, event) -> {
            if (gestureState.allMotionEvents.isEmpty()) {
                gestureState.initialTouchX = event.getX();
                gestureState.initialTouchY = event.getY();
            }

            float relativeX = event.getX() - gestureState.initialTouchX;
            float relativeY = event.getY() - gestureState.initialTouchY;

            float absoluteX = cursorX + halfWidth + relativeX * 2;
            float absoluteY = cursorY + halfHeight + relativeY * 2;
            float offsetX = absoluteX - event.getX();
            float offsetY = absoluteY - event.getY();

            MotionEvent copiedEventWithOffset = _obtainMotionEventWithOffset(event, offsetX, offsetY);
            gestureState.allMotionEvents.add(copiedEventWithOffset);

            if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                Log.d(TAG, "touch ended, isSingleFinger: " + gestureState.isSingleFinger);
                if (!gestureState.isSingleFinger) {
                    boolean alwaysSingleFinger = true;
                    for (MotionEvent e : gestureState.allMotionEvents) {
                        if (e.getPointerCount() > 1) {
                            alwaysSingleFinger = false;
                        }
                    }
                    if (!isCursorLocked && alwaysSingleFinger && (Math.abs(relativeX) > 10 || Math.abs(relativeY) > 10)) {
                        // ignore
                    } else {
                        _replayGestureViaAccessibility();
                    }
                }
                _recycleGestureEvents();
                return true;
            }

            if (!isCursorLocked) {
                if (gestureState.isSingleFinger || (event.getPointerCount() == 1 && (gestureState.allMotionEvents.size() == 5 || Math.abs(relativeX) > 10 || Math.abs(relativeY) > 10))) {
                    if (gestureState.allMotionEvents.size() == 5 && Math.abs(relativeX) < 1 && Math.abs(relativeY) < 1) {
                        Log.d(TAG, "no movement detected");
                        return true;
                    }
                    gestureState.isSingleFinger = true;
                    if (event.getPointerCount() == 1) {
                        _updateCursorPosition(relativeX * 0.5f, relativeY * 0.5f);
                        gestureState.initialTouchX = event.getX();
                        gestureState.initialTouchY = event.getY();
                    }
                    return true;
                }
            }
            return true;
        });
    }

    private void _setupTouchListenerForInputManager() {
        touchpadOverlay.setOnTouchListener((v, event) -> {
            if (gestureState.allMotionEvents.isEmpty()) {
                gestureState.initialTouchX = event.getX();
                gestureState.initialTouchY = event.getY();
            }

            float relativeX = event.getX() - gestureState.initialTouchX;
            float relativeY = event.getY() - gestureState.initialTouchY;

            float absoluteX = cursorX + halfWidth + relativeX * 2;
            float absoluteY = cursorY + halfHeight + relativeY * 2;
            float offsetX = absoluteX - event.getX();
            float offsetY = absoluteY - event.getY();

            MotionEvent copiedEventWithOffset = _obtainMotionEventWithOffset(event, offsetX, offsetY);
            gestureState.allMotionEvents.add(copiedEventWithOffset);

            if (event.getAction() == MotionEvent.ACTION_UP ||
                    event.getAction() == MotionEvent.ACTION_CANCEL) {
                Log.d(TAG, "touch ended, isSingleFinger: " + gestureState.isSingleFinger);
                if (!gestureState.isSingleFinger) {
                    if (!isCursorLocked && gestureState.lastReplayed == 0
                            && (Math.abs(relativeX) > 10 || Math.abs(relativeY) > 10)) {
                        // ignore
                    } else {
                        _replayBufferedEvents();
                    }
                }
                _recycleGestureEvents();
                return true;
            }

            if (!isCursorLocked && gestureState.lastReplayed == 0) {
                if (gestureState.isSingleFinger || (event.getPointerCount() == 1
                        && (gestureState.allMotionEvents.size() == 5
                        || Math.abs(relativeX) > 10 || Math.abs(relativeY) > 10))) {
                    if (gestureState.allMotionEvents.size() == 5
                            && Math.abs(relativeX) < 1 && Math.abs(relativeY) < 1) {
                        Log.d(TAG, "no movement detected");
                        return true;
                    }
                    gestureState.isSingleFinger = true;
                    if (event.getPointerCount() == 1) {
                        _updateCursorPosition(relativeX * 0.5f, relativeY * 0.5f);
                        gestureState.initialTouchX = event.getX();
                        gestureState.initialTouchY = event.getY();
                    }
                    return true;
                }

                if (event.getPointerCount() == 1) {
                    // buffer it
                    return true;
                }
            }

            _replayBufferedEvents();
            return true;
        });
    }

    public static void launchLastPackage(Context context, int displayId) {
        String lastPackageName = Pref.getLastPackageName();
        if (lastPackageName == null) {
            return;
        }
        ServiceUtils.launchPackage(context, lastPackageName, displayId);
    }

    private void _updateHelp() {
        if (isNightModeEnabled) {
            gestureHint.setVisibility(View.GONE);
            if (touchpadHintContainer != null) {
                touchpadHintContainer.setVisibility(View.GONE);
            }
            return;
        }
        gestureHint.setVisibility(View.VISIBLE);
        if (touchpadHintContainer != null) {
            touchpadHintContainer.setVisibility(View.VISIBLE);
        }
        int selectedMode = modeSpinner.getSelectedItemPosition();
        gestureHint.setText(selectedMode == MODE_CURSOR_LOCKED
                ? getString(R.string.touchpad_help_cursor_locked)
                : getString(R.string.touchpad_help_normal));
    }

    private void _bindHintRow(int rowId, int iconRes, int textRes) {
        View row = findViewById(rowId);
        ((ImageView) row.findViewById(R.id.hint_icon)).setImageResource(iconRes);
        ((TextView) row.findViewById(R.id.hint_text)).setText(textRes);
    }

    private void _recycleGestureEvents() {
        for (MotionEvent e : gestureState.allMotionEvents) {
            e.recycle();
        }
        gestureState.allMotionEvents.clear();
        gestureState.lastReplayed = 0;
        gestureState.isSingleFinger = false;
    }

    private void _replayBufferedEvents() {
        if (inputManager == null || gestureState.allMotionEvents.isEmpty()) {
            return;
        }

        List<MotionEvent> toReplay = new ArrayList<>();
        for (int i = gestureState.lastReplayed; i < gestureState.allMotionEvents.size(); i++) {
            toReplay.add(MotionEvent.obtain(gestureState.allMotionEvents.get(i)));
        }
        gestureState.lastReplayed = gestureState.allMotionEvents.size();

        ipcExecutor.execute(() -> {
            for (MotionEvent event : toReplay) {
                MotionEventHidden eventHidden = Refine.unsafeCast(event);
                eventHidden.setDisplayId(displayId);
                inputManager.injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC);
                event.recycle();
            }
        });
    }

    private static void _injectKeyEvent(IInputManager inputManager, int displayId, int action, int keyCode, int repeat, int metaState, int injectMode) {
        setFocus(inputManager, displayId);
        long now = SystemClock.uptimeMillis();
        KeyEvent event = new KeyEvent(now, now, action, keyCode, repeat, metaState, KeyCharacterMap.VIRTUAL_KEYBOARD, 0, 0,
                InputDevice.SOURCE_KEYBOARD);
        KeyEventHidden eventHidden = Refine.unsafeCast(event);
        eventHidden.setDisplayId(displayId);
        inputManager.injectInputEvent(event, injectMode);
    }

    /**
     * Creates or repositions the transparent touchpad overlay.
     *
     * The overlay is a TYPE_APPLICATION_OVERLAY with FLAG_NOT_FOCUSABLE, positioned
     * exactly over the touchpadArea TextView. Because it's FLAG_NOT_FOCUSABLE, touching
     * the overlay never steals input focus from the virtual display - this prevents the
     * IME on the virtual display from being dismissed when the user swipes.
     *
     * Called from touchpadArea's OnLayoutChangeListener, so it fires once when the
     * layout is first measured (creating the overlay) and again on rotation/resize.
     */
    private void _syncTouchpadOverlay() {
        int width = touchpadArea.getWidth();
        int height = touchpadArea.getHeight();
        if (width == 0 || height == 0) return;

        int[] loc = new int[2];
        touchpadArea.getLocationOnScreen(loc);

        if (touchpadOverlay == null) {
            // Opt-in a11y-overlay path (see _showMouseCursor). Must pair with the cursor
            // path: app overlays on display 0 are hidden globally while Settings is
            // focused, which would freeze touch delivery and strand the a11y cursor.
            if (Pref.getTouchpadAccessibilityOverlay()) {
                TouchpadAccessibilityService service = TouchpadAccessibilityService.getInstance();
                if (service != null) {
                    View overlay = service.addTouchOverlay(Display.DEFAULT_DISPLAY, loc[0], loc[1], width, height);
                    if (overlay != null) {
                        touchpadOverlay = overlay;
                        useAccessibilityTouchOverlay = true;
                        if (inputManager == null) {
                            _setupTouchListenerForAccessibility();
                        } else {
                            _setupTouchListenerForInputManager();
                        }
                        return;
                    }
                }
            }

            touchpadOverlay = new View(this);
            // FLAG_ALT_FOCUSABLE_IM: touchpad overlay (on display 0) claiming IME-focusable lets the IME layer go above it on the phone, which allows the phone-side keyboard appear for inputs on the cast display
            WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT
            );
            params.gravity = Gravity.TOP | Gravity.START;
            params.x = loc[0];
            params.y = loc[1];

            WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            wm.addView(touchpadOverlay, params);

            if (inputManager == null) {
                _setupTouchListenerForAccessibility();
            } else {
                _setupTouchListenerForInputManager();
            }
            return;
        }

        if (useAccessibilityTouchOverlay) {
            TouchpadAccessibilityService service = TouchpadAccessibilityService.getInstance();
            if (service != null) {
                service.updateTouchOverlayBounds(loc[0], loc[1], width, height);
            }
            return;
        }

        WindowManager.LayoutParams params = (WindowManager.LayoutParams) touchpadOverlay.getLayoutParams();
        if (params.x == loc[0] && params.y == loc[1] && params.width == width && params.height == height) {
            return;
        }
        params.x = loc[0];
        params.y = loc[1];
        params.width = width;
        params.height = height;
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        wm.updateViewLayout(touchpadOverlay, params);
    }

    private void _showMouseCursor(Display targetDisplay) {
        // Opt-in TYPE_ACCESSIBILITY_OVERLAY path: app overlays are hidden over System
        // Settings and other secure screens, but accessibility overlays stay visible.
        // Off by default because routing touches through the a11y input filter makes
        // multi-touch gestures slightly less responsive.
        if (Pref.getTouchpadAccessibilityOverlay()) {
            TouchpadAccessibilityService service = TouchpadAccessibilityService.getInstance();
            if (service != null && service.showCursor(displayId, R.drawable.mouse_cursor)) {
                useAccessibilityCursor = true;
                return;
            }
        }

        cursorParams = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
            WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE |
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        );

        cursorParams.x = 0;
        cursorParams.y = 0;

        cursorView = new ImageView(this);
        cursorView.setImageResource(R.drawable.mouse_cursor);

        Context displayContext = createDisplayContext(targetDisplay);
        WindowManager windowManager = (WindowManager) displayContext.getSystemService(Context.WINDOW_SERVICE);

        try {
            windowManager.addView(cursorView, cursorParams);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.show_cursor_failed), Toast.LENGTH_SHORT).show();
            Log.e(TAG, "failed to show cursor: " + e.getMessage());
        }
    }

    private void _updateCursorPosition(float deltaX, float deltaY) {
        cursorX += deltaX * sensitivity;
        cursorY += deltaY * sensitivity;

        if (cursorX < -halfWidth || cursorX > halfWidth ||
            cursorY < -halfHeight || cursorY > halfHeight) {
            Log.w(TAG, "cursor out of bounds - position: (" + cursorX + ", " + cursorY + ")");
        }

        cursorX = Math.max(-halfWidth, Math.min(cursorX, halfWidth));
        cursorY = Math.max(-halfHeight, Math.min(cursorY, halfHeight));

        if (useAccessibilityCursor) {
            TouchpadAccessibilityService service = TouchpadAccessibilityService.getInstance();
            if (service != null) {
                service.updateCursorPosition((int) cursorX, (int) cursorY);
            }
            return;
        }

        if (cursorView != null && cursorParams != null) {
            cursorParams.x = (int) cursorX;
            cursorParams.y = (int) cursorY;
            WindowManager windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
            try {
                windowManager.updateViewLayout(cursorView, cursorParams);
            } catch (Exception e) {
                Log.e(TAG, "failed to update cursor: " + e.getMessage());
            }
        }
    }

    private void _setCursorVisible(boolean visible) {
        if (useAccessibilityCursor) {
            TouchpadAccessibilityService service = TouchpadAccessibilityService.getInstance();
            if (service != null) {
                service.setCursorVisible(visible);
            }
            return;
        }
        if (cursorView != null) {
            cursorView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public static void performBackGesture(IInputManager inputManager, int displayId) {
        new Thread(() -> _performBackGestureSync(inputManager, displayId)).start();
    }

    private static void _performBackGestureSync(IInputManager inputManager, int displayId) {
        TouchpadAccessibilityService accessibilityService = TouchpadAccessibilityService.getInstance();
        if (inputManager != null && _trySetTaskFocus(displayId)) {
            _injectKeyEvent(inputManager, displayId, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0, INJECT_INPUT_EVENT_MODE_ASYNC);
            _injectKeyEvent(inputManager, displayId, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0, 0, INJECT_INPUT_EVENT_MODE_ASYNC);
            return;
        }
        if (accessibilityService != null) {
            accessibilityService.performBackGesture(displayId);
        } else if (inputManager != null) {
            _injectKeyEvent(inputManager, displayId, KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_BACK, 0, 0, INJECT_INPUT_EVENT_MODE_ASYNC);
            _injectKeyEvent(inputManager, displayId, KeyEvent.ACTION_UP, KeyEvent.KEYCODE_BACK, 0, 0, INJECT_INPUT_EVENT_MODE_ASYNC);
        }
    }

    private void _toggleNightMode() {
        isNightModeEnabled = !isNightModeEnabled;
        _applyNightMode();
    }

    private void _applyNightMode() {
        _applyNightModeSurface(touchpadRoot, defaultRootBackground);
        _applyNightModeSurface(topBar, defaultTopBarBackground);
        _applyNightModeSurface(bottomButtons, defaultBottomButtonsBackground);
        if (touchpadArea != null) {
            touchpadArea.setBackgroundResource(
                    isNightModeEnabled ? R.drawable.touchpad_background_night : R.drawable.touchpad_background);
        }
        if (scrollStrip != null) {
            scrollStrip.setBackgroundResource(
                    isNightModeEnabled ? R.drawable.scroll_track_background_night : R.drawable.scroll_track_background);
        }
        _applyStatusBarVisibility();
        if (isNightModeEnabled) {
            int nightColor = getColor(R.color.touchpad_night_button);
            ColorStateList nightTint = ColorStateList.valueOf(nightColor);
            ColorStateList nightIconTint = ColorStateList.valueOf(getColor(R.color.touchpad_night_button_icon));
            for (MaterialButton button : nightModeButtonTints.keySet()) {
                if (button.getIcon() == null) {
                    button.setBackground(new ColorDrawable(nightColor));
                } else {
                    button.setBackgroundTintList(nightTint);
                    button.setIconTint(nightIconTint);
                }
            }
        } else {
            for (Map.Entry<MaterialButton, ColorStateList> entry : nightModeButtonTints.entrySet()) {
                MaterialButton button = entry.getKey();
                if (button.getIcon() == null) {
                    button.setBackground(nightModeTextButtonBackgrounds.get(button));
                } else {
                    button.setBackgroundTintList(entry.getValue());
                    button.setIconTint(nightModeButtonIconTints.get(button));
                }
            }
        }
        _updateHelp();
    }

    private void _applyStatusBarVisibility() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller == null) {
                return;
            }
            if (isNightModeEnabled) {
                controller.hide(WindowInsets.Type.statusBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            } else {
                controller.show(WindowInsets.Type.statusBars());
            }
        } else {
            View decorView = getWindow().getDecorView();
            int systemUiFlags = decorView.getSystemUiVisibility();
            if (isNightModeEnabled) {
                systemUiFlags |= View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            } else {
                systemUiFlags &= ~View.SYSTEM_UI_FLAG_FULLSCREEN;
                systemUiFlags &= ~View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
            }
            decorView.setSystemUiVisibility(systemUiFlags);
        }
    }

    private void _registerNightModeButton(MaterialButton button) {
        if (button != null) {
            nightModeButtonTints.put(button, button.getBackgroundTintList());
            nightModeButtonIconTints.put(button, button.getIconTint());
            if (button.getIcon() == null) {
                nightModeTextButtonBackgrounds.put(button, button.getBackground());
            }
        }
    }

    private void _applyNightModeSurface(View target, Drawable defaultBackground) {
        if (target == null) {
            return;
        }
        if (isNightModeEnabled) {
            target.setBackgroundColor(getColor(R.color.touchpad_night_surface));
        } else {
            target.setBackground(defaultBackground);
        }
    }

    private void _setupScrollStrip() {
        scrollStrip = findViewById(R.id.scrollStrip);
        final float[] lastY = {0};
        scrollStrip.setOnTouchListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    lastY[0] = event.getY();
                    ipcExecutor.execute(() -> setFocus(inputManager, displayId));
                    return true;
                case MotionEvent.ACTION_MOVE:
                    float delta = event.getY() - lastY[0];
                    if (Math.abs(delta) > 5) {
                        float scroll = -delta / 50f;
                        ipcExecutor.execute(() -> _injectScroll(scroll));
                        lastY[0] = event.getY();
                    }
                    return true;
            }
            return false;
        });
    }

    private void _injectScroll(float scrollAmount) {
        if (inputManager != null) {
            long now = SystemClock.uptimeMillis();
            MotionEvent.PointerProperties[] props = {new MotionEvent.PointerProperties()};
            props[0].id = 0;
            props[0].toolType = MotionEvent.TOOL_TYPE_MOUSE;
            MotionEvent.PointerCoords[] coords = {new MotionEvent.PointerCoords()};
            coords[0].x = cursorX + halfWidth;
            coords[0].y = cursorY + halfHeight;
            coords[0].setAxisValue(MotionEvent.AXIS_VSCROLL, scrollAmount);
            MotionEvent event = MotionEvent.obtain(now, now, MotionEvent.ACTION_SCROLL,
                    1, props, coords, 0, 0, 1f, 1f, 0, 0, InputDevice.SOURCE_MOUSE, 0);
            MotionEventHidden eventHidden = Refine.unsafeCast(event);
            eventHidden.setDisplayId(displayId);
            inputManager.injectInputEvent(event, INJECT_INPUT_EVENT_MODE_ASYNC);
            event.recycle();
            return;
        }
        TouchpadAccessibilityService service = TouchpadAccessibilityService.getInstance();
        if (service != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            GestureDescription.Builder builder = new GestureDescription.Builder();
            builder.setDisplayId(displayId);
            float startY = halfHeight - scrollAmount * 100;
            float endY = halfHeight + scrollAmount * 100;
            Path path = new Path();
            path.moveTo(halfWidth, Math.max(0, startY));
            path.lineTo(halfWidth, Math.max(0, endY));
            builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 200));
            service.setFocus(displayId);
            service.dispatchGesture(builder.build(), null, null);
        }
    }

    private static boolean _trySetTaskFocus(int displayId) {
        try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceUtils.getActivityTaskManager().focusTopTask(displayId);
                return true;
            }
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                List<ActivityTaskManager.RootTaskInfo> taskInfos =
                        ServiceUtils.getActivityTaskManager().getAllRootTaskInfosOnDisplay(displayId);
                for (ActivityTaskManager.RootTaskInfo taskInfo : taskInfos) {
                    ServiceUtils.getActivityTaskManager().setFocusedRootTask(taskInfo.taskId);
                    return true;
                }
                return false;
            }
            List<Object> stackInfos = ServiceUtils.getActivityTaskManager().getAllStackInfosOnDisplay(displayId);
            if (!stackInfos.isEmpty()) {
                Object stackInfo = stackInfos.get(0);
                Field stackIdField = stackInfo.getClass().getDeclaredField("stackId");
                stackIdField.setAccessible(true);
                int stackId = stackIdField.getInt(stackInfo);
                ServiceUtils.getActivityTaskManager().setFocusedStack(stackId);
                return true;
            }
        } catch (Throwable e) {
            Log.e(TAG, "failed to set task focus", e);
        }
        return false;
    }

    public static boolean setFocus(IInputManager inputManager, int displayId) {
        if (inputManager != null && _trySetTaskFocus(displayId)) {
            return true;
        }
        try {
            TouchpadAccessibilityService accessibilityService = TouchpadAccessibilityService.getInstance();
            if (accessibilityService != null) {
                return accessibilityService.setFocus(displayId);
            }
        } catch (Throwable e) {
            Log.e(TAG, "failed to set focus", e);
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ipcExecutor.shutdown();
        WindowManager wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        TouchpadAccessibilityService service = TouchpadAccessibilityService.getInstance();
        if (useAccessibilityTouchOverlay) {
            if (service != null) {
                service.removeTouchOverlay();
            }
        } else if (touchpadOverlay != null && touchpadOverlay.getWindowToken() != null) {
            wm.removeView(touchpadOverlay);
        }
        if (useAccessibilityCursor) {
            if (service != null) {
                service.hideCursor();
            }
        } else if (cursorView != null && cursorView.getWindowToken() != null) {
            wm.removeView(cursorView);
        }
    }

    private MotionEvent _obtainMotionEventWithOffset(MotionEvent source, float offsetX, float offsetY) {
        int pointerCount = source.getPointerCount();
        
        MotionEvent.PointerProperties[] properties = new MotionEvent.PointerProperties[pointerCount];
        MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[pointerCount];
        
        for (int i = 0; i < pointerCount; i++) {
            properties[i] = new MotionEvent.PointerProperties();
            source.getPointerProperties(i, properties[i]);

            coords[i] = new MotionEvent.PointerCoords();
            source.getPointerCoords(i, coords[i]);
            coords[i].x += offsetX;
            coords[i].y += offsetY;
        }
        
        return MotionEvent.obtain(
            source.getDownTime(),
            source.getEventTime(),
            source.getAction(),
            pointerCount,
            properties,
            coords,
            source.getMetaState(),
            source.getButtonState(),
            source.getXPrecision(),
            source.getYPrecision(),
            0,
            source.getEdgeFlags(),
            source.getSource(),
            source.getFlags()
        );
    }

    private void _setupModeSpinner() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
            this,
            android.R.layout.simple_spinner_item,
            new String[]{getString(R.string.mode_normal), getString(R.string.mode_cursor_locked)}
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        modeSpinner.setAdapter(adapter);
        
        modeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                switch (position) {
                    case MODE_NORMAL:
                        isCursorLocked = false;
                        _setCursorVisible(true);
                        break;
                    case MODE_CURSOR_LOCKED:
                        isCursorLocked = true;
                        _setCursorVisible(false);
                        break;
                }
                _updateHelp();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // no-op
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Hide only; do NOT remove from WindowManager. The overlay carries
        // FLAG_ALT_FOCUSABLE_IM, which is what gives the phone an IME layering
        // surface for inputs on the cast display. Removing it on pause kills
        // cross-display IME until the activity is resumed.
        if (touchpadOverlay != null) touchpadOverlay.setVisibility(View.GONE);
        _setCursorVisible(false);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (touchpadOverlay != null) touchpadOverlay.setVisibility(View.VISIBLE);
        if (!isCursorLocked) _setCursorVisible(true);
    }

    private static final int[] ORIENTATIONS = {
        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT,
        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
    };

    private void _applyRotation() {
        setRequestedOrientation(ORIENTATIONS[rotation]);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("rotation", rotation);
        outState.putBoolean("night_mode_enabled", isNightModeEnabled);
    }

    private void _switchMode() {
        int currentMode = modeSpinner.getSelectedItemPosition();
        int nextMode = (currentMode + 1) % modeSpinner.getCount();
        modeSpinner.setSelection(nextMode);
    }

    private void _replayGestureViaAccessibility() {
        TouchpadAccessibilityService service = TouchpadAccessibilityService.getInstance();
        if (service == null || gestureState.allMotionEvents.isEmpty() || Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return;
        }

        SparseArray<StrokePoint> startPoints = new SparseArray<>();
        SparseArray<StrokePoint> endPoints = new SparseArray<>();
        long baseTime = gestureState.allMotionEvents.get(0).getDownTime();

        for (MotionEvent event : gestureState.allMotionEvents) {
            int action = event.getActionMasked();
            int pointerIndex = event.getActionIndex();
            
            switch (action) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_POINTER_DOWN:
                    int pointerId = event.getPointerId(pointerIndex);
                    startPoints.put(pointerId, new StrokePoint(
                        Math.max(0, event.getX(pointerIndex)),
                        Math.max(0, event.getY(pointerIndex)),
                        event.getEventTime() - baseTime
                    ));
                    break;
                    
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_POINTER_UP:
                    pointerId = event.getPointerId(pointerIndex);
                    endPoints.put(pointerId, new StrokePoint(
                        Math.max(0, event.getX(pointerIndex)),
                        Math.max(0, event.getY(pointerIndex)),
                        event.getEventTime() - baseTime
                    ));
                    break;
                    
                case MotionEvent.ACTION_MOVE:
                    for (int i = 0; i < event.getPointerCount(); i++) {
                        pointerId = event.getPointerId(i);
                        endPoints.put(pointerId, new StrokePoint(
                            Math.max(0, event.getX(i)),
                            Math.max(0, event.getY(i)),
                            event.getEventTime() - baseTime
                        ));
                    }
                    break;
            }
        }

        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.setDisplayId(displayId);

        for (int i = 0; i < startPoints.size(); i++) {
            int pointerId = startPoints.keyAt(i);
            StrokePoint start = startPoints.get(pointerId);
            StrokePoint end = endPoints.get(pointerId);

            if (end == null) {
                end = start;
            }

            Path strokePath = new Path();
            strokePath.moveTo(start.x, start.y);
            strokePath.lineTo(end.x, end.y);

            long duration = end.time - start.time;
            if (duration <= 0) duration = 100;

            gestureBuilder.addStroke(new GestureDescription.StrokeDescription(
                strokePath,
                start.time,
                duration,
                false
            ));
        }

        if (startPoints.size() > 0) {
            GestureDescription gestureDescription = gestureBuilder.build();
            service.setFocus(displayId);
            service.dispatchGesture(gestureDescription, null, null);
        }
    }
}
