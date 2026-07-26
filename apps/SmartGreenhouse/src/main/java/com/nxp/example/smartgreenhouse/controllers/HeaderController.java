package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.models.wifi.WifiScanResult;
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
        WifiAuthenticationContainer.OnWifiConnectClickListener {

    private static final Logger LOGGER =
            Logger.getLogger("[SMART GREENHOUSE: HEADER CONTROLLER]");

    private static final String INVALID_PASSWORD_MESSAGE = "Password harus 8-64 karakter.";

    private static final String CONNECTION_FAILED_MESSAGE = "Gagal terhubung. Periksa password.";

    private final MainPage mainPage;
    private final WifiHardwareService wifiService;

    private boolean wifiScanRunning;
    private boolean wifiConnectionRunning;
    private boolean wifiDisconnectionRunning;

    private Timer clockTimer;

    private int wifiScanRequestId;

    public HeaderController(MainPage mainPage) {
        this.mainPage = mainPage;
        this.wifiService = new WifiHardwareService();

        this.wifiScanRunning = false;
        this.wifiConnectionRunning = false;
        this.wifiDisconnectionRunning = false;

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

        if (isWifiOperationRunning()) {
            return;
        }

        if (network.isConnected()) {
            startWifiDisconnect(network);
            return;
        }

        this.mainPage.openWifiAuthentication(network);
    }

    @Override
    public void onAuthenticationBackClicked() {
        if (this.wifiConnectionRunning) {
            return;
        }

        LOGGER.log(Level.INFO, "WiFi authentication closed");
        this.mainPage.closeWifiAuthenticationToList();
    }

    @Override
    public void onWifiConnectClicked(final WifiNetwork network, final String password) {
        if (network == null) {
            return;
        }

        if (isWifiOperationRunning()) {
            return;
        }

        if (network.isConnected()) {
            return;
        }

        if (network.isSecured()) {
            int passwordLength = password == null ? 0 : password.length();
            if (passwordLength < 8 || passwordLength > 64) {
                LOGGER.log(Level.WARNING, "WiFi password must contain " + "8 to 64 characters");
                this.mainPage.showWifiAuthenticationError(INVALID_PASSWORD_MESSAGE);
                return;
            }
        }

        this.wifiConnectionRunning = true;

        this.mainPage.clearWifiAuthenticationError();
        this.mainPage.showWifiConnecting();
        LOGGER.log(Level.INFO, "WiFi connection started" + " | SSID: " + network.getName());

        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                boolean connectionSuccessful = false;
                                boolean actuallyConnected = false;
                                String errorMessage = null;
                                try {
                                    connectionSuccessful = HeaderController.this.wifiService.connect(network, password);
                                    actuallyConnected = HeaderController.this.wifiService.isConnected();
                                    if (!connectionSuccessful) {
                                        errorMessage = "Access point was not joined.";
                                    }
                                } catch (Exception exception) {
                                    connectionSuccessful = false;
                                    errorMessage = exception.toString();
                                    try {
                                        actuallyConnected = HeaderController.this.wifiService.isConnected();
                                    } catch (Exception statusException) {
                                        actuallyConnected = false;
                                        LOGGER.log(Level.WARNING, "Failed to read WiFi status" + " after connection error: " + statusException);
                                    }
                                }

                                final boolean success = connectionSuccessful;
                                final boolean connected = actuallyConnected;
                                final String error = errorMessage;
                                MicroUI.callSerially(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                HeaderController.this.wifiConnectionRunning = false;
                                                HeaderController.this.mainPage.updateWifiConnectionStatus(connected);
                                                if (!HeaderController.this.mainPage.isWifiAuthenticationOpen()) {
                                                    return;
                                                }

                                                if (success) {
                                                    LOGGER.log(Level.INFO, "WiFi connection successful" + " | SSID: " + network.getName());
                                                    HeaderController.this.mainPage.closeWifiAfterConnectionSuccess();
                                                } else {
                                                    LOGGER.log(Level.WARNING, "WiFi connection failed" + " | SSID: " + network.getName() + " | reason: " + error);
                                                    HeaderController.this.mainPage.stopWifiConnecting();
                                                    HeaderController.this.mainPage.showWifiAuthenticationError(CONNECTION_FAILED_MESSAGE);
                                                }
                                            }
                                        }
                                );
                            }
                        },
                        "wifi-connect"
                );

        worker.start();
    }

    private void startWifiDisconnect(final WifiNetwork network) {
        if (network == null) {
            return;
        }

        if (isWifiOperationRunning()) {
            return;
        }

        this.wifiDisconnectionRunning = true;

        this.wifiScanRequestId++;
        this.mainPage.stopWifiScanning();

        LOGGER.log(Level.INFO, "WiFi disconnection started" + " | SSID: " + network.getName());
        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                boolean disconnectSuccessful = false;
                                boolean actuallyConnected = true;
                                String errorMessage = null;
                                try {
                                    disconnectSuccessful = HeaderController.this.wifiService.disconnect();
                                    actuallyConnected = HeaderController.this.wifiService.isConnected();
                                } catch (Exception exception) {
                                    errorMessage = exception.toString();
                                    try {
                                        actuallyConnected = HeaderController.this.wifiService.isConnected();
                                    } catch (Exception statusException) {
                                        actuallyConnected = true;
                                        LOGGER.log(Level.WARNING, "Failed to read WiFi status" + " after disconnect error: " + statusException);
                                    }
                                }
                                final boolean success = disconnectSuccessful;
                                final boolean connected = actuallyConnected;
                                final String error = errorMessage;
                                MicroUI.callSerially(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                HeaderController.this.wifiDisconnectionRunning = false;
                                                HeaderController.this.mainPage.updateWifiConnectionStatus(connected);
                                                if (success && !connected) {
                                                    LOGGER.log(Level.INFO, "WiFi disconnection successful" + " | SSID: " + network.getName());
                                                } else {
                                                    LOGGER.log(Level.WARNING, "WiFi disconnection failed" + " | SSID: " + network.getName() + " | reason: " + error);
                                                }
                                                if (HeaderController.this.mainPage.isWifiOpen()) {
                                                    HeaderController.this.startWifiScan();
                                                }
                                            }
                                        }
                                );
                            }
                        },
                        "wifi-disconnect"
                );

        worker.start();
    }

    private void startWifiScan() {
        if (isWifiOperationRunning()) {
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
                                    final WifiScanResult scanResult = HeaderController.this.wifiService.scan();
                                    final WifiNetwork[] networks = scanResult.getNetworks();
                                    final boolean connected = scanResult.isConnected();
                                    LOGGER.log(Level.INFO, "Wifi service scan returned" + " | count: " + networks.length + " | connected: " + connected);
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

    private boolean isWifiOperationRunning() {
        return this.wifiScanRunning || this.wifiConnectionRunning || this.wifiDisconnectionRunning;
    }

    private void checkWifiHardware() {
        Thread worker =
                new Thread(
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
                        },
                        "wifi-hardware-check"
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
                },
                0,
                1000
        );
    }
}