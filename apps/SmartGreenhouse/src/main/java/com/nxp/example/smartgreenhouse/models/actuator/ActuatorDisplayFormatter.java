package com.nxp.example.smartgreenhouse.models.actuator;

import java.util.Calendar;
import java.util.TimeZone;

public final class ActuatorDisplayFormatter {

    private static final long MILLIS_PER_MINUTE = 60_000L;
    private static final int MINUTES_PER_HOUR = 60;
    private static final String EMPTY_VALUE = "-";

    private static final TimeZone JAKARTA_TIME_ZONE = TimeZone.getTimeZone("GMT+07:00");

    private static final String[] MONTH_NAMES = {
            "Januari",
            "Februari",
            "Maret",
            "April",
            "Mei",
            "Juni",
            "Juli",
            "Agustus",
            "September",
            "Oktober",
            "November",
            "Desember"
    };

    private static final Calendar CALENDAR = Calendar.getInstance(JAKARTA_TIME_ZONE);

    private ActuatorDisplayFormatter() {}

    public static synchronized String formatTime(long timestamp) {
        if (timestamp <= 0) {
            return EMPTY_VALUE;
        }

        CALENDAR.setTimeInMillis(timestamp);

        int hour = CALENDAR.get(Calendar.HOUR_OF_DAY);
        int minute = CALENDAR.get(Calendar.MINUTE);
        return formatTwoDigits(hour) + ":" + formatTwoDigits(minute);
    }

    public static synchronized String formatDate(long timestamp) {
        if (timestamp <= 0) {
            return EMPTY_VALUE;
        }

        CALENDAR.setTimeInMillis(timestamp);

        int day = CALENDAR.get(Calendar.DAY_OF_MONTH);
        int monthIndex = CALENDAR.get(Calendar.MONTH);
        int year = CALENDAR.get(Calendar.YEAR);
        if (monthIndex < 0 || monthIndex >= MONTH_NAMES.length) {
            return EMPTY_VALUE;
        }

        return day + " " + MONTH_NAMES[monthIndex] + " " + year;
    }

    public static String formatDuration(long durationMillis) {
        if (durationMillis < 0) {
            durationMillis = 0;
        }

        long totalMinutes = durationMillis / MILLIS_PER_MINUTE;
        if (totalMinutes < MINUTES_PER_HOUR) {
            return totalMinutes + " menit";
        }

        long hours = totalMinutes / MINUTES_PER_HOUR;
        long remainingMinutes = totalMinutes % MINUTES_PER_HOUR;

        if (remainingMinutes == 0) {
            return hours + " jam";
        }

        return hours + " jam " + remainingMinutes + " menit";
    }

    private static String formatTwoDigits(int value) {
        if (value < 10) {
            return "0" + value;
        }

        return String.valueOf(value);
    }
}