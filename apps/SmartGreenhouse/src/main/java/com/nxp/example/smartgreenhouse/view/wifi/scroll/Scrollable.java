package com.nxp.example.smartgreenhouse.view.wifi.scroll;

public interface Scrollable {

    void initializeViewport(int width, int height);

    void updateViewport(int x, int y);

    int[] getItemSizes();

}