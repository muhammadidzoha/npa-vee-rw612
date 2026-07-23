package com.nxp.example.smartgreenhouse.view.wifi;

import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Images;
import ej.annotation.NonNullByDefault;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Container;
import ej.mwt.util.Size;

public class WifiContainer extends Container {

    private final Image wifiFrame;

    private static final String TITLE = "Jaringan WiFi";

    public WifiContainer() {
        this.wifiFrame = Image.getImage(Images.WIFI_FRAME);
    }

    public int getWifiFrameWidth() {
        return wifiFrame.getWidth();
    }

    public int getWifiFrameHeight() {
        return wifiFrame.getHeight();
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {

    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        int frameWidth = wifiFrame.getWidth();
        int frameHeight = wifiFrame.getHeight();

        size.setSize(frameWidth, frameHeight);
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Font titleFont = Fonts.jetbrainsMonoBold8px();
        Painter.drawImage(g, this.wifiFrame, 0, 0);

        int ImageWidth = wifiFrame.getWidth();
        int titleX = (ImageWidth - titleFont.stringWidth(TITLE)) / 2;
        int titleY = 4;
        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, TITLE, titleFont, titleX, titleY);

        super.renderContent(g, contentWidth, contentHeight);
    }
}
