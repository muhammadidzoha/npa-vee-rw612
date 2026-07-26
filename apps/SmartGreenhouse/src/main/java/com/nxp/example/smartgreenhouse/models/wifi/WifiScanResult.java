package com.nxp.example.smartgreenhouse.models.wifi;

public final class WifiScanResult {

    private final WifiNetwork[] networks;
    private final boolean connected;

    public WifiScanResult(WifiNetwork[] networks, boolean connected) {
        if (networks == null) {
            this.networks = new WifiNetwork[0];
        } else {
            this.networks = networks;
        }

        this.connected = connected;
    }

    public WifiNetwork[] getNetworks() {
        return this.networks;
    }

    public boolean isConnected() {
        return this.connected;
    }
}