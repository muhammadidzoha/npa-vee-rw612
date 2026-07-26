package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.services.wifi.WifiHardwareService;
import com.nxp.example.smartgreenhouse.utils.Time;
import com.nxp.example.smartgreenhouse.views.MainPage;
import com.nxp.example.smartgreenhouse.views.overview.HeaderOverview;
import com.nxp.example.smartgreenhouse.views.wifi.WifiAuthenticationContainer;
import com.nxp.example.smartgreenhouse.views.wifi.WifiContainer;
import ej.bon.Timer;
import ej.bon.TimerTask;
import ej.ecom.wifi.WifiCapability;
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

    private final MainPage mainPage;
    private final WifiHardwareService wifiService;

    private boolean wifiScanRunning;
    private boolean wifiConnectionRunning;

    private Timer clockTimer;

    private int wifiScanRequestId;

    public HeaderController(MainPage mainPage) {
        this.mainPage = mainPage;
        this.wifiService = new WifiHardwareService();

        this.wifiScanRunning = false;
        this.wifiConnectionRunning = false;

        this.clockTimer = null;

        this.wifiScanRequestId = 0;
    }

    public void init() {
        startClock();
        this.mainPage.setOnWifiClick(this);
        this.mainPage.setOnWifiRefreshClick(this);
        this.mainPage.setOnWifiNetworkClick(this);
        this.mainPage.setOnWifiAuthenticationBackClick(this);
        this.mainPage.setOnWifiConnectClick(this);
        checkWifiHardware();
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

        this.mainPage.openWifiAuthentication(network);
    }

    @Override
    public void onAuthenticationBackClicked() {
        LOGGER.log(Level.INFO, "WiFi authentication closed");
        this.mainPage.closeWifiAuthenticationToList();
    }

    @Override
    public void onWifiConnectClicked(final WifiNetwork network, final String password) {
        if (network == null) {
            return;
        }

        if (this.wifiConnectionRunning) {
            return;
        }

        if (network.isConnected()) {
            return;
        }

        if (network.isSecured()) {
            int passwordLength = password == null ? 0 : password.length();

            if (passwordLength < 8 || passwordLength > 64) {
                LOGGER.log(Level.WARNING, "WiFi password must contain " + "8 to 64 characters");
                return;
            }
        }

        this.wifiConnectionRunning = true;

        this.mainPage.showWifiConnecting();

        LOGGER.log(Level.INFO, "WiFi connection started" + " | SSID: " + network.getName());

        Thread worker = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                boolean connectionSuccessful;
                                String errorMessage;
                                try {
                                    connectionSuccessful = HeaderController.this.wifiService.connect(network, password);
                                    errorMessage = connectionSuccessful ? null : "Access point was not joined.";
                                } catch (Exception exception) {
                                    connectionSuccessful = false;
                                    errorMessage = exception.toString();
                                }

                                final boolean success = connectionSuccessful;

                                final String error = errorMessage;
                                MicroUI.callSerially(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                HeaderController.this.wifiConnectionRunning = false;
                                                if (!HeaderController.this.mainPage.isWifiAuthenticationOpen()) {
                                                    return;
                                                }

                                                if (success) {
                                                    LOGGER.log(Level.INFO, "WiFi connection successful" + " | SSID: " + network.getName());

                                                    HeaderController.this.mainPage.updateWifiConnectionStatus(true);
                                                    HeaderController.this.mainPage.closeWifiAfterConnectionSuccess();
                                                } else {
                                                    LOGGER.log(Level.WARNING, "WiFi connection failed" + " | SSID: " + network.getName() + " | reason: " + error);

                                                    HeaderController.this.mainPage.stopWifiConnecting();
                                                    HeaderController.this.mainPage.updateWifiConnectionStatus(false);
                                                }
                                            }
                                        }
                                );
                            }
                        }
                );

        worker.start();
    }

    private void startWifiScan() {
        if (this.wifiScanRunning) {
            return;
        }

        final int requestId = ++this.wifiScanRequestId;

        this.wifiScanRunning = true;

        this.mainPage.showWifiScanning();

        LOGGER.log(Level.INFO, "WiFi scanning started");

        Thread scanThread =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final WifiNetwork[] networks = HeaderController.this.wifiService.scan();
                                    LOGGER.log(Level.INFO, "Wifi service scan returned" + " | count: " + networks.length);

                                    final boolean connected = HeaderController.this.wifiService.isConnected();
                                    MicroUI.callSerially(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    HeaderController.this.wifiScanRunning = false;
                                                    if (requestId != HeaderController.this.wifiScanRequestId) {
                                                        return;
                                                    }

                                                    if (!HeaderController.this.mainPage.isWifiOpen()) {
                                                        return;
                                                    }

                                                    HeaderController.this.mainPage.updateWifiNetworks(networks);
                                                    HeaderController.this.mainPage.updateWifiConnectionStatus(connected);

                                                    LOGGER.log(Level.INFO, "WiFi scanning completed");
                                                }
                                            }
                                    );

                                } catch (final Exception exception) {
                                    MicroUI.callSerially(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    HeaderController.this.wifiScanRunning = false;
                                                    HeaderController.this.mainPage.stopWifiScanning();
                                                    LOGGER.log(Level.WARNING, "WiFi scanning failed: " + exception);
                                                }
                                            }
                                    );
                                }
                            }
                        },
                        "wifi-scan"
                );
        scanThread.start();
    }

    private void checkWifiHardware() {
        Thread worker = new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    final WifiCapability capability = HeaderController.this.wifiService.getCapability();
                                    final boolean connected = HeaderController.this.wifiService.isConnected();
                                    LOGGER.log(Level.INFO, "WiFi capability: " + capability);
                                    MicroUI.callSerially(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    HeaderController.this.mainPage.updateWifiConnectionStatus(connected);
                                                }
                                            }
                                    );
                                } catch (Exception exception) {
                                    LOGGER.log(Level.WARNING, "WiFi hardware initialization failed: " + exception);
                                    MicroUI.callSerially(
                                            new Runnable() {
                                                @Override
                                                public void run() {
                                                    HeaderController.this.mainPage.updateWifiConnectionStatus(false);
                                                }
                                            }
                                    );
                                }
                            }
                        }
                );

        worker.start();
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
