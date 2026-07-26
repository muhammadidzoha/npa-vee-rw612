package com.nxp.example.smartgreenhouse.views.wifi;

import com.nxp.example.smartgreenhouse.models.wifi.WifiNetwork;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Icons;
import com.nxp.example.smartgreenhouse.style.Images;
import com.nxp.example.smartgreenhouse.views.circularprogress.CircularProgress;
import com.nxp.example.smartgreenhouse.views.keyboard.Keyboard;

import ej.annotation.NonNullByDefault;
import ej.bon.Timer;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Container;
import ej.mwt.util.Size;
import ej.widget.basic.ImageButton;
import ej.widget.basic.OnClickListener;


public final class WifiAuthenticationContainer extends Container {

    public interface OnAuthenticationBackClickListener {
        void onAuthenticationBackClicked();
    }

    public interface OnWifiConnectClickListener {
        void onWifiConnectClicked(WifiNetwork network, String password);
    }

    private static final String TITLE = "Autentikasi";

    private static final String EMPTY_NETWORK_NAME = "-";

    private static final String CONNECTING_TEXT = "Menghubungkan...";

    private static final int CONNECTING_PROGRESS_SIZE = 34;
    private static final int CONNECTING_TEXT_GAP = 6;

    private static final int HORIZONTAL_MARGIN = 16;

    private static final int BACK_BUTTON_SIZE = 24;
    private static final int BACK_BUTTON_X = 12;
    private static final int BACK_BUTTON_Y = 8;

    private static final int TITLE_Y = 8;
    private static final int TITLE_TO_NETWORK_GAP = 4;
    private static final int NETWORK_TO_PASSWORD_GAP = 8;

    private static final int PASSWORD_FIELD_HEIGHT = 30;

    private static final int PASSWORD_TO_KEYBOARD_GAP = 8;

    private static final int KEYBOARD_HORIZONTAL_MARGIN = 12;
    private static final int KEYBOARD_TO_BUTTON_GAP = 8;

    private static final int CONNECT_BUTTON_BOTTOM_MARGIN = 8;

    private final Image authenticationFrame;
    private final Image connectButtonImage;

    private final ImageButton backButton;
    private final WifiPasswordField passwordField;
    private final Keyboard keyboard;
    private final ImageButton connectButton;
    private final CircularProgress connectingProgress;
    private boolean connecting;

    private WifiNetwork network;

    private OnAuthenticationBackClickListener onAuthenticationBackClickListener;

    private OnWifiConnectClickListener onWifiConnectClickListener;

    public WifiAuthenticationContainer() {
        setEnabled(true);

        this.authenticationFrame = Image.getImage(Images.WIFI_AUTENTIKASI_FRAME);
        this.connectButtonImage = Image.getImage(Images.BUTTON_WIFI_CONNECT);

        this.backButton = new ImageButton(Icons.BACK_ICON_24);

        this.passwordField = new WifiPasswordField();

        Timer keyboardTimer = new Timer();

        this.keyboard = new Keyboard(
                        keyboardTimer,
                        WifiKeyboardStyles
                                .SPACE_KEY_SELECTOR,
                        WifiKeyboardStyles
                                .SHIFT_KEY_INACTIVE_SELECTOR,
                        WifiKeyboardStyles
                                .SHIFT_KEY_ACTIVE_SELECTOR,
                        WifiKeyboardStyles
                                .SWITCH_MAPPING_KEY_SELECTOR
                );

        this.keyboard.getEventGenerator().setEventHandler(this.passwordField);

        this.connectButton = new ImageButton(Images.BUTTON_WIFI_CONNECT);

        this.connectingProgress = new CircularProgress();
        this.connecting = false;

        this.network = null;

        this.onAuthenticationBackClickListener = null;

        this.onWifiConnectClickListener = null;

        this.backButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick() {
                        if (WifiAuthenticationContainer.this.connecting) {
                            return;
                        }
                        OnAuthenticationBackClickListener listener = WifiAuthenticationContainer.this.onAuthenticationBackClickListener;
                        if (listener != null) {
                            listener.onAuthenticationBackClicked();
                        }
                    }
                }
        );

        this.connectButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick() {
                        if (WifiAuthenticationContainer.this.connecting) {
                            return;
                        }
                        WifiNetwork selectedNetwork = WifiAuthenticationContainer.this.network;
                        OnWifiConnectClickListener listener = WifiAuthenticationContainer.this.onWifiConnectClickListener;
                        if (selectedNetwork != null && listener != null) {
                            listener.onWifiConnectClicked(selectedNetwork, WifiAuthenticationContainer.this.passwordField.getPassword());
                        }
                    }
                }
        );

        addChild(this.backButton);
        addChild(this.passwordField);
        addChild(this.keyboard);
        addChild(this.connectButton);
        addChild(this.connectingProgress);
    }

    public void setOnAuthenticationBackClickListener(OnAuthenticationBackClickListener listener) {
        this.onAuthenticationBackClickListener = listener;
    }

    public void setOnWifiConnectClickListener(OnWifiConnectClickListener listener) {
        this.onWifiConnectClickListener = listener;
    }

    public void showConnecting() {
        if (this.connecting) {
            return;
        }

        this.connecting = true;

        this.backButton.setEnabled(false);
        this.passwordField.setEnabled(false);
        this.connectButton.setEnabled(false);

        this.keyboard.deactivate();
        this.connectingProgress.start();

        requestLayOut();
        requestRender();
    }

    public void open(WifiNetwork network) {
        this.network = network;

        this.connecting = false;
        this.connectingProgress.stop();

        this.passwordField.clear();

        this.backButton.setEnabled(true);
        this.passwordField.setEnabled(true);
        this.connectButton.setEnabled(true);

        this.keyboard.activate();

        setEnabled(true);

        requestLayOut();
        requestRender();
    }

    public void close() {
        this.connecting = false;
        this.connectingProgress.stop();

        this.keyboard.deactivate();
        this.passwordField.clear();

        this.backButton.setEnabled(true);
        this.passwordField.setEnabled(true);
        this.connectButton.setEnabled(true);

        this.network = null;

        setEnabled(false);

        requestLayOut();
        requestRender();
    }

    public int getAuthenticationFrameWidth() {
        return this.authenticationFrame.getWidth();
    }

    public int getAuthenticationFrameHeight() {
        return this.authenticationFrame.getHeight();
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {
        Font titleFont = Fonts.jetbrainsMonoBold12px();
        Font networkFont = Fonts.jetbrainsMonoBold16px();

        int networkY = TITLE_Y + titleFont.getHeight() + TITLE_TO_NETWORK_GAP;
        int passwordY = networkY + networkFont.getHeight() + NETWORK_TO_PASSWORD_GAP;
        int passwordWidth = contentWidth - HORIZONTAL_MARGIN * 2;

        int connectButtonWidth = this.connectButtonImage.getWidth();
        int connectButtonHeight = this.connectButtonImage.getHeight();
        int connectButtonX = (contentWidth - connectButtonWidth) / 2;
        int connectButtonY = contentHeight - connectButtonHeight - CONNECT_BUTTON_BOTTOM_MARGIN;

        int keyboardX = KEYBOARD_HORIZONTAL_MARGIN;
        int keyboardY = passwordY + PASSWORD_FIELD_HEIGHT + PASSWORD_TO_KEYBOARD_GAP;
        int keyboardWidth = contentWidth - KEYBOARD_HORIZONTAL_MARGIN * 2;
        int keyboardHeight = connectButtonY - KEYBOARD_TO_BUTTON_GAP - keyboardY;
        if (keyboardHeight < 40) {
            keyboardHeight = 40;
        }

        if (this.connecting) {
            layOutChild(this.backButton, 0, contentHeight, BACK_BUTTON_SIZE, BACK_BUTTON_SIZE);
            layOutChild(this.passwordField, HORIZONTAL_MARGIN, contentHeight, passwordWidth, PASSWORD_FIELD_HEIGHT);
            layOutChild(this.keyboard, keyboardX, contentHeight, keyboardWidth, keyboardHeight);
            layOutChild(this.connectButton, connectButtonX, contentHeight, connectButtonWidth, connectButtonHeight);

            Font connectingFont = Fonts.jetbrainsMonoRegular8px();
            int groupHeight = CONNECTING_PROGRESS_SIZE + CONNECTING_TEXT_GAP + connectingFont.getHeight();
            int contentStartY = networkY + networkFont.getHeight() + NETWORK_TO_PASSWORD_GAP;
            int availableHeight = contentHeight - contentStartY;

            int progressX = (contentWidth - CONNECTING_PROGRESS_SIZE) / 2;
            int progressY = contentStartY + (availableHeight - groupHeight) / 2;

            layOutChild(this.connectingProgress, progressX, progressY, CONNECTING_PROGRESS_SIZE, CONNECTING_PROGRESS_SIZE);
            return;
        }

        layOutChild(this.backButton, BACK_BUTTON_X, BACK_BUTTON_Y, BACK_BUTTON_SIZE, BACK_BUTTON_SIZE);
        layOutChild(this.passwordField, HORIZONTAL_MARGIN, passwordY, passwordWidth, PASSWORD_FIELD_HEIGHT);
        layOutChild(this.keyboard, keyboardX, keyboardY, keyboardWidth, keyboardHeight);
        layOutChild(this.connectButton, connectButtonX, connectButtonY, connectButtonWidth, connectButtonHeight);
        layOutChild(this.connectingProgress, 0, contentHeight, CONNECTING_PROGRESS_SIZE, CONNECTING_PROGRESS_SIZE);
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        int frameWidth = this.authenticationFrame.getWidth();
        int frameHeight = this.authenticationFrame.getHeight();

        computeChildOptimalSize(this.backButton, BACK_BUTTON_SIZE, BACK_BUTTON_SIZE);
        computeChildOptimalSize(this.passwordField, frameWidth - HORIZONTAL_MARGIN * 2, PASSWORD_FIELD_HEIGHT);
        computeChildOptimalSize(this.keyboard, frameWidth - KEYBOARD_HORIZONTAL_MARGIN * 2, frameHeight);
        computeChildOptimalSize(this.connectButton, this.connectButtonImage.getWidth(), this.connectButtonImage.getHeight());
        computeChildOptimalSize(this.connectingProgress, CONNECTING_PROGRESS_SIZE, CONNECTING_PROGRESS_SIZE);

        size.setSize(frameWidth, frameHeight);
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Painter.drawImage(g, this.authenticationFrame, 0, 0);

        Font titleFont = Fonts.jetbrainsMonoBold12px();
        int titleWidth = titleFont.stringWidth(TITLE);
        int titleX = (contentWidth - titleWidth) / 2;

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, TITLE, titleFont, titleX, TITLE_Y);

        Font networkFont = Fonts.jetbrainsMonoBold16px();
        String networkName = this.network == null ? EMPTY_NETWORK_NAME : this.network.getName();
        networkName = fitText(networkName, networkFont, contentWidth - HORIZONTAL_MARGIN * 2);
        int networkWidth = networkFont.stringWidth(networkName);

        int networkX = (contentWidth - networkWidth) / 2;
        int networkY = TITLE_Y + titleFont.getHeight() + TITLE_TO_NETWORK_GAP;
        Painter.drawString(g, networkName, networkFont, networkX, networkY);

        if (this.connecting) {
            Font connectingFont = Fonts.jetbrainsMonoRegular8px();
            int connectingTextWidth = connectingFont.stringWidth(CONNECTING_TEXT);
            int connectingTextX = (contentWidth - connectingTextWidth) / 2;
            int connectingTextY = this.connectingProgress.getY() + CONNECTING_PROGRESS_SIZE + CONNECTING_TEXT_GAP;

            g.setColor(ApplicationColors.SECONDARY_COLOR);
            Painter.drawString(g, CONNECTING_TEXT, connectingFont, connectingTextX, connectingTextY);
        }

        super.renderContent(g, contentWidth, contentHeight);
    }

    private static String fitText(String text, Font font, int maximumWidth) {
        if (font.stringWidth(text) <= maximumWidth) {
            return text;
        }

        String ellipsis = "...";

        int availableWidth = maximumWidth - font.stringWidth(ellipsis);

        if (availableWidth <= 0) {
            return ellipsis;
        }

        int length = text.length();

        while (length > 0 && font.substringWidth(text, 0, length) > availableWidth) {
            length--;
        }

        return text.substring(0, length) + ellipsis;
    }
}