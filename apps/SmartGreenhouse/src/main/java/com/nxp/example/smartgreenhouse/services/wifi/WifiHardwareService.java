package com.nxp.example.smartgreenhouse.services.wifi;

import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;

import ej.ecom.wifi.AccessPoint;
import ej.ecom.wifi.SecurityMode;
import ej.ecom.wifi.WifiCapability;
import ej.ecom.wifi.WifiManager;

import java.io.IOException;


public final class WifiHardwareService {

    private final WifiManager wifiManager;

    public WifiHardwareService() {
        this.wifiManager = WifiManager.getInstance();
    }

    public WifiCapability getCapability() throws IOException {
        return this.wifiManager
                .getCapability();
    }

    public WifiNetwork[] scan() throws IOException {
        AccessPoint joinedAccessPoint =
                this.wifiManager.getJoined();

        AccessPoint[] accessPoints = this.wifiManager.scan(true);
        WifiNetwork[] networks = new WifiNetwork[accessPoints.length];

        for (int i = 0; i < accessPoints.length; i++) {
            AccessPoint accessPoint = accessPoints[i];
            SecurityMode securityMode = accessPoint.getSecurityMode();

            if (securityMode == null) {
                securityMode = SecurityMode.UNKNOWN;
            }

            boolean secured = securityMode != SecurityMode.OPEN;
            boolean connected = isSameAccessPoint(joinedAccessPoint, accessPoint);

            networks[i] = new WifiNetwork(accessPoint.getSSID(), accessPoint.getRSSI(), secured, connected, accessPoint, securityMode);
        }

        return networks;
    }

    public boolean connect(WifiNetwork network, String password) throws IOException {
        if (network == null) {
            throw new IllegalArgumentException("WifiNetwork tidak boleh null.");
        }

        AccessPoint accessPoint = network.getAccessPoint();

        if (accessPoint == null) {
            throw new IllegalArgumentException("AccessPoint asli tidak tersedia.");
        }

        SecurityMode securityMode = network.getSecurityMode();

        if (securityMode == null) {
            securityMode = SecurityMode.UNKNOWN;
        }

        String passphrase = password == null ? "" : password;
        if (securityMode == SecurityMode.OPEN) {
            passphrase = "";
        } else {
            validatePassword(passphrase);
        }

        this.wifiManager.join(accessPoint, passphrase, securityMode);

        AccessPoint joinedAccessPoint = this.wifiManager.getJoined();

        return isSameAccessPoint(joinedAccessPoint, accessPoint);
    }

    public void disconnect() throws IOException {
        this.wifiManager.leave();
    }

    public boolean isConnected() throws IOException {
        return this.wifiManager.getJoined() != null;
    }

    public String getConnectedSsid() throws IOException {
        AccessPoint accessPoint = this.wifiManager.getJoined();

        if (accessPoint == null) {
            return null;
        }

        return accessPoint.getSSID();
    }

    private static void validatePassword(String password) {
        int length = password.length();

        if (length < 8 || length > 64) {throw new IllegalArgumentException("Password Wi-Fi harus terdiri " + "dari 8 sampai 64 karakter.");}
    }

    private static boolean isSameAccessPoint(AccessPoint first, AccessPoint second) {
        if (first == null || second == null) {
            return false;
        }

        String firstSsid = first.getSSID();
        String secondSsid = second.getSSID();

        if (firstSsid == null || secondSsid == null) {
            return false;
        }

        return firstSsid.equals(secondSsid);
    }
}