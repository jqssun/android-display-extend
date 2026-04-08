package io.github.jqssun.displayextend;

import static android.app.Activity.RESULT_OK;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Binder;
import android.os.IBinder;

public class MediaProjectionService extends Service {

    public static Service instance;
    public static boolean isStarting = false;
    private final IBinder binder = new LocalBinder();
    private static final int NOTIFICATION_ID = 1;
    private static final String CHANNEL_ID = "MediaProjectionServiceChannel";

    public class LocalBinder extends Binder {
        MediaProjectionService getService() {
            return MediaProjectionService.this;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        State.log("MediaProjectionService onCreate");
        _createNotificationChannel();
        startForeground(NOTIFICATION_ID, _createNotification());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        State.log("MediaProjectionService onStartCommand");
        isStarting = false;
        if (intent != null && intent.hasExtra("data")) {
            MediaProjectionManager mediaProjectionManager = (MediaProjectionManager) getSystemService(Context.MEDIA_PROJECTION_SERVICE);
            Intent data = intent.getParcelableExtra("data");
            State.setMediaProjection(mediaProjectionManager.getMediaProjection(RESULT_OK, data));
            State.getMediaProjection().registerCallback(new MediaProjection.Callback() {
                @Override
                public void onStop() {
                    super.onStop();
                    State.log("MediaProjection onStop callback");
                }
            }, null);
            State.resumeJob();
        } else {
            MediaProjectionService.isStarting = false;
            State.log("MediaProjectionService received invalid data");
            State.resumeJob();
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        State.log("MediaProjectionService onDestroy");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    private void _createNotificationChannel() {
        NotificationChannel serviceChannel = new NotificationChannel(
                CHANNEL_ID,
                "Media Projection Service",
                NotificationManager.IMPORTANCE_DEFAULT
        );
        NotificationManager manager = getSystemService(NotificationManager.class);
        manager.createNotificationChannel(serviceChannel);
    }

    private Notification _createNotification() {
        Notification.Builder builder = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Media Projection Service")
                .setContentText("Running in the background")
                .setSmallIcon(android.R.drawable.ic_notification_overlay)
                .setPriority(Notification.PRIORITY_DEFAULT);
        return builder.build();
    }
} 