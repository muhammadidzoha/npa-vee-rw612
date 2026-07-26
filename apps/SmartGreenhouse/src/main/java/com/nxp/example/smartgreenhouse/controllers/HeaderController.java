package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.models.wifi.SampleWifiData;
import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.utils.Time;
import com.nxp.example.smartgreenhouse.views.MainPage;
import com.nxp.example.smartgreenhouse.views.overview.HeaderOverview;
import com.nxp.example.smartgreenhouse.views.wifi.WifiAuthenticationContainer;
import com.nxp.example.smartgreenhouse.views.wifi.WifiContainer;
import ej.bon.Timer;
import ej.bon.TimerTask;
import ej.microui.MicroUI;

import java.util.logging.Level;
import java.util.logging.Logger;

public class HeaderController implements
        HeaderOverview.onWifiClickListener,
        WifiContainer.OnRefreshClickListener,
        WifiContainer.OnWifiNetworkClickListener,
        WifiAuthenticationContainer.OnAuthenticationBackClickListener,
        WifiAuthenticationContainer.OnWifiConnectClickListener
{

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: HEADER CONTROLLER]");

    private static final long SIMULATED_SCAN_DURATION = 2_000L;

    private static final long SIMULATED_CONNECTION_DURATION = 2_000L;

    private final MainPage mainPage;
    private Timer clockTimer;

    private Timer wifiScanTimer;
    private Timer wifiConnectionTimer;
    private int wifiScanRequestId;
    private int wifiConnectionRequestId;

    public HeaderController(MainPage mainPage) {
        this.mainPage = mainPage;

        this.clockTimer = null;
        this.wifiScanTimer = null;
        this.wifiConnectionTimer = null;

        this.wifiScanRequestId = 0;
        this.wifiConnectionRequestId = 0;
    }

    public void init() {
        startClock();
        this.mainPage.setOnWifiClick(this);
        this.mainPage.setOnWifiRefreshClick(this);
        this.mainPage.setOnWifiNetworkClick(this);
        this.mainPage.setOnWifiAuthenticationBackClick(this);
        this.mainPage.setOnWifiConnectClick(this);
    }

    @Override
    public void onClicked() {
        if (this.mainPage.isWifiOpen()) {
            this.wifiScanRequestId++;
            this.mainPage.stopWifiScanning();
            this.mainPage.closeWifi();

            LOGGER.log(Level.INFO, "WiFi menu closed");
            return;
        }

        this.mainPage.openWifi();

        startWifiScan();
    }

    @Override
    public void onRefreshClicked() {
        if (!this.mainPage.isWifiOpen()) {
            return;
        }

        startWifiScan();
    }

    @Override
    public void onWifiNetworkClicked(WifiNetwork network) {
        if (network == null) {
            return;
        }

        if (network.isConnected()) {
            return;
        }

        LOGGER.log(Level.INFO, "Opening WiFi authentication for: " + network.getName());
        this.mainPage.openWifiAuthentication(network);
    }

    @Override
    public void onAuthenticationBackClicked() {
        this.wifiConnectionRequestId++;
        LOGGER.log(Level.INFO, "WiFi authentication closed");
        this.mainPage.closeWifiAuthenticationToList();
    }

    @Override
    public void onWifiConnectClicked(WifiNetwork network, String password) {
        if (network == null) {
            return;
        }

        if (network.isSecured() && password.length() == 0) {
            LOGGER.log(Level.WARNING, "WiFi password is empty for: " + network.getName());
            return;
        }

        final WifiNetwork selectedNetwork = network;
        final int requestId = ++this.wifiConnectionRequestId;

        this.mainPage.showWifiConnecting();

        LOGGER.log(
                Level.INFO,
                "WiFi connection started"
                        + " | SSID: "
                        + selectedNetwork.getName()
                        + " | password length: "
                        + password.length()
        );

        if (this.wifiConnectionTimer == null) {
            this.wifiConnectionTimer = new Timer();
        }

        this.wifiConnectionTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        MicroUI.callSerially(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (requestId != HeaderController.this.wifiConnectionRequestId) {
                                            return;
                                        }

                                        if (!HeaderController.this.mainPage.isWifiAuthenticationOpen()) {
                                            return;
                                        }

                                        LOGGER.log(Level.INFO, "WiFi connection successful" + " | SSID: " + selectedNetwork.getName());

                                        HeaderController.this.mainPage.updateWifiConnectionStatus(true);
                                        HeaderController.this.mainPage.closeWifiAfterConnectionSuccess();
                                    }
                                }
                        );
                    }
                },
                SIMULATED_CONNECTION_DURATION
        );
    }

    private void startWifiScan() {
        final int requestId = ++this.wifiScanRequestId;

        this.mainPage.showWifiScanning();

        LOGGER.log(Level.INFO, "WiFi scanning started");

        if (this.wifiScanTimer == null) {
            this.wifiScanTimer = new Timer();
        }

        this.wifiScanTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        final WifiNetwork[] networks = SampleWifiData.createSampleWifiData();
                        MicroUI.callSerially(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        if (requestId != HeaderController.this.wifiScanRequestId) {
                                            return;
                                        }
                                        if (!HeaderController.this.mainPage.isWifiOpen()) {
                                            return;
                                        }

                                        HeaderController.this.mainPage.updateWifiNetworks(networks);
                                        HeaderController.this.mainPage.updateWifiConnectionStatus(hasConnectedNetwork(networks));
                                        LOGGER.log(Level.INFO, "WiFi scanning completed");
                                    }
                                }
                        );
                    }
                },
                SIMULATED_SCAN_DURATION
        );
    }

    private static boolean hasConnectedNetwork(WifiNetwork[] networks) {
        if (networks == null) {
            return false;
        }

        for (WifiNetwork network : networks) {
            if (network != null && network.isConnected()) {
                return true;
            }
        }

        return false;
    }

    private void startClock() {
        if (this.clockTimer != null) {
            return;
        }
        this.clockTimer = new Timer();
        this.clockTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        final String currentTime = Time.formatCurrentTime();
                        MicroUI.callSerially(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        HeaderController.this.mainPage.updateTime(currentTime);
                                    }
                                }
                        );
                    }
                }, 0, 1000
        );
    }
}
