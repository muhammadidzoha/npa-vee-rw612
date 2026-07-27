package com.nxp.example.smartgreenhouse.services.lora;

/**
 * Pengujian sementara untuk memastikan Java dapat
 * membaca snapshot yang disimpan di native.
 *
 * Class ini akan diganti oleh LoRaHardwareService
 * pada poin 6.
 */
public final class LoRaNativeSmokeTest {

    private static final long POLL_INTERVAL_MS = 250L;

    private static boolean started;

    private LoRaNativeSmokeTest() {
        /*
         * Utility class.
         */
    }

    public static synchronized void start() {
        if (started) {
            return;
        }

        started = true;

        Thread worker =
                new Thread(
                        new Runnable() {
                            @Override
                            public void run() {
                                runSmokeTest();
                            }
                        },
                        "lora-native-smoke-test"
                );

        worker.start();
    }

    private static void runSmokeTest() {
        int[] snapshot =
                new int[LoRaNative.SNAPSHOT_SIZE];

        int lastSequence =
                -1;

        while (true) {
            boolean dataAvailable =
                    LoRaNative.readLatest(snapshot);

            if (dataAvailable) {
                int sequence =
                        snapshot[
                                LoRaNative.INDEX_SEQUENCE
                                ];

                /*
                 * Jangan cetak snapshot yang sama berulang kali.
                 */
                if (sequence != lastSequence) {
                    lastSequence =
                            sequence;

                    printSnapshot(
                            snapshot
                    );
                }
            }

            try {
                Thread.sleep(
                        POLL_INTERVAL_MS
                );
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();

                return;
            }
        }
    }

    private static void printSnapshot(
            int[] snapshot
    ) {
        System.out.println(
                "[LORA JAVA]"
                        + " sequence="
                        + snapshot[
                        LoRaNative.INDEX_SEQUENCE
                        ]
                        + " | nodeId="
                        + snapshot[
                        LoRaNative.INDEX_NODE_ID
                        ]
                        + " | nodeTarget="
                        + snapshot[
                        LoRaNative.INDEX_NODE_TARGET
                        ]
        );

        System.out.println(
                "[LORA JAVA]"
                        + " soilMoistureRaw="
                        + snapshot[
                        LoRaNative.INDEX_SOIL_MOISTURE
                        ]
                        + " | soilTemperatureRaw="
                        + snapshot[
                        LoRaNative.INDEX_SOIL_TEMPERATURE
                        ]
                        + " | conductivity="
                        + snapshot[
                        LoRaNative.INDEX_CONDUCTIVITY
                        ]
                        + " | soilPhRaw="
                        + snapshot[
                        LoRaNative.INDEX_SOIL_PH
                        ]
        );

        System.out.println(
                "[LORA JAVA]"
                        + " nitrogen="
                        + snapshot[
                        LoRaNative.INDEX_NITROGEN
                        ]
                        + " | phosphorus="
                        + snapshot[
                        LoRaNative.INDEX_PHOSPHORUS
                        ]
                        + " | potassium="
                        + snapshot[
                        LoRaNative.INDEX_POTASSIUM
                        ]
        );

        System.out.println(
                "[LORA JAVA]"
                        + " airTemperatureRaw="
                        + snapshot[
                        LoRaNative.INDEX_AIR_TEMPERATURE
                        ]
                        + " | airHumidityRaw="
                        + snapshot[
                        LoRaNative.INDEX_AIR_HUMIDITY
                        ]
                        + " | lightIntensity="
                        + snapshot[
                        LoRaNative.INDEX_LIGHT_INTENSITY
                        ]
        );

        System.out.println(
                "[LORA JAVA]"
                        + " RSSI="
                        + snapshot[
                        LoRaNative.INDEX_RSSI
                        ]
                        + " dBm"
                        + " | SNRQuarterDb="
                        + snapshot[
                        LoRaNative.INDEX_SNR_QUARTER_DB
                        ]
        );
    }
}