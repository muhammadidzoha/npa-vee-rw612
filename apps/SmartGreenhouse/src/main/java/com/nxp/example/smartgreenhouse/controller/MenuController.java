package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.menu.MenuItemData;
import com.nxp.example.smartgreenhouse.model.menu.SampleMenuItemData;
import com.nxp.example.smartgreenhouse.state.AppState;
import com.nxp.example.smartgreenhouse.view.HorizontalSwipeListener;
import com.nxp.example.smartgreenhouse.view.MainPage;
import com.nxp.example.smartgreenhouse.view.menu.MenuContainer;

import java.util.logging.Level;
import java.util.logging.Logger;

public class MenuController implements HorizontalSwipeListener, MenuContainer.OnMenuItemClickListener {

    private static final Logger LOGGER = Logger.getLogger("[SMART GREENHOUSE: MENU CONTROLLER]");

    private static final int CARD_COLUMNS = 3;
    private static final int CARD_ROWS = 2;
    private static final int CARDS_PER_PAGE = CARD_COLUMNS * CARD_ROWS;

    private final MainPage mainPage;
    private final AppState appState;
    private final DetailController detailController;

    private MenuItemData[] allItems;
    private int currentPage;

    public MenuController(MainPage mainPage, AppState appState, DetailController detailController) {
        this.mainPage = mainPage;
        this.appState = appState;
        this.detailController = detailController;
    }

    public void init() {
        this.allItems = SampleMenuItemData.createSampleMenuItems();
        this.currentPage = 0;
        this.mainPage.setOnMenuSwipeListener(this);
        this.mainPage.setOnMenuItemClickListener(this);
        showPage(0);
    }

    @Override
    public void onSwipeLeft() {
        int pageCount = (this.allItems.length + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE;
        if (this.currentPage < pageCount - 1) {
            showPage(this.currentPage + 1);
        }
    }

    @Override
    public void onSwipeRight() {
        if (this.currentPage > 0) {
            showPage(this.currentPage - 1);
        }
    }

    @Override
    public void onMenuItemClicked(MenuItemData item) {
        if (item == null) {
            return;
        }

        this.appState.setSelectedMenuItem(item);

        if (item.isSensor()) {
            LOGGER.log(Level.INFO, "Opening sensor detail: " + item.getTitle() + " | Node: " + this.appState.getSelectedNodeId() + " | Sensor ID: " + item.getSensorId());

            this.detailController.openSelectedSensor();
            return;
        }

        LOGGER.log(Level.INFO, "Actuator detail is not implemented: " + item.getTitle() + " | Node: " + this.appState.getSelectedNodeId() + " | Actuator ID: " + item.getActuatorId());
    }

    private void showPage(int pageIndex) {
        this.currentPage = pageIndex;

        int start = pageIndex * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, this.allItems.length);
        int count = end - start;

        MenuItemData[] pageItems = new MenuItemData[count];
        System.arraycopy(this.allItems, start, pageItems, 0, count);

        this.mainPage.updateMenuItems(pageItems);
        this.mainPage.updateMenuIndicator(getPageCount(), this.currentPage);
    }

    private int getPageCount() {
        return (this.allItems.length + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE;
    }
}