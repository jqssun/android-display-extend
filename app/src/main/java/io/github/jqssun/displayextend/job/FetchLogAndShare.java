package io.github.jqssun.displayextend.job;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import io.github.jqssun.displayextend.R;
import io.github.jqssun.displayextend.State;

import rikka.shizuku.Shizuku;

import java.io.File;
import java.io.IOException;

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
            Toast.makeText(State.currentActivity.get(), State.currentActivity.get().getString(R.string.user_service_unavailable), Toast.LENGTH_SHORT).show();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                State.currentActivity.get().startActivity(intent);
                Toast.makeText(State.currentActivity.get(), State.currentActivity.get().getString(R.string.grant_file_access), Toast.LENGTH_LONG).show();
                return;
            }
        }

        try {
            File downloadLogFile = new File(android.os.Environment.getExternalStoragePublicDirectory(
                    android.os.Environment.DIRECTORY_DOWNLOADS), "Extend.log");
            
            if (downloadLogFile.exists()) {
                downloadLogFile.delete();
            }

            State.userService.fetchLogs();

            if (!downloadLogFile.exists()) {
                Toast.makeText(State.currentActivity.get(), State.currentActivity.get().getString(R.string.log_not_generated), Toast.LENGTH_SHORT).show();
                return;
            }

            File cacheDir = State.currentActivity.get().getCacheDir();
            File cacheCopyFile = new File(cacheDir, "Extend.log");

            java.nio.file.Files.copy(
                downloadLogFile.toPath(),
                cacheCopyFile.toPath(),
                java.nio.file.StandardCopyOption.REPLACE_EXISTING
            );

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            Uri fileUri = FileProvider.getUriForFile(State.currentActivity.get(),
                    State.currentActivity.get().getPackageName() + ".provider",
                    cacheCopyFile);
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            State.currentActivity.get().startActivity(Intent.createChooser(shareIntent, State.currentActivity.get().getString(R.string.share_log)));
            
        } catch (RemoteException | IOException e) {
            Toast.makeText(State.currentActivity.get(), State.currentActivity.get().getString(R.string.check_download_log), Toast.LENGTH_LONG).show();
            throw new RuntimeException(e);
        }
    }
}
