package com.nxp.example.smartgreenhouse.view.overview;

import com.nxp.example.smartgreenhouse.model.SensorDisplayItem;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.view.SwipeListener;
import ej.annotation.NonNullByDefault;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Painter;
import ej.microui.event.Event;
import ej.microui.event.generator.Buttons;
import ej.microui.event.generator.Pointer;
import ej.widget.container.Grid;

public class Overview extends Grid {

    private static final int COLUMN_COUNT = 3;
    private static final int SWIPE_THRESHOLD = 50;

    private int touchStartX;
    private int touchStartY;

    private final SensorCard[] sensorCards;
    private SwipeListener swipeListener;

    public Overview(int sensorCount) {
        super(true, COLUMN_COUNT);
        setEnabled(true);

        this.sensorCards = new SensorCard[sensorCount];

        for (int i = 0; i < sensorCount; i++) {
            this.sensorCards[i] = new SensorCard();
            addChild(this.sensorCards[i]);
        }
    }

    public void setOnSwipeListener(SwipeListener swipeListener) {
        this.swipeListener = swipeListener;
    }

    public void setItems(SensorDisplayItem[] items) {
        int count = Math.min(items.length, this.sensorCards.length);

        for (int i = 0; i < count; i++) {
            this.sensorCards[i].setDisplayItem(items[i]);
        }

        requestRender();
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);

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
            int x = pointer.getX();
            int y = pointer.getY();

            int deltaX = x - this.touchStartX;
            int deltaY = y - this.touchStartY;

            if (Math.abs(deltaX) > Math.abs(deltaY) && Math.abs(deltaX) > SWIPE_THRESHOLD) {
                if (deltaX < 0 && this.swipeListener != null) {
                    this.swipeListener.onSwipeLeft();
                } else if (deltaX > 0 && this.swipeListener != null) {
                    this.swipeListener.onSwipeRight();
                }
                return true;
            }
        }

        return super.handleEvent(event);
    }
}