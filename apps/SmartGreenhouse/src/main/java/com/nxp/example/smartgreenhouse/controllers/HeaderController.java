package com.nxp.example.smartgreenhouse.controllers;

import com.nxp.example.smartgreenhouse.services.wifi.WifiHardwareService;
import com.nxp.example.smartgreenhouse.utils.Time;
import com.nxp.example.smartgreenhouse.views.MainPage;
import com.nxp.example.smartgreenhouse.views.overview.HeaderOverview;

import android.net.SntpClient;

import ej.bon.Timer;
import ej.bon.TimerTask;
import ej.bon.Util;
import ej.ecom.wifi.WifiCapability;
import ej.ecom.wifi.AccessPoint;
import ej.microui.MicroUI;

import ej.hoka.http.HttpRequest;
import ej.hoka.http.HttpResponse;
import ej.hoka.http.HttpServer;
import ej.hoka.http.requesthandler.RequestHandler;

import java.util.logging.Level;
import java.util.logging.Logger;

import java.io.UnsupportedEncodingException;

public class HeaderController {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: HEADER CONTROLLER]");

    private final MainPage mainPage;
    private final WifiHardwareService wifiService;

    private HttpServer provisioningHttpServer;

    private Timer clockTimer;

    private Runnable periodicTask;
    private Runnable wifiConnectedTask;

    private boolean wifiConnectionRunning;
    private volatile boolean provisioningSmokeTestRunning;

    private static final String[] NTP_SERVERS = {"time.google.com", "0.pool.ntp.org"};
    private static final int NTP_TIMEOUT_MS = 5000;
    private static final long NETWORK_READY_DELAY_MS = 3000L;
    private static final long PROVISIONING_SOFT_AP_TEST_DURATION_MS = 300000L;
    private static final long PROVISIONING_CLIENT_RESTART_DELAY_MS = 1500L;
    private long headerNtpTimeMillis;
    private long headerNtpReferenceMillis;
    private boolean headerTimeSynchronized;

    public HeaderController(MainPage mainPage) {
        if (mainPage == null) {
            throw new NullPointerException("mainPage tidak boleh null.");
        }

        this.mainPage = mainPage;
        this.wifiService = new WifiHardwareService();
        this.provisioningHttpServer = null;
        this.clockTimer = null;

        this.periodicTask = null;
        this.wifiConnectedTask = null;

        this.wifiConnectionRunning = false;
        this.provisioningSmokeTestRunning = false;

        this.headerNtpTimeMillis = 0L;
        this.headerNtpReferenceMillis = 0L;
        this.headerTimeSynchronized = false;
    }

    public void init() {
        this.mainPage.updateWifiConnectionStatus(false);
        registerProvisioningSmokeTestListener();
        startClock();
        connectConfiguredWifi();
    }

    public void setPeriodicTask(Runnable periodicTask) {
        this.periodicTask = periodicTask;
    }

    public void setWifiConnectedTask(Runnable wifiConnectedTask) {
        this.wifiConnectedTask = wifiConnectedTask;
    }

    private void registerProvisioningSmokeTestListener() {
        this.mainPage.setOnWifiClick(
                new HeaderOverview.onWifiClickListener() {
                    @Override
                    public void onClicked() {
                        HeaderController.this.startProvisioningSmokeTest();
                    }
                }
        );
    }

    private void startProvisioningSmokeTest() {
        if (this.provisioningSmokeTestRunning) {
            LOGGER.log(Level.WARNING, "Provisioning smoke test ignored" + " | test is already running");
            return;
        }

        if (this.wifiConnectionRunning) {
            LOGGER.log(Level.WARNING, "Provisioning smoke test ignored" + " | automatic WiFi connection" + " is still running");
            return;
        }

        this.provisioningSmokeTestRunning = true;
        this.mainPage.updateWifiConnectionStatus(false);

        LOGGER.log(Level.INFO, "Provisioning smoke test requested");
        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                HeaderController.this.runProvisioningSmokeTest();
                            }
                        }, "wifi-provisioning-smoke-test"
                );
        try {
            worker.start();
        } catch (Error error) {
            this.provisioningSmokeTestRunning = false;
            LOGGER.log(Level.SEVERE, "Unable to start provisioning" + " smoke test worker" + " | error=" + error);
        }
    }

    private void runProvisioningSmokeTest() {
        boolean reconnected = false;
        String testError = null;
        try {
            LOGGER.log(Level.INFO, "Provisioning smoke test started");
            AccessPoint[] accessPoints = this.wifiService.scanProvisioningNetworks();
            logProvisioningNetworks(accessPoints);
            this.wifiService.startProvisioningAccessPoint();
            this.provisioningHttpServer = createProvisioningHttpServer(accessPoints);
            LOGGER.log(Level.INFO, "Starting HOKA provisioning server" + " | port=80");
            this.provisioningHttpServer.start();
            LOGGER.log(Level.INFO, "HOKA provisioning server started" + " | port=80");
            LOGGER.log(
                    Level.INFO,
                    "Provisioning SoftAP smoke test active"
                            + " | SSID="
                            + this.wifiService
                            .getProvisioningSsid()
                            + " | password="
                            + this.wifiService
                            .getProvisioningPassword()
                            + " | durationMs="
                            + PROVISIONING_SOFT_AP_TEST_DURATION_MS
            );
            Thread.sleep(PROVISIONING_SOFT_AP_TEST_DURATION_MS);
            LOGGER.log(Level.INFO, "Provisioning SoftAP smoke test" + " duration completed");
        } catch (InterruptedException exception) {
            testError = "Provisioning worker interrupted" + " | error=" + exception;
        } catch (Exception exception) {
            testError = "Provisioning smoke test failed" + " | error=" + exception;
        } finally {
            if (this.provisioningHttpServer != null) {
                try {
                    LOGGER.log(Level.INFO, "Stopping HOKA provisioning server");
                    this.provisioningHttpServer.stop();
                    LOGGER.log(Level.INFO, "HOKA provisioning server stopped");
                } catch (Exception exception) {
                    LOGGER.log(Level.SEVERE, "Failed to stop HOKA provisioning server", exception);
                } finally {
                    this.provisioningHttpServer = null;
                }
            }
            try {
                this.wifiService.stopProvisioningAccessPoint();
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Unable to stop provisioning SoftAP" + " | error=" + exception);
                if (testError == null) {
                    testError = "Unable to stop SoftAP" + " | error=" + exception;
                }
            }
            try {
                Thread.sleep(PROVISIONING_CLIENT_RESTART_DELAY_MS);
            } catch (InterruptedException exception) {
                LOGGER.log(Level.WARNING, "Provisioning reconnect delay" + " interrupted" + " | error=" + exception);
            }
            try {
                LOGGER.log(
                        Level.INFO,
                        "Reconnecting configured WiFi"
                                + " after provisioning test"
                                + " | SSID="
                                + this.wifiService
                                .getConfiguredSsid()
                );

                reconnected = this.wifiService.connectConfiguredNetwork();
                if (reconnected) {
                    Thread.sleep(NETWORK_READY_DELAY_MS);
                    boolean timeSynchronized = synchronizeHeaderTime();
                    if (!timeSynchronized) {
                        LOGGER.log(Level.WARNING, "WiFi reconnected after" + " provisioning test," + " but NTP failed");
                    }
                }
            } catch (InterruptedException exception) {
                LOGGER.log(Level.WARNING, "Post-provisioning network delay" + " interrupted" + " | error=" + exception);
                if (testError == null) {
                    testError = "Reconnect delay interrupted" + " | error=" + exception;
                }
            } catch (Exception exception) {
                LOGGER.log(Level.WARNING, "Configured WiFi reconnect failed" + " after provisioning test" + " | error=" + exception);
                if (testError == null) {
                    testError = "Configured WiFi reconnect failed" + " | error=" + exception;
                }
            }
        }

        final boolean finalReconnectResult = reconnected;
        final String finalTestError = testError;
        MicroUI.callSerially(
                new Runnable() {
                    @Override
                    public void run() {
                        HeaderController.this.provisioningSmokeTestRunning = false;
                        HeaderController.this.mainPage.updateWifiConnectionStatus(finalReconnectResult);
                        if (finalTestError == null) {
                            LOGGER.log(
                                    Level.INFO,
                                    "Provisioning smoke test"
                                            + " completed successfully"
                                            + " | WiFi reconnected="
                                            + finalReconnectResult
                            );
                        } else {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Provisioning smoke test"
                                            + " completed with error"
                                            + " | WiFi reconnected="
                                            + finalReconnectResult
                                            + " | error="
                                            + finalTestError
                            );
                        }
                    }
                }
        );
    }

    private HttpServer createProvisioningHttpServer(final AccessPoint[] accessPoints) {
        final int networkCount = countProvisioningAccessPoints(accessPoints);
        LOGGER.log(Level.INFO, "Creating HOKA provisioning server configuration" + " | networkCount=" + networkCount);
        HttpServer server =
                HttpServer.builder()
                        .port(80)
                        .simultaneousConnections(1)
                        .workerCount(1)
                        .connectionTimeout(5000)
                        .build();

        server.get(
                "/",
                new RequestHandler() {
                    @Override
                    public void process(HttpRequest request, HttpResponse response) {
                        LOGGER.log(Level.INFO, "Provisioning HTTP request received" + " | method=GET" + " | path=/");
                        String page = buildProvisioningPage(accessPoints);
                        response.addHeader("content-type", "text/html; charset=UTF-8");
                        response.addHeader("cache-control", "no-store");
                        try {
                            response.setData(page, "UTF-8");
                        } catch (UnsupportedEncodingException exception) {
                            LOGGER.log(Level.WARNING, "UTF-8 encoding is unavailable" + " | using default encoding", exception);
                            response.setData(page);
                        }
                        LOGGER.log(Level.INFO, "Provisioning HTTP response prepared" + " | status=200" + " | networkCount=" + networkCount);
                    }
                }
        );

        LOGGER.log(Level.INFO, "HOKA provisioning server configured" + " | port=80" + " | started=false");
        return server;
    }

    private String buildProvisioningPage(AccessPoint[] accessPoints) {
        int networkCount = countProvisioningAccessPoints(accessPoints);
        StringBuilder html = new StringBuilder(4096);
        html.append("<!DOCTYPE html>");
        html.append("<html lang='id'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' " + "content='width=device-width, initial-scale=1.0'>");
        html.append("<title>Smart Greenhouse Wi-Fi</title>");
        html.append("<style>");
        html.append(
                "body{"
                        + "margin:0;"
                        + "padding:20px;"
                        + "font-family:Arial,sans-serif;"
                        + "background:#f3f6f4;"
                        + "color:#1f2933;"
                        + "}"
        );
        html.append(
                ".container{"
                        + "max-width:520px;"
                        + "margin:0 auto;"
                        + "background:#ffffff;"
                        + "padding:24px;"
                        + "border-radius:16px;"
                        + "box-shadow:0 4px 16px "
                        + "rgba(0,0,0,0.10);"
                        + "}"
        );
        html.append(
                "h1{"
                        + "margin:0 0 8px 0;"
                        + "font-size:24px;"
                        + "color:#166534;"
                        + "}"
        );
        html.append(
                ".description{"
                        + "margin:0 0 20px 0;"
                        + "color:#52606d;"
                        + "line-height:1.5;"
                        + "}"
        );
        html.append(
                ".network{"
                        + "display:block;"
                        + "margin-bottom:10px;"
                        + "padding:14px;"
                        + "border:1px solid #d9e2dc;"
                        + "border-radius:10px;"
                        + "background:#f9fbfa;"
                        + "}"
        );
        html.append(".network input{" + "margin-right:10px;" + "}");
        html.append(".ssid{" + "font-weight:bold;" + "word-break:break-word;" + "}");
        html.append(
                ".rssi{"
                        + "display:block;"
                        + "margin-left:26px;"
                        + "margin-top:5px;"
                        + "font-size:13px;"
                        + "color:#66788a;"
                        + "}"
        );
        html.append(
                ".empty{"
                        + "padding:16px;"
                        + "border-radius:10px;"
                        + "background:#fff4e5;"
                        + "color:#92400e;"
                        + "}"
        );
        html.append(
                ".note{"
                        + "margin-top:20px;"
                        + "padding:12px;"
                        + "border-radius:8px;"
                        + "background:#e8f5e9;"
                        + "font-size:13px;"
                        + "line-height:1.5;"
                        + "color:#245b2a;"
                        + "}"
        );
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<h1>Smart Greenhouse</h1>");
        html.append("<p class='description'>");
        html.append("Pilih jaringan Wi-Fi yang akan " + "digunakan oleh perangkat.");
        html.append("<br>");
        html.append("Jaringan ditemukan: ");
        html.append(networkCount);
        html.append("</p>");

        if (networkCount == 0) {
            html.append("<div class='empty'>");
            html.append("Tidak ada jaringan Wi-Fi " + "yang ditemukan.");
            html.append("</div>");
        } else {
            for (int index = 0; index < accessPoints.length; index++) {
                AccessPoint accessPoint = accessPoints[index];
                if (accessPoint == null) {
                    continue;
                }
                String ssid = escapeHtml(accessPoint.getSSID());
                html.append("<label class='network'>");
                html.append("<input type='radio' " + "name='ssid' " + "value='");
                html.append(ssid);
                html.append("'>");
                html.append("<span class='ssid'>");
                html.append(ssid);
                html.append("</span>");
                html.append("<span class='rssi'>");
                html.append("Kekuatan sinyal: ");
                html.append(accessPoint.getRSSI());
                html.append("</span>");
                html.append("</label>");
            }
        }

        html.append("<div class='note'>");
        html.append(
                "Pada tahap ini halaman baru menampilkan "
                        + "daftar jaringan hasil scan. "
                        + "Password dan tombol koneksi akan "
                        + "ditambahkan pada tahap berikutnya."
        );

        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private int countProvisioningAccessPoints(AccessPoint[] accessPoints) {
        if (accessPoints == null) {
            return 0;
        }

        int count = 0;
        for (int index = 0; index < accessPoints.length; index++) {
            if (accessPoints[index] != null) {
                count++;
            }
        }

        return count;
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length() + 16);
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '&':
                    escaped.append("&amp;");
                    break;
                case '<':
                    escaped.append("&lt;");
                    break;
                case '>':
                    escaped.append("&gt;");
                    break;
                case '"':
                    escaped.append("&quot;");
                    break;
                case '\'':
                    escaped.append("&#39;");
                    break;
                default:
                    escaped.append(character);
                    break;
            }
        }

        return escaped.toString();
    }

    private static void logProvisioningNetworks(AccessPoint[] accessPoints) {
        if (accessPoints == null || accessPoints.length == 0) {
            LOGGER.log(Level.WARNING, "No provisioning WiFi network found");
            return;
        }

        LOGGER.log(Level.INFO, "Provisioning networks selected" + " | count=" + accessPoints.length);
        for (int index = 0; index < accessPoints.length; index++) {
            AccessPoint accessPoint = accessPoints[index];
            LOGGER.log(
                    Level.INFO,
                    "Provisioning network"
                            + " | index="
                            + index
                            + " | SSID="
                            + accessPoint.getSSID()
                            + " | RSSI="
                            + accessPoint.getRSSI()
            );
        }
    }

    private void connectConfiguredWifi() {
        if (this.wifiConnectionRunning) {
            return;
        }

        this.wifiConnectionRunning = true;
        LOGGER.log(Level.INFO, "Automatic WiFi connection started" + " | SSID: " + this.wifiService.getConfiguredSsid());

        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                boolean connected;
                                String errorMessage;
                                try {
                                    WifiCapability capability = HeaderController.this.wifiService.getCapability();
                                    LOGGER.log(Level.INFO, "WiFi capability: " + capability);
                                    connected = HeaderController.this.wifiService.connectConfiguredNetwork();
                                    if (connected) {
                                        LOGGER.log(Level.INFO, "WiFi connected" + " | waiting for network before NTP");
                                        Thread.sleep(NETWORK_READY_DELAY_MS);
                                        boolean timeSynchronized = HeaderController.this.synchronizeHeaderTime();
                                        if (!timeSynchronized) {
                                            LOGGER.log(Level.WARNING, "WiFi connected but header time" + " was not synchronized.");
                                        }
                                    }
                                    errorMessage = connected ? null : "Configured network" + " was not joined.";
                                } catch (Exception exception) {
                                    connected = false;
                                    errorMessage = exception.toString();
                                }
                                final boolean connectionResult = connected;
                                final String connectionError = errorMessage;
                                MicroUI.callSerially(
                                        new Runnable() {
                                            @Override
                                            public void run() {
                                                HeaderController.this.wifiConnectionRunning = false;
                                                HeaderController.this.mainPage.updateWifiConnectionStatus(connectionResult);
                                                if (connectionResult) {
                                                    Runnable task = HeaderController.this.wifiConnectedTask;
                                                    if (task != null) {
                                                        try {
                                                            task.run();
                                                        } catch (RuntimeException exception) {
                                                            LOGGER.log(Level.WARNING, "WiFi connected task failed" + " | error=" + exception);
                                                        }
                                                    }
                                                    LOGGER.log(Level.INFO, "Automatic WiFi" + " connection successful" + " | SSID: " + HeaderController.this.wifiService.getConfiguredSsid());
                                                } else {
                                                    LOGGER.log(Level.WARNING,
                                                            "Automatic WiFi"
                                                                    + " connection failed"
                                                                    + " | SSID: "
                                                                    + HeaderController.this
                                                                    .wifiService
                                                                    .getConfiguredSsid()
                                                                    + " | reason: "
                                                                    + connectionError
                                                    );
                                                }
                                            }
                                        }
                                );
                            }
                        },
                        "wifi-auto-connect"
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
                        long currentUtcMillis = HeaderController.this.getCurrentHeaderUtcMillis();
                        final String currentTime = Time.formatJakartaTime(currentUtcMillis);
                        MicroUI.callSerially(
                                new Runnable() {
                                    @Override
                                    public void run() {
                                        HeaderController.this.mainPage.updateTime(currentTime);
                                        Runnable task = HeaderController.this.periodicTask;
                                        if (task == null) {
                                            return;
                                        }
                                        try {
                                            task.run();
                                        } catch (RuntimeException exception) {
                                            LOGGER.log(Level.WARNING, "Periodic application" + " task failed: " + exception);
                                        }
                                    }
                                }
                        );
                    }
                },
                0,
                1000
        );
    }

    private synchronized void setHeaderNtpTime(long ntpTimeMillis, long ntpReferenceMillis) {
        this.headerNtpTimeMillis = ntpTimeMillis;
        this.headerNtpReferenceMillis = ntpReferenceMillis;
        this.headerTimeSynchronized = true;
    }

    private synchronized long getCurrentHeaderUtcMillis() {
        if (!this.headerTimeSynchronized) {
            return 0L;
        }

        long elapsedMillis = Util.platformTimeMillis() - this.headerNtpReferenceMillis;
        if (elapsedMillis < 0L) {
            elapsedMillis =
                    0L;
        }

        return this.headerNtpTimeMillis + elapsedMillis;
    }

    private boolean synchronizeHeaderTime() {
        for (int index = 0; index < NTP_SERVERS.length; index++) {
            String server = NTP_SERVERS[index];
            LOGGER.log(Level.INFO, "Synchronizing header time" + " | server=" + server);
            try {
                SntpClient client = new SntpClient();
                boolean successful = client.requestTime(server, NTP_TIMEOUT_MS);
                if (!successful) {
                    LOGGER.log(Level.WARNING, "NTP request failed" + " | server=" + server);
                    continue;
                }
                setHeaderNtpTime(client.getNtpTime(), client.getNtpTimeReference());
                long currentUtcMillis = getCurrentHeaderUtcMillis();

                Util.setCurrentTimeMillis(currentUtcMillis);
                LOGGER.log(
                        Level.INFO,
                        "Header time synchronized"
                                + " | server="
                                + server
                                + " | utcMillis="
                                + currentUtcMillis
                                + " | jakartaTime="
                                + Time.formatJakartaTime(
                                currentUtcMillis
                        )
                                + " | roundTripMs="
                                + client.getRoundTripTime()
                );

                return true;
            } catch (RuntimeException exception) {
                LOGGER.log(
                        Level.WARNING,
                        "NTP synchronization error"
                                + " | server="
                                + server
                                + " | error="
                                + exception
                );
            }
        }

        LOGGER.log(Level.WARNING, "Header time synchronization failed.");
        return false;
    }
}