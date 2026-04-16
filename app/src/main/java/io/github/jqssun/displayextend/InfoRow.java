package io.github.jqssun.displayextend;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.ContextMenu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

public class InfoRow {

    public static void add(Context ctx, LinearLayout table, String label, String value) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int dp16 = (int) (16 * density);
        int dp12 = (int) (12 * density);
        int dp2 = (int) (2 * density);
        int dp56 = (int) (56 * density);

        LinearLayout item = new LinearLayout(ctx);
        item.setOrientation(LinearLayout.VERTICAL);
        item.setPadding(dp16, dp12, dp16, dp12);
        item.setMinimumHeight(dp56);

        TextView labelTv = new TextView(ctx);
        labelTv.setText(label);
        labelTv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyLarge);
        item.addView(labelTv);

        TextView valueTv = new TextView(ctx);
        valueTv.setText(value);
        valueTv.setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium);
        valueTv.setTextColor(ctx.getResources().getColor(android.R.color.darker_gray, null));
        valueTv.setPadding(0, dp2, 0, 0);
        item.addView(valueTv);

        item.setLongClickable(true);
        item.setOnCreateContextMenuListener((menu, v, menuInfo) -> {
            menu.setHeaderTitle(label);
            MenuItem copyItem = menu.add(R.string.copy);
            copyItem.setOnMenuItemClickListener(mi -> {
                ClipboardManager clipboard = (ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE);
                clipboard.setPrimaryClip(ClipData.newPlainText(label, value));
                return true;
            });
        });

        table.addView(item);
    }
}
