package com.nxp.example.smartgreenhouse.view.menu;

import com.nxp.example.smartgreenhouse.style.Images;
import ej.annotation.NonNullByDefault;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.microui.event.Event;
import ej.microui.event.generator.Buttons;
import ej.microui.event.generator.Pointer;
import ej.mwt.Container;
import ej.mwt.util.Size;

public class MenuContainer extends Container {

    public interface onSwipeDownListener {
        void onSwipeDown();
    }

    private static final int SWIPE_THRESHOLD = 35;
    private int touchStartX;
    private int touchStartY;

    private final Image menuFrame;

    private onSwipeDownListener onSwipeDownListener;

    public MenuContainer() {
        setEnabled(true);
        this.menuFrame = Image.getImage(Images.MENU_FRAME);
    }

    public int getMenuWidth() {
        return menuFrame.getWidth();
    }
    public int getMenuHeight() {
        return menuFrame.getHeight();
    }

    public void setOnSwipeDownListener(onSwipeDownListener listener) {
        this.onSwipeDownListener = listener;
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {

    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        int menuWidth = getMenuWidth();
        int menuHeight = getMenuHeight();

        size.setSize(menuWidth, menuHeight);
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Painter.drawImage(g, this.menuFrame, 0, 0);

        super.renderContent(g, contentWidth, contentHeight);
    }

    @Override
    public boolean handleEvent(int event) {
        if (Event.getType(event) != Pointer.EVENT_TYPE) {
            return super.handleEvent(event);
        }

        if (Buttons.isPressed(event)) {
            Pointer pointer = (Pointer) Event.getGenerator(event);
            this.touchStartX = pointer.getX();
            this.touchStartY = pointer.getY();
            return true;
        }

        if (Buttons.isReleased(event)) {
            Pointer pointer = (Pointer) Event.getGenerator(event);
            int deltaY = pointer.getY() - this.touchStartY;
            int deltaX = pointer.getX() - this.touchStartX;

            if (deltaY > 0 && Math.abs(deltaY) > Math.abs(deltaX) && Math.abs(deltaY) > SWIPE_THRESHOLD) {
                if (this.onSwipeDownListener != null) {
                    this.onSwipeDownListener.onSwipeDown();
                }
                return true;
            }
        }

        return super.handleEvent(event);
    };
}
