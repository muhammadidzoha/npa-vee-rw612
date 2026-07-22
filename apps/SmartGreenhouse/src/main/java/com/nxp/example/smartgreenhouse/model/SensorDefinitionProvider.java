package com.nxp.example.smartgreenhouse.model;

import com.nxp.example.smartgreenhouse.style.Icons;
import ej.microui.display.Image;

public class SensorDefinitionProvider {

    public static SensorDefinition[] getAll() {
        return new SensorDefinition[] {
                new SensorDefinition(SensorId.NITROGEN, Image.getImage(Icons.NITROGEN_ICON_32), "NITROGEN (N)", "ppm", 0, true),
                new SensorDefinition(SensorId.FOSFOR, Image.getImage(Icons.FOSFOR_ICON_32), "FOSFOR (P)", "ppm", 0, true),
                new SensorDefinition(SensorId.KALIUM, Image.getImage(Icons.KALIUM_ICON_32), "KALIUM (K)", "ppm", 0, true),
                new SensorDefinition(SensorId.KELEMBAPAN_TANAH, Image.getImage(Icons.KELEMBAPAN_TANAH_ICON_32), "KELEMBAPAN TANAH", "%", 0, true),
                new SensorDefinition(SensorId.PH_TANAH, Image.getImage(Icons.PH_TANAH_ICON_32), "PH TANAH", "", 1, true),
                new SensorDefinition(SensorId.SUHU_TANAH, Image.getImage(Icons.SUHU_TANAH_ICON_32), "SUHU TANAH", "°C", 1, true),
                new SensorDefinition(SensorId.SALINITAS_TANAH, Image.getImage(Icons.SALINITAS_TANAH_ICON_32), "SALINITAS TANAH", "μS/cm", 1, false),
                new SensorDefinition(SensorId.SUHU_UDARA, Image.getImage(Icons.SUHU_UDARA_ICON_32), "SUHU UDARA", "°C", 1, false),
                new SensorDefinition(SensorId.KELEMBAPAN_UDARA, Image.getImage(Icons.KELEMBAPAN_UDARA_ICON_32), "KELEMBAPAN UDARA", "%", 0, false),
                new SensorDefinition(SensorId.INTENSITAS_CAHAYA, Image.getImage(Icons.INTENSITAS_CAHAYA_ICON_32), "INTENSITAS CAHAYA", "lux", 0, false),
        };
    }
}
