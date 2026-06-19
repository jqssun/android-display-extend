package io.github.jqssun.displayextend;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class AccessibilityConsentActivity extends AppCompatActivity {

  public static void start(Context context) {
    Intent intent = new Intent(context, AccessibilityConsentActivity.class);
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    context.startActivity(intent);
  }

  @Override
  protected void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    new MaterialAlertDialogBuilder(this)
        .setTitle(R.string.accessibility_consent_title)
        .setMessage(R.string.accessibility_consent_message)
        .setCancelable(false)
        .setPositiveButton(
            R.string.accessibility_consent_agree,
            (d, w) -> {
              Pref.setAccessibilityConsent(this, true);
              TouchpadAccessibilityService.ensureServiceAvailable(this, true);
              finish();
            })
        .setNegativeButton(R.string.accessibility_consent_decline, (d, w) -> finish())
        .show();
  }
}
