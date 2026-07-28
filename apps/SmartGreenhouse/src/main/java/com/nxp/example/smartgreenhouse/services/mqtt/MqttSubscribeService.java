package com.nxp.example.smartgreenhouse.services.mqtt;

import com.nxp.example.smartgreenhouse.controllers.ActuatorDetailController;
import com.nxp.example.smartgreenhouse.models.actuator.ActuatorDataStore;
import ej.microui.MicroUI;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class MqttSubscribeService {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: MQTT SUBSCRIBE SERVICE]");

    private static final String BROKER_URI = "tcp://168.110.214.70:1883";

    private static final String CLIENT_ID = "clientku";

    private static final String TOPIC_FILTER = "gh01/node/255/status/+";

    private static final int SUBSCRIBE_QOS = 0;

    private static final boolean AUTHENTICATION_ENABLED = false;

    private static final String USERNAME = "";

    private static final String PASSWORD = "";

    private MqttClient mqttClient;

    private boolean workerStarted;

    private volatile boolean serviceRunning;

    private static final long CONNECTION_CHECK_INTERVAL_MS = 1000L;

    private static final long RECONNECT_INITIAL_DELAY_MS = 2000L;

    private static final long RECONNECT_MAX_DELAY_MS = 30000L;

    private static final String VALVE_TOPIC_PREFIX = "gh01/node/255/status/valve";

    private final ActuatorDataStore actuatorDataStore;

    private final ActuatorDetailController actuatorDetailController;

    public MqttSubscribeService(ActuatorDataStore actuatorDataStore, ActuatorDetailController actuatorDetailController) {
        if (actuatorDataStore == null) {
            throw new NullPointerException("actuatorDataStore tidak boleh null.");
        }

        if (actuatorDetailController == null) {
            throw new NullPointerException("actuatorDetailController tidak boleh null.");
        }
        this.actuatorDataStore = actuatorDataStore;
        this.actuatorDetailController = actuatorDetailController;

        this.mqttClient = null;
        this.workerStarted = false;
        this.serviceRunning = false;
    }

    public synchronized void start() {
        if (this.workerStarted) {
            LOGGER.log(Level.INFO, "MQTT connection worker is already running.");
            return;
        }

        this.workerStarted = true;
        this.serviceRunning = true;

        Thread mqttWorker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                MqttSubscribeService.this.runConnectionLoop();
                            }
                        },
                        "mqtt-worker"
                );

        try {
            mqttWorker.start();
        } catch (Error error) {
            this.workerStarted = false;
            this.serviceRunning = false;
            LOGGER.log(Level.SEVERE, "Unable to start MQTT worker" + " | error=" + error);
        }
    }

    private void connectAndSubscribeOnce() throws MqttException {
        LOGGER.log(Level.INFO, "Connecting to MQTT broker" + " | URI=" + BROKER_URI + " | clientId=" + CLIENT_ID);
        MqttConnectOptions connectOptions = new MqttConnectOptions();
        connectOptions.setConnectionTimeout(10);
        connectOptions.setKeepAliveInterval(60);

        if (AUTHENTICATION_ENABLED) {
            connectOptions.setUserName(USERNAME);
            connectOptions.setPassword(PASSWORD.toCharArray());
        }

        this.mqttClient.connect(connectOptions);
        LOGGER.log(Level.INFO, "MQTT broker connected" + " | URI=" + BROKER_URI + " | cleanSession=" + connectOptions.isCleanSession());
        LOGGER.log(Level.INFO, "Subscribing to MQTT topic" + " | topic=" + TOPIC_FILTER + " | qos=" + SUBSCRIBE_QOS);

        this.mqttClient.subscribe(TOPIC_FILTER, SUBSCRIBE_QOS);
        LOGGER.log(Level.INFO, "MQTT subscription successful" + " | topic=" + TOPIC_FILTER);
    }

    private void runConnectionLoop() {
        long reconnectDelay = RECONNECT_INITIAL_DELAY_MS;
        while (this.serviceRunning) {
            try {
                ensureClientCreated();
                if (!this.mqttClient.isConnected()) {
                    connectAndSubscribeOnce();
                    reconnectDelay = RECONNECT_INITIAL_DELAY_MS;
                }

                while (this.serviceRunning && this.mqttClient != null && this.mqttClient.isConnected()) {
                    Thread.sleep(CONNECTION_CHECK_INTERVAL_MS);
                }
            } catch (InterruptedException exception) {
                LOGGER.log(Level.WARNING, "MQTT worker was interrupted" + " | error=" + exception);
                break;
            } catch (MqttException exception) {
                LOGGER.log(Level.WARNING,
                        "MQTT connection attempt failed"
                                + " | reasonCode="
                                + exception.getReasonCode()
                                + " | message="
                                + exception.getMessage()
                                + " | cause="
                                + exception.getCause()
                );
            } catch (RuntimeException exception) {
                LOGGER.log(
                        Level.WARNING,
                        "Unexpected MQTT worker error"
                                + " | error="
                                + exception
                );
            }

            if (!this.serviceRunning) {
                break;
            }

            LOGGER.log(Level.INFO, "MQTT reconnect scheduled" + " | delayMs=" + reconnectDelay);
            try {
                Thread.sleep(reconnectDelay);
            } catch (InterruptedException exception) {
                LOGGER.log(Level.WARNING, "MQTT reconnect delay interrupted" + " | error=" + exception);
                break;
            }

            reconnectDelay *= 2L;
            if (reconnectDelay > RECONNECT_MAX_DELAY_MS) {
                reconnectDelay = RECONNECT_MAX_DELAY_MS;
            }
        }

        synchronized (this) {
            this.workerStarted = false;
            this.serviceRunning = false;
        }

        LOGGER.log(Level.INFO, "MQTT connection worker stopped.");
    }

    private synchronized void ensureClientCreated() throws MqttException {
        if (this.mqttClient != null) {
            return;
        }

        this.mqttClient = new MqttClient(BROKER_URI, CLIENT_ID);
        this.mqttClient.setCallback(
                new MqttCallback() {
                    @Override
                    public void connectionLost(Throwable cause) {
                        if (cause instanceof MqttException) {
                            MqttException mqttException = (MqttException) cause;
                            LOGGER.log(
                                    Level.WARNING,
                                    "MQTT connection lost"
                                            + " | reasonCode="
                                            + mqttException
                                            .getReasonCode()
                                            + " | message="
                                            + mqttException
                                            .getMessage()
                                            + " | cause="
                                            + mqttException
                                            .getCause()
                            );
                        } else {
                            LOGGER.log(Level.WARNING, "MQTT connection lost" + " | cause=" + cause);
                        }
                    }

                    @Override
                    public void messageArrived(String topic, MqttMessage message) {
                        byte[] payload = message.getPayload();
                        String payloadText = payload == null ? "" : new String(payload);
                        LOGGER.log(
                                Level.INFO,
                                "\r\n"
                                        + "[MQTT] Message received"
                                        + "\r\n"
                                        + "[MQTT] Topic   : "
                                        + topic
                                        + "\r\n"
                                        + "[MQTT] QoS     : "
                                        + message.getQos()
                                        + "\r\n"
                                        + "[MQTT] Payload : "
                                        + payloadText
                        );
                        handleValveStatus(topic, payloadText);
                    }
                }
        );
    }

    private void handleValveStatus(String topic, String payloadText) {
        final int valveId = parseValveId(topic);
        if (valveId <= 0) {
            LOGGER.log(Level.WARNING, "Unsupported actuator topic" + " | topic=" + topic);
            return;
        }
        if (payloadText == null) {
            LOGGER.log(Level.WARNING, "Empty MQTT actuator payload" + " | topic=" + topic);
            return;
        }

        String normalizedPayload = payloadText.trim();
        final boolean open;

        if ("ON".equalsIgnoreCase(normalizedPayload)) {
            open = true;
        } else if ("OFF".equalsIgnoreCase(normalizedPayload)) {
            open = false;
        } else {
            LOGGER.log(
                    Level.WARNING,
                    "Unsupported actuator payload"
                            + " | topic="
                            + topic
                            + " | payload="
                            + payloadText
            );
            return;
        }
        final long receivedTimestamp = System.currentTimeMillis();
        MicroUI.callSerially(
                new Runnable() {
                    @Override
                    public void run() {
                        boolean updated = MqttSubscribeService.this.actuatorDataStore.updateValveState(valveId, open, receivedTimestamp);
                        if (!updated) {
                            LOGGER.log(Level.WARNING, "Valve was not found" + " | valveId=" + valveId);
                            return;
                        }
                        MqttSubscribeService.this.actuatorDetailController.refreshIfOpen();
                        LOGGER.log(Level.INFO, "[MQTT] Valve status applied" + " | valveId=" + valveId + " | trayId=" + valveId + " | state=" + (open ? "ON" : "OFF"));
                    }
                }
        );
    }

    private static int parseValveId(String topic) {
        if (topic == null || !topic.startsWith(VALVE_TOPIC_PREFIX)) {
            return -1;
        }

        String valveIdText = topic.substring(VALVE_TOPIC_PREFIX.length());

        if (valveIdText.length() == 0) {
            return -1;
        }

        for (int index = 0; index < valveIdText.length(); index++) {
            char character = valveIdText.charAt(index);
            if (character < '0' || character > '9') {
                return -1;
            }
        }
        try {
            return Integer.parseInt(valveIdText);
        } catch (NumberFormatException exception) {
            return -1;
        }
    }

    public synchronized boolean isConnected() {
        return this.mqttClient != null && this.mqttClient.isConnected();
    }
}