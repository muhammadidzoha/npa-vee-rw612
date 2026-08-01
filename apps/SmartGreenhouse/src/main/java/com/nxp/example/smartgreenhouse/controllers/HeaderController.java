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

    public HeaderController(MainPage mainPage) {
        if (mainPage == null) {
            throw new NullPointerException("mainPage tidak boleh null.");
        }

        this.mainPage = mainPage;
        this.timeService = new TimeService();
        this.wifiProvisioningService =
                new WifiProvisioningService(this.timeService,
                        new WifiProvisioningService.Listener() {
                            @Override
                            public void onWifiConnectionStatusChanged(boolean connected) {
                                HeaderController.this.mainPage.updateWifiConnectionStatus(connected);
                            }
                        }
                );

        this.periodicTask = null;
    }

    public void init() {
        this.mainPage.updateWifiConnectionStatus(false);
        registerProvisioningSmokeTestListener();
        startClock();
        this.wifiProvisioningService.connectConfiguredWifi();
    }

    public void setPeriodicTask(Runnable periodicTask) {
        this.periodicTask = periodicTask;
    }

    public void setWifiConnectedTask(Runnable wifiConnectedTask) {
        this.wifiProvisioningService.setWifiConnectedTask(wifiConnectedTask);
    }

    private void registerProvisioningSmokeTestListener() {
        this.mainPage.setOnWifiClick(
                new HeaderOverview.onWifiClickListener() {
                    @Override
                    public void onClicked() {
                        HeaderController.this.wifiProvisioningService.startProvisioningSmokeTest();
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
