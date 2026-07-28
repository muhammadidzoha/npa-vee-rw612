package com.nxp.example.smartgreenhouse.services.mqtt;

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

    public MqttSubscribeService() {
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

            LOGGER.log(
                    Level.INFO,
                    "MQTT broker connected"
                            + " | URI="
                            + BROKER_URI
            );

            LOGGER.log(
                    Level.INFO,
                    "Subscribing to MQTT topic"
                            + " | topic="
                            + TOPIC_FILTER
                            + " | qos="
                            + SUBSCRIBE_QOS
            );

            newClient.subscribe(TOPIC_FILTER, SUBSCRIBE_QOS);
            LOGGER.log(
                    Level.INFO,
                    "MQTT subscription successful"
                            + " | topic="
                            + TOPIC_FILTER
            );

        } catch (InterruptedException exception) {
            synchronized (this) {
                this.startRequested =
                        false;
            }

            LOGGER.log(
                    Level.WARNING,
                    "MQTT worker was interrupted"
                            + " | error="
                            + exception
            );

        } catch (MqttException exception) {
            synchronized (this) {
                this.startRequested =
                        false;
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
                this.startRequested =
                        false;
            }

            LOGGER.log(
                    Level.SEVERE,
                    "Unexpected MQTT error"
                            + " | error="
                            + exception
            );
        }
    }

    public synchronized boolean isConnected() {
        return this.mqttClient != null && this.mqttClient.isConnected();
    }
}