package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.*;
import com.nxp.example.smartgreenhouse.view.MainPage;
import com.nxp.example.smartgreenhouse.view.SwipeListener;

import java.util.ArrayList;
import java.util.List;

public class OverviewController implements SwipeListener {

    private final MainPage mainPage;
    private SensorData[] sensorNodes;

    private int currentIndex;

    public OverviewController(MainPage mainPage) {
        this.mainPage = mainPage;
    }

    public void init() {
        int visibleCount = countVisibleSensors();
        mainPage.initOverview(visibleCount);
        mainPage.setOnOverviewSwipeListener(this);
    }

    public void setSensorNodes(SensorData[] nodes) {
        this.sensorNodes = nodes;
        this.currentIndex = 0;

        if (nodes.length > 0) {
            updateDisplay();
        }

    }

    @Override
    public void onSwipeLeft() {
        if (this.sensorNodes == null) return;

        int nextIndex = this.currentIndex + 1;
        if (nextIndex < this.sensorNodes.length) {
            this.currentIndex = nextIndex;
            updateDisplay();
        }
    }

    @Override
    public void onSwipeRight() {
        if (this.sensorNodes == null) return;

        int prevIndex = this.currentIndex - 1;
        if (prevIndex >= 0) {
            this.currentIndex = prevIndex;
            updateDisplay();
        }
    }

    private void updateDisplay() {
        String text = "TRAY " + (this.currentIndex + 1);
        mainPage.updateTrayLabel(text);
        updateSensorData(this.sensorNodes[this.currentIndex]);
        mainPage.updateTotalIndicator(this.sensorNodes.length);
        mainPage.updateSelectedIndicator(this.currentIndex);
    }

    public void updateSensorData(SensorData data) {
        SensorDefinition[] definitions = SensorDefinitionProvider.getAll();
        List<SensorDisplayItem> items = new ArrayList<>();

        for (SensorDefinition def : definitions) {
            if (def.isVisible()) {
                SensorDisplayItem item = SensorDisplayBuilder.build(def, data);
                items.add(item);
            }
        }

        SensorDisplayItem[] result = new SensorDisplayItem[items.size()];
        items.toArray(result);

        mainPage.updateSensorCards(result);
    }

    private int countVisibleSensors() {
        int count = 0;
        for (SensorDefinition def : SensorDefinitionProvider.getAll()) {
            if (def.isVisible()) {
                count++;
            }
        }
        return count;
    }
}