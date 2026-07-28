package com.nxp.example.smartgreenhouse.views;

import com.nxp.example.smartgreenhouse.models.actuator.ActuatorDisplayItem;
import com.nxp.example.smartgreenhouse.models.menu.MenuItemData;
import com.nxp.example.smartgreenhouse.models.sensor.SensorDisplayItem;
import com.nxp.example.smartgreenhouse.models.sensor.SensorHistorySummary;
import com.nxp.example.smartgreenhouse.models.sensor.SensorThreshold;
import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.views.actuator.ActuatorToggleListener;
import com.nxp.example.smartgreenhouse.views.detail.ActuatorDetail;
import com.nxp.example.smartgreenhouse.views.detail.SensorDetail;
import com.nxp.example.smartgreenhouse.views.linechart.ChartPoint;
import com.nxp.example.smartgreenhouse.views.menu.MenuContainer;
import com.nxp.example.smartgreenhouse.views.overview.FooterOverview;
import com.nxp.example.smartgreenhouse.views.overview.HeaderOverview;
import com.nxp.example.smartgreenhouse.views.overview.Overview;
import com.nxp.example.smartgreenhouse.views.wifi.WifiAuthenticationContainer;
import com.nxp.example.smartgreenhouse.views.wifi.WifiContainer;
import ej.annotation.NonNullByDefault;
import ej.microui.display.Display;
import ej.mwt.Container;
import ej.mwt.util.Size;

public class MainPage extends Container {

    private final HeaderOverview headerOverview;
    private final Overview overview;
    private final FooterOverview footerOverview;
    private final WifiContainer wifiContainer;
    private final WifiAuthenticationContainer wifiAuthenticationContainer;
    private final MenuContainer menuContainer;
    private final SensorDetail sensorDetail;
    private final ActuatorDetail actuatorDetail;

    private final TrayLabel trayLabel;
    private final Indicator indicator;

    private boolean menuOpen;
    private boolean wifiOpen;
    private boolean wifiAuthenticationOpen;
    private boolean sensorDetailOpen;
    private boolean actuatorDetailOpen;


    public MainPage() {
        setEnabled(true);

        this.menuOpen = false;
        this.wifiOpen = false;
        this.wifiAuthenticationOpen = false;
        this.sensorDetailOpen = false;

        this.headerOverview = new HeaderOverview();
        this.trayLabel = new TrayLabel();
        this.overview = new Overview();
        this.indicator = new Indicator();
        this.footerOverview = new FooterOverview();
        this.wifiContainer = new WifiContainer();
        this.wifiAuthenticationContainer = new WifiAuthenticationContainer();
        this.wifiAuthenticationContainer.setEnabled(false);
        this.menuContainer = new MenuContainer();

        this.sensorDetail = new SensorDetail();
        this.sensorDetail.setEnabled(false);

        this.actuatorDetail = new ActuatorDetail();
        this.actuatorDetail.setEnabled(false);
        this.actuatorDetailOpen = false;

        addChild(overview);
        addChild(headerOverview);
        addChild(trayLabel);
        addChild(indicator);
        addChild(footerOverview);
        addChild(wifiContainer);
        addChild(wifiAuthenticationContainer);
        addChild(menuContainer);

        addChild(sensorDetail);
        addChild(actuatorDetail);
    }

    public void setOnWifiClick(HeaderOverview.onWifiClickListener onWifiClick) {
        this.headerOverview.setOnWifiClickListener(onWifiClick);
    }

    public void setOnWifiRefreshClick(WifiContainer.OnRefreshClickListener listener) {
        this.wifiContainer.setOnRefreshClickListener(listener);
    }

    public void setOnWifiNetworkClick(WifiContainer.OnWifiNetworkClickListener listener) {
        this.wifiContainer.setOnWifiNetworkClickListener(listener);
    }

    public void setOnWifiAuthenticationBackClick(WifiAuthenticationContainer.OnAuthenticationBackClickListener listener) {
        this.wifiAuthenticationContainer.setOnAuthenticationBackClickListener(listener);
    }

    public void setOnWifiConnectClick(WifiAuthenticationContainer.OnWifiConnectClickListener listener) {
        this.wifiAuthenticationContainer.setOnWifiConnectClickListener(listener);
    }

    public void setOnOverviewSwipeListener(HorizontalSwipeListener horizontalSwipeListener) {
        this.overview.setOnSwipeListener(horizontalSwipeListener);
    }

    public void setOnFooterSwipeListener(FooterOverview.onSwipeUpListener onSwipeUpListener) {
        this.footerOverview.setOnSwipeUpListener(onSwipeUpListener);
    }

    public void setOnMenuContainerSwipeListener(MenuContainer.onSwipeDownListener onSwipeDownListener) {
        this.menuContainer.setOnSwipeDownListener(onSwipeDownListener);
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

    public void setOnSensorDetailSwipeListener(HorizontalSwipeListener listener) {
        this.sensorDetail.setOnSwipeListener(listener);
    }

    public void setOnActuatorDetailBackListener(ActuatorDetail.onBackListener listener) {
        this.actuatorDetail.setOnBackListener(listener);
    }

    public void setOnActuatorToggleRequestedListener(ActuatorToggleListener listener) {
        this.actuatorDetail.setOnToggleRequestedListener(listener);
    }

    public void setOnActuatorDetailSwipeListener(HorizontalSwipeListener listener) {
        this.actuatorDetail.setOnSwipeListener(listener);
    }

    public void showWifiScanning() {
        this.wifiContainer.showScanning();
    }

    public void stopWifiScanning() {
        this.wifiContainer.stopScanning();
    }

    public void updateWifiNetworks(WifiNetwork[] networks) {
        this.wifiContainer.setNetworks(networks);
    }

    public void updateTime(String time) {
        this.headerOverview.setTime(time);
    }

    public void updateWifiConnectionStatus(boolean connected) {
        this.headerOverview.setWifiConnected(connected);
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
        this.overview.setItems(items);
    }

    public void updateMenuItems(MenuItemData[] items) {
        this.menuContainer.setItems(items);
    }

    public void updateMenuIndicator(int total, int selected) {
        this.menuContainer.setIndicator(total, selected);
    }

    public void updateSensorDetail(
            String title,
            SensorDisplayItem item,
            double minValue,
            double maxValue,
            ChartPoint[] historyPoints,
            SensorHistorySummary historySummary,
            SensorThreshold sensorThreshold,
            int indicatorTotal,
            int indicatorSelectedIndex
    ) {
        this.sensorDetail.setDetailTitle(title);
        this.sensorDetail.setSensorItem(item, minValue, maxValue, historyPoints, historySummary, sensorThreshold, indicatorTotal, indicatorSelectedIndex);
    }

    public void updateActuatorDetail(String detailTitle, ActuatorDisplayItem displayItem) {
        this.actuatorDetail.setDetailTitle(detailTitle);
        this.actuatorDetail.setActuatorItem(displayItem);
    }

    public void clearSensorDetail() {
        this.sensorDetail.setDetailTitle("");
        this.sensorDetail.clearSensorItem();
    }

    public boolean isWifiOpen() {
        return this.wifiOpen;
    }

    public boolean isSensorDetailOpen() {
        return this.sensorDetailOpen;
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

    public boolean isWifiAuthenticationOpen() {
        return this.wifiAuthenticationOpen;
    }

    public void openWifiAuthentication(WifiNetwork network) {
        this.menuOpen = false;
        this.wifiOpen = false;

        this.sensorDetailOpen = false;
        this.actuatorDetailOpen = false;

        this.wifiAuthenticationOpen = true;

        this.wifiContainer.setEnabled(false);
        this.menuContainer.setEnabled(false);

        this.sensorDetail.setEnabled(false);
        this.actuatorDetail.setEnabled(false);

        this.headerOverview.setEnabled(false);
        this.overview.setEnabled(false);
        this.footerOverview.setEnabled(false);

        this.wifiAuthenticationContainer.setEnabled(true);

        this.wifiAuthenticationContainer.open(network);

        requestLayOut();
        requestRender();
    }

    public void showWifiConnecting() {
        if (!this.wifiAuthenticationOpen) {
            return;
        }
        this.wifiAuthenticationContainer.showConnecting();
    }

    public void stopWifiConnecting() {
        if (!this.wifiAuthenticationOpen) {
            return;
        }

        this.wifiAuthenticationContainer.stopConnecting();
    }

    public void showWifiAuthenticationError(String message) {
        if (!this.wifiAuthenticationOpen) {
            return;
        }

        this.wifiAuthenticationContainer.showError(message);
    }

    public void clearWifiAuthenticationError() {
        if (!this.wifiAuthenticationOpen) {
            return;
        }

        this.wifiAuthenticationContainer.clearError();
    }

    public void closeWifiAuthenticationToList() {
        this.wifiAuthenticationOpen = false;

        this.wifiAuthenticationContainer.close();
        this.wifiAuthenticationContainer.setEnabled(false);

        this.headerOverview.setEnabled(true);
        this.overview.setEnabled(true);
        this.footerOverview.setEnabled(true);

        this.menuContainer.setEnabled(true);
        this.wifiContainer.setEnabled(true);

        this.wifiOpen = true;

        requestLayOut();
        requestRender();
    }

    public void closeWifiAfterConnectionSuccess() {
        this.wifiAuthenticationOpen = false;
        this.wifiOpen = false;
        this.menuOpen = false;

        this.wifiAuthenticationContainer.close();
        this.wifiAuthenticationContainer.setEnabled(false);

        this.wifiContainer.setEnabled(true);

        this.headerOverview.setEnabled(true);
        this.overview.setEnabled(true);
        this.footerOverview.setEnabled(true);
        this.menuContainer.setEnabled(true);

        requestLayOut();
        requestRender();
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

    public void openActuatorDetail() {
        this.sensorDetailOpen = false;
        this.sensorDetail.setEnabled(false);

        this.menuOpen = false;
        this.menuContainer.setEnabled(false);

        this.wifiOpen = false;
        this.wifiContainer.setEnabled(false);

        this.actuatorDetailOpen = true;
        this.actuatorDetail.setEnabled(true);

        this.headerOverview.setEnabled(false);
        this.overview.setEnabled(false);
        this.footerOverview.setEnabled(false);

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

    public void closeActuatorDetail() {
        this.actuatorDetailOpen = false;
        this.actuatorDetail.setEnabled(false);

        this.headerOverview.setEnabled(true);
        this.overview.setEnabled(true);
        this.footerOverview.setEnabled(true);
        this.menuContainer.setEnabled(true);
        this.wifiContainer.setEnabled(true);

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

        int overviewWidth = 465;
        int overviewHeight = 202;
        int overviewX = (contentWidth - overviewWidth) / 2;
        int overviewY = headerHeight + ((mainContentHeight - overviewHeight) / 2) - 1;

        layOutChild(this.overview, overviewX, overviewY, overviewWidth, overviewHeight);

        int footerY = contentHeight - footerHeight;
        int indicatorHeight = this.indicator.getIndicatorHeight();
        int gapIndicatorToFooter = 12;
        int indicatorY = footerY - indicatorHeight - gapIndicatorToFooter;
        layOutChild(this.indicator, 0, indicatorY, contentWidth, indicatorHeight);

        layOutChild(this.footerOverview, 0, footerY, contentWidth, footerHeight);

        int wifiWidth = this.wifiContainer.getWidth();
        int wifiHeight = this.wifiContainer.getHeight();
        int marginWifiFrame = 16;
        int wifiX = contentWidth - wifiWidth - marginWifiFrame;
        int wifiY = this.wifiOpen ? headerHeight : contentHeight;
        layOutChild(this.wifiContainer, wifiX, wifiY, wifiWidth, wifiHeight);

        int authenticationWidth = this.wifiAuthenticationContainer.getAuthenticationFrameWidth();
        int authenticationHeight = this.wifiAuthenticationContainer.getAuthenticationFrameHeight();
        int authenticationX = contentWidth - authenticationWidth - marginWifiFrame;
        int authenticationY = this.wifiAuthenticationOpen ? headerHeight : contentHeight;
        layOutChild(this.wifiAuthenticationContainer, authenticationX, authenticationY, authenticationWidth, authenticationHeight);

        int menuWidth = this.menuContainer.getMenuWidth();
        int menuHeight = this.menuContainer.getMenuHeight();
        int menuX = (contentWidth - menuWidth) / 2;
        int menuOpenY = contentHeight - menuHeight;
        int menuY = this.menuOpen ? menuOpenY : contentHeight;
        layOutChild(this.menuContainer, menuX, menuY, menuWidth, menuHeight);

        int sensorDetailY = this.sensorDetailOpen ? 0 : contentHeight;
        layOutChild(this.sensorDetail, 0, sensorDetailY, contentWidth, contentHeight);

        int actuatorDetailY = this.actuatorDetailOpen ? 0 : contentHeight;
        layOutChild(this.actuatorDetail, 0, actuatorDetailY, contentWidth, contentHeight);
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

        computeChildOptimalSize(this.overview, displayWidth, mainContentHeight);

        computeChildOptimalSize(this.indicator, displayWidth, indicatorHeight);
        computeChildOptimalSize(this.footerOverview, displayWidth, footerHeight);

        computeChildOptimalSize(this.wifiContainer, this.wifiContainer.getWifiFrameWidth(), this.wifiContainer.getWifiFrameHeight());

        computeChildOptimalSize(this.menuContainer, displayWidth, this.menuContainer.getMenuHeight());

        computeChildOptimalSize(this.sensorDetail, displayWidth, displayHeight);
        computeChildOptimalSize(this.actuatorDetail, displayWidth, displayHeight);

        computeChildOptimalSize(this.wifiAuthenticationContainer, this.wifiAuthenticationContainer.getAuthenticationFrameWidth(), this.wifiAuthenticationContainer.getAuthenticationFrameHeight());

        size.setSize(displayWidth, displayHeight);
    }
}