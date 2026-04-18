package io.github.jqssun.displayextend;

import android.accessibilityservice.AccessibilityService;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.util.SparseArray;
import android.view.Display;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import android.widget.ImageView;

import io.github.jqssun.displayextend.shizuku.PermissionManager;
import io.github.jqssun.displayextend.shizuku.ShizukuUtils;

import java.util.ArrayList;
import java.util.List;

public class TouchpadAccessibilityService extends AccessibilityService {
    private static TouchpadAccessibilityService instance;

    private ImageView cursorView;
    private WindowManager cursorWindowManager;
    private WindowManager.LayoutParams cursorParams;
    private int cursorDisplayId = Display.INVALID_DISPLAY;

    private View touchOverlayView;
    private WindowManager touchOverlayWindowManager;
    private WindowManager.LayoutParams touchOverlayParams;

    public static boolean isAccessibilityServiceEnabled(Context context) {
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );
        if (enabledServices == null || enabledServices.isEmpty()) {
            return false;
        }
        ComponentName serviceComponent = new ComponentName(context, TouchpadAccessibilityService.class);
        String flattened = serviceComponent.flattenToString();
        String flattenedShort = serviceComponent.flattenToShortString();
        for (String enabledService : enabledServices.split(":")) {
            String normalized = enabledService.trim();
            if (normalized.equals(flattened) || normalized.equals(flattenedShort)) {
                return true;
            }
        }
        return false;
    }

    public static void startServiceByShizuku(Context context) {
        if(TouchpadAccessibilityService.getInstance() != null) {
            return;
        }
        if (PermissionManager.grant("android.permission.WRITE_SECURE_SETTINGS")) {
            String existingServices = Settings.Secure.getString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);

            String newService = "io.github.jqssun.displayextend/.TouchpadAccessibilityService";
            String finalServices;

            if (existingServices != null && !existingServices.isEmpty()) {
                finalServices = existingServices + ":" + newService;
            } else {
                finalServices = newService;
            }

            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, finalServices);
            Settings.Secure.putString(context.getContentResolver(), Settings.Secure.ACCESSIBILITY_ENABLED, "1");
            _startLocalService(context);
        }
    }

    public static boolean ensureServiceAvailable(Context context, boolean openSettingsFallback) {
        if (isAccessibilityServiceEnabled(context)) {
            _startLocalService(context);
            return true;
        }
        if (ShizukuUtils.hasPermission()) {
            startServiceByShizuku(context);
            if (isAccessibilityServiceEnabled(context)) {
                _startLocalService(context);
                return true;
            }
        }
        if (openSettingsFallback) {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        }
        return false;
    }

    private static void _startLocalService(Context context) {
        Intent serviceIntent = new Intent(context, TouchpadAccessibilityService.class);
        context.startService(serviceIntent);
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        State.log("TouchpadAccessibilityService connected");
        State.refreshUI();
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        removeTouchOverlay();
        hideCursor();
        instance = null;
        State.log("TouchpadAccessibilityService disconnected");
        State.refreshUI();
        return super.onUnbind(intent);
    }

    public boolean showCursor(int displayId, int iconRes) {
        if (cursorView != null && cursorDisplayId == displayId) {
            cursorView.setVisibility(View.VISIBLE);
            return true;
        }
        hideCursor();

        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display targetDisplay = displayManager != null ? displayManager.getDisplay(displayId) : null;
        if (targetDisplay == null) {
            return false;
        }

        Context displayContext = createDisplayContext(targetDisplay);
        WindowManager wm = (WindowManager) displayContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return false;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.x = 0;
        params.y = 0;

        ImageView cursor = new ImageView(displayContext);
        cursor.setImageResource(iconRes);

        try {
            wm.addView(cursor, params);
        } catch (Throwable e) {
            Log.e("AccessibilityService", "failed to add cursor overlay: " + e.getMessage());
            return false;
        }

        cursorView = cursor;
        cursorWindowManager = wm;
        cursorParams = params;
        cursorDisplayId = displayId;
        return true;
    }

    public void updateCursorPosition(int x, int y) {
        if (cursorView == null || cursorWindowManager == null || cursorParams == null) {
            return;
        }
        cursorParams.x = x;
        cursorParams.y = y;
        try {
            cursorWindowManager.updateViewLayout(cursorView, cursorParams);
        } catch (Throwable e) {
            Log.e("AccessibilityService", "failed to update cursor overlay: " + e.getMessage());
        }
    }

    public void setCursorVisible(boolean visible) {
        if (cursorView != null) {
            cursorView.setVisibility(visible ? View.VISIBLE : View.GONE);
        }
    }

    public void hideCursor() {
        if (cursorView != null && cursorWindowManager != null) {
            try {
                cursorWindowManager.removeView(cursorView);
            } catch (Throwable ignored) {
            }
        }
        cursorView = null;
        cursorWindowManager = null;
        cursorParams = null;
        cursorDisplayId = Display.INVALID_DISPLAY;
    }

    public View addTouchOverlay(int displayId, int x, int y, int width, int height) {
        removeTouchOverlay();

        DisplayManager displayManager = (DisplayManager) getSystemService(Context.DISPLAY_SERVICE);
        Display targetDisplay = displayManager != null ? displayManager.getDisplay(displayId) : null;
        if (targetDisplay == null) {
            return null;
        }

        Context displayContext = createDisplayContext(targetDisplay);
        WindowManager wm = (WindowManager) displayContext.getSystemService(Context.WINDOW_SERVICE);
        if (wm == null) {
            return null;
        }

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                width, height,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM,
                PixelFormat.TRANSLUCENT);
        params.gravity = android.view.Gravity.TOP | android.view.Gravity.START;
        params.x = x;
        params.y = y;

        View overlay = new View(displayContext);

        try {
            wm.addView(overlay, params);
        } catch (Throwable e) {
            Log.e("AccessibilityService", "failed to add touch overlay: " + e.getMessage());
            return null;
        }

        touchOverlayView = overlay;
        touchOverlayWindowManager = wm;
        touchOverlayParams = params;
        return overlay;
    }

    public void updateTouchOverlayBounds(int x, int y, int width, int height) {
        if (touchOverlayView == null || touchOverlayWindowManager == null || touchOverlayParams == null) {
            return;
        }
        touchOverlayParams.x = x;
        touchOverlayParams.y = y;
        touchOverlayParams.width = width;
        touchOverlayParams.height = height;
        try {
            touchOverlayWindowManager.updateViewLayout(touchOverlayView, touchOverlayParams);
        } catch (Throwable e) {
            Log.e("AccessibilityService", "failed to update touch overlay: " + e.getMessage());
        }
    }

    public void removeTouchOverlay() {
        if (touchOverlayView != null && touchOverlayWindowManager != null) {
            try {
                touchOverlayWindowManager.removeView(touchOverlayView);
            } catch (Throwable ignored) {
            }
        }
        touchOverlayView = null;
        touchOverlayWindowManager = null;
        touchOverlayParams = null;
    }
    
    public static TouchpadAccessibilityService getInstance() {
        return instance;
    }

    public static void disableAll(Context context) {
        if (PermissionManager.grant("android.permission.WRITE_SECURE_SETTINGS")) {
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES, "");
            Settings.Secure.putString(context.getContentResolver(),
                    Settings.Secure.ACCESSIBILITY_ENABLED, "0");
        }
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getKeyCode() == KeyEvent.KEYCODE_HOME && State.lastSingleAppDisplay > 0) {
            TouchpadActivity.launchLastPackage(getApplicationContext(), State.lastSingleAppDisplay);
            return true;
        }
        return false;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
    }

    @Override
    public void onInterrupt() {
    }

    private boolean _isActionableNode(AccessibilityNodeInfo node) {
        if (node == null || !node.isVisibleToUser()) {
            return false;
        }
        return node.isFocusable()
                || node.isClickable()
                || node.isLongClickable()
                || node.isCheckable()
                || node.isScrollable()
                || node.isEditable();
    }

    private List<AccessibilityNodeInfo> _findActionableNodes(AccessibilityNodeInfo root, List<AccessibilityNodeInfo> results) {
        if (root == null) return results;
        
        if (_isActionableNode(root)) {
            results.add(root);
            if (results.size() >= 5) {
                return results;
            }
        }
        
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                _findActionableNodes(child, results);
                if (results.size() >= 5) {
                    return results;
                }
            }
        }
        
        return results;
    }

    private List<AccessibilityWindowInfo> _getWindowsForDisplay(int displayId) {
        List<AccessibilityWindowInfo> targetDisplayWindows = null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            SparseArray<List<AccessibilityWindowInfo>> windows = getWindowsOnAllDisplays();
            if (windows != null) {
                targetDisplayWindows = windows.get(displayId);
            }
        }
        if (targetDisplayWindows == null || targetDisplayWindows.isEmpty()) {
            List<AccessibilityWindowInfo> fallbackWindows = getWindows();
            if (fallbackWindows != null && !fallbackWindows.isEmpty()) {
                List<AccessibilityWindowInfo> filteredWindows = new ArrayList<>();
                for (AccessibilityWindowInfo window : fallbackWindows) {
                    if (PlatformCompat.windowMatchesDisplay(window, displayId)) {
                        filteredWindows.add(window);
                    }
                }
                if (!filteredWindows.isEmpty()) {
                    targetDisplayWindows = filteredWindows;
                }
            }
        }
        return targetDisplayWindows;
    }

    private AccessibilityWindowInfo _findTopActionableWindow(int displayId) {
        List<AccessibilityWindowInfo> targetDisplayWindows = _getWindowsForDisplay(displayId);
        android.util.Log.d("AccessibilityService", "window list: " + (targetDisplayWindows != null ? targetDisplayWindows.size() : 0) + " windows");
        
        if (targetDisplayWindows == null || targetDisplayWindows.isEmpty()) {
            return null;
        }

        AccessibilityWindowInfo topWindow = null;
        int topLayer = Integer.MIN_VALUE;

        for (AccessibilityWindowInfo window : targetDisplayWindows) {
            if (!PlatformCompat.windowMatchesDisplay(window, displayId)) {
                continue;
            }
            AccessibilityNodeInfo rootNode = window.getRoot();
            if (rootNode == null) {
                continue;
            }
            boolean actionable = false;
            try {
                List<AccessibilityNodeInfo> actionableNodes = _findActionableNodes(rootNode, new ArrayList<>());
                actionable = !actionableNodes.isEmpty();
                for (AccessibilityNodeInfo node : actionableNodes) {
                    try {
                        node.recycle();
                    } catch (Throwable ignored) {
                    }
                }
            } finally {
                try {
                    rootNode.recycle();
                } catch (Throwable ignored) {
                }
            }
            if (!actionable) {
                continue;
            }
            if (window.getLayer() > topLayer) {
                topLayer = window.getLayer();
                topWindow = window;
            }
        }
        return topWindow;
    }

    public boolean setFocus(int displayId) {
        AccessibilityWindowInfo topWindow = _findTopActionableWindow(displayId);
        if (topWindow == null) {
            android.util.Log.d("AccessibilityService", "could not find actionable top window");
            return false;
        }
        if (topWindow.isFocused()) {
            android.util.Log.d("AccessibilityService", "top actionable window already focused");
            return true;
        }
        android.util.Log.d("AccessibilityService", "found top actionable window, type: " + topWindow.getType() + ", layer: " + topWindow.getLayer());

        AccessibilityNodeInfo rootNode = topWindow.getRoot();
        android.util.Log.d("AccessibilityService", "root node: " + (rootNode != null ? "found" : "null"));
        if (rootNode == null) {
            return false;
        }

        boolean rootRecycled = false;
        try {
            List<AccessibilityNodeInfo> actionableNodes = _findActionableNodes(rootNode, new ArrayList<>());
            android.util.Log.d("AccessibilityService", "found " + actionableNodes.size() + " actionable nodes");

            boolean focusSuccess = false;
            for (AccessibilityNodeInfo node : actionableNodes) {
                try {
                    boolean focusResult = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                    android.util.Log.d("AccessibilityService", "set focus: " + (focusResult ? "success" : "failed"));
                    if (focusResult) {
                        focusSuccess = true;
                        break;
                    }
                    try {
                        focusResult = node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                        if (focusResult) {
                            focusSuccess = true;
                            break;
                        }
                    } catch (Throwable ignored) {
                    }
                } catch (Throwable ignored) {
                    try {
                        boolean focusResult = node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                        if (focusResult) {
                            focusSuccess = true;
                            break;
                        }
                    } catch (Throwable ignored2) {
                    }
                }
            }
            try {
                for (AccessibilityNodeInfo node : actionableNodes) {
                    node.recycle();
                    if (node == rootNode) {
                        rootRecycled = true;
                    }
                }
            } catch (Throwable ignored) {
            }
            return focusSuccess;
        } finally {
            if (!rootRecycled) {
                try {
                    rootNode.recycle();
                } catch (Throwable ignored) {
                }
            }
            android.util.Log.d("AccessibilityService", "recycled root node");
        }
    }

    public void performBackGesture(int displayId) {
        android.util.Log.d("AccessibilityService", "performing back gesture, displayId: " + displayId);
        setFocus(displayId);
        boolean backResult = performGlobalAction(GLOBAL_ACTION_BACK);
        android.util.Log.d("AccessibilityService", "back action: " + (backResult ? "success" : "failed"));
    }
}
