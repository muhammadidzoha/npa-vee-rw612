package com.nxp.example.smartgreenhouse.view.overview;

import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Icons;
import com.nxp.example.smartgreenhouse.style.Images;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Widget;
import ej.mwt.util.Size;

public class FooterOverview extends Widget {
    private static final String TITLE = "USAP KE ATAS";
    private final Image image;
    private final Image icon;

    public FooterOverview() {
        this.image = Image.getImage(Images.FOOTER_FRAME);
        this.icon = Image.getImage(Icons.SWIPE_UP_ICON_24);
    }

    public int getFooterHeight() {
        return this.image.getHeight();
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        size.setSize(this.image.getWidth(), this.image.getHeight());
    }

    @Override
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Font titleFont = Fonts.jetbrainsMonoBold10px();

        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);

        Painter.drawImage(g, this.image, 0, 0);

        int iconX = (contentWidth - this.icon.getWidth()) / 2;
        Painter.drawImage(g, this.icon, iconX, 1);

        int textWidth = titleFont.stringWidth(TITLE);
        int textX = (contentWidth - textWidth) / 2;
        int textY = 22;

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        Painter.drawString(g, TITLE, titleFont, textX, textY);

    }
}
