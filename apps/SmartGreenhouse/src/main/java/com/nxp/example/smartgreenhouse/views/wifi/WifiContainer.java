package com.nxp.example.smartgreenhouse.views.wifi;

import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Images;
import com.nxp.example.smartgreenhouse.views.circularprogress.CircularProgress;
import com.nxp.example.smartgreenhouse.views.wifi.scroll.Scroll;
import com.nxp.example.smartgreenhouse.views.wifi.scroll.ScrollableList;
import ej.annotation.NonNullByDefault;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Container;
import ej.mwt.util.Size;
import ej.widget.basic.ImageButton;
import ej.widget.basic.OnClickListener;

public class WifiContainer extends Container {

    public interface OnRefreshClickListener {
        void onRefreshClicked();
    }

    public interface OnWifiNetworkClickListener {
        void onWifiNetworkClicked(WifiNetwork network);
    }

    private static final String TITLE = "Jaringan WiFi";
    private static final int TITLE_GAP = 7;

    private static final String SCANNING_TEXT = "Memindai jaringan...";

    private static final int TITLE_Y = 4;

    private static final int MAX_VISIBLE_ITEMS = 6;

    private static final int REFRESH_BUTTON_WIDTH = 122;
    private static final int REFRESH_BUTTON_HEIGHT = 16;
    private static final int REFRESH_BUTTON_BOTTOM_MARGIN = 4;

    private static final int PROGRESS_SIZE = 34;
    private static final int PROGRESS_TEXT_GAP = 6;

    private final Image wifiFrame;

    private final Scroll scroll;
    private ScrollableList list;

    private final CircularProgress circularProgress;
    private final ImageButton refreshButton;

    private boolean scanning;

    private OnRefreshClickListener onRefreshClickListener;
    private OnWifiNetworkClickListener onWifiNetworkClickListener;

    public WifiContainer() {
        setEnabled(true);

        this.wifiFrame = Image.getImage(Images.WIFI_FRAME);
        this.list = new ScrollableList();
        this.scroll = new Scroll();
        this.scroll.setChild(this.list);

        this.circularProgress = new CircularProgress();
        this.refreshButton = new ImageButton(Images.BUTTON_WIFI);
        this.refreshButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick() {
                        OnRefreshClickListener listener = WifiContainer.this.onRefreshClickListener;

                        if (!WifiContainer.this.scanning && listener != null) {
                            listener.onRefreshClicked();
                        }
                    }
                }
        );

        this.scanning = false;
        this.onRefreshClickListener = null;
        this.onWifiNetworkClickListener = null;

        addChild(this.scroll);
        addChild(this.circularProgress);
        addChild(this.refreshButton);
    }

    public int getWifiFrameWidth() {
        return wifiFrame.getWidth();
    }

    public int getWifiFrameHeight() {
        return wifiFrame.getHeight();
    }

    public void setOnRefreshClickListener(OnRefreshClickListener listener) {
        this.onRefreshClickListener = listener;
    }

    public void setOnWifiNetworkClickListener(OnWifiNetworkClickListener listener) {
        this.onWifiNetworkClickListener = listener;
    }

    public void showScanning() {
        if (this.scanning) {
            return;
        }

        this.scanning = true;

        this.refreshButton.setEnabled(false);
        this.circularProgress.start();

        requestLayOut();
        requestRender();
    }

    public void stopScanning() {
        if (!this.scanning) {
            return;
        }

        this.scanning = false;

        this.refreshButton.setEnabled(true);
        this.circularProgress.stop();

        requestLayOut();
        requestRender();
    }

    public void setNetworks(WifiNetwork[] networks) {
        ScrollableList newList = new ScrollableList();
        if (networks != null) {
            for (int i = 0; i < networks.length; i++) {
                WifiList item = new WifiList();
                item.setNetwork(networks[i], i + 1);
                item.setOnWifiNetworkClickListener(this.onWifiNetworkClickListener);
                newList.addChild(item);
            }
        }

        this.list = newList;

        this.scroll.setChild(this.list);

        this.scroll.reset();

        stopScanning();

        requestLayOut();
        requestRender();
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {
        Font titleFont = Fonts.jetbrainsMonoBold8px();
        int titleHeight = titleFont.getHeight();
        int scrollY = TITLE_Y + titleHeight + TITLE_GAP;
        int scrollHeight = MAX_VISIBLE_ITEMS * WifiList.ITEM_HEIGHT + (MAX_VISIBLE_ITEMS - 1) * WifiList.ITEM_GAP;
        int refreshX = (contentWidth - REFRESH_BUTTON_WIDTH) / 2;
        int refreshY = contentHeight - REFRESH_BUTTON_HEIGHT - REFRESH_BUTTON_BOTTOM_MARGIN;

        if (this.scanning) {
            layOutChild(this.scroll, 0, contentHeight, contentWidth, scrollHeight);
            layOutChild(this.refreshButton, refreshX, contentHeight, REFRESH_BUTTON_WIDTH, REFRESH_BUTTON_HEIGHT);
            Font scanningFont = Fonts.jetbrainsMonoRegular8px();

            int groupHeight = PROGRESS_SIZE + PROGRESS_TEXT_GAP + scanningFont.getHeight();
            int progressX = (contentWidth - PROGRESS_SIZE) / 2;

            int availableHeight = refreshY - scrollY;

            int progressY = scrollY + (availableHeight - groupHeight) / 2;

            layOutChild(this.circularProgress, progressX, progressY, PROGRESS_SIZE, PROGRESS_SIZE);
        } else {
            layOutChild(this.scroll, 0, scrollY, contentWidth, scrollHeight);
            layOutChild(this.circularProgress, 0, contentHeight, PROGRESS_SIZE, PROGRESS_SIZE);
            layOutChild(this.refreshButton, refreshX, refreshY, REFRESH_BUTTON_WIDTH, REFRESH_BUTTON_HEIGHT);
        }
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        Font titleFont = Fonts.jetbrainsMonoBold8px();
        int titleHeight = titleFont.getHeight();
        int scrollY = TITLE_Y + titleHeight + TITLE_GAP;
        int scrollHeight = this.wifiFrame.getHeight() - scrollY;
        computeChildOptimalSize(this.scroll, this.wifiFrame.getWidth(), scrollHeight);
        computeChildOptimalSize(this.circularProgress, PROGRESS_SIZE, PROGRESS_SIZE);
        computeChildOptimalSize(this.refreshButton, REFRESH_BUTTON_WIDTH, REFRESH_BUTTON_HEIGHT);

        size.setSize(this.wifiFrame.getWidth(), this.wifiFrame.getHeight());
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Font titleFont = Fonts.jetbrainsMonoBold8px();
        Painter.drawImage(g, this.wifiFrame, 0, 0);

        int titleX = (contentWidth - titleFont.stringWidth(TITLE)) / 2;
        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, TITLE, titleFont, titleX, TITLE_Y);

        if (this.scanning) {
            Font scanningFont = Fonts.jetbrainsMonoRegular8px();

            int textWidth = scanningFont.stringWidth(SCANNING_TEXT);

            int textX = (contentWidth - textWidth) / 2;
            int textY = this.circularProgress.getY() + PROGRESS_SIZE + PROGRESS_TEXT_GAP;
            Painter.drawString(g, SCANNING_TEXT, scanningFont, textX, textY);
        }

        super.renderContent(g, contentWidth, contentHeight);
    }

}