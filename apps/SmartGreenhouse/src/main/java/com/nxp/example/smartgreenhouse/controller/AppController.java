package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.sensor.SampleSensorData;
import com.nxp.example.smartgreenhouse.model.sensor.SensorData;
import com.nxp.example.smartgreenhouse.view.MainPage;

public class AppController {
    private final MainPage mainPage;
    private final HeaderController headerController;
    private final OverviewController overviewController;
    private final MenuController menuController;

    public AppController() {
        this.mainPage = new MainPage();
        this.headerController = new HeaderController(this.mainPage);
        this.overviewController = new OverviewController(this.mainPage);
        this.menuController = new MenuController(this.mainPage);
    }

    public MainPage getMainPage() {
        return this.mainPage;
    }

    public void start() {
        this.headerController.init();
        this.overviewController.init();
        this.menuController.init();
        loadSensorData();
    }

    private void loadSensorData() {
        SensorData[] nodes = SampleSensorData.createSampleSensorData();
        this.overviewController.setSensorNodes(nodes);
    }
}