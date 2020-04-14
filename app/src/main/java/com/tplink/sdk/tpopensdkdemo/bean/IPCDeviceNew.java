package com.tplink.sdk.tpopensdkdemo.bean;

import com.tplink.sdk.tpopensdk.openctx.IPCDevice;

import java.io.Serializable;

public class IPCDeviceNew extends IPCDevice implements Serializable {
    public IPCDeviceNew(long pointer) {
        super(pointer);
    }
}
