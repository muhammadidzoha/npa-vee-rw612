package com.nxp.example.smartgreenhouse.model;

public class SensorStatusEvaluator {

    public static SensorStatus evaluate(int sensorId, float value) {
        switch (sensorId) {
            case SensorId.NITROGEN:
                if (value >= 20 && value <= 50) return SensorStatus.OPTIMAL;
                if ((value >= 10 && value < 20) || (value > 50 && value <= 70)) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            case SensorId.FOSFOR:
                if (value >= 15 && value <= 30) return SensorStatus.OPTIMAL;
                if ((value >= 10 && value < 15) || (value > 30 && value <= 50)) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            case SensorId.KALIUM:
                if (value >= 150 && value <= 250) return SensorStatus.OPTIMAL;
                if ((value >= 100 && value < 150) || (value > 250 && value <= 350)) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            case SensorId.KELEMBAPAN_TANAH:
                if (value >= 40 && value <= 80) return SensorStatus.OPTIMAL;
                if ((value >= 20 && value < 40) || (value > 80 && value <= 90)) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            case SensorId.SUHU_TANAH:
            case SensorId.SUHU_UDARA:
                if (value >= 20 && value <= 30) return SensorStatus.OPTIMAL;
                if ((value >= 15 && value < 20) || (value > 30 && value <= 35)) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            case SensorId.PH_TANAH:
                if (value >= 6.0 && value <= 7.0) return SensorStatus.OPTIMAL;
                if ((value >= 5.5 && value < 6.0) || (value > 7.0 && value <= 7.5)) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            case SensorId.SALINITAS_TANAH:
                if (value >= 0.0 && value <= 2.0) return SensorStatus.OPTIMAL;
                if (value > 2.0 && value <= 4.0) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            case SensorId.KELEMBAPAN_UDARA:
                if (value >= 50 && value <= 70) return SensorStatus.OPTIMAL;
                if ((value >= 40 && value < 50) || (value > 70 && value <= 80)) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            case SensorId.INTENSITAS_CAHAYA:
                if (value >= 10000 && value <= 50000) return SensorStatus.OPTIMAL;
                if ((value >= 5000 && value < 10000) || (value > 50000 && value <= 70000)) return SensorStatus.WASPADA;
                return SensorStatus.BAHAYA;

            default:
                return SensorStatus.OPTIMAL;
        }
    }
}
