package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.menu.MenuItemData;
import com.nxp.example.smartgreenhouse.model.sensor.*;
import com.nxp.example.smartgreenhouse.state.AppState;
import com.nxp.example.smartgreenhouse.view.MainPage;
import com.nxp.example.smartgreenhouse.view.detail.SensorDetail;
import com.nxp.example.smartgreenhouse.view.linechart.ChartPoint;

public class DetailController implements SensorDetail.onBackListener {

    private final MainPage mainPage;
    private final AppState appState;
    private final SensorDataStore sensorDataStore;
    private final SensorHistoryStore sensorHistoryStore;

    public DetailController(MainPage mainPage, AppState appState, SensorDataStore sensorDataStore, SensorHistoryStore sensorHistoryStore) {
        this.mainPage = mainPage;
        this.appState = appState;
        this.sensorDataStore = sensorDataStore;
        this.sensorHistoryStore = sensorHistoryStore;
    }

    public void init() {
        this.mainPage.setOnSensorDetailBackListener(this);
    }

    public void openSelectedSensor() {
        MenuItemData selectedMenuItem =
                this.appState.getSelectedMenuItem();

        if (selectedMenuItem == null || !selectedMenuItem.isSensor()) {
            return;
        }

        int nodeId = this.appState.getSelectedNodeId();

        if (nodeId < 0) {
            this.mainPage.clearSensorDetail();
            return;
        }

        SensorData currentSensorData = this.sensorDataStore.getNodeById(nodeId);

        if (currentSensorData == null) {
            this.mainPage.clearSensorDetail();
            return;
        }

        int sensorId = selectedMenuItem.getSensorId();

        SensorDefinition definition = SensorDefinitionProvider.getById(sensorId);

        if (definition == null) {
            this.mainPage.clearSensorDetail();
            return;
        }

        SensorDisplayItem currentDisplayItem = SensorDisplayBuilder.build(definition, currentSensorData);

        double gaugeMinimum = SensorGaugeRangeProvider.getMinimum(sensorId);

        double gaugeMaximum = SensorGaugeRangeProvider.getMaximum(sensorId);

        SensorHistoryEntry[] historyEntries = this.sensorHistoryStore.getByNodeId(nodeId);

        ChartPoint[] historyPoints = buildChartPoints(historyEntries, definition);

        String title = definition.getTitle() + " - TRAY " + nodeId;

        this.mainPage.updateSensorDetail(title, currentDisplayItem, gaugeMinimum, gaugeMaximum, historyPoints);

        this.mainPage.openSensorDetail();
    }

    private ChartPoint[] buildChartPoints(SensorHistoryEntry[] historyEntries, SensorDefinition definition) {
        if (historyEntries == null || historyEntries.length == 0 || definition == null) {
            return new ChartPoint[0];
        }

        ChartPoint[] chartPoints = new ChartPoint[historyEntries.length];

        for (int i = 0; i < historyEntries.length; i++) {
            SensorHistoryEntry historyEntry = historyEntries[i];

            if (historyEntry == null || historyEntry.getSensorData() == null) {
                chartPoints[i] = new ChartPoint("", "", -1);
                continue;
            }

            SensorDisplayItem historyDisplayItem = SensorDisplayBuilder.build(definition, historyEntry.getSensorData());

            chartPoints[i] = new ChartPoint(historyEntry.getShortTime(), historyEntry.getFullTime(), historyDisplayItem.getValue());
        }

        return chartPoints;
    }

    @Override
    public void onBack() {
        this.mainPage.closeSensorDetail();
    }
}
