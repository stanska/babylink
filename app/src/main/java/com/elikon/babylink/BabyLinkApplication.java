package com.elikon.babylink;

import android.app.Application;

public class BabyLinkApplication extends Application {

    public enum DeviceType {
        NONE, CHILD, PARENT
    }

    private DeviceType deviceType = DeviceType.NONE;

    public DeviceType getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(DeviceType deviceType) {
        this.deviceType = deviceType;
    }
}