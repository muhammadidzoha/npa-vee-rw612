package com.nxp.example.smartgreenhouse.model;

import com.nxp.example.smartgreenhouse.utils.SensorValueFormatter;

public class SensorDisplayItem {

    private final SensorDefinition definition;
    private final float value;
    private final SensorStatus sensorStatus;

    public SensorDisplayItem(SensorDefinition definition, float value, SensorStatus sensorStatus) {
        this.definition = definition;
        this.value = value;
        this.sensorStatus = sensorStatus;
    }

    public SensorDefinition getDefinition() {
        return definition;
    }

    public float getValue() {
        return value;
    }

    public SensorStatus getSensorStatus() {
        return sensorStatus;
    }

    public String getFormattedValue() {
        return SensorValueFormatter.format(this.definition, this.value);
    }
}