package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.services.wifi.WifiHardwareService;
import com.nxp.example.smartgreenhouse.utils.Time;
import com.nxp.example.smartgreenhouse.views.MainPage;

import android.net.SntpClient;

import ej.bon.Timer;
import ej.bon.TimerTask;
import ej.bon.Util;
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

    private static final String[] NTP_SERVERS = {"time.google.com", "0.pool.ntp.org"};
    private static final int NTP_TIMEOUT_MS = 5000;
    private static final long NETWORK_READY_DELAY_MS = 3000L;
    private long headerNtpTimeMillis;
    private long headerNtpReferenceMillis;
    private boolean headerTimeSynchronized;

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

        this.headerNtpTimeMillis = 0L;
        this.headerNtpReferenceMillis = 0L;
        this.headerTimeSynchronized = false;
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
                                        LOGGER.log(Level.INFO, "WiFi connected" + " | waiting for network before NTP");
                                        Thread.sleep(NETWORK_READY_DELAY_MS);
                                        boolean timeSynchronized = HeaderController.this.synchronizeHeaderTime();
                                        if (!timeSynchronized) {
                                            LOGGER.log(Level.WARNING, "WiFi connected but header time" + " was not synchronized.");
                                        }
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
                        long currentUtcMillis = HeaderController.this.getCurrentHeaderUtcMillis();
                        final String currentTime = Time.formatJakartaTime(currentUtcMillis);
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
                                            LOGGER.log(Level.WARNING, "Periodic application" + " task failed: " + exception);
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

    private synchronized void setHeaderNtpTime(long ntpTimeMillis, long ntpReferenceMillis) {
        this.headerNtpTimeMillis = ntpTimeMillis;
        this.headerNtpReferenceMillis = ntpReferenceMillis;
        this.headerTimeSynchronized = true;
    }

    private synchronized long getCurrentHeaderUtcMillis() {
        if (!this.headerTimeSynchronized) {
            return 0L;
        }

        long elapsedMillis = Util.platformTimeMillis() - this.headerNtpReferenceMillis;
        if (elapsedMillis < 0L) {
            elapsedMillis =
                    0L;
        }

        return this.headerNtpTimeMillis + elapsedMillis;
    }

    private boolean synchronizeHeaderTime() {
        for (int index = 0; index < NTP_SERVERS.length; index++) {
            String server = NTP_SERVERS[index];
            LOGGER.log(Level.INFO, "Synchronizing header time" + " | server=" + server);
            try {
                SntpClient client = new SntpClient();
                boolean successful = client.requestTime(server, NTP_TIMEOUT_MS);
                if (!successful) {
                    LOGGER.log(Level.WARNING, "NTP request failed" + " | server=" + server);
                    continue;
                }
                setHeaderNtpTime(client.getNtpTime(), client.getNtpTimeReference());
                long currentUtcMillis = getCurrentHeaderUtcMillis();

                Util.setCurrentTimeMillis(currentUtcMillis);
                LOGGER.log(
                        Level.INFO,
                        "Header time synchronized"
                                + " | server="
                                + server
                                + " | utcMillis="
                                + currentUtcMillis
                                + " | jakartaTime="
                                + Time.formatJakartaTime(
                                currentUtcMillis
                        )
                                + " | roundTripMs="
                                + client.getRoundTripTime()
                );

                return true;
            } catch (RuntimeException exception) {
                LOGGER.log(
                        Level.WARNING,
                        "NTP synchronization error"
                                + " | server="
                                + server
                                + " | error="
                                + exception
                );
            }
        }

        LOGGER.log(Level.WARNING, "Header time synchronization failed.");
        return false;
    }
}