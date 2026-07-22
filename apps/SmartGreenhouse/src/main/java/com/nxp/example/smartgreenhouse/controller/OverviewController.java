package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.*;
import com.nxp.example.smartgreenhouse.view.MainPage;

import java.util.ArrayList;
import java.util.List;

public class OverviewController {

    private final MainPage mainPage;

    public OverviewController(MainPage mainPage) {
        this.mainPage = mainPage;
    }

    public void init() {
        int visibleCount = countVisibleSensors();
        mainPage.initOverview(visibleCount);
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