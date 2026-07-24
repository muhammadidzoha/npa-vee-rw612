package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.menu.MenuItemData;
import com.nxp.example.smartgreenhouse.model.sensor.*;
import com.nxp.example.smartgreenhouse.state.AppState;
import com.nxp.example.smartgreenhouse.view.MainPage;
import com.nxp.example.smartgreenhouse.view.detail.SensorDetail;

public class DetailController implements SensorDetail.onBackListener {

    private final MainPage mainPage;
    private final AppState appState;
    private final SensorDataStore sensorDataStore;

    public DetailController(MainPage mainPage, AppState appState, SensorDataStore sensorDataStore) {
        this.mainPage = mainPage;
        this.appState = appState;
        this.sensorDataStore = sensorDataStore;
    }

    public void init() {
        this.mainPage.setOnSensorDetailBackListener(this);
    }

    public void openSelectedSensor() {
        MenuItemData selectedMenuItem = this.appState.getSelectedMenuItem();

        if (selectedMenuItem == null || !selectedMenuItem.isSensor()) {
            return;
        }

        int nodeId = this.appState.getSelectedNodeId();

        if (nodeId < 0) {
            this.mainPage.clearSensorDetail();
            return;
        }

        SensorData sensorData = this.sensorDataStore.getNodeById(nodeId);

        if (sensorData == null) {
            this.mainPage.clearSensorDetail();
            return;
        }

        int sensorId = selectedMenuItem.getSensorId();

        SensorDefinition definition = SensorDefinitionProvider.getById(sensorId);

        if (definition == null) {
            this.mainPage.clearSensorDetail();
            return;
        }

        SensorDisplayItem displayItem = SensorDisplayBuilder.build(definition, sensorData);

        double gaugeMinimum = SensorGaugeRangeProvider.getMinimum(sensorId);

        double gaugeMaximum = SensorGaugeRangeProvider.getMaximum(sensorId);

        String title = definition.getTitle() + " - TRAY " + nodeId;

        this.mainPage.updateSensorDetail(title, displayItem, gaugeMinimum, gaugeMaximum);

        this.mainPage.openSensorDetail();
    }

    @Override
    public void onBack() {
        this.mainPage.closeSensorDetail();
    }
}
