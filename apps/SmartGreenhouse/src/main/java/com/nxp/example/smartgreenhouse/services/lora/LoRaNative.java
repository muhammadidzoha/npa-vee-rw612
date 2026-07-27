package com.nxp.example.smartgreenhouse.services.lora;

/**
 * Jembatan antara aplikasi Java dan data LoRa
 * yang disimpan di sisi native C.
 */
public final class LoRaNative {

    /*
     * Jumlah elemen dalam satu snapshot data LoRa.
     */
    public static final int SNAPSHOT_SIZE = 15;

    /*
     * Posisi masing-masing data di dalam int[] snapshot.
     */
    public static final int INDEX_SEQUENCE = 0;
    public static final int INDEX_NODE_ID = 1;
    public static final int INDEX_NODE_TARGET = 2;
    public static final int INDEX_SOIL_MOISTURE = 3;
    public static final int INDEX_SOIL_TEMPERATURE = 4;
    public static final int INDEX_CONDUCTIVITY = 5;
    public static final int INDEX_SOIL_PH = 6;
    public static final int INDEX_NITROGEN = 7;
    public static final int INDEX_PHOSPHORUS = 8;
    public static final int INDEX_POTASSIUM = 9;
    public static final int INDEX_AIR_TEMPERATURE = 10;
    public static final int INDEX_AIR_HUMIDITY = 11;
    public static final int INDEX_LIGHT_INTENSITY = 12;
    public static final int INDEX_RSSI = 13;
    public static final int INDEX_SNR_QUARTER_DB = 14;

    private LoRaNative() {
        /*
         * Utility class, tidak boleh dibuat object.
         */
    }

    /**
     * Membaca satu snapshot terbaru dari native.
     *
     * @param destination array tujuan dengan panjang minimal SNAPSHOT_SIZE.
     * @return true jika native sudah mempunyai payload LoRa yang valid.
     */
    public static boolean readLatest(int[] destination) {
        if (destination == null) {
            throw new NullPointerException(
                    "Destination tidak boleh null."
            );
        }

        if (destination.length < SNAPSHOT_SIZE) {
            throw new IllegalArgumentException(
                    "Destination harus memiliki minimal "
                            + SNAPSHOT_SIZE
                            + " elemen."
            );
        }

        return readLatestNative(destination);
    }

    /**
     * Implementasi method ini berada di npavee.c.
     */
    private static native boolean readLatestNative(
            int[] destination
    );
}