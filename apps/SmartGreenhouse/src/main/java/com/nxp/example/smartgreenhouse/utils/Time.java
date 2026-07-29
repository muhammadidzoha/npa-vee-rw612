package com.nxp.example.smartgreenhouse.utils;

import ej.bon.Util;

import java.util.Calendar;
import java.util.TimeZone;

public final class Time {

    /*
     * Offset Asia/Jakarta:
     * UTC + 7 jam.
     */
    private static final long JAKARTA_OFFSET_MILLIS =
            7L * 60L * 60L * 1000L;

    /*
     * Calendar menggunakan UTC karena offset Jakarta
     * ditambahkan secara eksplisit.
     */
    private static final TimeZone UTC_TIME_ZONE =
            TimeZone.getTimeZone("GMT");

    private static final Calendar CALENDAR =
            Calendar.getInstance(UTC_TIME_ZONE);

    private Time() {
    }

    /**
     * Menampilkan waktu aktual Asia/Jakarta.
     *
     * Method ini dipanggil setiap satu detik oleh
     * HeaderController.
     */
    public static synchronized String formatCurrentTime() {
        long jakartaTimestamp =
                Util.currentTimeMillis()
                        + JAKARTA_OFFSET_MILLIS;

        CALENDAR.setTimeInMillis(
                jakartaTimestamp
        );

        int hour =
                CALENDAR.get(
                        Calendar.HOUR_OF_DAY
                );

        int minute =
                CALENDAR.get(
                        Calendar.MINUTE
                );

        return formatTwoDigits(hour)
                + ":"
                + formatTwoDigits(minute);
    }

    private static String formatTwoDigits(
            int value
    ) {
        if (value < 10) {
            return "0" + value;
        }

        return String.valueOf(value);
    }
}