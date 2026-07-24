package com.nxp.example.smartgreenhouse.model.sensor;

public final class SensorHistoryEntry {

    private final String shortTime;
    private final String fullTime;
    private final SensorData sensorData;

    public SensorHistoryEntry(String shortTime, String fullTime, SensorData sensorData) {
        this.shortTime = shortTime == null ? "" : shortTime;
        this.fullTime = fullTime == null ? "" : fullTime;
        this.sensorData = copySensorData(sensorData);
    }

    public String getShortTime() {
        return this.shortTime;
    }

    public String getFullTime() {
        return this.fullTime;
    }

    public int getNodeId() {
        if (this.sensorData == null) {
            return -1;
        }

        return this.sensorData.getNodeId();
    }

    public SensorData getSensorData() {
        return this.sensorData;
    }

    private static SensorData copySensorData(SensorData source) {
        if (source == null) {
            return null;
        }

        SensorData copy = new SensorData(source.getNodeId(), source.getTargetId());

        copy.setN(source.getN());
        copy.setP(source.getP());
        copy.setK(source.getK());
        copy.setSm(source.getSm());
        copy.setpH(source.getpH());
        copy.setSt(source.getSt());
        copy.setEc(source.getEc());
        copy.setAt(source.getAt());
        copy.setAh(source.getAh());
        copy.setLux(source.getLux());

        return copy;
    }
}