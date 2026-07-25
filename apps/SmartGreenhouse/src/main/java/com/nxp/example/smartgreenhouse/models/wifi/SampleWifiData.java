package com.nxp.example.smartgreenhouse.models.wifi;

public class SampleWifiData {

    public static WifiNetwork[] createSampleWifiData() {
        return new WifiNetwork[]{
                new WifiNetwork("Indihome", -45, true, true),
                new WifiNetwork("Biznet", -62, true, false),
                new WifiNetwork("MyRepublic", -71, true, false),
                new WifiNetwork("Telkomsel", -55, true, false),
                new WifiNetwork("OPEN_WIFI", -80, false, false),
                new WifiNetwork("Cafe_Guest", -68, false, false),
                new WifiNetwork("Office_5G", -50, true, false),
                new WifiNetwork("XL", -75, true, false),
                new WifiNetwork("Indosat", -82, true, false),
                new WifiNetwork("Tri", -77, false, false),
        };
    }

}
