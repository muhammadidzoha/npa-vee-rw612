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

    private boolean startRequested;

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
        this.startRequested = false;
    }

    public synchronized void start() {
        if (this.startRequested) {
            LOGGER.log(Level.INFO, "MQTT start was already requested.");
            return;
        }

        if (this.mqttClient != null && this.mqttClient.isConnected()) {
            LOGGER.log(Level.INFO, "MQTT client is already connected.");
            return;
        }

        this.startRequested = true;
        Thread mqttWorker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                MqttSubscribeService.this.connectAndSubscribe();
                            }
                        },
                        "mqtt-connect"
                );

        try {
            mqttWorker.start();
        } catch (Error error) {
            this.startRequested = false;
            LOGGER.log(Level.SEVERE, "Unable to start MQTT thread" + " | error=" + error);
        }
    }

    private void connectAndSubscribe() {
        try {
            Thread.sleep(2000L);
            LOGGER.log(Level.INFO,
                    "Connecting to MQTT broker"
                            + " | URI="
                            + BROKER_URI
                            + " | clientId="
                            + CLIENT_ID
            );

            MqttClient newClient = new MqttClient(BROKER_URI, CLIENT_ID);
            newClient.setCallback(
                    new MqttCallback() {
                        @Override
                        public void connectionLost(Throwable cause) {
                            synchronized (MqttSubscribeService.this) {
                                MqttSubscribeService.this.startRequested = false;
                            }
                            LOGGER.log(Level.WARNING, "MQTT connection lost" + " | cause=" + cause);
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

            MqttConnectOptions connectOptions = new MqttConnectOptions();

            connectOptions.setConnectionTimeout(10);
            connectOptions.setKeepAliveInterval(30);

            if (AUTHENTICATION_ENABLED) {
                connectOptions.setUserName(USERNAME);
                connectOptions.setPassword(PASSWORD.toCharArray());
            }

            synchronized (this) {
                this.mqttClient = newClient;
            }

            newClient.connect(connectOptions);
            LOGGER.log(Level.INFO, "MQTT broker connected" + " | URI=" + BROKER_URI);
            LOGGER.log(Level.INFO, "Subscribing to MQTT topic" + " | topic=" + TOPIC_FILTER + " | qos=" + SUBSCRIBE_QOS);

            newClient.subscribe(TOPIC_FILTER, SUBSCRIBE_QOS);
            LOGGER.log(Level.INFO, "MQTT subscription successful" + " | topic=" + TOPIC_FILTER);
        } catch (InterruptedException exception) {
            synchronized (this) {
                this.startRequested =
                        false;
            }

            LOGGER.log(Level.WARNING, "MQTT worker was interrupted" + " | error=" + exception);
        } catch (MqttException exception) {
            synchronized (this) {
                this.startRequested = false;
            }

            LOGGER.log(
                    Level.SEVERE,
                    "MQTT operation failed"
                            + " | reasonCode="
                            + exception.getReasonCode()
                            + " | message="
                            + exception.getMessage()
                            + " | cause="
                            + exception.getCause()
            );
        } catch (RuntimeException exception) {
            synchronized (this) {
                this.startRequested = false;
            }
            LOGGER.log(Level.SEVERE, "Unexpected MQTT error" + " | error=" + exception);
        }
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