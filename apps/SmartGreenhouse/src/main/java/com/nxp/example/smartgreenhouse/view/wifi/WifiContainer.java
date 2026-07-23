package com.nxp.example.smartgreenhouse.view.wifi;

import com.nxp.example.smartgreenhouse.model.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Images;
import com.nxp.example.smartgreenhouse.view.wifi.scroll.Scroll;
import com.nxp.example.smartgreenhouse.view.wifi.scroll.ScrollableList;
import ej.annotation.NonNullByDefault;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Container;
import ej.mwt.util.Size;

public class WifiContainer extends Container {

    private static final String TITLE = "Jaringan WiFi";
    private static final int TITLE_GAP = 7;

    private final Image wifiFrame;
    private final Image buttonWifi;
    private final Scroll scroll;
    private final ScrollableList list;

    public WifiContainer() {
        setEnabled(true);
        this.wifiFrame = Image.getImage(Images.WIFI_FRAME);
        this.buttonWifi = Image.getImage(Images.BUTTON_WIFI);

        this.list = new ScrollableList();
        this.scroll = new Scroll();
        this.scroll.setChild(this.list);

        addChild(this.scroll);
    }

    public int getWifiFrameWidth() {
        return wifiFrame.getWidth();
    }

    public int getWifiFrameHeight() {
        return wifiFrame.getHeight();
    }

    public void setNetworks(WifiNetwork[] networks) {
        for (int i = 0; i < networks.length; i++) {
            WifiList item = new WifiList();
            item.setNetwork(networks[i], i + 1);
            this.list.addChild(item);
        }
        requestRender();
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {
        Font titleFont = Fonts.jetbrainsMonoBold8px();

        int titleY = 4;
        int titleHeight = titleFont.getHeight();
        int scrollY = titleY + titleHeight + TITLE_GAP;
        int maxVisibleItems = 6;
        int scrollHeight = maxVisibleItems * WifiList.ITEM_HEIGHT + (maxVisibleItems - 1) * WifiList.ITEM_GAP;

        layOutChild(this.scroll, 0, scrollY, contentWidth, scrollHeight);
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        Font titleFont = Fonts.jetbrainsMonoBold8px();

        int titleY = 4;
        int titleHeight = titleFont.getHeight();
        int scrollY = titleY + titleHeight + TITLE_GAP;
        int scrollHeight = this.wifiFrame.getHeight() - scrollY;

        computeChildOptimalSize(this.scroll, this.wifiFrame.getWidth(), scrollHeight);

        size.setSize(this.wifiFrame.getWidth(), this.wifiFrame.getHeight());
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Font titleFont = Fonts.jetbrainsMonoBold8px();

        Painter.drawImage(g, this.wifiFrame, 0, 0);

        int titleX = (contentWidth - titleFont.stringWidth(TITLE)) / 2;
        int titleY = 4;
        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, TITLE, titleFont, titleX, titleY);

        int frameHeight = this.wifiFrame.getHeight();
        int rectX = (contentWidth - 122) / 2;
        int rectY = frameHeight - 16 - 4;
        Painter.drawImage(g, this.buttonWifi, rectX, rectY);

        super.renderContent(g, contentWidth, contentHeight);
    }

}