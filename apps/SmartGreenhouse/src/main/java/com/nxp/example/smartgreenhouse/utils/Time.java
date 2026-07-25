package com.nxp.example.smartgreenhouse.utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;

public final class Time {
    private static final ZoneOffset WIB_OFFSET = ZoneOffset.ofHours(7);

    private static final String EMPTY_TIME = "--:--";

    private Time() {}

    public static OffsetDateTime nowWib() {
        return Instant.now().atOffset(WIB_OFFSET);
    }

    public static String formatCurrentTime() {
        return formatTime(nowWib());
    }

    public static String formatTime(OffsetDateTime dateTime) {
        if (dateTime == null) {
            return EMPTY_TIME;
        }

        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();

        return twoDigits(hour) + ":" + twoDigits(minute);
    }

    private static String twoDigits(int value) {
        if (value < 10) {
            return "0" + value;
        }

        return Integer.toString(value);
    }
}