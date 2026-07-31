package com.nxp.example.smartgreenhouse.services.wifi;

import ej.ecom.wifi.AccessPoint;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ProvisioningHttpServer {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: PROVISIONING HTTP SERVER]");

    private static final int HTTP_PORT = 80;
    private static final int CLIENT_TIMEOUT_MS = 5000;
    private static final int MAX_REQUEST_BYTES = 4096;

    private volatile boolean running;

    private ServerSocket serverSocket;
    private Thread serverThread;
    private AccessPoint[] accessPoints;

    public ProvisioningHttpServer() {
        this.running = false;
        this.serverSocket = null;
        this.serverThread = null;
        this.accessPoints = new AccessPoint[0];
    }

    public synchronized void start(AccessPoint[] scannedAccessPoints)
            throws IOException {

        if (this.running) {
            LOGGER.log(Level.WARNING, "Provisioning HTTP server is already running");
            return;
        }

        this.accessPoints = copyAccessPoints(scannedAccessPoints);
        final ServerSocket newServerSocket = new ServerSocket(HTTP_PORT);

        this.serverSocket = newServerSocket;
        this.running = true;

        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                ProvisioningHttpServer.this.runServer(newServerSocket);
                            }
                        },
                        "wifi-provisioning-http"
                );

        this.serverThread = worker;
        try {
            worker.start();
        } catch (Error error) {
            this.running = false;
            this.serverSocket = null;
            this.serverThread = null;
            closeServerSocket(newServerSocket);
            throw error;
        }

        LOGGER.log(Level.INFO, "Provisioning HTTP server started" + " | port=" + HTTP_PORT + " | networkCount=" + this.accessPoints.length);
    }

    public synchronized void stop() {
        if (!this.running && this.serverSocket == null) {
            return;
        }

        LOGGER.log(Level.INFO, "Stopping provisioning HTTP server");

        this.running = false;

        ServerSocket socketToClose = this.serverSocket;
        this.serverSocket = null;
        this.serverThread = null;

        closeServerSocket(socketToClose);

        LOGGER.log(Level.INFO, "Provisioning HTTP server stopped");
    }

    public synchronized boolean isRunning() {
        return this.running;
    }

    private void runServer(ServerSocket activeServerSocket) {
        while (this.running) {
            Socket clientSocket = null;
            try {
                clientSocket = activeServerSocket.accept();
                LOGGER.log(Level.INFO, "Provisioning HTTP client connected" + " | remote=" + clientSocket.getRemoteSocketAddress());
                handleClient(clientSocket);
            } catch (IOException exception) {
                if (this.running) {
                    LOGGER.log(Level.WARNING, "Provisioning HTTP request failed" + " | error=" + exception);
                }
            } catch (RuntimeException exception) {
                LOGGER.log(Level.WARNING, "Unexpected provisioning HTTP error" + " | error=" + exception);
            } finally {
                closeSocket(clientSocket);
            }
        }
    }

    private void handleClient(Socket clientSocket)
            throws IOException {

        clientSocket.setSoTimeout(CLIENT_TIMEOUT_MS);

        InputStream inputStream = clientSocket.getInputStream();
        OutputStream outputStream = clientSocket.getOutputStream();
        consumeHttpRequest(inputStream);

        String html = createProvisioningPage();
        byte[] body = html.getBytes("UTF-8");

        String responseHeaders =
                "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/html; charset=UTF-8\r\n"
                        + "Content-Length: "
                        + body.length
                        + "\r\n"
                        + "Cache-Control: no-store\r\n"
                        + "Connection: close\r\n"
                        + "\r\n";

        outputStream.write(responseHeaders.getBytes("UTF-8"));

        outputStream.write(body);
        outputStream.flush();

        LOGGER.log(Level.INFO, "Provisioning page sent" + " | bytes=" + body.length);
    }

    private String createProvisioningPage() {
        StringBuilder html = new StringBuilder();

        html.append("<!doctype html>");
        html.append("<html lang=\"id\">");
        html.append("<head>");
        html.append("<meta charset=\"utf-8\">");
        html.append("<meta name=\"viewport\"" + " content=\"width=device-width," + " initial-scale=1\">");
        html.append("<title>Smart Greenhouse Wi-Fi</title>");

        html.append("<style>");
        html.append("body{font-family:Arial,sans-serif;" + "background:#f4f7f5;" + "margin:0;padding:24px;color:#17352a;}");
        html.append(
                ".card{max-width:520px;margin:0 auto;"
                        + "background:#fff;border-radius:14px;"
                        + "padding:22px;"
                        + "box-shadow:0 4px 18px rgba(0,0,0,.12);}"
        );
        html.append("h1{font-size:24px;margin-top:0;}");
        html.append(".network{padding:12px 0;" + "border-bottom:1px solid #dce7e1;}");
        html.append(".ssid{font-weight:bold;word-break:break-word;}");
        html.append(".rssi{font-size:13px;color:#60756c;" + "margin-top:4px;}");
        html.append(".note{font-size:14px;color:#60756c;" + "line-height:1.5;}");
        html.append("</style>");

        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"card\">");

        html.append("<h1>Smart Greenhouse</h1>");
        html.append(
                "<p class=\"note\">"
                        + "Pilih jaringan Wi-Fi akan tersedia"
                        + " pada tahap berikutnya."
                        + " Halaman ini membuktikan server HTTP"
                        + " board dapat diakses melalui SoftAP."
                        + "</p>"
        );

        html.append("<h2>Jaringan ditemukan</h2>");

        if (this.accessPoints.length == 0) {
            html.append("<p>Tidak ada jaringan Wi-Fi yang ditemukan.</p>");
        } else {
            for (int index = 0; index < this.accessPoints.length; index++) {
                AccessPoint accessPoint = this.accessPoints[index];
                if (accessPoint == null) {
                    continue;
                }

                html.append("<div class=\"network\">");

                html.append("<div class=\"ssid\">");
                html.append(escapeHtml(accessPoint.getSSID()));
                html.append("</div>");

                html.append("<div class=\"rssi\">");
                html.append("Kekuatan sinyal: ");
                html.append(accessPoint.getRSSI());
                html.append(" dBm");
                html.append("</div>");

                html.append("</div>");
            }
        }

        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private static void consumeHttpRequest(InputStream inputStream) throws IOException {
        int state = 0;
        int receivedBytes = 0;

        while (receivedBytes < MAX_REQUEST_BYTES) {
            int currentByte = inputStream.read();

            if (currentByte < 0) {
                return;
            }

            receivedBytes++;

            if (state == 0 && currentByte == '\r') {
                state = 1;
            } else if (state == 1 && currentByte == '\n') {
                state = 2;
            } else if (state == 2 && currentByte == '\r') {
                state = 3;
            } else if (state == 3 && currentByte == '\n') {
                return;
            } else {
                state = currentByte == '\r' ? 1 : 0;
            }
        }
    }

    private static AccessPoint[] copyAccessPoints(AccessPoint[] source) {
        if (source == null || source.length == 0) {
            return new AccessPoint[0];
        }

        AccessPoint[] copy = new AccessPoint[source.length];

        for (int index = 0; index < source.length; index++) {
            copy[index] = source[index];
        }

        return copy;
    }

    private static String escapeHtml(String value) {
        if (value == null) {
            return "";
        }

        StringBuilder escaped = new StringBuilder(value.length());
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

    private static void closeSocket(Socket socket) {
        if (socket == null) {
            return;
        }

        try {
            socket.close();
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Unable to close provisioning client socket" + " | error=" + exception);
        }
    }

    private static void closeServerSocket(ServerSocket socket) {
        if (socket == null) {
            return;
        }

        try {
            socket.close();
        } catch (IOException exception) {
            LOGGER.log(Level.FINE, "Unable to close provisioning server socket" + " | error=" + exception);
        }
    }
}