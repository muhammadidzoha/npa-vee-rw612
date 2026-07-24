package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.sensor.*;
import com.nxp.example.smartgreenhouse.state.AppState;
import com.nxp.example.smartgreenhouse.view.MainPage;

public class AppController {
    private final MainPage mainPage;

    private final HeaderController headerController;
    private final OverviewController overviewController;
    private final MenuController menuController;
    private final DetailController detailController;
    private final SensorHistoryStore sensorHistoryStore;

    public AppController() {
        this.mainPage = new MainPage();
        AppState appState = new AppState();
        SensorDataStore sensorDataStore = new SensorDataStore();
        this.sensorHistoryStore = new SensorHistoryStore();

        this.headerController = new HeaderController(this.mainPage);
        this.overviewController = new OverviewController(this.mainPage, appState, sensorDataStore);
        this.detailController = new DetailController(this.mainPage, appState, sensorDataStore, this.sensorHistoryStore);
        this.menuController = new MenuController(this.mainPage, appState, this.detailController);
    }

    public MainPage getMainPage() {
        return this.mainPage;
    }

    public void start() {
        this.headerController.init();
        this.overviewController.init();
        this.menuController.init();
        this.detailController.init();
        loadSensorData();
    }

    private void loadSensorData() {
        SensorData[] nodes = SampleSensorData.createSampleSensorData();
        this.overviewController.setSensorNodes(nodes);
        this.sensorHistoryStore.addAll(SampleSensorHistoryData.create());
    }
}