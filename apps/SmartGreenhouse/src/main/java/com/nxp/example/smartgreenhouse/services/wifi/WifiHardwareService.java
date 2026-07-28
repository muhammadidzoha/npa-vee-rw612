package com.nxp.example.smartgreenhouse.services.wifi;

import ej.ecom.wifi.AccessPoint;
import ej.ecom.wifi.SecurityMode;
import ej.ecom.wifi.WifiCapability;
import ej.ecom.wifi.WifiManager;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class WifiHardwareService {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: WIFI HARDWARE SERVICE]");

    private static final String CONFIGURED_SSID = "Iphone 5G";
    private static final String CONFIGURED_PASSWORD = "1sampai9";

    private final WifiManager wifiManager;

    public WifiHardwareService() {
        this.wifiManager = WifiManager.getInstance();
    }

    public synchronized WifiCapability getCapability() throws IOException {
        return this.wifiManager.getCapability();
    }

    public synchronized boolean connectConfiguredNetwork() throws IOException {
        validateConfiguration();

        AccessPoint joinedAccessPoint = this.wifiManager.getJoined();

        if (isConfiguredAccessPoint(joinedAccessPoint)) {
            LOGGER.log(Level.INFO, "Already connected" + " | SSID: " + CONFIGURED_SSID);
            return true;
        }

        if (joinedAccessPoint != null) {
            LOGGER.log(Level.WARNING, "Already connected to another network" + " | current SSID: " + joinedAccessPoint.getSSID() + " | configured SSID: " + CONFIGURED_SSID);
            return false;
        }

        LOGGER.log(Level.INFO, "Searching configured WiFi" + " | SSID: " + CONFIGURED_SSID);

        AccessPoint[] accessPoints = this.wifiManager.scan(false);
        AccessPoint configuredAccessPoint = findConfiguredAccessPoint(accessPoints);

        if (configuredAccessPoint == null) {
            LOGGER.log(Level.WARNING, "Configured WiFi was not found" + " | SSID: " + CONFIGURED_SSID);
            return false;
        }

        SecurityMode securityMode = configuredAccessPoint.getSecurityMode();

        if (securityMode == null) {
            securityMode = SecurityMode.UNKNOWN;
        }

        String passphrase = CONFIGURED_PASSWORD;

        if (securityMode == SecurityMode.OPEN) {
            passphrase = "";
        } else {
            validatePassword(passphrase);
        }

        LOGGER.log(Level.INFO, "Joining configured WiFi" + " | SSID: " + CONFIGURED_SSID + " | security: " + securityMode);

        this.wifiManager.join(configuredAccessPoint, passphrase, securityMode);
        joinedAccessPoint = this.wifiManager.getJoined();

        boolean connected = isConfiguredAccessPoint(joinedAccessPoint);
        LOGGER.log(connected ? Level.INFO : Level.WARNING, connected ? "Configured WiFi connected" + " | SSID: " + CONFIGURED_SSID : "Configured WiFi connection failed" + " | SSID: " + CONFIGURED_SSID);
        return connected;
    }

    public synchronized boolean isConnectedToConfiguredNetwork() throws IOException {
        return isConfiguredAccessPoint(this.wifiManager.getJoined());
    }

    public String getConfiguredSsid() {
        return CONFIGURED_SSID;
    }

    private static AccessPoint findConfiguredAccessPoint(AccessPoint[] accessPoints) {
        if (accessPoints == null) {
            return null;
        }

        for (int index = 0; index < accessPoints.length; index++) {
            AccessPoint accessPoint = accessPoints[index];
            if (isConfiguredAccessPoint(accessPoint)) {
                return accessPoint;
            }
        }

        return null;
    }

    private static boolean isConfiguredAccessPoint(AccessPoint accessPoint) {
        if (accessPoint == null) {
            return false;
        }

        String ssid = accessPoint.getSSID();
        return CONFIGURED_SSID.equals(ssid);
    }

    private static void validateConfiguration() {
        if (CONFIGURED_SSID == null || CONFIGURED_SSID.length() == 0 || "MASUKKAN_SSID_DI_SINI".equals(CONFIGURED_SSID)) {
            throw new IllegalStateException("SSID Wi-Fi belum dikonfigurasi.");
        }
    }

    private static void validatePassword(String password) {
        int passwordLength = password == null ? 0 : password.length();
        if (passwordLength < 8 || passwordLength > 64) {
            throw new IllegalArgumentException("Password Wi-Fi harus terdiri" + " dari 8 sampai 64 karakter.");
        }
    }
}