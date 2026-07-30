package com.nxp.example.smartgreenhouse.utils;

public final class Time {

    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long SECONDS_PER_MINUTE = 60L;
    private static final long MINUTES_PER_HOUR = 60L;
    private static final long HOURS_PER_DAY = 24L;

    private static final long WIB_OFFSET_MILLIS = 7L * MINUTES_PER_HOUR * SECONDS_PER_MINUTE * MILLIS_PER_SECOND;

    private Time() {}

    public static String formatCurrentTime() {
        long utcMillis = System.currentTimeMillis();
        long wibMillis = utcMillis + WIB_OFFSET_MILLIS;

        long totalMinutes = wibMillis / MILLIS_PER_SECOND / SECONDS_PER_MINUTE;
        int minute = (int) (totalMinutes % MINUTES_PER_HOUR);
        int hour = (int) ((totalMinutes / MINUTES_PER_HOUR) % HOURS_PER_DAY);
        return twoDigits(hour) + ":" + twoDigits(minute);
    }

    private static String twoDigits(int value) {
        if (value < 10) {
            return "0" + value;
        }

        return Integer.toString(value);
    }
}