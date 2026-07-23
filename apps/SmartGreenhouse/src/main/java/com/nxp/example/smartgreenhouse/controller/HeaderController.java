package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.utils.Time;
import com.nxp.example.smartgreenhouse.view.MainPage;
import com.nxp.example.smartgreenhouse.view.overview.HeaderOverview;
import ej.bon.Timer;
import ej.bon.TimerTask;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.logging.Logger;

public class HeaderController implements HeaderOverview.onWifiClickListener {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: HEADER CONTROLLER]");

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
        boolean wifiMenuOpen = this.mainPage.getWifiOpen();
        this.mainPage.setWifiOpen(!wifiMenuOpen);
    }

    private void startClock() {
        if (this.clockTimer == null) {
            this.clockTimer = new Timer();
            this.clockTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    String time = Time.formatTime(ZonedDateTime.now(ZoneId.of("+07:00")));
                    mainPage.updateTime(time);
                }
            }, 0, 1000);
        }
    }
}
