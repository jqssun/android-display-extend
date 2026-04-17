package android.app;

import android.content.ComponentName;
import android.content.Intent;

public class TaskInfo {
    public int taskId;
    public boolean isRunning;
    public Intent baseIntent;
    public ComponentName baseActivity;
    public ComponentName topActivity;
    public long lastActiveTime;
    public int displayId;
    public int topActivityType;
    public boolean isVisibleRequested;
    public boolean isFocused;
    public boolean isVisible;
}
