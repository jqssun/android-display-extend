package io.github.jqssun.displayextend;

import android.app.ActivityOptions;
import android.content.Intent;
import android.graphics.Color;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Display;
import android.view.MotionEventHidden;
import android.view.Surface;
import android.view.Window;
import android.view.WindowManager;
import android.view.SurfaceView;
import android.view.SurfaceHolder;

import androidx.appcompat.app.AppCompatActivity;

import io.github.jqssun.displayextend.job.CreateVirtualDisplay;
import io.github.jqssun.displayextend.job.VirtualDisplayArgs;
import io.github.jqssun.displayextend.shizuku.ServiceUtils;

import dev.rikka.tools.refine.Refine;

public class BridgeActivity extends AppCompatActivity {

    private static BridgeActivity instance;

    public static BridgeActivity getInstance() {
        return instance;
    }

    private SurfaceView surfaceView;

    private static float[] adjustTouchCoordinates(float x, float y, int rotation,
                                                  int targetWidth, int targetHeight, int sourceWidth, int sourceHeight) {
        float scaleX = (float) targetWidth / sourceWidth;
        float scaleY = (float) targetHeight / sourceHeight;

        x *= scaleX;
        y *= scaleY;

        float[] result = new float[2];
        switch (rotation) {
            case Surface.ROTATION_0:
                result[0] = x;
                result[1] = y;
                break;
            case Surface.ROTATION_90:
                result[0] = y;
                result[1] = targetWidth - x;
                break;
            case Surface.ROTATION_180:
                result[0] = targetWidth - x;
                result[1] = targetHeight - y;
                break;
            case Surface.ROTATION_270:
                result[0] = targetHeight - y;
                result[1] = x;
                break;
        }
        return result;
    }

    public static void stopVirtualDisplay() {
        if (State.bridgeVirtualDisplay == null) {
            return;
        }
        State.bridgeDisplayId = -1;
        State.bridgeVirtualDisplay.release();
        State.bridgeVirtualDisplay = null;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = this;

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        getWindow().setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Window window = getWindow();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            window.setDecorFitsSystemWindows(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            WindowManager.LayoutParams layoutParams = window.getAttributes();
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
            window.setAttributes(layoutParams);
        }

        window.setStatusBarColor(Color.TRANSPARENT);
        window.setNavigationBarColor(Color.TRANSPARENT);

        surfaceView = new SurfaceView(this);
        VirtualDisplayArgs args = getIntent().getParcelableExtra("virtualDisplayArgs");
        
        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Surface surface = holder.getSurface();
                
                if (State.bridgeVirtualDisplay == null) {
                    stopVirtualDisplay();
                    State.bridgeVirtualDisplay = CreateVirtualDisplay.createVirtualDisplay(args, surface);
                    State.log("BridgeActivity created new virtual display");
                } else {
                    State.bridgeVirtualDisplay.setSurface(surface);
                    State.log("BridgeActivity reusing existing virtual display");
                }
                
                State.breadcrumbManager.popBreadcrumb();
                Display jumpToDisplay = State.bridgeVirtualDisplay.getDisplay();
                State.breadcrumbManager.pushBreadcrumb(getString(R.string.screen_breadcrumb, jumpToDisplay.getDisplayId()),
                    () -> DisplayDetailFragment.newInstance(jumpToDisplay.getDisplayId()));
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                if (State.bridgeVirtualDisplay != null) {
                    ImageReader imageReader = ImageReader.newInstance(args.width, args.height, 1, 2);
                    State.bridgeVirtualDisplay.setSurface(imageReader.getSurface());
                }
            }
        });
        
        surfaceView.setOnTouchListener((v, event) -> {
            if (State.bridgeVirtualDisplay != null) {
                Display virtualDisplay = State.bridgeVirtualDisplay.getDisplay();
                int rotation = virtualDisplay.getRotation();
                int displayId = virtualDisplay.getDisplayId();

                float x = event.getX();
                float y = event.getY();
                
                float[] adjustedCoords = adjustTouchCoordinates(x, y, rotation, 
                    args.width, args.height,
                    surfaceView.getWidth(), surfaceView.getHeight());
                
                event.setLocation(adjustedCoords[0], adjustedCoords[1]);
                
                MotionEventHidden motionEventHidden = Refine.unsafeCast(event);
                motionEventHidden.setDisplayId(displayId);
                try {
                    ServiceUtils.getInputManager().injectInputEvent(event, 0);
                } catch (Exception e) {
                    Log.e("BridgeActivity", "failed to inject touch event", e);
                }
            }
            return true;
        });
        
        setContentView(surfaceView);
    }

    @Override
    protected void onDestroy() {
        Log.i("BridgeActivity", "BridgeActivity onDestroy");
        super.onDestroy();
        Intent intent = new Intent(this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        ActivityOptions options = ActivityOptions.makeBasic();
        options.setLaunchDisplayId(0);
        this.startActivity(intent, options.toBundle());
        instance = null;
    }
}