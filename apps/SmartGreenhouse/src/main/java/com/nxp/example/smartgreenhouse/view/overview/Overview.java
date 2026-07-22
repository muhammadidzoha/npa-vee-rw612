package com.nxp.example.smartgreenhouse.view.overview;

import com.nxp.example.smartgreenhouse.model.SensorDisplayItem;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import ej.annotation.NonNullByDefault;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Painter;
import ej.widget.container.Grid;

public class Overview extends Grid {

    private static final int COLUMN_COUNT = 3;

    private final SensorCard[] sensorCards;

    public Overview(int sensorCount) {
        super(true, COLUMN_COUNT);

        this.sensorCards = new SensorCard[sensorCount];

        for (int i = 0; i < sensorCount; i++) {
            this.sensorCards[i] = new SensorCard();
            addChild(this.sensorCards[i]);
        }
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
}