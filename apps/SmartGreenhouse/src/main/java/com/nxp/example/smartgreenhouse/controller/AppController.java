package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.actuator.ActuatorDataStore;
import com.nxp.example.smartgreenhouse.model.actuator.SampleActuatorData;
import com.nxp.example.smartgreenhouse.model.sensor.*;
import com.nxp.example.smartgreenhouse.state.AppState;
import com.nxp.example.smartgreenhouse.view.MainPage;

public class AppController {
    private final MainPage mainPage;

    private final HeaderController headerController;
    private final OverviewController overviewController;
    private final MenuController menuController;
    private final SensorDetailController sensorDetailController;
    private final ActuatorDetailController actuatorDetailController;
    private final SensorHistoryStore sensorHistoryStore;
    private final ActuatorDataStore actuatorDataStore;

    public AppController() {
        this.mainPage = new MainPage();
        AppState appState = new AppState();
        SensorDataStore sensorDataStore = new SensorDataStore();
        this.sensorHistoryStore = new SensorHistoryStore();
        this.actuatorDataStore = new ActuatorDataStore();

        this.headerController = new HeaderController(this.mainPage);
        this.overviewController = new OverviewController(this.mainPage, appState, sensorDataStore);
        this.sensorDetailController = new SensorDetailController(this.mainPage, appState, sensorDataStore, this.sensorHistoryStore);
        this.actuatorDetailController = new ActuatorDetailController(this.mainPage, appState, this.actuatorDataStore);
        this.menuController = new MenuController(this.mainPage, appState, this.sensorDetailController, this.actuatorDetailController);
    }

    public MainPage getMainPage() {
        return this.mainPage;
    }

    public void start() {
        loadSensorData();

        this.headerController.init();
        this.overviewController.init();
        this.menuController.init();
        this.sensorDetailController.init();
        this.actuatorDetailController.init();
    }

    private void loadSensorData() {
        SensorData[] nodes = SampleSensorData.createSampleSensorData();
        this.overviewController.setSensorNodes(nodes);
        this.sensorHistoryStore.addAll(SampleSensorHistoryData.create());
        SampleActuatorData.load(this.actuatorDataStore);
    }
}