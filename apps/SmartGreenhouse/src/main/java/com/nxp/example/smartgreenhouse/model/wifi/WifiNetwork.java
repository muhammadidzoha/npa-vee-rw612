package com.nxp.example.smartgreenhouse.model.wifi;

public class WifiNetwork {

    private final String name;
    private final float rssi;
    private final boolean secured;
    private final boolean connected;

    public WifiNetwork(String name, float rssi, boolean secured, boolean connected) {
        this.name = name;
        this.rssi = rssi;
        this.secured = secured;
        this.connected = connected;
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
}
