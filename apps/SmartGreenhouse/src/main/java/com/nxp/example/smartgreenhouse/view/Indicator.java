package com.nxp.example.smartgreenhouse.view;

import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Images;
import ej.annotation.NonNullByDefault;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Widget;
import ej.mwt.util.Size;

public class Indicator extends Widget {

    private int total;
    private int selected;

    private final Image activeDot;
    private final Image inactiveDot;

    private static final int DOT_GAP = 3;

    public Indicator() {
        this.total = 0;
        this.selected = 0;
        this.activeDot = Image.getImage(Images.DOT_ACTIVE);
        this.inactiveDot = Image.getImage(Images.DOT_INACTIVE);
    }

    public int getIndicatorHeight() {
        return this.activeDot.getHeight();
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
        requestRender();
    }

    public int getSelected() {
        return selected;
    }

    public void setSelected(int selected) {
        this.selected = selected;
        requestRender();
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        int dotWidth = this.activeDot.getWidth();
        int dotHeight = this.activeDot.getHeight();
        int totalWidth = (dotWidth * this.total) + (DOT_GAP * (this.total - 1));

        size.setSize(totalWidth, dotHeight);
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);

        int dotWidth = this.activeDot.getWidth();
        int dotHeight = this.activeDot.getHeight();
        int totalWidth = (dotWidth * this.total) + (DOT_GAP * (this.total - 1));
        int y = (contentHeight - dotHeight) / 2;
        int x = (contentWidth - totalWidth) / 2;

        for (int i = 0; i < this.total; i++) {
            Image dot = (i == this.selected) ? this.activeDot : this.inactiveDot;
            Painter.drawImage(g, dot, x, y);
            x += dotWidth + DOT_GAP;
        }
    }
}
