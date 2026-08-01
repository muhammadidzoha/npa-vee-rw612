package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.services.time.TimeService;
import com.nxp.example.smartgreenhouse.services.wifi.WifiProvisioningService;
import com.nxp.example.smartgreenhouse.views.MainPage;
import com.nxp.example.smartgreenhouse.views.overview.HeaderOverview;

import ej.microui.MicroUI;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HeaderController {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: HEADER CONTROLLER]");

    private final MainPage mainPage;
    private final TimeService timeService;
    private final WifiProvisioningService wifiProvisioningService;

    private Runnable periodicTask;
    private Runnable wifiConnectedTask;
    private Runnable wifiProvisioningStartedTask;

    public HeaderController(MainPage mainPage) {
        if (mainPage == null) {
            throw new NullPointerException("mainPage tidak boleh null.");
        }

        this.mainPage = mainPage;
        this.timeService = new TimeService();

        this.wifiProvisioningService = new WifiProvisioningService(
                new WifiProvisioningService.Listener() {
                    @Override
                    public void onStateChanged(int state) {
                        LOGGER.log(Level.INFO, "WiFi provisioning state changed | state=" + state);
                    }

                    @Override
                    public void onWifiConnectionStatusChanged(final boolean connected) {
                        HeaderController.this.updateWifiConnectionStatus(connected);
                    }

                    @Override
                    public void onProvisioningReady(String ssid, String password, String portalUrl, int networkCount) {
                        LOGGER.log(Level.INFO, "WiFi provisioning ready | SSID=" + ssid + " | portal=" + portalUrl + " | networkCount=" + networkCount);
                    }

                    @Override
                    public void onConnecting(String ssid) {
                        LOGGER.log(Level.INFO, "WiFi connecting | SSID=" + ssid);
                    }

                    @Override
                    public void onConnected(String ssid) {
                        LOGGER.log(Level.INFO, "WiFi connected | SSID=" + ssid);
                        HeaderController.this.runWifiConnectedTask();
                    }

                    @Override
                    public void onFailed(String message) {
                        LOGGER.log(Level.WARNING, "WiFi process failed | message=" + message);
                    }
                }
        );

        this.periodicTask = null;
        this.wifiConnectedTask = null;
        this.wifiProvisioningStartedTask = null;
    }

    public void init() {
        this.mainPage.updateWifiConnectionStatus(false);
        registerWifiClickListener();
        startClock();
        this.wifiProvisioningService.tryAutoConnect();
    }

    public void setPeriodicTask(Runnable periodicTask) {
        this.periodicTask = periodicTask;
    }

    public void setWifiConnectedTask(Runnable wifiConnectedTask) {
        this.wifiConnectedTask = wifiConnectedTask;
    }

    public void setWifiProvisioningStartedTask(Runnable wifiProvisioningStartedTask) {
        this.wifiProvisioningStartedTask = wifiProvisioningStartedTask;
    }

    public boolean synchronizeTime() {
        return this.timeService.synchronizeTime();
    }

    private void registerWifiClickListener() {
        this.mainPage.setOnWifiClick(
                new HeaderOverview.onWifiClickListener() {
                    @Override
                    public void onClicked() {
                        HeaderController.this.startProvisioningFromUser();
                    }
                }
        );
    }

    private void startProvisioningFromUser() {
        if (this.wifiProvisioningService.isBusy()) {
            LOGGER.log(Level.WARNING, "WiFi provisioning request ignored | service is busy");
            return;
        }

        if (!runWifiProvisioningStartedTask()) {
            LOGGER.log(Level.WARNING, "WiFi provisioning cancelled | preparation task failed");
            return;
        }

        boolean started = this.wifiProvisioningService.startProvisioning();

        if (!started) {
            LOGGER.log(Level.WARNING, "WiFi provisioning could not be started");
        }
    }

    private boolean runWifiProvisioningStartedTask() {
        Runnable task = this.wifiProvisioningStartedTask;

        if (task == null) {
            return true;
        }

        try {
            task.run();
            return true;
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "WiFi provisioning started task failed | error=" + exception);
            return false;
        }
    }

    private void runWifiConnectedTask() {
        Runnable task = this.wifiConnectedTask;

        if (task == null) {
            return;
        }

        try {
            task.run();
        } catch (RuntimeException exception) {
            LOGGER.log(Level.WARNING, "WiFi connected task failed | error=" + exception);
        }
    }

    private void updateWifiConnectionStatus(final boolean connected) {
        MicroUI.callSerially(
                new Runnable() {
                    @Override
                    public void run() {
                        HeaderController.this.mainPage.updateWifiConnectionStatus(connected);
                    }
                }
        );
    }

    private void startClock() {
        this.timeService.startClock(
                new TimeService.ClockListener() {
                    @Override
                    public void onTimeChanged(final String currentTime) {
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
                                            LOGGER.log(Level.WARNING, "Periodic application task failed: " + exception);
                                        }
                                    }
                                }
                        );
                    }
                }
        );
    }
}