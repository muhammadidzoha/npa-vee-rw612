package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.SampleSensorData;
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
        SensorData[] nodes = SampleSensorData.createSampleSensorData();
        this.overviewController.setSensorNodes(nodes);
    }
}