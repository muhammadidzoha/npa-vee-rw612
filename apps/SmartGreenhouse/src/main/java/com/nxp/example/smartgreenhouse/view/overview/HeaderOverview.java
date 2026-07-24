package com.nxp.example.smartgreenhouse.view.overview;

import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Icons;
import com.nxp.example.smartgreenhouse.style.Images;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.microui.event.Event;
import ej.microui.event.generator.Buttons;
import ej.microui.event.generator.Pointer;
import ej.mwt.Widget;
import ej.mwt.style.Style;
import ej.mwt.util.Alignment;
import ej.mwt.util.Size;

public class HeaderOverview extends Widget {

    public interface onWifiClickListener {
        void onClicked();
    }

    private onWifiClickListener listener;

    private final Image headerFrame;
    private final Image icon;

    private static final String APP = "SMART GREENHOUSE";
    private static final String ICON_TEXT = "WiFi";

    private String time = "--:--:--";

    private static final int titleX = 22;
    private static final int titleY = 6;
    private static final int offsetClockX = 10;

    private int wifiAreaX;
    private int wifiAreaY;
    private int wifiAreaWidth;
    private int wifiAreaHeight;

    public HeaderOverview() {
        this.headerFrame = Image.getImage(Images.HEADER_FRAME);
        this.icon = Image.getImage(Icons.WIFI_ICON_16);
        setEnabled(true);
    }

    public int getHeaderHeight() {
        return this.headerFrame.getHeight();
    }

    public void setTime(String time) {
        this.time = time;
        requestRender();
    }

    public void setOnWifiClickListener(onWifiClickListener listener) {
        this.listener = listener;
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        size.setSize(this.headerFrame.getWidth(), this.headerFrame.getHeight());
    }

    @Override
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Style style = getStyle();

        Font appFont = Fonts.jetbrainsMonoBold12px();
        Font textFont = Fonts.jetbrainsMonoBold10px();

        int imageX = Alignment.computeLeftX(this.headerFrame.getWidth(), 0, contentWidth, style.getHorizontalAlignment());
        int imageY = Alignment.computeTopY(this.headerFrame.getHeight(), 0, contentHeight, style.getVerticalAlignment());

        int rightX = contentWidth - 14;

        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);

        Painter.drawImage(g, this.headerFrame, imageX, imageY);

        g.setColor(ApplicationColors.PRIMARY_COLOR);
        Painter.drawString(g, APP, appFont, titleX, titleY);

        int iconTitleWidth = textFont.stringWidth(ICON_TEXT);
        int startRightContent = contentWidth - (contentWidth * 22 / 100) - iconTitleWidth;
        int titleBaselineY = titleY + textFont.getBaselinePosition();
        int iconY = titleY + (textFont.getHeight() - icon.getHeight()) / 2 - 1;
        Painter.drawImage(g, this.icon, startRightContent, iconY);

        int iconX = this.icon.getWidth();
        int gap = iconX + 3;
        int iconTitleY = titleBaselineY - textFont.getBaselinePosition();
        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, ICON_TEXT, textFont, startRightContent + gap, iconTitleY);

        this.wifiAreaX = startRightContent;
        this.wifiAreaY = iconY;
        this.wifiAreaWidth = (startRightContent + gap + iconTitleWidth) - startRightContent;
        this.wifiAreaHeight = this.icon.getHeight();

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        int clockWidth = textFont.stringWidth(this.time);
        rightX -= (clockWidth + offsetClockX);
        int timeY = titleBaselineY - textFont.getBaselinePosition();
        Painter.drawString(g, this.time, textFont, rightX, timeY);
    }

    @Override
    public boolean handleEvent(int event) {
        if (Event.getType(event) != Pointer.EVENT_TYPE) {
            return super.handleEvent(event);
        }

        if (Buttons.isReleased(event)) {
            Pointer pointer = (Pointer) Event.getGenerator(event);
            int x = pointer.getX();
            int y = pointer.getY();

            if (isWifiClicked(x, y)) {
                if (this.listener != null) {
                    this.listener.onClicked();
                }
                return true;
            }
        }

        return super.handleEvent(event);
    }

    private boolean isWifiClicked(int x, int y) {
        return x >= this.wifiAreaX
                && x <= this.wifiAreaX + this.wifiAreaWidth
                && y >= this.wifiAreaY
                && y <= this.wifiAreaY + this.wifiAreaHeight;
    }
}
