package io.github.jqssun.displayextend;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.widget.Toast;

import java.util.HashSet;
import java.util.Set;

public final class MirrorIntegrationHelper {
    private static final String MIRROR_PACKAGE_NAME = "io.github.jqssun.displaymirror";
    private static final String MIRROR_OPEN_OVERVIEW_ACTION =
            "io.github.jqssun.displaymirror.action.OPEN_OVERVIEW";
    private static final String MIRROR_OPEN_TOUCHSCREEN_ACTION =
            "io.github.jqssun.displaymirror.action.OPEN_TOUCHSCREEN";
    private static final String MIRROR_EXTRA_SOURCE_SCREEN = "source_screen";
    private static final String MIRROR_SOURCE_EXTEND_OVERVIEW = "extend_overview";
    private static final String MIRROR_EXTRA_DISPLAY_ID = "display_id";
    private static final Uri MIRROR_PROJECT_URI =
            Uri.parse("https://github.com/jqssun/android-display-mirror");
    private static final Uri MIRROR_TOUCHSCREEN_DISPLAYS_URI =
            Uri.parse("content://io.github.jqssun.displaymirror.touchscreen/displays");
    private static final String COLUMN_DISPLAY_ID = "display_id";

    private MirrorIntegrationHelper() {
    }

    public static void openMirrorOverview(Context context) {
        Intent intent = new Intent(MIRROR_OPEN_OVERVIEW_ACTION);
        intent.setPackage(MIRROR_PACKAGE_NAME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(MIRROR_EXTRA_SOURCE_SCREEN, MIRROR_SOURCE_EXTEND_OVERVIEW);
        _startOrFallback(context, intent);
    }

    public static void openMirrorTouchscreen(Context context, int displayId) {
        Intent intent = new Intent(MIRROR_OPEN_TOUCHSCREEN_ACTION);
        intent.setPackage(MIRROR_PACKAGE_NAME);
        intent.addCategory(Intent.CATEGORY_DEFAULT);
        intent.putExtra(MIRROR_EXTRA_DISPLAY_ID, displayId);
        _startOrFallback(context, intent);
    }

    public static boolean isTouchscreenAvailableForDisplay(Context context, int displayId) {
        return getTouchscreenDisplayIds(context).contains(displayId);
    }

    public static Set<Integer> getTouchscreenDisplayIds(Context context) {
        Set<Integer> displayIds = new HashSet<>();
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(
                    MIRROR_TOUCHSCREEN_DISPLAYS_URI,
                    new String[]{COLUMN_DISPLAY_ID},
                    null,
                    null,
                    null
            );
            if (cursor == null) {
                return displayIds;
            }
            int displayIdIndex = cursor.getColumnIndex(COLUMN_DISPLAY_ID);
            while (cursor.moveToNext()) {
                if (displayIdIndex >= 0) {
                    displayIds.add(cursor.getInt(displayIdIndex));
                }
            }
        } catch (Exception ignored) {
            // Treat provider unavailability the same as no active touchscreen-capable displays.
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return displayIds;
    }

    private static void _startOrFallback(Context context, Intent intent) {
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            return;
        }
        Toast.makeText(context, R.string.mirror_app_not_installed, Toast.LENGTH_SHORT).show();
        context.startActivity(new Intent(Intent.ACTION_VIEW, MIRROR_PROJECT_URI));
    }
}
