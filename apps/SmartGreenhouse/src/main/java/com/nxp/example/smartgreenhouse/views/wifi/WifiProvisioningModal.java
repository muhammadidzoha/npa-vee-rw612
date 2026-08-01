package com.nxp.example.smartgreenhouse.views.wifi;

import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Images;

import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.microui.event.Event;
import ej.microui.event.generator.Buttons;
import ej.microui.event.generator.Pointer;
import ej.mwt.Widget;
import ej.mwt.util.Size;

public final class WifiProvisioningModal extends Widget {

    public interface OnCloseListener {
        void onClose();
    }

    private static final int MODE_STARTING = 0;
    private static final int MODE_READY = 1;
    private static final int MODE_CONNECTING = 2;
    private static final int MODE_FAILED = 3;

    private final Image qrCode;

    private int mode;
    private String provisioningSsid;
    private String provisioningPassword;
    private String portalUrl;
    private String connectingSsid;
    private int networkCount;

    private OnCloseListener closeListener;

    public WifiProvisioningModal() {
        this.qrCode = Image.getImage(Images.QR);
        this.mode = MODE_STARTING;
        this.provisioningSsid = "smartgreenhouse";
        this.provisioningPassword = "smartgreenhouse";
        this.portalUrl = "http://192.168.4.1/";
        this.connectingSsid = "";
        this.networkCount = 0;
        this.closeListener = null;
        setEnabled(false);
    }

    public void setOnCloseListener(OnCloseListener listener) {
        this.closeListener = listener;
    }

    public void showStarting() {
        this.mode = MODE_STARTING;
        this.connectingSsid = "";
        requestRender();
    }

    public void showReady(String ssid, String password, String portalUrl, int networkCount) {
        this.mode = MODE_READY;
        this.provisioningSsid = ssid == null ? "smartgreenhouse" : ssid;
        this.provisioningPassword = password == null ? "smartgreenhouse" : password;
        this.portalUrl = portalUrl == null ? "http://192.168.4.1/" : portalUrl;
        this.networkCount = networkCount;
        requestRender();
    }

    public void showConnecting(String ssid) {
        this.mode = MODE_CONNECTING;
        this.connectingSsid = ssid == null ? "" : ssid;
        requestRender();
    }

    public void showFailed() {
        this.mode = MODE_FAILED;
        requestRender();
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        size.setSize(480, 272);
    }

    @Override
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);

        Font titleFont = Fonts.jetbrainsMonoBold12px();
        Font textFont = Fonts.jetbrainsMonoBold10px();

        drawCenteredText(g, "Wi-Fi", titleFont, contentWidth, 18, ApplicationColors.PRIMARY_COLOR);

        if (this.mode == MODE_STARTING) {
            renderStarting(g, contentWidth, textFont);
            return;
        }

        if (this.mode == MODE_READY) {
            renderReady(g, contentWidth, textFont);
            return;
        }

        if (this.mode == MODE_CONNECTING) {
            renderConnecting(g, contentWidth, textFont);
            return;
        }

        renderFailed(g, contentWidth, textFont);
    }

    private void renderStarting(GraphicsContext g, int contentWidth, Font textFont) {
        drawCenteredText(g, "Menyiapkan pengaturan Wi-Fi...", textFont, contentWidth, 118, ApplicationColors.SECONDARY_COLOR);
        drawCenteredText(g, "Memindai jaringan yang tersedia.", textFont, contentWidth, 140, ApplicationColors.SECONDARY_COLOR);
    }

    private void renderReady(GraphicsContext g, int contentWidth, Font textFont) {
        int qrX = 28;
        int qrY = 62;

        Painter.drawImage(g, this.qrCode, qrX, qrY);

        int textX = 178;
        int currentY = 52;
        int lineGap = 18;

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, "1. Scan QR atau hubungkan manual:", textFont, textX, currentY);

        currentY += lineGap;
        Painter.drawString(g, "SSID:", textFont, textX, currentY);

        currentY += lineGap;
        Painter.drawString(g, this.provisioningSsid, textFont, textX, currentY);

        currentY += lineGap;
        Painter.drawString(g, "Password:", textFont, textX, currentY);

        currentY += lineGap;
        Painter.drawString(g, this.provisioningPassword, textFont, textX, currentY);

        currentY += lineGap + 4;
        Painter.drawString(g, "2. Buka:", textFont, textX, currentY);

        currentY += lineGap;
        Painter.drawString(g, this.portalUrl, textFont, textX, currentY);

        currentY += lineGap + 4;
        Painter.drawString(g, "Jaringan ditemukan: " + this.networkCount, textFont, textX, currentY);

        drawCenteredText(g, "Gunakan HP untuk melanjutkan pengaturan Wi-Fi.", textFont, contentWidth, 232, ApplicationColors.SECONDARY_COLOR);
    }

    private void renderConnecting(GraphicsContext g, int contentWidth, Font textFont) {
        drawCenteredText(g, this.connectingSsid, textFont, contentWidth, 110, ApplicationColors.PRIMARY_COLOR);
        drawCenteredText(g, "Connecting...", textFont, contentWidth, 138, ApplicationColors.SECONDARY_COLOR);
        drawCenteredText(g, "Mohon tunggu.", textFont, contentWidth, 164, ApplicationColors.SECONDARY_COLOR);
    }

    private void renderFailed(GraphicsContext g, int contentWidth, Font textFont) {
        drawCenteredText(g, "Provisioning Wi-Fi gagal.", textFont, contentWidth, 110, ApplicationColors.THIRD_COLOR);
        drawCenteredText(g, "Silakan coba kembali.", textFont, contentWidth, 138, ApplicationColors.SECONDARY_COLOR);
        drawCenteredText(g, "Sentuh layar untuk menutup.", textFont, contentWidth, 182, ApplicationColors.SECONDARY_COLOR);
    }

    private void drawCenteredText(GraphicsContext g, String text, Font font, int contentWidth, int y, int color) {
        int textWidth = font.stringWidth(text);
        int x = (contentWidth - textWidth) / 2;

        g.setColor(color);
        Painter.drawString(g, text, font, x, y);
    }

    @Override
    public boolean handleEvent(int event) {
        if (Event.getType(event) != Pointer.EVENT_TYPE) {
            return true;
        }

        if (this.mode == MODE_FAILED && Buttons.isReleased(event)) {
            if (this.closeListener != null) {
                this.closeListener.onClose();
            }
        }

        return true;
    }
}