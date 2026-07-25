package com.nxp.example.smartgreenhouse.model.actuator;

public final class ActuatorDisplayBuilder {

    private ActuatorDisplayBuilder() {
    }

    public static ActuatorDisplayItem buildPump(PumpData pumpData, long currentTimestamp) {
        if (pumpData == null) {
            return null;
        }

        long activeDuration = pumpData.getCurrentActiveDuration(currentTimestamp);
        long lastActivatedAt = pumpData.getActiveSince();
        long lastUpdated = pumpData.getLastUpdated();
        String durationText = ActuatorDisplayFormatter.formatDuration(activeDuration);
        String lastActivatedTimeText = ActuatorDisplayFormatter.formatTime(lastActivatedAt);
        String lastActivatedDateText = ActuatorDisplayFormatter.formatDate(lastActivatedAt);
        String lastUpdatedTimeText = ActuatorDisplayFormatter.formatTime(lastUpdated);

        return new ActuatorDisplayItem(
                ActuatorId.POMPA_AIR,
                ActuatorDisplayItem.NO_TRAY_ID,
                pumpData.isActive(),
                false,
                durationText,
                lastActivatedTimeText,
                lastActivatedDateText,
                lastUpdatedTimeText,
                ActuatorDisplayItem.NO_TRAY_INDEX,
                0
        );
    }

    public static ActuatorDisplayItem buildValve(ValveData valveData, PumpData pumpData, int trayIndex, int trayCount) {
        if (valveData == null) {
            return null;
        }

        boolean pumpActive = pumpData != null && pumpData.isActive();
        boolean valveOpen = valveData.isOpen();

        boolean flowActive = pumpActive && valveOpen;

        long lastOpenedAt = valveData.getLastOpenedAt();
        long lastUpdated = valveData.getLastUpdated();

        String lastActivatedTimeText = ActuatorDisplayFormatter.formatTime(lastOpenedAt);
        String lastActivatedDateText = ActuatorDisplayFormatter.formatDate(lastOpenedAt);

        String lastUpdatedTimeText = ActuatorDisplayFormatter.formatTime(lastUpdated);
        String durationText = "";

        return new ActuatorDisplayItem(
                ActuatorId.KATUP_AIR,
                valveData.getTrayId(),
                valveOpen,
                flowActive,
                durationText,
                lastActivatedTimeText,
                lastActivatedDateText,
                lastUpdatedTimeText,
                trayIndex,
                trayCount
        );
    }
}