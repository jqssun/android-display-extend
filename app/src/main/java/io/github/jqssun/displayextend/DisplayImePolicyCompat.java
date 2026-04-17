package io.github.jqssun.displayextend;

import android.content.Context;
import android.view.WindowManager;

final class DisplayImePolicyCompat {
    static final int LOCAL = _resolveConstant("DISPLAY_IME_POLICY_LOCAL", 0);
    static final int FALLBACK_DISPLAY = _resolveConstant("DISPLAY_IME_POLICY_FALLBACK_DISPLAY", 1);
    static final int HIDE = _resolveConstant("DISPLAY_IME_POLICY_HIDE", 2);

    private DisplayImePolicyCompat() {
    }

    static int[] values() {
        return new int[]{LOCAL, FALLBACK_DISPLAY, HIDE};
    }

    static String[] labels(Context context) {
        return new String[]{
                context.getString(R.string.ime_policy_local),
                context.getString(R.string.ime_policy_fallback_display),
                context.getString(R.string.ime_policy_hide)
        };
    }

    static String toDebugString(int policy) {
        if (policy == LOCAL) {
            return "LOCAL";
        }
        if (policy == FALLBACK_DISPLAY) {
            return "FALLBACK_DISPLAY";
        }
        if (policy == HIDE) {
            return "HIDE";
        }
        return String.valueOf(policy);
    }

    static int indexOf(int policy) {
        int[] values = values();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == policy) {
                return i;
            }
        }
        return 0;
    }

    private static int _resolveConstant(String fieldName, int fallback) {
        try {
            return WindowManager.class.getField(fieldName).getInt(null);
        } catch (Throwable ignored) {
            return fallback;
        }
    }
}
