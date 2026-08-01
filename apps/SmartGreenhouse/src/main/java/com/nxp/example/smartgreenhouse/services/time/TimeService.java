package com.nxp.example.smartgreenhouse.services.time;

import com.nxp.example.smartgreenhouse.utils.Time;

import android.net.SntpClient;

import ej.bon.Timer;
import ej.bon.TimerTask;
import ej.bon.Util;

import java.util.logging.Level;
import java.util.logging.Logger;

public final class TimeService {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: TIME SERVICE]");

    private static final String[] NTP_SERVERS = {"time.google.com", "0.pool.ntp.org"};
    private static final int NTP_TIMEOUT_MS = 5000;

    private Timer clockTimer;

    private long ntpTimeMillis;
    private long ntpReferenceMillis;

    private boolean timeSynchronized;

    public TimeService() {
        this.clockTimer = null;

        this.ntpTimeMillis = 0L;
        this.ntpReferenceMillis = 0L;

        this.timeSynchronized = false;
    }

    public interface ClockListener {
        void onTimeChanged(String currentTime);
    }

    public void startClock(final ClockListener listener) {
        if (this.clockTimer != null) {
            return;
        }

        this.clockTimer = new Timer();
        this.clockTimer.schedule(
                new TimerTask() {
                    @Override
                    public void run() {
                        long currentUtcMillis = getCurrentUtcMillis();
                        String currentTime = Time.formatJakartaTime(currentUtcMillis);
                        if (listener != null) {
                            listener.onTimeChanged(currentTime);
                        }
                    }
                },
                0,
                1000
        );
    }

    public boolean synchronizeTime() {
        for (int index = 0; index < NTP_SERVERS.length; index++) {
            String server = NTP_SERVERS[index];
            LOGGER.log(Level.INFO, "Synchronizing time" + " | server=" + server);
            try {
                SntpClient client = new SntpClient();
                boolean successful = client.requestTime(server, NTP_TIMEOUT_MS);
                if (!successful) {
                    LOGGER.log(Level.WARNING, "NTP request failed" + " | server=" + server);
                    continue;
                }
                setNtpTime(client.getNtpTime(), client.getNtpTimeReference());
                long currentUtcMillis = getCurrentUtcMillis();
                Util.setCurrentTimeMillis(currentUtcMillis);
                LOGGER.log(
                        Level.INFO,
                        "Time synchronized"
                                + " | server="
                                + server
                                + " | utcMillis="
                                + currentUtcMillis
                                + " | jakartaTime="
                                + Time.formatJakartaTime(
                                currentUtcMillis
                        )
                                + " | roundTripMs="
                                + client.getRoundTripTime()
                );
                return true;
            } catch (RuntimeException exception) {
                LOGGER.log(
                        Level.WARNING,
                        "NTP synchronization error"
                                + " | server="
                                + server
                                + " | error="
                                + exception
                );
            }
        }
        LOGGER.log(Level.WARNING, "Time synchronization failed.");
        return false;
    }

    public synchronized boolean isTimeSynchronized() {
        return this.timeSynchronized;
    }

    private synchronized void setNtpTime(long ntpTimeMillis, long ntpReferenceMillis) {
        this.ntpTimeMillis = ntpTimeMillis;
        this.ntpReferenceMillis = ntpReferenceMillis;
        this.timeSynchronized = true;
    }

    private synchronized long getCurrentUtcMillis() {
        if (!this.timeSynchronized) {
            return 0L;
        }

        long elapsedMillis = Util.platformTimeMillis() - this.ntpReferenceMillis;
        if (elapsedMillis < 0L) {
            elapsedMillis = 0L;
        }

        return this.ntpTimeMillis + elapsedMillis;
    }
}