package com.nxp.example.smartgreenhouse.view;

import com.nxp.example.smartgreenhouse.model.menu.MenuItemData;
import com.nxp.example.smartgreenhouse.model.sensor.SensorDisplayItem;
import com.nxp.example.smartgreenhouse.model.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.view.detail.SensorDetail;
import com.nxp.example.smartgreenhouse.view.linechart.ChartPoint;
import com.nxp.example.smartgreenhouse.view.menu.MenuContainer;
import com.nxp.example.smartgreenhouse.view.overview.FooterOverview;
import com.nxp.example.smartgreenhouse.view.overview.HeaderOverview;
import com.nxp.example.smartgreenhouse.view.overview.Overview;
import com.nxp.example.smartgreenhouse.view.wifi.WifiContainer;
import ej.annotation.NonNullByDefault;
import ej.microui.display.Display;
import ej.mwt.Container;
import ej.mwt.util.Size;

public class MainPage extends Container {

    private final HeaderOverview headerOverview;
    private final Overview overview;
    private final FooterOverview footerOverview;
    private final WifiContainer wifiContainer;
    private final MenuContainer menuContainer;
    private final SensorDetail sensorDetail;

    private final TrayLabel trayLabel;
    private final Indicator indicator;

    private boolean menuOpen;
    private boolean wifiOpen;
    private boolean sensorDetailOpen;


    public MainPage() {
        setEnabled(true);

        this.headerOverview = new HeaderOverview();
        this.trayLabel = new TrayLabel();
        this.overview = new Overview();
        this.indicator = new Indicator();
        this.footerOverview = new FooterOverview();
        this.wifiContainer = new WifiContainer();
        this.menuContainer = new MenuContainer();

        this.sensorDetail = new SensorDetail();

        addChild(overview);
        addChild(headerOverview);
        addChild(trayLabel);
        addChild(indicator);
        addChild(footerOverview);
        addChild(wifiContainer);
        addChild(menuContainer);

        addChild(sensorDetail);
    }

    public void setOnWifiClick(HeaderOverview.onWifiClickListener onWifiClick) {
        if (this.headerOverview != null) {
            this.headerOverview.setOnWifiClickListener(onWifiClick);
        }
    }

    public void setOnOverviewSwipeListener(HorizontalSwipeListener horizontalSwipeListener) {
        if (this.overview != null) {
            this.overview.setOnSwipeListener(horizontalSwipeListener);
        }
    }

    public void setOnFooterSwipeListener(FooterOverview.onSwipeUpListener onSwipeUpListener) {
        if (this.footerOverview != null) {
            this.footerOverview.setOnSwipeUpListener(onSwipeUpListener);
        }
    }

    public void setOnMenuContainerSwipeListener(MenuContainer.onSwipeDownListener onSwipeDownListener) {
        if (this.menuContainer != null) {
            this.menuContainer.setOnSwipeDownListener(onSwipeDownListener);
        }
    }

    public void setOnMenuSwipeListener(HorizontalSwipeListener listener) {
        this.menuContainer.setOnSwipeListener(listener);
    }

    public void setOnMenuItemClickListener(MenuContainer.OnMenuItemClickListener listener) {
        this.menuContainer.setOnMenuItemClickListener(listener);
    }

    public void setOnSensorDetailBackListener(SensorDetail.onBackListener listener) {
        this.sensorDetail.setOnBackListener(listener);
    }

    public void updateWifiNetworks(WifiNetwork[] networks) {
        this.wifiContainer.setNetworks(networks);
    }

    public void updateTime(String time) {
        this.headerOverview.setTime(time);
    }

    public void updateTrayLabel(String text) {
        this.trayLabel.setText(text);
    }

    public void updateTotalIndicator(int total) {
        this.indicator.setTotal(total);
    }

    public void updateSelectedIndicator(int selected) {
        this.indicator.setSelected(selected);
    }

    public void updateSensorCards(SensorDisplayItem[] items) {
        if (this.overview != null) {
            this.overview.setItems(items);
        }
    }

    public void updateMenuItems(MenuItemData[] items) {
        this.menuContainer.setItems(items);
    }

    public void updateMenuIndicator(int total, int selected) {
        this.menuContainer.setIndicator(total, selected);
    }

    public void updateSensorDetailTitle(String text) {
        this.sensorDetail.setDetailTitle(text);
    }

    public void updateSensorDetail(String title, SensorDisplayItem item, double minValue, double maxValue, ChartPoint[] historyPoints) {
        this.sensorDetail.setDetailTitle(title);
        this.sensorDetail.setSensorItem(item, minValue, maxValue, historyPoints);
    }

    public void clearSensorDetail() {
        this.sensorDetail.setDetailTitle("");
        this.sensorDetail.clearSensorItem();
    }

    public boolean isWifiOpen() {
        return this.wifiOpen;
    }

    public void openMenu() {
        this.wifiOpen = false;
        this.menuOpen = true;
        requestLayOut();
    }

    public void closeMenu() {
        this.menuOpen = false;
        requestLayOut();
    }

    public void openWifi() {
        this.menuOpen = false;
        this.wifiOpen = true;
        requestLayOut();
    }

    public void closeWifi() {
        this.wifiOpen = false;
        requestLayOut();
    }

    public void openSensorDetail() {
        this.menuOpen = false;
        this.wifiOpen = false;
        this.sensorDetailOpen = true;

        this.sensorDetail.setEnabled(true);
        this.overview.setEnabled(false);
        this.footerOverview.setEnabled(false);
        this.headerOverview.setEnabled(false);
        this.menuContainer.setEnabled(false);

        requestLayOut();
        requestRender();
    }

    public void closeSensorDetail() {
        this.sensorDetailOpen = false;

        this.sensorDetail.setEnabled(false);
        this.overview.setEnabled(true);
        this.footerOverview.setEnabled(true);
        this.headerOverview.setEnabled(true);
        this.menuContainer.setEnabled(true);

        requestLayOut();
        requestRender();
    }

    public HeaderOverview getHeaderOverview() {
        return this.headerOverview;
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {
        int headerHeight = this.headerOverview.getHeaderHeight();
        int footerHeight = this.footerOverview.getFooterHeight();
        int mainContentHeight = contentHeight - headerHeight - footerHeight;

        if (mainContentHeight < 0) {
            mainContentHeight = 0;
        }

        layOutChild(this.headerOverview, 0, 0, contentWidth, headerHeight);

        int trayLabelHeight = this.trayLabel.getTrayLabelHeight();
        int gapHeaderToTray = 2;
        int trayY = headerHeight + gapHeaderToTray;
        layOutChild(this.trayLabel, 0, trayY, contentWidth, trayLabelHeight);

        if (this.overview != null) {
            int overviewWidth = 465;
            int overviewHeight = 202;
            int overviewX = (contentWidth - overviewWidth) / 2;
            int overviewY = headerHeight + ((mainContentHeight - overviewHeight) / 2) - 1;

            layOutChild(this.overview, overviewX, overviewY, overviewWidth, overviewHeight);
        }

        int footerY = contentHeight - footerHeight;
        int indicatorHeight = this.indicator.getIndicatorHeight();
        int gapIndicatorToFooter = 12;
        int indicatorY = footerY - indicatorHeight - gapIndicatorToFooter;
        layOutChild(this.indicator, 0, indicatorY, contentWidth, indicatorHeight);

        layOutChild(this.footerOverview, 0, footerY, contentWidth, footerHeight);

        if (this.wifiContainer != null) {
            int wifiWidth = this.wifiContainer.getWidth();
            int wifiHeight = this.wifiContainer.getHeight();
            int marginWifiFrame = 16;
            int wifiX = contentWidth - wifiWidth - marginWifiFrame;
            int wifiY = this.wifiOpen ? headerHeight : contentHeight;
            layOutChild(this.wifiContainer, wifiX, wifiY, wifiWidth, wifiHeight);
        }

        if (this.menuContainer != null) {
            int menuWidth = this.menuContainer.getMenuWidth();
            int menuHeight = this.menuContainer.getMenuHeight();
            int menuX = (contentWidth - menuWidth) / 2;
            int menuOpenY = contentHeight - menuHeight;
            int menuY = this.menuOpen ? menuOpenY : contentHeight;
            layOutChild(this.menuContainer, menuX, menuY, menuWidth, menuHeight);
        }

        int sensorDetailY = this.sensorDetailOpen ? 0 : contentHeight;

        layOutChild(this.sensorDetail, 0, sensorDetailY, contentWidth, contentHeight);
    }

    @Override
    @NonNullByDefault
    protected void computeContentOptimalSize(Size size) {
        int displayWidth = Display.getDisplay().getWidth();
        int displayHeight = Display.getDisplay().getHeight();

        int headerHeight = this.headerOverview.getHeaderHeight();
        int trayLabelHeight = this.trayLabel.getTrayLabelHeight();
        int indicatorHeight = this.indicator.getIndicatorHeight();
        int footerHeight = this.footerOverview.getFooterHeight();
        int mainContentHeight = displayHeight - headerHeight - footerHeight;

        if (mainContentHeight < 0) {
            mainContentHeight = 0;
        }

        computeChildOptimalSize(this.headerOverview, displayWidth, headerHeight);
        computeChildOptimalSize(this.trayLabel, displayWidth, trayLabelHeight);

        if (this.overview != null) {
            computeChildOptimalSize(this.overview, displayWidth, mainContentHeight);
        }

        computeChildOptimalSize(this.indicator, displayWidth, indicatorHeight);
        computeChildOptimalSize(this.footerOverview, displayWidth, footerHeight);

        if (this.wifiContainer != null) {
            computeChildOptimalSize(this.wifiContainer, this.wifiContainer.getWifiFrameWidth(), this.wifiContainer.getWifiFrameHeight());
        }

        if (this.menuContainer != null) {
            computeChildOptimalSize(this.menuContainer, displayWidth, this.menuContainer.getMenuHeight());
        }

        if (this.sensorDetail != null) {
            computeChildOptimalSize(this.sensorDetail, displayWidth, displayHeight);
        }

        size.setSize(displayWidth, displayHeight);
    }
}