package com.nxp.example.smartgreenhouse.services.wifi;

import com.nxp.example.smartgreenhouse.services.time.TimeService;

import ej.ecom.wifi.AccessPoint;
import ej.ecom.wifi.WifiCapability;
import ej.microui.MicroUI;

import ej.hoka.http.HttpRequest;
import ej.hoka.http.HttpResponse;
import ej.hoka.http.HttpServer;
import ej.hoka.http.requesthandler.RequestHandler;
import ej.hoka.http.body.ParameterParser;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;


public final class WifiProvisioningService {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: WIFI PROVISIONING SERVICE]");

    public interface Listener {
        void onWifiConnectionStatusChanged(boolean connected);
    }

    private final WifiHardwareService wifiService;
    private final TimeService timeService;
    private final Listener listener;

    private HttpServer provisioningHttpServer;

    private Runnable wifiConnectedTask;

    private boolean wifiConnectionRunning;
    private volatile boolean provisioningSmokeTestRunning;

    private volatile boolean provisioningCredentialsSubmitted;
    private volatile String submittedProvisioningSsid;
    private volatile String submittedProvisioningPassword;

    private static final long NETWORK_READY_DELAY_MS = 3000L;
    private static final long PROVISIONING_CREDENTIAL_POLL_INTERVAL_MS = 250L;
    private static final long PROVISIONING_HTTP_RESPONSE_GRACE_MS = 1500L;
    private static final long PROVISIONING_SOFT_AP_TEST_DURATION_MS = 300000L;
    private static final long PROVISIONING_CLIENT_RESTART_DELAY_MS = 1500L;

    public WifiProvisioningService(TimeService timeService, Listener listener) {
        if (timeService == null) {
            throw new NullPointerException("timeService tidak boleh null.");
        }

        this.wifiService = new WifiHardwareService();
        this.timeService = timeService;
        this.listener = listener;

        this.provisioningHttpServer = null;
        this.wifiConnectedTask = null;

        this.wifiConnectionRunning = false;
        this.provisioningSmokeTestRunning = false;

        this.provisioningCredentialsSubmitted = false;
        this.submittedProvisioningSsid = null;
        this.submittedProvisioningPassword = null;
    }

    public void setWifiConnectedTask(Runnable wifiConnectedTask) {
        this.wifiConnectedTask = wifiConnectedTask;
    }

    private void notifyConnectionStatus(boolean connected) {
        if (this.listener != null) {
            this.listener.onWifiConnectionStatusChanged(connected);
        }
    }

    public void startProvisioningSmokeTest() {
        if (this.provisioningSmokeTestRunning) {
            LOGGER.log(Level.WARNING, "Provisioning smoke test ignored" + " | test is already running");
            return;
        }

        if (this.wifiConnectionRunning) {
            LOGGER.log(Level.WARNING, "Provisioning smoke test ignored" + " | automatic WiFi connection" + " is still running");
            return;
        }

        this.provisioningSmokeTestRunning = true;
        notifyConnectionStatus(false);

        LOGGER.log(Level.INFO, "Provisioning smoke test requested");
        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                WifiProvisioningService.this.runProvisioningSmokeTest();
                            }
                        },
                        "wifi-provisioning-smoke-test"
                );

        try {
            worker.start();
        } catch (Error error) {
            this.provisioningSmokeTestRunning = false;
            LOGGER.log(Level.SEVERE, "Unable to start provisioning" + " smoke test worker" + " | error=" + error);
        }
    }

    private void runProvisioningSmokeTest() {
        boolean connected = false;
        boolean credentialsSubmitted = false;
        boolean selectedNetworkConnected = false;

        String targetSsid = null;
        String targetPassword = null;
        String testError = null;

        clearSubmittedProvisioningCredentials();

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
                    "Provisioning portal active"
                            + " | SSID="
                            + this.wifiService
                            .getProvisioningSsid()
                            + " | password="
                            + this.wifiService
                            .getProvisioningPassword()
                            + " | timeoutMs="
                            + PROVISIONING_SOFT_AP_TEST_DURATION_MS
            );

            credentialsSubmitted = waitForProvisioningCredentials(PROVISIONING_SOFT_AP_TEST_DURATION_MS);
            if (credentialsSubmitted) {
                targetSsid = this.submittedProvisioningSsid;
                targetPassword = this.submittedProvisioningPassword;
                LOGGER.log(
                        Level.INFO,
                        "Provisioning submission detected"
                                + " | SSID="
                                + targetSsid
                                + " | waitingForHttpResponseMs="
                                + PROVISIONING_HTTP_RESPONSE_GRACE_MS
                );

                Thread.sleep(PROVISIONING_HTTP_RESPONSE_GRACE_MS);
            } else {
                LOGGER.log(Level.WARNING, "Provisioning portal timed out" + " | timeoutMs=" + PROVISIONING_SOFT_AP_TEST_DURATION_MS);
            }
        } catch (InterruptedException exception) {
            testError = "Provisioning worker interrupted" + " | error=" + exception;
        } catch (Exception exception) {
            testError = "Provisioning process failed" + " | error=" + exception;
        } finally {
            if (this.provisioningHttpServer != null) {
                try {
                    LOGGER.log(Level.INFO, "Stopping HOKA provisioning server");
                    this.provisioningHttpServer.stop();
                    LOGGER.log(Level.INFO, "HOKA provisioning server stopped");
                } catch (Exception exception) {
                    LOGGER.log(Level.WARNING, "Unable to stop HOKA provisioning server" + " | error=" + exception);
                    if (testError == null) {
                        testError = "Unable to stop HOKA server" + " | error=" + exception;
                    }
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
                LOGGER.log(Level.WARNING, "Provisioning client restart delay interrupted" + " | error=" + exception);
            }

            if (credentialsSubmitted && targetSsid != null) {
                try {
                    LOGGER.log(Level.INFO, "Connecting to submitted WiFi" + " | SSID=" + targetSsid);
                    selectedNetworkConnected = this.wifiService.connectToNetwork(targetSsid, targetPassword);
                    connected = selectedNetworkConnected;
                    LOGGER.log(
                            selectedNetworkConnected
                                    ? Level.INFO
                                    : Level.WARNING,
                            "Submitted WiFi connection completed"
                                    + " | SSID="
                                    + targetSsid
                                    + " | connected="
                                    + selectedNetworkConnected
                    );
                } catch (Exception exception) {
                    LOGGER.log(
                            Level.WARNING,
                            "Submitted WiFi connection failed"
                                    + " | SSID="
                                    + targetSsid
                                    + " | error="
                                    + exception
                    );

                    if (testError == null) {
                        testError =
                                "Submitted WiFi connection failed"
                                        + " | SSID="
                                        + targetSsid
                                        + " | error="
                                        + exception;
                    }
                }
            }

            if (!connected) {
                try {
                    LOGGER.log(Level.INFO, "Reconnecting configured WiFi" + " | SSID=" + this.wifiService.getConfiguredSsid());
                    connected = this.wifiService.connectConfiguredNetwork();
                    LOGGER.log(
                            connected
                                    ? Level.INFO
                                    : Level.WARNING,
                            "Configured WiFi fallback completed"
                                    + " | SSID="
                                    + this.wifiService
                                    .getConfiguredSsid()
                                    + " | connected="
                                    + connected
                    );
                } catch (Exception exception) {
                    LOGGER.log(Level.WARNING, "Configured WiFi fallback failed" + " | error=" + exception);
                    if (testError == null) {
                        testError = "Configured WiFi fallback failed" + " | error=" + exception;
                    }
                }
            }

            if (connected) {
                try {
                    Thread.sleep(NETWORK_READY_DELAY_MS);
                    boolean timeSynchronized = this.timeService.synchronizeTime();
                    if (!timeSynchronized) {
                        LOGGER.log(Level.WARNING, "WiFi connected but NTP synchronization failed");
                    }
                } catch (InterruptedException exception) {
                    LOGGER.log(Level.WARNING, "Post-provisioning network delay interrupted" + " | error=" + exception);
                    if (testError == null) {
                        testError = "Post-provisioning delay interrupted" + " | error=" + exception;
                    }
                }
            }

            clearSubmittedProvisioningCredentials();
            targetPassword = null;
        }

        final boolean finalConnectionResult = connected;
        final boolean finalCredentialsSubmitted = credentialsSubmitted;
        final boolean finalSelectedNetworkConnected = selectedNetworkConnected;

        final String finalTargetSsid = targetSsid;
        final String finalTestError = testError;
        MicroUI.callSerially(
                new Runnable() {
                    @Override
                    public void run() {
                        WifiProvisioningService.this.provisioningSmokeTestRunning = false;
                        WifiProvisioningService.this.notifyConnectionStatus(finalConnectionResult);
                        if (finalSelectedNetworkConnected) {
                            LOGGER.log(Level.INFO, "WiFi provisioning completed successfully" + " | SSID=" + finalTargetSsid);
                        } else if (finalCredentialsSubmitted) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Selected WiFi was not connected"
                                            + " | requestedSSID="
                                            + finalTargetSsid
                                            + " | fallbackConnected="
                                            + finalConnectionResult
                                            + " | error="
                                            + finalTestError
                            );
                        } else {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Provisioning portal closed without submission"
                                            + " | configuredWiFiConnected="
                                            + finalConnectionResult
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
                        setProvisioningHtmlResponse(response, page);
                        LOGGER.log(Level.INFO, "Provisioning HTTP response prepared" + " | status=200" + " | networkCount=" + networkCount);
                    }
                }
        );

        server.post(
                "/connect",
                new RequestHandler() {
                    @Override
                    public void process(HttpRequest request, HttpResponse response) {
                        LOGGER.log(Level.INFO, "Provisioning HTTP request received" + " | method=POST" + " | path=/connect");
                        try {
                            Map<String, String> parameters = request.parseBody(new ParameterParser());
                            String ssid = parameters.get("ssid");
                            String password = parameters.get("password");
                            if (ssid == null || ssid.length() == 0) {
                                LOGGER.log(Level.WARNING, "Provisioning form rejected" + " | reason=SSID is empty");
                                setProvisioningHtmlResponse(response, buildProvisioningMessagePage(
                                        "Data belum lengkap",
                                                "Pilih salah satu jaringan Wi-Fi.",
                                                false
                                        )
                                );
                                return;
                            }

                            if (password == null) {
                                password = "";
                            }

                            if (!isProvisioningSsidAllowed(accessPoints, ssid)) {
                                LOGGER.log(Level.WARNING, "Provisioning form rejected" + " | reason=SSID is not in scan result" + " | SSID=" + ssid);
                                setProvisioningHtmlResponse(
                                        response,
                                        buildProvisioningMessagePage(
                                                "Jaringan tidak valid",
                                                "Jaringan yang dipilih tidak terdapat "
                                                        + "dalam hasil pemindaian perangkat.",
                                                false
                                        )
                                );

                                return;
                            }

                            if (password.length() < 8 || password.length() > 64) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Provisioning form rejected"
                                                + " | reason=Invalid password length"
                                                + " | SSID="
                                                + ssid
                                                + " | passwordLength="
                                                + password.length()
                                );

                                setProvisioningHtmlResponse(
                                        response,
                                        buildProvisioningMessagePage(
                                                "Password tidak valid",
                                                "Password Wi-Fi harus terdiri dari "
                                                        + "8 sampai 64 karakter.",
                                                false
                                        )
                                );

                                return;
                            }

                            if (WifiProvisioningService.this
                                    .provisioningCredentialsSubmitted) {

                                LOGGER.log(
                                        Level.INFO,
                                        "Provisioning connection already queued"
                                );

                                setProvisioningHtmlResponse(
                                        response,
                                        buildProvisioningMessagePage(
                                                "Koneksi sedang diproses",
                                                "Perangkat sedang mencoba terhubung "
                                                        + "ke jaringan yang telah dipilih.",
                                                true
                                        )
                                );

                                return;
                            }

                            LOGGER.log(
                                    Level.INFO,
                                    "Provisioning credentials received"
                                            + " | SSID="
                                            + ssid
                                            + " | passwordLength="
                                            + password.length()
                            );

                            setProvisioningHtmlResponse(
                                    response,
                                    buildProvisioningMessagePage(
                                            "Menghubungkan perangkat",
                                            "Data Wi-Fi berhasil diterima. "
                                                    + "Perangkat akan menutup hotspot "
                                                    + "dan mencoba terhubung ke "
                                                    + ssid
                                                    + ". Koneksi HP ke halaman ini "
                                                    + "akan terputus.",
                                            true
                                    )
                            );

                            WifiProvisioningService.this.submittedProvisioningSsid = ssid;
                            WifiProvisioningService.this.submittedProvisioningPassword = password;
                            WifiProvisioningService.this.provisioningCredentialsSubmitted = true;
                            LOGGER.log(
                                    Level.INFO,
                                    "Provisioning credentials queued"
                                            + " | SSID="
                                            + ssid
                                            + " | passwordLength="
                                            + password.length()
                            );
                        } catch (IOException exception) {
                            LOGGER.log(Level.SEVERE, "Failed to parse provisioning form", exception);
                            setProvisioningHtmlResponse(
                                    response,
                                    buildProvisioningMessagePage(
                                            "Terjadi kesalahan",
                                            "Perangkat tidak dapat membaca "
                                                    + "data formulir.",
                                            false
                                    )
                            );
                        }
                    }
                }
        );

        LOGGER.log(
                Level.INFO,
                "HOKA provisioning server configured"
                        + " | port=80"
                        + " | started=false"
                        + " | routes=GET /, POST /connect"
        );

        return server;
    }

    private String buildProvisioningPage(AccessPoint[] accessPoints) {
        int networkCount = countProvisioningAccessPoints(accessPoints);
        StringBuilder html = new StringBuilder(6144);
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
                        + "box-shadow:0 4px 16px rgba(0,0,0,0.10);"
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
                        + "cursor:pointer;"
                        + "}"
        );
        html.append(".network input{" + "margin-right:10px;" + "}");
        html.append(
                ".ssid{"
                        + "font-weight:bold;"
                        + "word-break:break-word;"
                        + "}"
        );
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
                ".field-label{"
                        + "display:block;"
                        + "margin-top:20px;"
                        + "margin-bottom:8px;"
                        + "font-weight:bold;"
                        + "}"
        );
        html.append(
                ".password{"
                        + "width:100%;"
                        + "box-sizing:border-box;"
                        + "padding:13px;"
                        + "border:1px solid #b8c4bc;"
                        + "border-radius:9px;"
                        + "font-size:16px;"
                        + "}"
        );
        html.append(
                ".button{"
                        + "width:100%;"
                        + "margin-top:20px;"
                        + "padding:14px;"
                        + "border:0;"
                        + "border-radius:10px;"
                        + "background:#166534;"
                        + "color:#ffffff;"
                        + "font-size:16px;"
                        + "font-weight:bold;"
                        + "cursor:pointer;"
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
        html.append("Pilih jaringan Wi-Fi dan masukkan password " + "yang akan digunakan oleh perangkat.");
        html.append("<br>");
        html.append("Jaringan ditemukan: ");
        html.append(networkCount);
        html.append("</p>");

        if (networkCount == 0) {
            html.append("<div class='empty'>");
            html.append("Tidak ada jaringan Wi-Fi yang ditemukan.");
            html.append("</div>");
        } else {
            html.append("<form method='post' action='/connect'>");
            for (int index = 0; index < accessPoints.length; index++) {
                AccessPoint accessPoint = accessPoints[index];
                if (accessPoint == null) {
                    continue;
                }

                String ssid = escapeHtml(accessPoint.getSSID());
                html.append("<label class='network'>");
                html.append("<input type='radio' " + "name='ssid' " + "value='");
                html.append(ssid);
                html.append("'");

                if (index == 0) {
                    html.append(" checked");
                }

                html.append(">");
                html.append("<span class='ssid'>");
                html.append(ssid);
                html.append("</span>");
                html.append("<span class='rssi'>");
                html.append("Kekuatan sinyal: ");
                html.append(accessPoint.getRSSI());
                html.append("</span>");
                html.append("</label>");
            }
            html.append(
                    "<label class='field-label' "
                            + "for='password'>"
                            + "Password Wi-Fi"
                            + "</label>"
            );
            html.append(
                    "<input class='password' "
                            + "id='password' "
                            + "name='password' "
                            + "type='password' "
                            + "minlength='8' "
                            + "maxlength='64' "
                            + "required "
                            + "placeholder='Masukkan password Wi-Fi'>"
            );
            html.append("<button class='button' type='submit'>" + "Hubungkan" + "</button>");
            html.append("</form>");
        }

        html.append("<div class='note'>");
        html.append("Tahap pengujian: perangkat hanya menerima " + "SSID dan password. Perangkat belum " + "berpindah ke jaringan yang dipilih.");
        html.append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private String buildProvisioningMessagePage(String title, String message, boolean success) {
        String background = success ? "#e8f5e9" : "#fff4e5";
        String textColor = success ? "#166534" : "#92400e";
        StringBuilder html = new StringBuilder(2048);
        html.append("<!DOCTYPE html>");
        html.append("<html lang='id'>");
        html.append("<head>");
        html.append("<meta charset='UTF-8'>");
        html.append("<meta name='viewport' " + "content='width=device-width, initial-scale=1.0'>");
        html.append("<title>");
        html.append(escapeHtml(title));
        html.append("</title>");
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
                        + "box-shadow:0 4px 16px rgba(0,0,0,0.10);"
                        + "}"
        );
        html.append(
                ".message{"
                        + "padding:18px;"
                        + "border-radius:10px;"
                        + "background:"
        );
        html.append(background);
        html.append(";color:");
        html.append(textColor);
        html.append(";line-height:1.5;" + "}");
        html.append(
                ".back{"
                        + "display:inline-block;"
                        + "margin-top:20px;"
                        + "padding:12px 18px;"
                        + "border-radius:9px;"
                        + "background:#166534;"
                        + "color:#ffffff;"
                        + "text-decoration:none;"
                        + "font-weight:bold;"
                        + "}"
        );
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class='container'>");
        html.append("<h1>");
        html.append(escapeHtml(title));
        html.append("</h1>");
        html.append("<div class='message'>");
        html.append(escapeHtml(message));
        html.append("</div>");
        html.append("<a class='back' href='/'>" + "Kembali" + "</a>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private void setProvisioningHtmlResponse(HttpResponse response, String page) {
        response.addHeader("content-type", "text/html; charset=UTF-8");
        response.addHeader("cache-control", "no-store");
        try {
            response.setData(page, "UTF-8");
        } catch (UnsupportedEncodingException exception) {
            LOGGER.log(Level.WARNING, "UTF-8 encoding is unavailable" + " | using default encoding", exception);
            response.setData(page);
        }
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

    private static boolean isProvisioningSsidAllowed(AccessPoint[] accessPoints, String ssid) {
        if (accessPoints == null || ssid == null || ssid.length() == 0) {
            return false;
        }

        for (int index = 0; index < accessPoints.length; index++) {
            AccessPoint accessPoint = accessPoints[index];
            if (accessPoint == null) {
                continue;
            }

            if (ssid.equals(accessPoint.getSSID())) {
                return true;
            }
        }

        return false;
    }

    private boolean waitForProvisioningCredentials(long timeoutMilliseconds) throws InterruptedException {
        long deadline = System.currentTimeMillis() + timeoutMilliseconds;
        while (!this.provisioningCredentialsSubmitted) {
            long remaining = deadline - System.currentTimeMillis();
            if (remaining <= 0L) {
                return false;
            }
            long sleepDuration = remaining < PROVISIONING_CREDENTIAL_POLL_INTERVAL_MS ? remaining : PROVISIONING_CREDENTIAL_POLL_INTERVAL_MS;
            Thread.sleep(sleepDuration);
        }

        return true;
    }

    private void clearSubmittedProvisioningCredentials() {
        this.provisioningCredentialsSubmitted = false;
        this.submittedProvisioningSsid = null;
        this.submittedProvisioningPassword = null;
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

    public void connectConfiguredWifi() {
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
                                    WifiCapability capability = WifiProvisioningService.this.wifiService.getCapability();
                                    LOGGER.log(Level.INFO, "WiFi capability: " + capability);
                                    connected = WifiProvisioningService.this.wifiService.connectConfiguredNetwork();
                                    if (connected) {
                                        LOGGER.log(Level.INFO, "WiFi connected" + " | waiting for network before NTP");
                                        Thread.sleep(NETWORK_READY_DELAY_MS);
                                        boolean timeSynchronized = WifiProvisioningService.this.timeService.synchronizeTime();
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
                                                WifiProvisioningService.this.wifiConnectionRunning = false;
                                                WifiProvisioningService.this.notifyConnectionStatus(connectionResult);
                                                if (connectionResult) {
                                                    Runnable task = WifiProvisioningService.this.wifiConnectedTask;
                                                    if (task != null) {
                                                        try {
                                                            task.run();
                                                        } catch (RuntimeException exception) {
                                                            LOGGER.log(Level.WARNING, "WiFi connected task failed" + " | error=" + exception);
                                                        }
                                                    }
                                                    LOGGER.log(
                                                            Level.INFO,
                                                            "Automatic WiFi"
                                                                    + " connection successful"
                                                                    + " | SSID: "
                                                                    + WifiProvisioningService.this
                                                                    .wifiService
                                                                    .getConfiguredSsid()
                                                    );
                                                } else {
                                                    LOGGER.log(
                                                            Level.WARNING,
                                                            "Automatic WiFi"
                                                                    + " connection failed"
                                                                    + " | SSID: "
                                                                    + WifiProvisioningService.this
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
}
