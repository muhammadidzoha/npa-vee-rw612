package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.models.actuator.*;
import com.nxp.example.smartgreenhouse.models.menu.MenuItemData;
import com.nxp.example.smartgreenhouse.state.AppState;
import com.nxp.example.smartgreenhouse.views.HorizontalSwipeListener;
import com.nxp.example.smartgreenhouse.views.MainPage;
import com.nxp.example.smartgreenhouse.views.actuator.ActuatorToggleListener;
import com.nxp.example.smartgreenhouse.views.detail.ActuatorDetail;

public class ActuatorDetailController implements ActuatorDetail.onBackListener, ActuatorToggleListener, HorizontalSwipeListener {

    private final MainPage mainPage;
    private final AppState appState;
    private final ActuatorDataStore actuatorDataStore;

    private ActuatorDisplayItem currentDisplayItem;

    public ActuatorDetailController(MainPage mainPage, AppState appState, ActuatorDataStore actuatorDataStore) {
        this.mainPage = mainPage;
        this.appState = appState;
        this.actuatorDataStore = actuatorDataStore;
        this.currentDisplayItem = null;
    }

    public void init() {
        this.mainPage.setOnActuatorDetailBackListener(this);
        this.mainPage.setOnActuatorToggleRequestedListener(this);
        this.mainPage.setOnActuatorDetailSwipeListener(this);
    }

    public void openSelectedActuator() {
        boolean actuatorAvailable = refreshSelectedActuator();

        if (!actuatorAvailable) {
            return;
        }

        this.mainPage.openActuatorDetail();
    }

    private boolean refreshSelectedActuator() {
        MenuItemData selectedMenuItem = this.appState.getSelectedMenuItem();

        if (selectedMenuItem == null || selectedMenuItem.isSensor()) {
            return false;
        }

        int actuatorId = selectedMenuItem.getActuatorId();
        long currentTimestamp = System.currentTimeMillis();

        String detailTitle;
        ActuatorDisplayItem displayItem;

        if (actuatorId == ActuatorId.POMPA_AIR) {
            detailTitle = "POMPA AIR";
            displayItem = ActuatorDisplayBuilder.buildPump(this.actuatorDataStore.getPumpData(), currentTimestamp);
        } else if (actuatorId == ActuatorId.KATUP_AIR) {
            int trayId = this.appState.getSelectedNodeId();
            ValveData valveData = this.actuatorDataStore.getValveByTrayId(trayId);

            if (valveData == null) {
                return false;
            }

            detailTitle = "KATUP AIR - TRAY " + trayId;

            int trayCount = this.actuatorDataStore.getValveCount();
            int trayIndex = this.actuatorDataStore.getValveIndexByTrayId(trayId);
            displayItem = ActuatorDisplayBuilder.buildValve(valveData, this.actuatorDataStore.getPumpData(), trayIndex, trayCount);
        } else {
            return false;
        }

        if (displayItem == null) {
            return false;
        }

        this.currentDisplayItem = displayItem;
        this.mainPage.updateActuatorDetail(detailTitle, displayItem);

        return true;
    }

    @Override
    public void onBack() {
        this.currentDisplayItem = null;
        this.mainPage.closeActuatorDetail();
    }

    @Override
    public void onToggleRequested(boolean targetState) {
        if (this.currentDisplayItem == null) {
            return;
        }

        long currentTimestamp = System.currentTimeMillis();

        if (this.currentDisplayItem.isPump()) {
            this.actuatorDataStore.updatePumpState(targetState, currentTimestamp);
        } else if (this.currentDisplayItem.isValve()) {
            this.actuatorDataStore.updateValveState(this.currentDisplayItem.getTrayId(), targetState, currentTimestamp);
        }

        refreshSelectedActuator();
    }

    @Override
    public void onSwipeLeft() {
        moveToNextTray();
    }

    @Override
    public void onSwipeRight() {
        moveToPreviousTray();
    }

    private void moveToNextTray() {
        if (this.currentDisplayItem == null || !this.currentDisplayItem.isValve()) {
            return;
        }

        int currentTrayId = this.currentDisplayItem.getTrayId();
        int targetTrayId = this.actuatorDataStore.getNextValveTrayId(currentTrayId);

        updateSelectedTray(currentTrayId, targetTrayId);
    }

    private void moveToPreviousTray() {
        if (this.currentDisplayItem == null || !this.currentDisplayItem.isValve()) {
            return;
        }

        int currentTrayId = this.currentDisplayItem.getTrayId();
        int targetTrayId = this.actuatorDataStore.getPreviousValveTrayId(currentTrayId);

        updateSelectedTray(currentTrayId, targetTrayId);
    }

    private void updateSelectedTray(int currentTrayId, int targetTrayId) {
        if (targetTrayId == ActuatorDisplayItem.NO_TRAY_ID) {
            return;
        }

        if (targetTrayId == currentTrayId) {
            return;
        }

        ValveData targetValve = this.actuatorDataStore.getValveByTrayId(targetTrayId);

        if (targetValve == null) {
            return;
        }

        this.appState.setSelectedNodeId(targetTrayId);

        refreshSelectedActuator();
    }
}
