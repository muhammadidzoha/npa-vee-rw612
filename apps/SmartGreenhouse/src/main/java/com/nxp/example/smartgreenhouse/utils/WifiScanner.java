package com.nxp.example.smartgreenhouse.utils;

import com.nxp.example.smartgreenhouse.model.wifi.WifiNetwork;
import ej.ecom.wifi.AccessPoint;
import ej.ecom.wifi.SecurityMode;
import ej.ecom.wifi.WifiManager;

import java.util.logging.Level;
import java.util.logging.Logger;

public class WifiScanner {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: WIFI SCANNER]");

    public static WifiNetwork[] scan() {
        try {
            WifiManager wifiManager = WifiManager.getInstance();
            AccessPoint[] accessPoints = wifiManager.scan();

            LOGGER.log(Level.INFO, "Found " + accessPoints.length + " access points");

            WifiNetwork[] networks = new WifiNetwork[accessPoints.length];
            for (int i = 0; i < accessPoints.length; i++) {
                AccessPoint ap = accessPoints[i];
                String ssid = ap.getSSID();
                float rssi = ap.getRSSI();
                SecurityMode mode = ap.getSecurityMode();
                boolean secured = (mode != null && mode != SecurityMode.UNKNOWN);

                LOGGER.log(Level.INFO, "AP " + (i + 1) + ": " + ssid + " | RSSI: " + (int) rssi + " dBm" + " | Secured: " + secured);

                networks[i] = new WifiNetwork(ssid, rssi, secured, false);
            }

            return networks;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "WiFi scan failed: " + e.getMessage());
            return new WifiNetwork[0];
        }
    }

}
