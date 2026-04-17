package io.github.jqssun.displayextend.job;

import android.app.Activity;
import android.content.Context;
import android.hardware.display.DisplayManager;
import android.hardware.input.InputManager;
import android.view.Display;
import android.view.InputDevice;

import io.github.jqssun.displayextend.PlatformCompat;
import io.github.jqssun.displayextend.State;

public class InputDeviceMonitor {
    public static void init(InputManager inputManager) {
        int[] deviceIds = inputManager.getInputDeviceIds();
        for (int deviceId : deviceIds) {
            android.view.InputDevice device = inputManager.getInputDevice(deviceId);
            if (PlatformCompat.isExternalInputDevice(device)) {
                State.log("found external input device: " + device.getName());
            }
        }
        inputManager.registerInputDeviceListener(new InputManager.InputDeviceListener() {
            @Override
            public void onInputDeviceAdded(int i) {
                if (State.lastSingleAppDisplay > 0) {
                    Activity mainActivity = State.currentActivity.get();
                    if (mainActivity != null) {
                        InputManager inputManager = (InputManager) mainActivity.getSystemService(Context.INPUT_SERVICE);
                        DisplayManager displayManager = (DisplayManager) mainActivity.getSystemService(Context.DISPLAY_SERVICE);
                        Display display = displayManager.getDisplay(State.lastSingleAppDisplay);
                        InputDevice inputDevice = inputManager.getInputDevice(i);
                        if (display != null && inputDevice != null) {
                            State.log("onInputDeviceAdded: " + inputDevice.getName());
                            State.startNewJob(new BindInputToDisplay(inputDevice, display));
                        }
                    }
                }
            }

            @Override
            public void onInputDeviceRemoved(int i) {

            }

            @Override
            public void onInputDeviceChanged(int i) {

            }
        }, null);
    }
}
