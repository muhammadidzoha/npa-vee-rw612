package com.nxp.example.smartgreenhouse.utils;

import java.util.Calendar;
import java.util.TimeZone;

public final class Time {

    private Time() {}

    public static String formatCurrentTime() {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("GMT+07:00"));
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minute = calendar.get(Calendar.MINUTE);

        return twoDigits(hour)
                + ":"
                + twoDigits(minute)
                + " WIB";
    }

    private static String twoDigits(int value) {
        if (value < 10) {
            return "0" + value;
        }

        return Integer.toString(value);
    }
}