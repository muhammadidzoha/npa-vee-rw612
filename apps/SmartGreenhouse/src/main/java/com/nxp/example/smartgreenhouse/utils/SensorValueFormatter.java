package com.nxp.example.smartgreenhouse.utils;

import com.nxp.example.smartgreenhouse.model.sensor.SensorDefinition;

public class SensorValueFormatter {

    public static String format(SensorDefinition definition, float value) {
        int decimalPlace = definition.getDecimalPlace();
        if (decimalPlace == 0) {
            return String.valueOf((int) value);
        }

        int beforePoint = (int) value;
        int exponentValue = (int) Math.pow(10, decimalPlace);
        int afterPoint = (int) ((value - beforePoint) * exponentValue);

        return beforePoint + "." + afterPoint;
    }
}
