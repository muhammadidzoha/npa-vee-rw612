package com.nxp.example.smartgreenhouse.services.wifi;

import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.models.wifi.WifiScanResult;

import ej.ecom.wifi.AccessPoint;
import ej.ecom.wifi.SecurityMode;
import ej.ecom.wifi.WifiCapability;
import ej.ecom.wifi.WifiManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class WifiHardwareService {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: Wifi Hardware Service]");

    private final WifiManager wifiManager;

    public WifiHardwareService() {
        this.wifiManager = WifiManager.getInstance();
    }

    public synchronized WifiCapability getCapability() throws IOException {
        return this.wifiManager.getCapability();
    }

    public synchronized WifiScanResult scan() throws IOException {

        LOGGER.log(Level.INFO, "Before WifiManager.scan(false)");

        AccessPoint[] accessPoints = this.wifiManager.scan(false);
        LOGGER.log(Level.INFO, "After WifiManager.scan(false)" + " | count: " + accessPoints.length);
        LOGGER.log(Level.INFO, "Before WifiManager.getJoined() after scan");

        AccessPoint joinedAccessPoint = this.wifiManager.getJoined();
        LOGGER.log(Level.INFO, "After WifiManager.getJoined() after scan");

        WifiNetwork[] networks = new WifiNetwork[accessPoints.length];
        for (int i = 0; i < accessPoints.length; i++) {
            AccessPoint accessPoint = accessPoints[i];
            if (accessPoint == null) {
                continue;
            }

            String ssid = accessPoint.getSSID();
            if (ssid == null || ssid.length() == 0) {
                continue;
            }

            SecurityMode securityMode = accessPoint.getSecurityMode();
            if (securityMode == null) {
                securityMode = SecurityMode.UNKNOWN;
            }

            boolean secured = securityMode != SecurityMode.OPEN;
            boolean networkConnected = isSameAccessPoint(joinedAccessPoint, accessPoint);

            networks[i] = new WifiNetwork(ssid, accessPoint.getRSSI(), secured, networkConnected, accessPoint, securityMode);
        }

        WifiNetwork[] validNetworks = removeNullNetworks(networks);

        boolean connected = joinedAccessPoint != null;
        LOGGER.log(Level.INFO, "WifiHardwareService scan completed" + " | networks: " + validNetworks.length + " | connected: " + connected);

        return new WifiScanResult(validNetworks, connected);
    }

    private static WifiNetwork[] removeNullNetworks(WifiNetwork[] networks) {
        int validCount = 0;

        for (WifiNetwork network : networks) {
            if (network != null) {
                validCount++;
            }
        }

        WifiNetwork[] result = new WifiNetwork[validCount];

        int resultIndex = 0;

        for (WifiNetwork network : networks) {
            if (network != null) {
                result[resultIndex] = network;
                resultIndex++;
            }
        }

        return result;
    }

    public synchronized boolean connect(WifiNetwork network, String password) throws IOException {
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

        LOGGER.log(Level.INFO, "Before WifiManager.join()" + " | SSID: " + accessPoint.getSSID());

        this.wifiManager.join(accessPoint, passphrase, securityMode);
        LOGGER.log(Level.INFO, "After WifiManager.join()" + " | SSID: " + accessPoint.getSSID());
        LOGGER.log(Level.INFO, "Before WifiManager.getJoined() after join");
        AccessPoint joinedAccessPoint = this.wifiManager.getJoined();

        LOGGER.log(Level.INFO, "After WifiManager.getJoined() after join");
        boolean connected = isSameAccessPoint(joinedAccessPoint, accessPoint);

        LOGGER.log(Level.INFO, "WiFi connection result" + " | SSID: " + accessPoint.getSSID() + " | connected: " + connected);

        return connected;
    }

    public synchronized void disconnect() throws IOException {

        LOGGER.log(Level.INFO, "Before WifiManager.leave()");

        this.wifiManager.leave();

        LOGGER.log(Level.INFO, "After WifiManager.leave()");
    }

    public synchronized boolean isConnected() throws IOException {
        return this.wifiManager.getJoined() != null;
    }

    public synchronized String getConnectedSsid() throws IOException {
        AccessPoint accessPoint = this.wifiManager.getJoined();

        if (accessPoint == null) {
            return null;
        }

        return accessPoint.getSSID();
    }

    private static void validatePassword(String password) {
        int length = password.length();

        if (length < 8 || length > 64) {
            throw new IllegalArgumentException("Password Wi-Fi harus terdiri " + "dari 8 sampai 64 karakter.");
        }
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