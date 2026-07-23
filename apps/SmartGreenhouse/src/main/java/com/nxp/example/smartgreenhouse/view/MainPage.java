package com.nxp.example.smartgreenhouse.view;

import com.nxp.example.smartgreenhouse.model.SensorDisplayItem;
import com.nxp.example.smartgreenhouse.view.menu.MenuContainer;
import com.nxp.example.smartgreenhouse.view.overview.FooterOverview;
import com.nxp.example.smartgreenhouse.view.overview.HeaderOverview;
import com.nxp.example.smartgreenhouse.view.overview.Overview;
import ej.annotation.NonNullByDefault;
import ej.microui.display.Display;
import ej.mwt.Container;
import ej.mwt.util.Size;

public class MainPage extends Container {

    private final HeaderOverview headerOverview;
    private final Overview overview;
    private final FooterOverview footerOverview;
    private final MenuContainer menuContainer;

    private final TrayLabel trayLabel;
    private final Indicator indicator;

    private boolean menuOpen;


    public MainPage() {
        setEnabled(true);

        this.headerOverview = new HeaderOverview();
        this.trayLabel = new TrayLabel();
        this.overview = new Overview();
        this.indicator = new Indicator();
        this.footerOverview = new FooterOverview();
        this.menuContainer = new MenuContainer();

        addChild(overview);
        addChild(headerOverview);
        addChild(trayLabel);
        addChild(indicator);
        addChild(footerOverview);
        addChild(menuContainer);
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

    public void updateTime(String time) {
        this.headerOverview.setTime(time);
        this.headerOverview.requestRender();
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

    public void setMenuOpen(boolean menuOpen) {
        this.menuOpen = menuOpen;
        requestLayOut();
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

        if (this.menuContainer != null) {
            int menuWidth = this.menuContainer.getWidth();
            int menuHeight = this.menuContainer.getHeight();
            int menuX = (contentWidth - menuWidth) / 2;
            int menuOpenY = contentHeight - menuHeight;
            int menuY = this.menuOpen ? menuOpenY : contentHeight;
            layOutChild(this.menuContainer, menuX, menuY, menuWidth, menuHeight);
        }
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

        if (this.menuContainer != null) {
            computeChildOptimalSize(this.menuContainer, displayWidth, this.menuContainer.getMenuHeight());
        }

        size.setSize(displayWidth, displayHeight);
    }
}