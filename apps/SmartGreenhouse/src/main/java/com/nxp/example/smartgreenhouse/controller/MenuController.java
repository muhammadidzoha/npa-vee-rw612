package com.nxp.example.smartgreenhouse.controller;

import com.nxp.example.smartgreenhouse.model.menu.MenuItemData;
import com.nxp.example.smartgreenhouse.model.menu.SampleMenuItemData;
import com.nxp.example.smartgreenhouse.view.HorizontalSwipeListener;
import com.nxp.example.smartgreenhouse.view.MainPage;

public class MenuController implements HorizontalSwipeListener {

    private static final int CARD_COLUMNS = 3;
    private static final int CARD_ROWS = 2;
    private static final int CARDS_PER_PAGE = CARD_COLUMNS * CARD_ROWS;

    private final MainPage mainPage;
    private MenuItemData[] allItems;
    private int currentPage;

    public MenuController(MainPage mainPage) {
        this.mainPage = mainPage;
    }

    public void init() {
        this.allItems = SampleMenuItemData.createSampleMenuItems();
        this.currentPage = 0;
        this.mainPage.setOnMenuSwipeListener(this);
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

    private void showPage(int pageIndex) {
        this.currentPage = pageIndex;

        int start = pageIndex * CARDS_PER_PAGE;
        int end = Math.min(start + CARDS_PER_PAGE, this.allItems.length);
        int count = end - start;

        MenuItemData[] pageItems = new MenuItemData[count];
        System.arraycopy(this.allItems, start, pageItems, 0, count);

        this.mainPage.updateMenuItems(pageItems);
//        this.mainPage.updateMenuIndicator(
//                (this.allItems.length + CARDS_PER_PAGE - 1) / CARDS_PER_PAGE,
//                this.currentPage
//        );
    }
}