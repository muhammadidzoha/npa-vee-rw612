package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.models.wifi.SampleWifiData;
import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.utils.Time;
import com.nxp.example.smartgreenhouse.views.MainPage;
import com.nxp.example.smartgreenhouse.views.overview.HeaderOverview;
import ej.bon.Timer;
import ej.bon.TimerTask;
import ej.microui.MicroUI;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;

public class HeaderController implements HeaderOverview.onWifiClickListener {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: HEADER CONTROLLER]");

    private static final ZoneOffset WIB_OFFSET = ZoneOffset.ofHours(7);

    private final MainPage mainPage;
    private Timer clockTimer;

    public HeaderController(MainPage mainPage) {
        this.mainPage = mainPage;
    }

    public void init() {
        startClock();
        this.mainPage.setOnWifiClick(this);
    }

    @Override
    public void onClicked() {
        if (this.mainPage.isWifiOpen()) {
            this.mainPage.closeWifi();
            LOGGER.log(Level.INFO, "WiFi menu closed");
            return;
        }

        this.mainPage.openWifi();

        LOGGER.log(Level.INFO, "WiFi menu opened, scanning...");

        WifiNetwork[] networks = SampleWifiData.createSampleWifiData();
        this.mainPage.updateWifiNetworks(networks);
    }

    private void startClock() {
        if (this.clockTimer != null) {
            return;
        }

        this.clockTimer = new Timer();
        this.clockTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                final String currentTime = Time.formatTime(ZonedDateTime.now(WIB_OFFSET));
                MicroUI.callSerially(new Runnable() {
                    @Override
                    public void run() {
                        mainPage.updateTime(currentTime);
                    }
                });
            }
        }, 0, 1000);
    }
}
