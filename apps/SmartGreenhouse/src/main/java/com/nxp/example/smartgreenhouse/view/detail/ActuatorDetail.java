package com.nxp.example.smartgreenhouse.view.detail;

import com.nxp.example.smartgreenhouse.model.actuator.ActuatorDisplayItem;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Icons;
import com.nxp.example.smartgreenhouse.style.Images;
import com.nxp.example.smartgreenhouse.view.HorizontalSwipeListener;
import com.nxp.example.smartgreenhouse.view.actuator.ActuatorToggle;

import com.nxp.example.smartgreenhouse.view.actuator.ActuatorToggleListener;
import ej.microui.display.Display;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.microui.event.Event;
import ej.microui.event.generator.Buttons;
import ej.microui.event.generator.Pointer;
import ej.mwt.Container;
import ej.mwt.Widget;
import ej.mwt.util.Size;
import ej.widget.basic.ImageButton;
import ej.widget.basic.OnClickListener;

public class ActuatorDetail extends Container {

    public interface onBackListener {
        void onBack();
    }
    
    private static final int BACK_ICON_SIZE = 24;

    private static final int PADDING_TOP_HEADER = 26;
    private static final int PADDING_LEFT_ICON = 15;
    private static final int PADDING_TOP_ICON = 3;

    private static final int PADDING_TOP_TITLE = 8;
    private static final int TITLE_OFFSET_Y = 1;

    private static final int PADDING_TOP_TO_HEADER = 5;

    private static final int PADDING_LEFT_TO_BODY_FRAME = 16;
    private static final int PADDING_RIGHT_TO_BODY_FRAME = 16;
    private static final int PADDING_TOP_FRAME_TO_BODY_FRAME = 11;

    private static final int GAP_STATUS_FRAME = 10;
    private static final int STATUS_RIGHT_PADDING = 12;
    private static final int STATUS_LABEL_PADDING_LEFT = 12;

    private static final int TOGGLE_WIDTH = 43;
    private static final int TOGGLE_HEIGHT = 18;

    private static final int CONTROL_RIGHT_PADDING = 12;
    private static final int CONTROL_TEXT_TO_TOGGLE_GAP = 8;

    private static final int MIDDLE_INFO_TOP_GAP = 8;
    private static final int MIDDLE_INFO_TEXT_GAP = 7;

    private static final int PUMP_TIMER_ICON_OFFSET_X = 1;
    private static final int PUMP_TIMER_ICON_OFFSET_Y = 8;
    private static final int PUMP_TEXT_OFFSET_X = 40;

    private static final int VALVE_STATE_OFFSET_X = 4;
    private static final int VALVE_DIVIDER_OFFSET_X = 70;
    private static final int VALVE_FLOW_OFFSET_X = 94;

    private static final int VALVE_LABEL_TO_VALUE_GAP = 1;

    private static final int PADDING_TOP_ACTIVATION_TO_BODY_FRAME = 10;

    private static final int ACTIVATION_ICON_OFFSET_X = 10;
    private static final int ACTIVATION_TEXT_OFFSET_X = 66;

    private static final int CONDITION_TITLE_TO_VALUE_GAP = 4;

    private static final int PADDING_TOP_UPDATE_TO_ACTIVATION = 8;
    private static final int UPDATE_HORIZONTAL_PADDING = 16;

    private static final int DOT_TOP_GAP = 4;
    private static final int DOT_HORIZONTAL_GAP = 3;

    private static final int SWIPE_HORIZONTAL_THRESHOLD = 60;
    private static final int SWIPE_VERTICAL_TOLERANCE = 40;

    private final Image headerFrame;
    private final Image bodyFrame;
    private final Image actuatorFrame;
    private final Image actuatorStatusFrame;
    private final Image actuatorActivationFrame;
    private final Image optimalFrame;
    private final Image dotActive;
    private final Image dotInactive;

    private final Image statusOptimalFrame;
    private final Image statusBahayaFrame;

    private final Image divideHorizontal;
    private final Image divideVertical;

    private final Image timerIcon;
    private final Image kalenderIcon;
    private final Image settingIcon;

    private final Image pumpIcon;
    private final Image valveIcon;

    private final ImageButton backButton;
    private final ActuatorToggle actuatorToggle;

    private final Font detailTitleFont;
    private final Font statusControlFont;
    private final Font controlOptionFont;
    private final Font statusAlertFont;
    private final Font activationTitleFont;
    private final Font activationTimeFont;
    private final Font activationDateFont;
    private final Font conditionFont;
    private final Font valveLabelFont;
    private final Font valveValueFont;
    private final Font pumpInfoFont;
    private final Font durationValueFont;
    private final Font updateFont;

    private onBackListener onBackListener;
    private HorizontalSwipeListener onSwipeListener;

    private boolean swipeTracking;
    private int swipeStartX;
    private int swipeStartY;

    private String detailTitle;
    private ActuatorDisplayItem actuatorItem;

    public ActuatorDetail() {
        this.headerFrame = Image.getImage(Images.HEADER_DETAIL_FRAME);
        this.bodyFrame = Image.getImage(Images.ACTUATOR_OUTLINE_FRAME);
        this.actuatorFrame = Image.getImage(Images.ACTUATOR_DETAIL_FRAME);
        this.actuatorStatusFrame = Image.getImage(Images.ACTUATOR_STATUS_FRAME);
        this.actuatorActivationFrame = Image.getImage(Images.ACTUATOR_ACTIVATION_FRAME);
        this.optimalFrame = Image.getImage(Images.OPTIMAL_FRAME);
        this.statusOptimalFrame = Image.getImage(Images.OPTIMAL_ALERT_FRAME_M);
        this.statusBahayaFrame = Image.getImage(Images.BAHAYA_ALERT_FRAME_M);
        this.divideHorizontal = Image.getImage(Images.DIVIDE_HORIZONTAL);
        this.divideVertical = Image.getImage(Images.DIVIDE_VERTICAL);
        this.dotActive = Image.getImage(Images.DOT_ACTIVE);
        this.dotInactive = Image.getImage(Images.DOT_INACTIVE);

        this.timerIcon = Image.getImage(Icons.TIMER_ICON_32);
        this.kalenderIcon = Image.getImage(Icons.KALENDER_ICON_48);
        this.settingIcon = Image.getImage(Icons.SETTING_ICON_48);
        this.pumpIcon = Image.getImage(Icons.POMPA_AIR_ICON_64);
        this.valveIcon = Image.getImage(Icons.KATUP_AIR_ICON_64);

        this.detailTitleFont = Fonts.jetbrainsMonoBold12px();
        this.statusControlFont = Fonts.jetbrainsMonoBold10px();
        this.controlOptionFont = Fonts.jetbrainsMonoBold10px();
        this.statusAlertFont = Fonts.jetbrainsMonoRegular10px();
        this.activationTitleFont = Fonts.jetbrainsMonoRegular10px();
        this.activationTimeFont = Fonts.jetbrainsMonoBold14px();
        this.activationDateFont = Fonts.jetbrainsMonoRegular8px();
        this.conditionFont = Fonts.jetbrainsMonoBold8px();
        this.valveLabelFont = Fonts.jetbrainsMonoRegular8px();
        this.valveValueFont = Fonts.jetbrainsMonoBold14px();
        this.pumpInfoFont = Fonts.jetbrainsMonoRegular8px();
        this.durationValueFont = Fonts.jetbrainsMonoBold12px();
        this.updateFont = Fonts.jetbrainsMonoBold10px();

        this.detailTitle = "";

        this.actuatorItem = null;
        this.onSwipeListener = null;
        this.swipeTracking = false;
        this.swipeStartX = 0;
        this.swipeStartY = 0;

        this.actuatorToggle = new ActuatorToggle();

        this.backButton = new ImageButton(Icons.BACK_ICON_24);
        this.backButton.setOnClickListener(
                new OnClickListener() {

                    @Override
                    public void onClick() {
                        if (ActuatorDetail.this.onBackListener
                                != null) {

                            ActuatorDetail.this
                                    .onBackListener
                                    .onBack();
                        }
                    }
                }
        );

        addChild(this.actuatorToggle);
        addChild(this.backButton);
    }

    public void setOnBackListener(onBackListener listener) {
        this.onBackListener = listener;
    }

    public void setOnToggleRequestedListener(ActuatorToggleListener listener) {
        this.actuatorToggle.setOnToggleRequestedListener(listener);
    }

    public void setOnSwipeListener(HorizontalSwipeListener listener) {
        this.onSwipeListener = listener;
    }

    public void setActuatorItem(ActuatorDisplayItem actuatorItem) {
        this.actuatorItem = actuatorItem;
        this.actuatorToggle.setChecked(actuatorItem != null && actuatorItem.isActive());

        requestLayOut();
        requestRender();
    }

    public void setDetailTitle(String detailTitle) {
        this.detailTitle = detailTitle == null ? "" : detailTitle;

        requestRender();
    }

    private boolean isPumpMode() {
        return this.actuatorItem != null && this.actuatorItem.isPump();
    }

    private int getBodyFrameX(int contentWidth) {
        return (contentWidth - this.bodyFrame.getWidth()) / 2;
    }

    private int getBodyFrameY() {
        return PADDING_TOP_HEADER + this.headerFrame.getHeight() + PADDING_TOP_TO_HEADER;
    }

    private int getActuatorFrameX(int contentWidth) {
        return getBodyFrameX(contentWidth) + PADDING_LEFT_TO_BODY_FRAME;
    }

    private int getActuatorFrameY() {
        return getBodyFrameY() + PADDING_TOP_FRAME_TO_BODY_FRAME;
    }

    private int getStatusFrameX(int contentWidth) {
        return getBodyFrameX(contentWidth) + this.bodyFrame.getWidth() - PADDING_RIGHT_TO_BODY_FRAME - this.actuatorStatusFrame.getWidth();
    }

    private int getStatusFrameY() {
        return getActuatorFrameY();
    }

    private int getControlFrameY() {
        return getStatusFrameY() + this.actuatorStatusFrame.getHeight() + GAP_STATUS_FRAME;
    }

    private int getMiddleInfoY() {
        return getControlFrameY() + this.actuatorStatusFrame.getHeight() + MIDDLE_INFO_TOP_GAP;
    }

    private int getActivationFrameY() {
        return getBodyFrameY() + this.bodyFrame.getHeight() + PADDING_TOP_ACTIVATION_TO_BODY_FRAME;
    }

    private int getUpdateFrameY() {
        return getActivationFrameY() + this.actuatorActivationFrame.getHeight() + PADDING_TOP_UPDATE_TO_ACTIVATION;
    }

    private String getControlLeftText() {
        return isPumpMode() ? "MATI" : "TUTUP";
    }

    private String getControlRightText() {
        return isPumpMode() ? "NYALA" : "BUKA";
    }

    private int getToggleX(int contentWidth) {
        int statusFrameX = getStatusFrameX(contentWidth);
        int rightTextWidth = this.controlOptionFont.stringWidth(getControlRightText());
        int rightTextX = statusFrameX + this.actuatorStatusFrame.getWidth() - CONTROL_RIGHT_PADDING - rightTextWidth;

        return rightTextX - CONTROL_TEXT_TO_TOGGLE_GAP - TOGGLE_WIDTH;
    }

    private int getToggleY() {
        return getControlFrameY() + (this.actuatorStatusFrame.getHeight() - TOGGLE_HEIGHT) / 2;
    }

    private boolean isInsideWidget(Widget widget, int pointerX, int pointerY) {
        return pointerX >= widget.getX() && pointerX < widget.getX() + widget.getWidth() && pointerY >= widget.getY() && pointerY < widget.getY() + widget.getHeight();
    }

    @Override
    public boolean handleEvent(int event) {
        if (this.actuatorItem == null || !this.actuatorItem.isValve()) {
            return super.handleEvent(event);
        }

        if (Event.getType(event) != Pointer.EVENT_TYPE) {
            return super.handleEvent(event);
        }

        Pointer pointer = (Pointer) Event.getGenerator(event);
        int action = Buttons.getAction(event);

        int pointerX = pointer.getX();
        int pointerY = pointer.getY();

        if (action == Buttons.PRESSED) {
            if (isInsideWidget(this.backButton, pointerX, pointerY) || isInsideWidget(this.actuatorToggle, pointerX, pointerY)) {
                this.swipeTracking = false;
                return super.handleEvent(event);
            }

            this.swipeTracking = true;
            this.swipeStartX = pointerX;
            this.swipeStartY = pointerY;

            return true;
        }

        if (action == Pointer.DRAGGED) {
            return this.swipeTracking;
        }

        if (action == Buttons.RELEASED) {
            if (!this.swipeTracking) {
                return super.handleEvent(event);
            }

            this.swipeTracking = false;

            int deltaX = pointerX - this.swipeStartX;
            int deltaY = pointerY - this.swipeStartY;

            int horizontalDistance = Math.abs(deltaX);
            int verticalDistance = Math.abs(deltaY);

            boolean validHorizontalSwipe = horizontalDistance >= SWIPE_HORIZONTAL_THRESHOLD && verticalDistance <= SWIPE_VERTICAL_TOLERANCE && horizontalDistance > verticalDistance;

            if (!validHorizontalSwipe) {
                return true;
            }

            if (this.onSwipeListener == null) {
                return true;
            }

            if (deltaX < 0) {
                this.onSwipeListener.onSwipeLeft();
            } else {
                this.onSwipeListener.onSwipeRight();
            }

            return true;
        }

        return super.handleEvent(event);
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {
        layOutChild(this.backButton, PADDING_LEFT_ICON, PADDING_TOP_ICON, BACK_ICON_SIZE, BACK_ICON_SIZE);
        layOutChild(this.actuatorToggle, getToggleX(contentWidth), getToggleY(), TOGGLE_WIDTH, TOGGLE_HEIGHT);
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        int displayWidth = Display.getDisplay().getWidth();
        int displayHeight = Display.getDisplay().getHeight();

        computeChildOptimalSize(this.backButton, BACK_ICON_SIZE, BACK_ICON_SIZE);
        computeChildOptimalSize(this.actuatorToggle, TOGGLE_WIDTH, TOGGLE_HEIGHT);

        size.setSize(displayWidth, displayHeight);
    }

    @Override
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);


        Painter.drawImage(g, this.headerFrame, 0, PADDING_TOP_HEADER);

        int bodyFrameX = getBodyFrameX(contentWidth);
        int bodyFrameY = getBodyFrameY();
        Painter.drawImage(g, this.bodyFrame, bodyFrameX, bodyFrameY);

        int actuatorFrameX = getActuatorFrameX(contentWidth);
        int actuatorFrameY = getActuatorFrameY();
        Painter.drawImage(g, this.actuatorFrame, actuatorFrameX, actuatorFrameY);

        drawActuatorIcon(g, actuatorFrameX, actuatorFrameY);

        int statusFrameX = getStatusFrameX(contentWidth);
        int statusFrameY = getStatusFrameY();
        Painter.drawImage(g, this.actuatorStatusFrame, statusFrameX, statusFrameY);

        int controlFrameY = getControlFrameY();
        Painter.drawImage(g, this.actuatorStatusFrame, statusFrameX, controlFrameY);

        drawDetailTitle(g, contentWidth);
        drawStatusSection(g, statusFrameX, statusFrameY);
        drawControlSection(g, contentWidth, statusFrameX, controlFrameY);
        drawMiddleInformation(g, statusFrameX, getMiddleInfoY());
        drawActivationCards(g, contentWidth);
        drawLastUpdateFrame(g, contentWidth);
        drawTrayIndicators(g, contentWidth);

        super.renderContent(g, contentWidth, contentHeight);
    }

    private void drawDetailTitle(GraphicsContext g, int contentWidth) {
        if (this.detailTitle.isEmpty()) {
            return;
        }

        int titleWidth = this.detailTitleFont.stringWidth(this.detailTitle);
        int titleX = (contentWidth - titleWidth) / 2;
        int titleY = PADDING_TOP_TITLE - TITLE_OFFSET_Y;

        g.setColor(ApplicationColors.PRIMARY_COLOR);
        Painter.drawString(g, this.detailTitle, this.detailTitleFont, titleX, titleY);
    }

    private void drawActuatorIcon(GraphicsContext g, int frameX, int frameY) {
        Image actuatorIcon = isPumpMode() ? this.pumpIcon : this.valveIcon;
        int iconX = frameX + (this.actuatorFrame.getWidth() - actuatorIcon.getWidth()) / 2;
        int iconY = frameY + (this.actuatorFrame.getHeight() - actuatorIcon.getHeight()) / 2;
        Painter.drawImage(g, actuatorIcon, iconX, iconY);
    }

    private void drawStatusSection(GraphicsContext g, int frameX, int frameY) {
        int labelY = frameY + (this.actuatorStatusFrame.getHeight() - this.statusControlFont.getHeight()) / 2;
        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, "STATUS", this.statusControlFont, frameX + STATUS_LABEL_PADDING_LEFT, labelY);

        boolean activeStatus = this.actuatorItem != null && this.actuatorItem.isActive();
        String statusText;
        if (isPumpMode()) {
            statusText = activeStatus ? "NYALA" : "MATI";
        } else {
            statusText = activeStatus ? "BUKA" : "TUTUP";
        }
        Image statusFrame = activeStatus ? this.statusOptimalFrame : this.statusBahayaFrame;
        int statusTextColor = activeStatus ? ApplicationColors.GREEN : ApplicationColors.RED;

        int statusFrameX = frameX + this.actuatorStatusFrame.getWidth() - STATUS_RIGHT_PADDING - statusFrame.getWidth();
        int statusFrameY = frameY + (this.actuatorStatusFrame.getHeight() - statusFrame.getHeight()) / 2;
        Painter.drawImage(g, statusFrame, statusFrameX, statusFrameY);

        int statusTextWidth = this.statusAlertFont.stringWidth(statusText);
        int statusTextX = statusFrameX + (statusFrame.getWidth() - statusTextWidth) / 2 + 1;
        int statusTextY = statusFrameY + (statusFrame.getHeight() - this.statusAlertFont.getHeight()) / 2;
        g.setColor(statusTextColor);
        Painter.drawString(g, statusText, this.statusAlertFont, statusTextX, statusTextY);
    }

    private void drawControlSection(GraphicsContext g, int contentWidth, int frameX, int frameY) {
        int textY = frameY + (this.actuatorStatusFrame.getHeight() - this.controlOptionFont.getHeight()) / 2;

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, "KONTROL", this.statusControlFont, frameX + STATUS_LABEL_PADDING_LEFT, textY);

        String leftText = getControlLeftText();
        int toggleX = getToggleX(contentWidth);

        int leftTextWidth = this.controlOptionFont.stringWidth(leftText);
        int leftTextX = toggleX - CONTROL_TEXT_TO_TOGGLE_GAP - leftTextWidth;
        Painter.drawString(g, leftText, this.controlOptionFont, leftTextX, textY);

        Painter.drawString(g, getControlRightText(), this.controlOptionFont, toggleX + TOGGLE_WIDTH + CONTROL_TEXT_TO_TOGGLE_GAP, textY);
    }

    private void drawMiddleInformation(GraphicsContext g, int frameX, int frameY) {
        Painter.drawImage(g, this.divideHorizontal, frameX, frameY);

        if (isPumpMode()) {
            drawPumpInformation(g, frameX, frameY);
        } else {
            drawValveInformation(g, frameX, frameY);
        }
    }

    private void drawPumpInformation(GraphicsContext g, int frameX, int frameY) {
        int iconX = frameX + PUMP_TIMER_ICON_OFFSET_X;
        int iconY = frameY + PUMP_TIMER_ICON_OFFSET_Y;
        Painter.drawImage(g, this.timerIcon, iconX, iconY);

        int textX = frameX + PUMP_TEXT_OFFSET_X;
        int titleY = frameY + 7;
        g.setColor(ApplicationColors.PRIMARY_COLOR);
        Painter.drawString(g, "LAMA POMPA AKTIF", this.pumpInfoFont, textX, titleY);

        int durationY = titleY + this.pumpInfoFont.getHeight() + MIDDLE_INFO_TEXT_GAP;
        g.setColor(ApplicationColors.SECONDARY_COLOR);

        Painter.drawString(g, "DURASI", this.pumpInfoFont, textX, durationY);

        String durationText = this.actuatorItem == null ? "-" : this.actuatorItem.getDurationText();
        Painter.drawString(g, durationText, this.durationValueFont, textX + 52, durationY - 2);
    }

    private void drawValveInformation(GraphicsContext g, int frameX, int frameY) {
        int labelY = frameY + 7;
        int valueY = labelY + this.valveLabelFont.getHeight() + VALVE_LABEL_TO_VALUE_GAP;

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, "STATE", this.valveLabelFont, frameX + VALVE_STATE_OFFSET_X, labelY);

        Painter.drawImage(g, this.divideVertical, frameX + VALVE_DIVIDER_OFFSET_X, labelY);
        Painter.drawString(g, "FLOW", this.valveLabelFont, frameX + VALVE_FLOW_OFFSET_X, labelY);

        boolean valveOpen = this.actuatorItem != null && this.actuatorItem.isActive();
        boolean flowActive = this.actuatorItem != null && this.actuatorItem.isFlowActive();
        String stateText = valveOpen ? "BUKA" : "TUTUP";
        String flowText = flowActive ? "AKTIF" : "TIDAK AKTIF";
        g.setColor(valveOpen ? ApplicationColors.GREEN : ApplicationColors.RED);
        Painter.drawString(g, stateText, this.valveValueFont, frameX + VALVE_STATE_OFFSET_X, valueY);

        g.setColor(flowActive ? ApplicationColors.GREEN : ApplicationColors.RED);
        Painter.drawString(g, flowText, this.valveValueFont, frameX + VALVE_FLOW_OFFSET_X, valueY);
    }

    private void drawActivationCards(GraphicsContext g, int contentWidth) {
        int bodyFrameX = getBodyFrameX(contentWidth);
        int activationFrameY = getActivationFrameY();

        int rightFrameX = bodyFrameX + this.bodyFrame.getWidth() - this.actuatorActivationFrame.getWidth();
        Painter.drawImage(g, this.actuatorActivationFrame, bodyFrameX, activationFrameY);

        Painter.drawImage(g, this.actuatorActivationFrame, rightFrameX, activationFrameY);

        drawLeftActivationCard(g, bodyFrameX, activationFrameY
        );

        drawRightActivationCard(g, rightFrameX, activationFrameY);
    }

    private void drawLeftActivationCard(GraphicsContext g, int frameX, int frameY) {

        int iconX = frameX + ACTIVATION_ICON_OFFSET_X;
        int iconY = frameY + (this.actuatorActivationFrame.getHeight() - this.kalenderIcon.getHeight()) / 2;

        Painter.drawImage(g, this.kalenderIcon, iconX, iconY);

        int groupHeight = this.activationTitleFont.getHeight() + this.activationTimeFont.getHeight() + this.activationDateFont.getHeight();
        int groupY = iconY + (this.kalenderIcon.getHeight() - groupHeight) / 2;

        int textX = frameX + ACTIVATION_TEXT_OFFSET_X;

        int timeY = groupY + this.activationTitleFont.getHeight();

        int dateY = timeY + this.activationTimeFont.getHeight();

        String cardTitle = isPumpMode() ? "AKTIF TERAKHIR" : "TERAKHIR DIBUKA";

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, cardTitle, this.activationTitleFont, textX, groupY);

        String activationTime = this.actuatorItem == null ? "-" : this.actuatorItem.getLastActivatedTimeText();
        Painter.drawString(g, activationTime, this.activationTimeFont, textX, timeY);
        String activationDate = this.actuatorItem == null ? "-" : this.actuatorItem.getLastActivatedDateText();
        Painter.drawString(g, activationDate, this.activationDateFont, textX, dateY);
    }

    private void drawRightActivationCard(GraphicsContext g, int frameX, int frameY) {
        int iconX = frameX + ACTIVATION_ICON_OFFSET_X;
        int iconY = frameY + (this.actuatorActivationFrame.getHeight() - this.settingIcon.getHeight()) / 2;

        Painter.drawImage(g, this.settingIcon, iconX, iconY);

        int groupHeight = this.activationTitleFont.getHeight() + CONDITION_TITLE_TO_VALUE_GAP + this.conditionFont.getHeight();
        int groupY = iconY + (this.settingIcon.getHeight() - groupHeight) / 2;

        int textX = frameX + ACTIVATION_TEXT_OFFSET_X;

        int conditionY = groupY + this.activationTitleFont.getHeight() + CONDITION_TITLE_TO_VALUE_GAP;

        g.setColor(ApplicationColors.SECONDARY_COLOR);

        Painter.drawString(g, "KONDISI AKTIF", this.activationTitleFont, textX, groupY);

        String conditionText = isPumpMode() ? "Kelembapan Tanah < 40%" : "Terbuka saat pompa menyala";

        Painter.drawString(g, conditionText, this.conditionFont, textX, conditionY);
    }

    private void drawLastUpdateFrame(GraphicsContext g, int contentWidth) {
        int frameX = getBodyFrameX(contentWidth);
        int frameY = getUpdateFrameY();
        Painter.drawImage(g, this.optimalFrame, frameX, frameY);

        int textY = frameY + (this.optimalFrame.getHeight() - this.updateFont.getHeight()) / 2;

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, "TERAKHIR DIPERBARUI", this.updateFont, frameX + UPDATE_HORIZONTAL_PADDING, textY);

        String timeText = this.actuatorItem == null ? "-" : this.actuatorItem.getLastUpdatedTimeText();

        int timeWidth = this.updateFont.stringWidth(timeText);

        int timeX = frameX + this.optimalFrame.getWidth() - UPDATE_HORIZONTAL_PADDING - timeWidth;

        Painter.drawString(g, timeText, this.updateFont, timeX, textY);
    }

    private void drawTrayIndicators(GraphicsContext g, int contentWidth) {
        if (this.actuatorItem == null || !this.actuatorItem.isValve()) {
            return;
        }

        int trayCount = this.actuatorItem.getTrayCount();
        int activeTrayIndex = this.actuatorItem.getTrayIndex();

        if (trayCount <= 0 || activeTrayIndex < 0 || activeTrayIndex >= trayCount) {
            return;
        }

        int groupWidth = 0;
        for (int i = 0; i < trayCount; i++) {
            Image dotImage = i == activeTrayIndex ? this.dotActive : this.dotInactive;
            groupWidth += dotImage.getWidth();

            if (i < trayCount - 1) {
                groupWidth += DOT_HORIZONTAL_GAP;
            }
        }

        int frameX = getBodyFrameX(contentWidth);
        int dotX = frameX + (this.optimalFrame.getWidth() - groupWidth) / 2;
        int dotY = getUpdateFrameY() + this.optimalFrame.getHeight() + DOT_TOP_GAP;

        for (int i = 0; i < trayCount; i++) {
            Image dotImage = i == activeTrayIndex ? this.dotActive : this.dotInactive;
            Painter.drawImage(g, dotImage, dotX, dotY);

            dotX += dotImage.getWidth() + DOT_HORIZONTAL_GAP;
        }
    }

    @Override
    protected void onHidden() {
        this.swipeTracking = false;
    }
}