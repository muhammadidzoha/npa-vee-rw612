package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.SensorData;
import com.nxp.example.smartgreenhouse.view.MainPage;
import com.nxp.example.smartgreenhouse.view.overview.HeaderOverview;

import java.util.logging.Logger;

public class AppController {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: APP CONTROLLER]");

    private final MainPage mainPage;
    private final MainPageController mainPageController;
    private final OverviewController overviewController;

    public AppController() {
        this.mainPage = new MainPage();
        this.mainPageController = new MainPageController(this.mainPage);
        this.overviewController = new OverviewController(this.mainPage);
    }

    public MainPage getMainPage() {
        return this.mainPage;
    }

    public void start() {
        this.overviewController.init();
        this.mainPageController.startClock();
        initListener();
        loadSensorData();
    }

    private void initListener() {
        this.mainPage.getHeaderOverview().setOnHeaderClickListener(new HeaderOverview.OnHeaderClickListener() {
            @Override
            public void onIconClicked() {
                LOGGER.info("Icon clicked");
            }
        });
    }

    private void loadSensorData() {
        SensorData dummyData = new SensorData(1, 1);
        dummyData.setN(35);
        dummyData.setP(20);
        dummyData.setK(180);
        dummyData.setSm(65);
        dummyData.setSt(25);
        dummyData.setpH(6.5f);
        dummyData.setEc(1.5f);
        dummyData.setAt(27);
        dummyData.setAh(60);
        dummyData.setLux(25000);

        this.overviewController.updateSensorData(dummyData);
    }
}