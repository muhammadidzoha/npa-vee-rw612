package com.nxp.example.smartgreenhouse.model.actuator;

public final class ValveData {

    private final int valveId;

    private final int trayId;

    private boolean open;

    private long lastOpenedAt;

    private long lastUpdated;

    public ValveData(int valveId, int trayId) {
        this.valveId = valveId;
        this.trayId = trayId;

        this.open = false;
        this.lastOpenedAt = 0;
        this.lastUpdated = 0;
    }

    public int getValveId() {
        return this.valveId;
    }

    public int getTrayId() {
        return this.trayId;
    }

    public boolean isOpen() {
        return this.open;
    }

    public long getLastOpenedAt() {
        return this.lastOpenedAt;
    }

    public long getLastUpdated() {
        return this.lastUpdated;
    }

    public void updateState(boolean newOpen, long timestamp) {
        boolean previousOpen = this.open;

        if (!previousOpen && newOpen) {
            this.lastOpenedAt = timestamp;
        }

        this.open = newOpen;
        this.lastUpdated = timestamp;
    }
}