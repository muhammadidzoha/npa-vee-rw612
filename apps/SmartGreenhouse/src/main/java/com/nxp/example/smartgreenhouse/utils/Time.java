package com.nxp.example.smartgreenhouse.utils;

import java.time.ZonedDateTime;

public class Time {
    public static String formatTime(ZonedDateTime zdt) {
        String h = zdt.getHour() < 10 ? "0" + zdt.getHour() : zdt.getHour() + "";
        String m = zdt.getMinute() < 10 ? "0" + zdt.getMinute() : zdt.getMinute() + "";
        return h + ":" + m;
    }
}
