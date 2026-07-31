package com.nxp.example.smartgreenhouse.services.wifi;

import ej.ecom.wifi.AccessPoint;
import ej.ecom.wifi.WifiCapability;
import ej.ecom.wifi.WifiManager;
import ej.ecom.wifi.SecurityMode;
import ej.ecom.wifi.SoftAPConfiguration;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;


public final class WifiHardwareService {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: WIFI HARDWARE SERVICE]");

    private static final String CONFIGURED_SSID = "Iphone 5G";
    private static final String CONFIGURED_PASSWORD = "1sampai9";
    private static final String PROVISIONING_SSID = "smartgreenhouse";
    private static final String PROVISIONING_PASSWORD = "smartgreenhouse";
    private static final int MAX_PROVISIONING_NETWORKS = 12;

    private final WifiManager wifiManager;

    public WifiHardwareService() {
        this.wifiManager = WifiManager.getInstance();
    }

    public synchronized WifiCapability getCapability() throws IOException {
        return this.wifiManager.getCapability();
    }

    public synchronized boolean connectConfiguredNetwork() throws IOException {
        validateConfiguration();
        validatePassword(CONFIGURED_PASSWORD);

        AccessPoint joinedAccessPoint = this.wifiManager.getJoined();
        if (isConfiguredAccessPoint(joinedAccessPoint)) {
            LOGGER.log(Level.INFO, "Already connected" + " | SSID: " + CONFIGURED_SSID);
            return true;
        }

        if (joinedAccessPoint != null) {
            LOGGER.log(Level.INFO, "Leaving previous WiFi" + " | current SSID: " + joinedAccessPoint.getSSID());
            this.wifiManager.leave();
        }

        LOGGER.log(Level.INFO, "Joining configured WiFi directly" + " | SSID: " + CONFIGURED_SSID);

        this.wifiManager.join(CONFIGURED_SSID, CONFIGURED_PASSWORD);
        LOGGER.log(Level.INFO, "WiFi join operation completed" + " | checking connection status...");

        joinedAccessPoint = this.wifiManager.getJoined();
        boolean connected = isConfiguredAccessPoint(joinedAccessPoint);

        if (connected) {
            LOGGER.log(Level.INFO, "Configured WiFi connected" + " | SSID: " + CONFIGURED_SSID);
        } else {
            LOGGER.log(Level.WARNING, "Configured WiFi connection failed" + " | SSID: " + CONFIGURED_SSID);
        }

        return connected;
    }

    public synchronized boolean isConnectedToConfiguredNetwork() throws IOException {
        return isConfiguredAccessPoint(this.wifiManager.getJoined());
    }

    public String getConfiguredSsid() {
        return CONFIGURED_SSID;
    }
    public String getProvisioningSsid() {
        return PROVISIONING_SSID;
    }

    public String getProvisioningPassword() {
        return PROVISIONING_PASSWORD;
    }

    public synchronized void disconnectCurrentNetwork() throws IOException {
        AccessPoint joinedAccessPoint = this.wifiManager.getJoined();
        if (joinedAccessPoint == null) {
            LOGGER.log(Level.INFO, "No joined WiFi network to disconnect");
            return;
        }

        LOGGER.log(Level.INFO, "Leaving current WiFi" + " | SSID: " + joinedAccessPoint.getSSID());
        this.wifiManager.leave();

        LOGGER.log(Level.INFO, "Current WiFi left");
    }

    public synchronized AccessPoint[] scanProvisioningNetworks() throws IOException {
        stopProvisioningAccessPoint();

        disconnectCurrentNetwork();

        LOGGER.log(Level.INFO, "Provisioning WiFi scan started" + " | active=false");
        AccessPoint[] scannedAccessPoints = this.wifiManager.scan(false);
        AccessPoint[] selectedAccessPoints = selectProvisioningNetworks(scannedAccessPoints);
        LOGGER.log(Level.INFO, "Provisioning WiFi scan completed" + " | scanned=" + getAccessPointCount(scannedAccessPoints) + " | selected=" + selectedAccessPoints.length);
        return selectedAccessPoints;
    }

    public synchronized void startProvisioningAccessPoint() throws IOException {
        validatePassword(PROVISIONING_PASSWORD);
        disconnectCurrentNetwork();

        if (this.wifiManager.isSoftAPEnabled()) {
            LOGGER.log(Level.INFO, "Provisioning SoftAP already enabled" + " | SSID: " + PROVISIONING_SSID);
            return;
        }

        SoftAPConfiguration configuration = new SoftAPConfiguration();
        configuration.setName(PROVISIONING_SSID);
        configuration.setSSID(PROVISIONING_SSID);
        configuration.setPassphrase(PROVISIONING_PASSWORD);
        configuration.setSecurityMode(SecurityMode.WPA2);
        LOGGER.log(Level.INFO, "Starting provisioning SoftAP" + " | SSID: " + PROVISIONING_SSID);
        this.wifiManager.enableSoftAP(configuration);

        LOGGER.log(Level.INFO, "Provisioning SoftAP enabled" + " | SSID: " + PROVISIONING_SSID);
    }

    public synchronized void stopProvisioningAccessPoint() throws IOException {
        if (!this.wifiManager.isSoftAPEnabled()) {
            return;
        }

        LOGGER.log(Level.INFO, "Stopping provisioning SoftAP");
        this.wifiManager.disableSoftAP();
        LOGGER.log(Level.INFO, "Provisioning SoftAP stopped");
    }

    public synchronized boolean isProvisioningAccessPointEnabled() throws IOException {
        return this.wifiManager.isSoftAPEnabled();
    }

    public synchronized boolean connectToNetwork(String ssid, String password) throws IOException {
        validateSsid(ssid);
        validatePassword(password);
        stopProvisioningAccessPoint();
        AccessPoint joinedAccessPoint = this.wifiManager.getJoined();
        if (joinedAccessPoint != null) {
            String joinedSsid = joinedAccessPoint.getSSID();
            if (ssid.equals(joinedSsid)) {
                LOGGER.log(Level.INFO, "Already connected to requested WiFi" + " | SSID: " + ssid);
                return true;
            }

            this.wifiManager.leave();
        }

        LOGGER.log(Level.INFO, "Joining requested WiFi" + " | SSID: " + ssid);
        this.wifiManager.join(ssid, password);
        AccessPoint result = this.wifiManager.getJoined();
        boolean connected = result != null && ssid.equals(result.getSSID());
        LOGGER.log(connected ? Level.INFO : Level.WARNING, "Requested WiFi join completed" + " | SSID: " + ssid + " | connected=" + connected);
        return connected;
    }

    private static AccessPoint[] selectProvisioningNetworks(AccessPoint[] scannedAccessPoints) {
        if (scannedAccessPoints == null || scannedAccessPoints.length == 0) {
            return new AccessPoint[0];
        }

        AccessPoint[] selectedAccessPoints = new AccessPoint[MAX_PROVISIONING_NETWORKS];
        int selectedCount = 0;
        for (int index = 0; index < scannedAccessPoints.length; index++) {
            AccessPoint candidate = scannedAccessPoints[index];
            if (!isUsableProvisioningNetwork(candidate)) {
                continue;
            }
            int duplicateIndex = findAccessPointIndexBySsid(selectedAccessPoints, selectedCount, candidate.getSSID());
            if (duplicateIndex >= 0) {
                AccessPoint existing = selectedAccessPoints[duplicateIndex];
                if (candidate.getRSSI() > existing.getRSSI()) {
                    selectedAccessPoints[duplicateIndex] = candidate;
                }
                continue;
            }

            if (selectedCount < MAX_PROVISIONING_NETWORKS) {
                selectedAccessPoints[selectedCount] = candidate;
                selectedCount++;
                continue;
            }

            int weakestIndex = findWeakestAccessPointIndex(selectedAccessPoints, selectedCount);
            if (weakestIndex >= 0 && candidate.getRSSI() > selectedAccessPoints[weakestIndex].getRSSI()) {
                selectedAccessPoints[weakestIndex] = candidate;
            }
        }

        sortAccessPointsByRssi(selectedAccessPoints, selectedCount);
        AccessPoint[] result = new AccessPoint[selectedCount];
        for (int index = 0; index < selectedCount; index++) {
            result[index] = selectedAccessPoints[index];
        }

        return result;
    }

    private static boolean isUsableProvisioningNetwork(AccessPoint accessPoint) {
        if (accessPoint == null) {
            return false;
        }

        String ssid = accessPoint.getSSID();
        if (ssid == null || ssid.length() == 0) {
            return false;
        }
        return !PROVISIONING_SSID.equals(ssid);
    }

    private static int findAccessPointIndexBySsid(AccessPoint[] accessPoints, int count, String ssid) {
        for (int index = 0; index < count; index++) {
            AccessPoint accessPoint = accessPoints[index];
            if (accessPoint != null && ssid.equals(accessPoint.getSSID())) {
                return index;
            }
        }

        return -1;
    }

    private static int findWeakestAccessPointIndex(AccessPoint[] accessPoints, int count) {
        if (count <= 0) {
            return -1;
        }

        int weakestIndex = 0;
        for (int index = 1; index < count; index++) {
            if (accessPoints[index].getRSSI() < accessPoints[weakestIndex].getRSSI()) {
                weakestIndex = index;
            }
        }
        return weakestIndex;
    }

    private static void sortAccessPointsByRssi(AccessPoint[] accessPoints, int count) {
        for (int outerIndex = 0; outerIndex < count - 1; outerIndex++) {
            for (int innerIndex = outerIndex + 1; innerIndex < count; innerIndex++) {
                AccessPoint outerAccessPoint = accessPoints[outerIndex];
                AccessPoint innerAccessPoint = accessPoints[innerIndex];
                if (innerAccessPoint.getRSSI() > outerAccessPoint.getRSSI()) {
                    accessPoints[outerIndex] = innerAccessPoint;
                    accessPoints[innerIndex] = outerAccessPoint;
                }
            }
        }
    }

    private static int getAccessPointCount(AccessPoint[] accessPoints) {
        return accessPoints == null ? 0 : accessPoints.length;
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

    private static void validateSsid(String ssid) {
        if (ssid == null || ssid.length() == 0) {
            throw new IllegalArgumentException("SSID Wi-Fi tidak boleh kosong.");
        }
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