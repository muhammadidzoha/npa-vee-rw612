package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.utils.Time;
import com.nxp.example.smartgreenhouse.view.MainPage;
import ej.bon.Timer;
import ej.bon.TimerTask;

import java.time.ZoneId;
import java.time.ZonedDateTime;

public class MainPageController {
    private final MainPage mainPage;
    private Timer clockTimer;

    public MainPageController(MainPage mainPage) {
        this.mainPage = mainPage;
    }

    public void startClock() {
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
