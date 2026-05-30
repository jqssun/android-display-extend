package io.github.jqssun.displayextend.job;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;
import java.io.File;
import java.io.IOException;
import rikka.shizuku.Shizuku;

public class FetchLogAndShare implements Job {
  private final AcquireShizuku acquireShizuku = new AcquireShizuku();
  private boolean userServiceRequested = false;

  private final Context context;

  public FetchLogAndShare(Context context) {
    this.context = context;
  }

  @Override
  public void start() throws YieldException {
    acquireShizuku.start();
    if (!acquireShizuku.acquired) {
      return;
    }
    if (State.userService == null) {
      if (!userServiceRequested) {
        userServiceRequested = true;
        Shizuku.peekUserService(State.userServiceArgs, State.userServiceConnection);
        Shizuku.bindUserService(State.userServiceArgs, State.userServiceConnection);
        State.resumeJobLater(1000);
        throw new YieldException("waiting for user service");
      }
      Toast.makeText(
              State.currentActivity.get(),
              State.currentActivity.get().getString(R.string.user_service_unavailable),
              Toast.LENGTH_SHORT)
          .show();
      return;
    }

    try {
      File logFile = new File(State.currentActivity.get().getCacheDir(), "Extend.log");
      try (ParcelFileDescriptor sink =
          ParcelFileDescriptor.open(
              logFile,
              ParcelFileDescriptor.MODE_CREATE
                  | ParcelFileDescriptor.MODE_WRITE_ONLY
                  | ParcelFileDescriptor.MODE_TRUNCATE)) {
        State.userService.fetchLogs(sink);
      }

      if (logFile.length() == 0) {
        Toast.makeText(
                State.currentActivity.get(),
                State.currentActivity.get().getString(R.string.no_logs_to_export),
                Toast.LENGTH_SHORT)
            .show();
        return;
      }

      Intent shareIntent = new Intent(Intent.ACTION_SEND);
      shareIntent.setType("text/plain");
      Uri fileUri =
          FileProvider.getUriForFile(
              State.currentActivity.get(),
              State.currentActivity.get().getPackageName() + ".provider",
              logFile);
      shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
      shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
      State.currentActivity
          .get()
          .startActivity(
              Intent.createChooser(
                  shareIntent, State.currentActivity.get().getString(R.string.share_log)));

    } catch (RemoteException | IOException e) {
      Toast.makeText(
              State.currentActivity.get(),
              State.currentActivity.get().getString(R.string.log_export_failed),
              Toast.LENGTH_LONG)
          .show();
      throw new RuntimeException(e);
    }
  }
}
