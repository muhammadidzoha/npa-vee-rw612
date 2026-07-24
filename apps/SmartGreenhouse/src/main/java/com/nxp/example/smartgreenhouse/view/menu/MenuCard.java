package com.nxp.example.smartgreenhouse.view.menu;

import com.nxp.example.smartgreenhouse.model.menu.MenuItemData;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Images;
import ej.annotation.NonNullByDefault;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Widget;
import ej.mwt.util.Size;

public class MenuCard extends Widget {

    private final Image menuFrame;
    private MenuItemData displayItem;

    private static final int ICON_LEFT_PADDING = 4;

    public MenuCard() {
        this.menuFrame = Image.getImage(Images.CARD_MENU_FRAME);
    }

    public void setDisplayItem(MenuItemData item) {
        this.displayItem = item;
        requestRender();
    }

    public MenuItemData getDisplayItem() {
        return this.displayItem;
    }

    public int getCardWidth() {
        return this.menuFrame.getWidth();
    }

    public int getCardHeight() {
        return this.menuFrame.getHeight();
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        size.setSize(this.menuFrame.getWidth(), this.menuFrame.getHeight());
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        if (this.displayItem == null) {
            return;
        }

        Font titleFont = Fonts.jetbrainsMonoRegular12px();
        Image icon = this.displayItem.getIcon();
        String title = this.displayItem.getTitle();

        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);

        int imageX = (contentWidth - this.menuFrame.getWidth()) / 2;
        int imageY = (contentHeight - this.menuFrame.getHeight()) / 2;
        Painter.drawImage(g, this.menuFrame, imageX, imageY);

        int iconHeight = icon.getHeight();
        int titleHeight = titleFont.getHeight();
        int iconToTitleGap = 6;
        int totalContentHeight = iconHeight + iconToTitleGap + titleHeight;

        int contentStartY = imageY + (this.menuFrame.getHeight() - totalContentHeight) / 2;

        int iconX = imageX + (this.menuFrame.getWidth() - icon.getWidth()) / 2;
        int iconY = contentStartY;
        Painter.drawImage(g, icon, iconX, iconY);

        int titleX = imageX + (this.menuFrame.getWidth() - titleFont.stringWidth(title)) / 2;
        int titleY = iconY + iconHeight + iconToTitleGap;
        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, title, titleFont, titleX, titleY);
    }
}