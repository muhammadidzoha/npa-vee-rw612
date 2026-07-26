package com.nxp.example.smartgreenhouse.models.wifi;

import ej.ecom.wifi.AccessPoint;
import ej.ecom.wifi.SecurityMode;

public class WifiNetwork {

    private final String name;
    private final float rssi;
    private final boolean secured;
    private final boolean connected;
    private final AccessPoint accessPoint;
    private final SecurityMode securityMode;

    public WifiNetwork(String name, float rssi, boolean secured, boolean connected, AccessPoint accessPoint, SecurityMode securityMode) {
        this.name = name;
        this.rssi = rssi;
        this.secured = secured;
        this.connected = connected;
        this.accessPoint = accessPoint;
        this.securityMode = securityMode;
    }

    public String getName() {
        return name;
    }

    public float getRssi() {
        return rssi;
    }

    public boolean isSecured() {
        return secured;
    }

    public boolean isConnected() {
        return connected;
    }

    public AccessPoint getAccessPoint() {
        return this.accessPoint;
    }

    public SecurityMode getSecurityMode() {
        return this.securityMode;
    }
}
