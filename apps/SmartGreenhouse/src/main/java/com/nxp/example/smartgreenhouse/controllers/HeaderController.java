package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.services.wifi.WifiHardwareService;
import com.nxp.example.smartgreenhouse.utils.Time;
import com.nxp.example.smartgreenhouse.views.MainPage;

import android.net.SntpClient;
import ej.bon.Util;

import ej.bon.Timer;
import ej.bon.TimerTask;
import ej.ecom.wifi.WifiCapability;
import ej.microui.MicroUI;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HeaderController {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: HEADER CONTROLLER]");

    private final MainPage mainPage;
    private final WifiHardwareService wifiService;

    private Timer clockTimer;

    private Runnable periodicTask;
    private Runnable wifiConnectedTask;

    private boolean wifiConnectionRunning;

    private static final String NTP_SERVER = "0.pool.ntp.org";

    private static final int NTP_PORT = 123;

    private static final int NTP_TIMEOUT_MS = 5000;

    public HeaderController(MainPage mainPage) {
        if (mainPage == null) {
            throw new NullPointerException("mainPage tidak boleh null.");
        }

        this.mainPage = mainPage;
        this.wifiService = new WifiHardwareService();
        this.clockTimer = null;

        this.periodicTask = null;
        this.wifiConnectedTask = null;

        this.wifiConnectionRunning = false;
    }

    public void init() {
        this.mainPage.updateWifiConnectionStatus(false);
        startClock();
        connectConfiguredWifi();
    }

    public void setPeriodicTask(Runnable periodicTask) {
        this.periodicTask = periodicTask;
    }

    public void setWifiConnectedTask(Runnable wifiConnectedTask) {
        this.wifiConnectedTask = wifiConnectedTask;
    }

    private void connectConfiguredWifi() {
        if (this.wifiConnectionRunning) {
            return;
        }

        this.wifiConnectionRunning = true;
        LOGGER.log(Level.INFO, "Automatic WiFi connection started" + " | SSID: " + this.wifiService.getConfiguredSsid());

        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                boolean connected;
                                String errorMessage;
                                try {
                                    WifiCapability capability = HeaderController.this.wifiService.getCapability();
                                    LOGGER.log(Level.INFO, "WiFi capability: " + capability);
                                    connected = HeaderController.this.wifiService.connectConfiguredNetwork();
                                    if (connected) {
                                        HeaderController.this.synchronizeApplicationTime();
                                    }
                                    errorMessage = connected ? null : "Configured network" + " was not joined.";
                                } catch (Exception exception) {
                                    connected = false;
                                    errorMessage = exception.toString();
                                }
                                final boolean connectionResult = connected;
                                final String connectionError = errorMessage;
                                MicroUI.callSerially(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                HeaderController.this.wifiConnectionRunning = false;
                                                HeaderController.this.mainPage.updateWifiConnectionStatus(connectionResult);
                                                if (connectionResult) {
                                                    Runnable task = HeaderController.this.wifiConnectedTask;
                                                    if (task != null) {
                                                        try {
                                                            task.run();
                                                        } catch (RuntimeException exception) {
                                                            LOGGER.log(Level.WARNING, "WiFi connected task failed" + " | error=" + exception);
                                                        }
                                                    }
                                                    LOGGER.log(Level.INFO, "Automatic WiFi" + " connection successful" + " | SSID: " + HeaderController.this.wifiService.getConfiguredSsid());
                                                } else {
                                                    LOGGER.log(Level.WARNING,
                                                            "Automatic WiFi"
                                                                    + " connection failed"
                                                                    + " | SSID: "
                                                                    + HeaderController.this
                                                                    .wifiService
                                                                    .getConfiguredSsid()
                                                                    + " | reason: "
                                                                    + connectionError
                                                    );
                                                }
                                            }
                                        }
                                );
                            }
                        },
                        "wifi-auto-connect"
                );

        worker.start();
    }

    private void startClock() {
        if (this.clockTimer != null) {
            return;
        }

        this.clockTimer = new Timer();
        this.clockTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        final String currentTime = Time.formatCurrentTime();
                        MicroUI.callSerially(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        HeaderController.this.mainPage.updateTime(currentTime);
                                        Runnable task = HeaderController.this.periodicTask;
                                        if (task == null) {
                                            return;
                                        }
                                        try {
                                            task.run();
                                        } catch (RuntimeException exception) {
                                            LOGGER.log(
                                                    Level.WARNING,
                                                    "Periodic application"
                                                            + " task failed: "
                                                            + exception
                                            );
                                        }
                                    }
                                }
                        );
                    }
                },
                0,
                1000
        );
    }

    private boolean synchronizeApplicationTime() {
        LOGGER.log(Level.INFO,
                "Synchronizing network time"
                        + " | server="
                        + NTP_SERVER
                        + " | port="
                        + NTP_PORT
        );

        try {
            SntpClient sntpClient = new SntpClient();
            boolean requestSuccessful = sntpClient.requestTime(NTP_SERVER, NTP_PORT, NTP_TIMEOUT_MS);

            if (!requestSuccessful) {
                LOGGER.log(
                        Level.WARNING,
                        "Network time synchronization failed"
                                + " | server="
                                + NTP_SERVER
                );

                return false;
            }

            long networkTime = sntpClient.getNtpTime() + Util.platformTimeMillis() - sntpClient.getNtpTimeReference();
            Util.setCurrentTimeMillis(networkTime);

            LOGGER.log(
                    Level.INFO,
                    "Network time synchronized"
                            + " | epochMs="
                            + System.currentTimeMillis()
                            + " | JakartaTime="
                            + Time.formatCurrentTime()
            );

            return true;

        } catch (RuntimeException exception) {
            LOGGER.log(
                    Level.WARNING,
                    "Network time synchronization error"
                            + " | error="
                            + exception
            );

            return false;
        }
    }
}