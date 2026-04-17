package io.github.jqssun.displayextend;

import android.app.ActivityTaskManager;
import android.app.WindowConfiguration;
import android.content.ComponentName;
import android.content.Intent;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public final class TaskInfoCompat {
    private TaskInfoCompat() {
    }

    public static int getTaskId(ActivityTaskManager.RootTaskInfo taskInfo) {
        return _readInt(taskInfo, "taskId", 0);
    }

    public static boolean isRunning(ActivityTaskManager.RootTaskInfo taskInfo) {
        return _readBoolean(taskInfo, "isRunning", false);
    }

    public static int getDisplayId(ActivityTaskManager.RootTaskInfo taskInfo) {
        return _readInt(taskInfo, "displayId", 0);
    }

    public static long getLastActiveTime(ActivityTaskManager.RootTaskInfo taskInfo) {
        return _readLong(taskInfo, "lastActiveTime", 0L);
    }

    public static boolean isVisibleRequested(ActivityTaskManager.RootTaskInfo taskInfo) {
        return _readBoolean(taskInfo, "isVisibleRequested", false);
    }

    public static boolean isVisible(ActivityTaskManager.RootTaskInfo taskInfo) {
        return _readBoolean(taskInfo, "isVisible", false);
    }

    public static boolean isFocused(ActivityTaskManager.RootTaskInfo taskInfo) {
        return _readBoolean(taskInfo, "isFocused", false);
    }

    public static int getActivityType(ActivityTaskManager.RootTaskInfo taskInfo) {
        Integer activityType = _invokeInt(taskInfo, "getActivityType");
        if (activityType != null && activityType != WindowConfiguration.ACTIVITY_TYPE_UNDEFINED) {
            return activityType;
        }
        return _readInt(taskInfo, "topActivityType", WindowConfiguration.ACTIVITY_TYPE_UNDEFINED);
    }

    public static String getPackageName(ActivityTaskManager.RootTaskInfo taskInfo) {
        ComponentName topActivity = (ComponentName) _readField(taskInfo, "topActivity");
        ComponentName baseActivity = (ComponentName) _readField(taskInfo, "baseActivity");
        Intent baseIntent = (Intent) _readField(taskInfo, "baseIntent");

        ComponentName componentName = topActivity != null ? topActivity : baseActivity;
        if (componentName != null) {
            return componentName.getPackageName();
        }
        if (baseIntent == null) {
            return null;
        }
        if (baseIntent.getComponent() != null) {
            return baseIntent.getComponent().getPackageName();
        }
        return baseIntent.getPackage();
    }

    private static Object _readField(Object instance, String fieldName) {
        if (instance == null) {
            return null;
        }
        try {
            Field field = instance.getClass().getField(fieldName);
            return field.get(instance);
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static int _readInt(Object instance, String fieldName, int defaultValue) {
        Object value = _readField(instance, fieldName);
        return value instanceof Integer ? (Integer) value : defaultValue;
    }

    private static long _readLong(Object instance, String fieldName, long defaultValue) {
        Object value = _readField(instance, fieldName);
        return value instanceof Long ? (Long) value : defaultValue;
    }

    private static boolean _readBoolean(Object instance, String fieldName, boolean defaultValue) {
        Object value = _readField(instance, fieldName);
        return value instanceof Boolean ? (Boolean) value : defaultValue;
    }

    private static Integer _invokeInt(Object instance, String methodName) {
        if (instance == null) {
            return null;
        }
        try {
            Method method = instance.getClass().getMethod(methodName);
            Object value = method.invoke(instance);
            return value instanceof Integer ? (Integer) value : null;
        } catch (Throwable ignored) {
            return null;
        }
    }
}
