package com.nxp.example.smartgreenhouse.views.wifi;

import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Icons;
import com.nxp.example.smartgreenhouse.style.Images;
import ej.annotation.NonNullByDefault;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Widget;
import ej.mwt.util.Size;

public class WifiList extends Widget {

    public static final int ITEM_HEIGHT = 11;
    public static final int ITEM_GAP = 8;
    public static final int ITEM_TOTAL_HEIGHT = ITEM_HEIGHT + ITEM_GAP;

    private static final int LEFT_PADDING = 8;
    private static final int RIGHT_PADDING = 8;
    private static final int NUMBER_TO_CIRCLE_GAP = 7;
    private static final int CIRCLE_SIZE = 7;
    private static final int CIRCLE_TO_SSID_GAP = 7;
    private static final int LOCK_TO_RSSI_GAP = 3;

    private static final String STATUS_CONNECTED_TEXT = "Aktif";

    private WifiNetwork network;
    private int index;

    private final Image dot;
    private final Image statusConnected;

    public WifiList() {
        this.dot = Image.getImage(Images.DOT_ACTIVE);
        this.statusConnected = Image.getImage(Images.OPTIMAL_ALERT_FRAME_S);
    }

    public void setNetwork(WifiNetwork network, int index) {
        this.network = network;
        this.index = index;
        requestRender();
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        size.setSize(size.getWidth(), ITEM_TOTAL_HEIGHT);
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        if (this.network == null) {
            return;
        }

        Font numberFont = Fonts.jetbrainsMonoBold8px();
        Font ssidFont = Fonts.jetbrainsMonoBold8px();
        Font rssiFont = Fonts.jetbrainsMonoRegular5px();

        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);

        String numberStr;
        if (this.index < 10) {
            numberStr = "0" + this.index;
        } else {
            numberStr = "" + this.index;
        }

        int numberCenterY = (ITEM_HEIGHT - numberFont.getHeight()) / 2;
        int x = LEFT_PADDING;
        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, numberStr, numberFont, x, numberCenterY);
        x += numberFont.stringWidth(numberStr) + NUMBER_TO_CIRCLE_GAP;

        int dotCenterY = (ITEM_HEIGHT - this.dot.getHeight()) / 2;
        Painter.drawImage(g, this.dot, x, dotCenterY);
        x += CIRCLE_SIZE + CIRCLE_TO_SSID_GAP;

        if (this.network.isConnected()) {
            int ssidCenterY = (ITEM_HEIGHT - ssidFont.getHeight()) / 2;
            g.setColor(ApplicationColors.SECONDARY_COLOR);
            Painter.drawString(g, this.network.getName(), ssidFont, x, ssidCenterY);

            int statusX = contentWidth - RIGHT_PADDING - statusConnected.getWidth();
            int statusCenterY = (ITEM_HEIGHT - statusConnected.getHeight()) / 2;
            Painter.drawImage(g, this.statusConnected, statusX, statusCenterY);

            int textWidth = rssiFont.stringWidth(STATUS_CONNECTED_TEXT);
            int textX = statusX + (this.statusConnected.getWidth() - textWidth) / 2;
            int textY = statusCenterY + (this.statusConnected.getHeight() - rssiFont.getHeight()) / 2;
            g.setColor(ApplicationColors.STATUS_OPTIMAL_COLOR);
            Painter.drawString(g, STATUS_CONNECTED_TEXT, rssiFont, textX, textY);
        } else {
            String rssiStr = this.network.getRssi() + " dBm";
            int rssiWidth = rssiFont.stringWidth(rssiStr);
            int rssiX = contentWidth - RIGHT_PADDING - rssiWidth;

            int ssidEndX = rssiX;
            if (this.network.isSecured()) {
                Image lockIcon = Image.getImage(Icons.LOCK_ICON_5);
                if (lockIcon != null) {
                    int lockX = rssiX - LOCK_TO_RSSI_GAP - lockIcon.getWidth();
                    int lockCenterY = (ITEM_HEIGHT - lockIcon.getHeight()) / 2;
                    Painter.drawImage(g, lockIcon, lockX, lockCenterY);
                    ssidEndX = lockX;
                }
            }

            int ssidAvailableWidth = ssidEndX - x;
            if (ssidAvailableWidth > 0) {
                int ssidCenterY = (ITEM_HEIGHT - ssidFont.getHeight()) / 2;
                g.setColor(ApplicationColors.SECONDARY_COLOR);
                Painter.drawString(g, this.network.getName(), ssidFont, x, ssidCenterY);
            }

            int rssiCenterY = (ITEM_HEIGHT - rssiFont.getHeight()) / 2;
            g.setColor(ApplicationColors.SECONDARY_COLOR);
            Painter.drawString(g, rssiStr, rssiFont, rssiX, rssiCenterY);
        }
    }

}