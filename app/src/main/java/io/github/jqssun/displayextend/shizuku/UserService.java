package io.github.jqssun.displayextend.shizuku;

import android.content.Context;
import android.content.Intent;
import android.hardware.display.IDisplayManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import android.os.RemoteException;
import android.view.Display;

import androidx.annotation.Keep;

import rikka.shizuku.SystemServiceHelper;

public class UserService extends IUserService.Stub  {
    private Context context;
    private boolean listenVolumeKey = false;
    private Process listenVolumeKeyProcess;
    private Thread volumeKeyThread;

    public UserService() {
        Log.i("UserService", "constructor");
    }

    @Keep
    public UserService(Context context) {
        this.context = context;
        Log.i("UserService", "constructor with Context: context=" + context.toString());
    }
    
    /**
     * Reserved destroy method
     */
    @Override
    public void destroy() {
        Log.i("UserService", "destroy");
        stopListenVolumeKey();
        System.exit(0);
    }

    @Override
    public void exit() {
        destroy();
    }

    @Override
    public String fetchLogs() throws RemoteException  {
        try {
            Process process = Runtime.getRuntime().exec("logcat -d -f /sdcard/Download/Extend.log");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            reader.close();
            process.waitFor();

            return output.toString();
        } catch (Exception e) {
            Log.e("UserService", "logcat -d failed", e);
            throw new RemoteException("Failed to execute logcat -d: " + e.getMessage());
        }
    }

    @Override
    public String dumpInput() throws RemoteException {
        return _exec("dumpsys input");
    }

    private String _exec(String command) throws RemoteException {
        try {
            Process process = Runtime.getRuntime().exec(command);
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));

            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }

            reader.close();
            process.waitFor();

            return output.toString();
        } catch (Exception e) {
            Log.e("UserService", "execute command failed: " + command, e);
            throw new RemoteException("Failed to execute command: " + command + " " + e.getMessage());
        }
    }

    public void setScreenPower(int powerMode) {
        Log.i("UserService", "try to setScreenPower: " + powerMode);
        IDisplayManager displayManager = IDisplayManager.Stub.asInterface(SystemServiceHelper.getSystemService(Context.DISPLAY_SERVICE));
        if (Build.VERSION.SDK_INT >= 35) {
            if (powerMode == SurfaceControl.POWER_MODE_OFF) {
                try {
                    displayManager.requestDisplayPower(Display.DEFAULT_DISPLAY, false);
                    Log.i("UserService", "requestDisplayPower by bool");
                } catch(Throwable e) {
                    Log.e("UserService", "failed to power off screen", e);
                    try {
                        displayManager.requestDisplayPower(Display.DEFAULT_DISPLAY, SurfaceControl.POWER_MODE_OFF);
                        Log.i("UserService", "requestDisplayPower by int");
                    } catch(Throwable e2) {
                        Log.e("UserService", "failed to power off screen", e2);
                    }
                }
            } else {
                try {
                    displayManager.requestDisplayPower(Display.DEFAULT_DISPLAY, true);
                    Log.i("UserService", "requestDisplayPower by bool");
                } catch (Throwable e) {
                    Log.e("UserService", "failed to power up screen", e);
                    try {
                        displayManager.requestDisplayPower(Display.DEFAULT_DISPLAY, SurfaceControl.POWER_MODE_NORMAL);
                        Log.i("UserService", "requestDisplayPower by int");
                    } catch(Throwable e2) {
                        Log.e("UserService", "failed to power up screen", e2);
                    }
                }
            }
        } else {
            IBinder d = SurfaceControl.getBuiltInDisplay();
            if (d == null) {
                Log.i("UserService", "Could not get built-in display");
            } else {
                SurfaceControl.setDisplayPowerMode(d, powerMode);
                Log.i("UserService", "setDisplayPowerMode success");
            }
        }
    }

    public void startListenVolumeKey() throws RemoteException {
        if (listenVolumeKey) {
            return;
        }
        listenVolumeKey = true;
        Thread thread = new Thread(() -> {
            try {
                listenVolumeKeyProcess = Runtime.getRuntime().exec("getevent");
                java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(listenVolumeKeyProcess.getInputStream()));
                while (true) {
                    String line = reader.readLine();
                    if (line == null || !listenVolumeKey) {
                        break;
                    }
                    if (!line.endsWith("0000 0000 00000000") &&
                        (line.endsWith("0001 0072 00000001") || line.endsWith("0001 0073 00000001"))) {
                        Log.i("UserService", "try to exit pure black activity");
                        setScreenPower(SurfaceControl.POWER_MODE_NORMAL);
                        if (context != null) {
                            Intent intent = new Intent("io.github.jqssun.displayextend.EXIT_PURE_BLACK");
                            intent.setPackage("io.github.jqssun.displayextend");
                            context.sendBroadcast(intent);
                        } else {
                            Log.i("UserService", "context is null, can not send EXIT_PURE_BLACK");
                        }
                    }
                }
                reader.close();
                listenVolumeKeyProcess.waitFor();
                if (android.os.Build.VERSION.SDK_INT >= 26) {
                    listenVolumeKeyProcess.destroyForcibly();
                } else {
                    listenVolumeKeyProcess.destroy();
                }
            } catch (Exception e) {
                Log.e("UserService", "Listen volume key failed", e);
            }
        });
        volumeKeyThread = thread;
        thread.start();
    }

    public void stopListenVolumeKey() {
        listenVolumeKey = false;
        if (listenVolumeKeyProcess != null) {
            if (android.os.Build.VERSION.SDK_INT >= 26) {
                listenVolumeKeyProcess.destroyForcibly();
            } else {
                listenVolumeKeyProcess.destroy();
            }
            listenVolumeKeyProcess = null;
        }
        if (volumeKeyThread != null) {
            volumeKeyThread.interrupt();
            volumeKeyThread = null;
        }
    }
}
