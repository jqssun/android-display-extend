package io.github.jqssun.displayextend;

import android.accessibilityservice.AccessibilityService;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;

import io.github.jqssun.displayextend.shizuku.PermissionManager;

import java.util.ArrayList;
import java.util.List;

public class TouchpadAccessibilityService extends AccessibilityService {
    private static TouchpadAccessibilityService instance;

    public static boolean isAccessibilityServiceEnabled(Context context) {
        String serviceName = context.getPackageName() + "/" + TouchpadAccessibilityService.class.getCanonicalName();
        String enabledServices = Settings.Secure.getString(
                context.getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        );

        if (enabledServices != null) {
            return enabledServices.contains(serviceName);
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
            Intent serviceIntent = new Intent(context, TouchpadAccessibilityService.class);
            context.startService(serviceIntent);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;
        State.log("TouchpadAccessibilityService connected");
    }
    
    @Override
    public boolean onUnbind(Intent intent) {
        instance = null;
        State.log("TouchpadAccessibilityService disconnected");
        return super.onUnbind(intent);
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

    private List<AccessibilityNodeInfo> _findFocusableNodes(AccessibilityNodeInfo root, List<AccessibilityNodeInfo> results) {
        if (root == null) return results;
        
        if (root.isFocusable()) {
            results.add(root);
            if (results.size() >= 3) {
                return results;
            }
        }
        
        for (int i = 0; i < root.getChildCount(); i++) {
            AccessibilityNodeInfo child = root.getChild(i);
            if (child != null) {
                _findFocusableNodes(child, results);
                if (results.size() >= 3) {
                    return results;
                }
            }
        }
        
        return results;
    }

    public boolean setFocus(int displayId) {
        List<AccessibilityWindowInfo> targetDisplayWindows = null;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.R) {
            targetDisplayWindows = getWindows();
        } else {
            SparseArray<List<AccessibilityWindowInfo>> windows = getWindowsOnAllDisplays();
            targetDisplayWindows = windows.get(displayId);
        }
        android.util.Log.d("AccessibilityService", "window list: " + (targetDisplayWindows != null ? targetDisplayWindows.size() : 0) + " windows");
        
        if (targetDisplayWindows != null && !targetDisplayWindows.isEmpty()) {
            AccessibilityWindowInfo topWindow = null;
            int topLayer = -1;
            
            for (AccessibilityWindowInfo window : targetDisplayWindows) {
                if (window.getType() != AccessibilityWindowInfo.TYPE_APPLICATION) {
                    continue;
                }
                if (window.getDisplayId() == displayId) {
                    if (window.getLayer() > topLayer) {
                        topLayer = window.getLayer();
                        topWindow = window;
                    }
                }
            }
            if (topWindow == null) {
                android.util.Log.d("AccessibilityService", "could not find top window");
                return false;
            }
            if (topWindow.isFocused()) {
                android.util.Log.d("AccessibilityService", "already focused");
                return false;
            }
            android.util.Log.d("AccessibilityService", "found top window, layer: " + topLayer);

            AccessibilityNodeInfo rootNode = topWindow.getRoot();
            android.util.Log.d("AccessibilityService", "root node: " + (rootNode != null ? "found" : "null"));

            if (rootNode != null) {
                try {
                    List<AccessibilityNodeInfo> focusableNodes = _findFocusableNodes(rootNode, new ArrayList<>());
                    android.util.Log.d("AccessibilityService", "found " + focusableNodes.size() + " focusable nodes");

                    boolean focusSuccess = false;
                    for (AccessibilityNodeInfo node : focusableNodes) {
                        try {
                            boolean focusResult = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                            android.util.Log.d("AccessibilityService", "set focus: " + (focusResult ? "success" : "failed"));
                            if (focusResult) {
                                focusSuccess = true;
                                break;
                            } else {
                                focusResult = node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                                if (focusResult) {
                                    focusSuccess = true;
                                    break;
                                } else {
                                    // add to blacklist
                                }
                            }
                        } catch(Throwable e) {
                            try {
                                boolean focusResult = node.performAction(AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS);
                                if (focusResult) {
                                    focusSuccess = true;
                                    break;
                                }
                            } catch (Throwable e2) {
                                // ignore
                            }
                        }
                    }

                    try {
                        for (AccessibilityNodeInfo node : focusableNodes) {
                            node.recycle();
                        }
                    } catch(Throwable e) {
                        // ignore
                    }

                    if (focusSuccess) {
                        return true;
                    } else {
                        android.util.Log.d("AccessibilityService", "no nodes could be focused");
                    }
                } finally {
                    try {
                        rootNode.recycle();
                    } catch(Throwable e) {
                        // ignore
                    }
                    android.util.Log.d("AccessibilityService", "recycled root node");
                }
            }
        }
        return false;
    }

    public void performBackGesture(int displayId) {
        android.util.Log.d("AccessibilityService", "performing back gesture, displayId: " + displayId);
        if (setFocus(displayId)) {
            boolean backResult = performGlobalAction(GLOBAL_ACTION_BACK);
            android.util.Log.d("AccessibilityService", "back action: " + (backResult ? "success" : "failed"));
        }
    }
}