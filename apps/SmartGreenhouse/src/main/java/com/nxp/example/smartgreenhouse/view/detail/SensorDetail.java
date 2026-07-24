package com.nxp.example.smartgreenhouse.view.detail;

import com.nxp.example.smartgreenhouse.model.sensor.SensorDisplayItem;
import com.nxp.example.smartgreenhouse.model.sensor.SensorHistorySummary;
import com.nxp.example.smartgreenhouse.model.sensor.SensorThreshold;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Icons;
import com.nxp.example.smartgreenhouse.style.Images;
import com.nxp.example.smartgreenhouse.utils.SensorValueFormatter;
import com.nxp.example.smartgreenhouse.view.gauge.GaugeWidget;
import com.nxp.example.smartgreenhouse.view.linechart.ChartPoint;
import com.nxp.example.smartgreenhouse.view.linechart.LineChart;

import ej.microui.display.Display;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.mwt.Container;
import ej.mwt.util.Size;
import ej.widget.basic.ImageButton;
import ej.widget.basic.OnClickListener;

public class SensorDetail extends Container {

    public interface onBackListener {
        void onBack();
    }

    private static final String[] SUMMARY_TITLES = {
            "MIN",
            "MAKS",
            "RATA-RATA"
    };

    private final Image headerFrame;
    private final Image historyCardFrame;
    private final Image historyUpdateCardFrame;
    private final Image optimalFrame;

    private final ImageButton backButton;
    private final GaugeWidget gauge;
    private final LineChart lineChart;

    private SensorDisplayItem selectedSensorItem;
    private SensorHistorySummary historySummary;
    private SensorThreshold sensorThreshold;

    private onBackListener onBackListener;

    private String detailTitle;

    private final Font detailTitleFont;

    private final Font summaryTitleFont;
    private final Font summaryValueFont;
    private final Font summaryValueSmallFont;
    private final Font summaryUnitFont;
    private final Font summaryUnitSmallFont;

    private final Font updateTitleFont;
    private final Font updateValueFont;
    private final Font updateValueSmallFont;

    private final Font optimalTitleFont;
    private final Font optimalValueFont;
    private final Font optimalUnitFont;

    private static final int BACK_ICON_SIZE = 24;

    private static final int PADDING_TOP_HEADER = 26;
    private static final int PADDING_LEFT_ICON = 15;
    private static final int PADDING_TOP_ICON = 3;

    private static final int PADDING_TOP_TITLE = 8;
    private static final int TITLE_OFFSET_Y = 1;

    private static final int GAUGE_LEFT = 16;
    private static final int GAUGE_TOP_GAP_FROM_HEADER = 5;

    private static final int GAUGE_TO_GRAPH_GAP = 15;

    private static final int GRAPH_FRAME_PADDING_LEFT = 0;
    private static final int GRAPH_FRAME_PADDING_TOP = 0;

    private static final int SUMMARY_LEFT = 16;
    private static final int SUMMARY_TOP_GAP_FROM_CHART = 10;
    private static final int SUMMARY_CARD_GAP = 15;
    private static final int SUMMARY_TO_OPTIMAL_GAP = 10;

    private static final int HISTORY_CARD_WIDTH = 81;
    private static final int HISTORY_CARD_HEIGHT = 65;

    private static final int HISTORY_UPDATE_CARD_WIDTH = 160;

    private static final int SUMMARY_TITLE_TOP = 11;
    private static final int SUMMARY_VALUE_TOP = 35;

    private static final int UPDATE_TITLE_TOP = 10;
    private static final int UPDATE_VALUE_TOP = 31;

    private static final int SUMMARY_VALUE_HORIZONTAL_PADDING = 6;
    private static final int UPDATE_VALUE_HORIZONTAL_PADDING = 10;

    private static final int UPDATE_TEXT_LEFT_PADDING = 15;

    private static final int OPTIMAL_FRAME_LEFT = 16;

    private static final int OPTIMAL_TITLE_LEFT_PADDING = 15;
    private static final int OPTIMAL_VALUE_RIGHT_PADDING = 15;
    private static final int OPTIMAL_TITLE_VALUE_OFFSET_Y = 1;

    private static final int OPTIMAL_RANGE_SEPARATOR_GAP = 4;
    private static final int OPTIMAL_VALUE_RIGHT_OFFSET = 5;

    public SensorDetail() {
        setEnabled(true);

        this.headerFrame = Image.getImage(Images.HEADER_DETAIL_FRAME);
        this.historyCardFrame = Image.getImage(Images.HISTORY_CARD_FRAME);
        this.historyUpdateCardFrame = Image.getImage(Images.HISTORY_UPDATE_CARD_FRAME);
        this.optimalFrame = Image.getImage(Images.OPTIMAL_FRAME);

        this.backButton = new ImageButton(Icons.BACK_ICON_24);
        this.gauge = new GaugeWidget();
        this.lineChart = new LineChart("", new ChartPoint[0], true);

        this.selectedSensorItem = null;
        this.historySummary = SensorHistorySummary.empty();
        this.sensorThreshold = null;

        this.detailTitle = "";
        this.detailTitleFont = Fonts.jetbrainsMonoBold12px();

        this.summaryTitleFont = Fonts.jetbrainsMonoBold12px();
        this.summaryValueFont = Fonts.jetbrainsMonoBold12px();
        this.summaryValueSmallFont = Fonts.jetbrainsMonoBold10px();
        this.summaryUnitFont = Fonts.jetbrainsMonoBold10px();
        this.summaryUnitSmallFont = Fonts.jetbrainsMonoBold8px();

        this.updateTitleFont = Fonts.jetbrainsMonoBold12px();
        this.updateValueFont = Fonts.jetbrainsMonoBold16px();
        this.updateValueSmallFont = Fonts.jetbrainsMonoBold14px();

        this.optimalTitleFont = Fonts.jetbrainsMonoBold12px();
        this.optimalValueFont = Fonts.jetbrainsMonoBold10px();
        this.optimalUnitFont = Fonts.jetbrainsMonoBold8px();

        this.backButton.setOnClickListener(
                new OnClickListener() {
                    @Override
                    public void onClick() {
                        if (SensorDetail.this.onBackListener != null) {
                            SensorDetail.this.onBackListener.onBack();
                        }
                    }
                }
        );

        addChild(this.gauge);
        addChild(this.lineChart);
        addChild(this.backButton);
    }

    public void setOnBackListener(onBackListener listener) {
        this.onBackListener = listener;
    }

    public void setDetailTitle(String detailTitle) {
        if (detailTitle == null) {
            this.detailTitle = "";
        } else {
            this.detailTitle = detailTitle;
        }

        requestRender();
    }

    public void setSensorItem(SensorDisplayItem item, double minValue, double maxValue, ChartPoint[] historyPoints, SensorHistorySummary historySummary, SensorThreshold sensorThreshold) {
        if (item == null) {
            clearSensorItem();
            return;
        }

        this.selectedSensorItem = item;
        this.sensorThreshold = sensorThreshold;

        if (historySummary == null) {
            this.historySummary = SensorHistorySummary.empty();
        } else {
            this.historySummary = historySummary;
        }

        this.gauge.setData(item, minValue, maxValue);

        this.lineChart.setTitle("RIWAYAT");
        this.lineChart.setUnit(item.getDefinition().getUnit());
        this.lineChart.setValueDecimalPlaces(item.getDefinition().getDecimalPlace());

        if (historyPoints == null) {
            this.lineChart.setPoints(new ChartPoint[0]);
        } else {
            this.lineChart.setPoints(historyPoints);
        }

        requestLayOut();
        requestRender();
    }

    public void clearSensorItem() {
        this.selectedSensorItem = null;
        this.historySummary = SensorHistorySummary.empty();
        this.sensorThreshold = null;

        this.gauge.clearData();

        this.lineChart.clearPoints();
        this.lineChart.setUnit("");
        this.lineChart.setValueDecimalPlaces(0);

        requestLayOut();
        requestRender();
    }

    private int getGaugeY() {
        return PADDING_TOP_HEADER + this.headerFrame.getHeight() + GAUGE_TOP_GAP_FROM_HEADER;
    }

    private int getLineChartX() {
        return GAUGE_LEFT + this.gauge.getGaugeWidth() + GAUGE_TO_GRAPH_GAP + GRAPH_FRAME_PADDING_LEFT;
    }

    private int getLineChartY() {
        return getGaugeY() + GRAPH_FRAME_PADDING_TOP;
    }

    private int getSummaryCardsY() {
        return getLineChartY() + this.lineChart.getChartHeight() + SUMMARY_TOP_GAP_FROM_CHART;
    }

    private int getOptimalFrameY() {
        return getSummaryCardsY() + HISTORY_CARD_HEIGHT + SUMMARY_TO_OPTIMAL_GAP;
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {
        if (this.selectedSensorItem != null) {
            layOutChild(this.gauge, GAUGE_LEFT, getGaugeY(), this.gauge.getGaugeWidth(), this.gauge.getGaugeHeight());
            layOutChild(this.lineChart, getLineChartX(), getLineChartY(), this.lineChart.getChartWidth(), this.lineChart.getChartHeight());
        } else {
            layOutChild(this.gauge, 0, contentHeight, this.gauge.getGaugeWidth(), this.gauge.getGaugeHeight());
            layOutChild(this.lineChart, 0, contentHeight, this.lineChart.getChartWidth(), this.lineChart.getChartHeight());
        }

        layOutChild(this.backButton, PADDING_LEFT_ICON, PADDING_TOP_ICON, BACK_ICON_SIZE, BACK_ICON_SIZE);
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        int displayWidth = Display.getDisplay().getWidth();
        int displayHeight = Display.getDisplay().getHeight();

        computeChildOptimalSize(this.backButton, BACK_ICON_SIZE, BACK_ICON_SIZE);
        computeChildOptimalSize(this.gauge, this.gauge.getGaugeWidth(), this.gauge.getGaugeHeight());
        computeChildOptimalSize(this.lineChart, this.lineChart.getChartWidth(), this.lineChart.getChartHeight());

        size.setSize(displayWidth, displayHeight);
    }

    @Override
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        g.setColor(ApplicationColors.BACKGROUND);
        Painter.fillRectangle(g, 0, 0, contentWidth, contentHeight);

        Painter.drawImage(g, this.headerFrame, 0, PADDING_TOP_HEADER);

        drawDetailTitle(g, contentWidth);

        if (this.selectedSensorItem != null) {
            drawHistorySummaryCards(g);
            drawOptimalRangeFrame(g);
        }

        super.renderContent(g, contentWidth, contentHeight);
    }

    private void drawDetailTitle(GraphicsContext g, int contentWidth) {
        if (this.detailTitle == null || this.detailTitle.isEmpty()) {
            return;
        }

        int titleWidth = this.detailTitleFont.stringWidth(this.detailTitle);
        int titleX = (contentWidth - titleWidth) / 2;
        int titleY = PADDING_TOP_TITLE - TITLE_OFFSET_Y;
        g.setColor(ApplicationColors.PRIMARY_COLOR);
        Painter.drawString(g, this.detailTitle, this.detailTitleFont, titleX, titleY);
    }

    private void drawHistorySummaryCards(GraphicsContext g) {
        int cardY = getSummaryCardsY();

        for (int i = 0; i < SUMMARY_TITLES.length; i++) {
            int cardX = SUMMARY_LEFT + (i * (HISTORY_CARD_WIDTH + SUMMARY_CARD_GAP));
            drawSmallHistoryCard(g, cardX, cardY, SUMMARY_TITLES[i], getSummaryValue(i));
        }

        int updateCardX = SUMMARY_LEFT + (SUMMARY_TITLES.length * (HISTORY_CARD_WIDTH + SUMMARY_CARD_GAP));

        drawLastUpdateCard(g, updateCardX, cardY);
    }

    private String getSummaryValue(int index) {
        switch (index) {
            case 0:
                return getMinimumText();
            case 1:
                return getMaximumText();
            case 2:
                return getAverageText();
            default:
                return "-";
        }
    }

    private void drawSmallHistoryCard(GraphicsContext g, int cardX, int cardY, String title, String value) {
        Painter.drawImage(g, this.historyCardFrame, cardX, cardY);
        g.setColor(ApplicationColors.SECONDARY_COLOR);

        int titleWidth = this.summaryTitleFont.stringWidth(title);
        int titleX = cardX + ((HISTORY_CARD_WIDTH - titleWidth) / 2);
        int titleY = cardY + SUMMARY_TITLE_TOP;
        Painter.drawString(g, title, this.summaryTitleFont, titleX, titleY);

        String unit = getCurrentUnit();
        int availableWidth = HISTORY_CARD_WIDTH - (SUMMARY_VALUE_HORIZONTAL_PADDING * 2);
        int normalTotalWidth = this.summaryValueFont.stringWidth(value);
        if (!unit.isEmpty()) {
            normalTotalWidth += this.summaryUnitFont.stringWidth(unit);
        }
        boolean useSmallFont = normalTotalWidth > availableWidth;

        Font valueFont = useSmallFont ? this.summaryValueSmallFont : this.summaryValueFont;
        Font unitFont = useSmallFont ? this.summaryUnitSmallFont : this.summaryUnitFont;
        int valueWidth = valueFont.stringWidth(value);
        int unitWidth = 0;

        if (!unit.isEmpty()) {
            unitWidth = unitFont.stringWidth(unit);
        }
        int totalWidth = valueWidth + unitWidth;
        int valueX = cardX + ((HISTORY_CARD_WIDTH - totalWidth) / 2);
        int valueY = cardY + SUMMARY_VALUE_TOP;
        Painter.drawString(g, value, valueFont, valueX, valueY
        );

        if (!unit.isEmpty()) {
            int unitX = valueX + valueWidth;
            int valueBaselineY = valueY + valueFont.getBaselinePosition();
            int unitY = valueBaselineY - unitFont.getBaselinePosition();
            Painter.drawString(g, unit, unitFont, unitX, unitY);
        }
    }

    private void drawLastUpdateCard(GraphicsContext g, int cardX, int cardY) {
        Painter.drawImage(g, this.historyUpdateCardFrame, cardX, cardY);
        g.setColor(ApplicationColors.SECONDARY_COLOR);

        String title = "TERAKHIR UPDATE";
        String value = getLastUpdateText();

        int textX = cardX + UPDATE_TEXT_LEFT_PADDING;
        int titleY = cardY + UPDATE_TITLE_TOP;
        Painter.drawString(g, title, this.updateTitleFont, textX, titleY);

        Font valueFont = getUpdateValueFont(value);
        int valueY = cardY + UPDATE_VALUE_TOP;
        Painter.drawString(g, value, valueFont, textX, valueY);
    }

    private void drawOptimalRangeFrame(GraphicsContext g) {
        int frameX = OPTIMAL_FRAME_LEFT;
        int frameY = getOptimalFrameY();

        Painter.drawImage(g, this.optimalFrame, frameX, frameY);

        g.setColor(ApplicationColors.SECONDARY_COLOR);
        String title = "RENTANG OPTIMAL";
        int titleX = frameX + OPTIMAL_TITLE_LEFT_PADDING;
        int titleY = frameY + ((this.optimalFrame.getHeight() - this.optimalTitleFont.getHeight()) / 2) + OPTIMAL_TITLE_VALUE_OFFSET_Y;
        Painter.drawString(g, title, this.optimalTitleFont, titleX, titleY);

        String minValue = getOptimalMinimumText();
        String maxValue = getOptimalMaximumText();
        String unit = getOptimalUnitText();
        String separator = "-";

        if (unit == null) {
            unit = "";
        }

        int minValueWidth = this.optimalValueFont.stringWidth(minValue);
        int maxValueWidth = this.optimalValueFont.stringWidth(maxValue);
        int separatorWidth = this.optimalValueFont.stringWidth(separator);

        int unitWidth = 0;
        if (!unit.isEmpty()) {
            unitWidth = this.optimalUnitFont.stringWidth(unit);
        }

        int valueY = frameY + ((this.optimalFrame.getHeight() - this.optimalValueFont.getHeight()) / 2) + OPTIMAL_TITLE_VALUE_OFFSET_Y;
        int valueBaselineY = valueY + this.optimalValueFont.getBaselinePosition();
        int unitY = valueBaselineY - this.optimalUnitFont.getBaselinePosition();

        int currentRightX = frameX + this.optimalFrame.getWidth() - OPTIMAL_VALUE_RIGHT_PADDING - OPTIMAL_VALUE_RIGHT_OFFSET;
        if (!unit.isEmpty()) {
            currentRightX -= unitWidth;
            Painter.drawString(g, unit, this.optimalUnitFont, currentRightX, unitY);
        }
        currentRightX -= maxValueWidth;
        Painter.drawString(g, maxValue, this.optimalValueFont, currentRightX, valueY);

        currentRightX -= OPTIMAL_RANGE_SEPARATOR_GAP;
        currentRightX -= separatorWidth;
        Painter.drawString(g, separator, this.optimalValueFont, currentRightX, valueY);

        currentRightX -= OPTIMAL_RANGE_SEPARATOR_GAP;
        if (!unit.isEmpty()) {
            currentRightX -= unitWidth;
            Painter.drawString(g, unit, this.optimalUnitFont, currentRightX, unitY);
        }

        currentRightX -= minValueWidth;
        Painter.drawString(g, minValue, this.optimalValueFont, currentRightX, valueY);
    }

    private Font getUpdateValueFont(String value) {
        if (value == null) {
            return this.updateValueFont;
        }

        int availableWidth = HISTORY_UPDATE_CARD_WIDTH - (UPDATE_VALUE_HORIZONTAL_PADDING * 2);
        int normalWidth = this.updateValueFont.stringWidth(value);

        if (normalWidth <= availableWidth) {
            return this.updateValueFont;
        }

        return this.updateValueSmallFont;
    }

    private String getMinimumText() {
        if (this.historySummary == null || !this.historySummary.hasData()) {
            return "-";
        }

        return formatValue(this.historySummary.getMinimum());
    }

    private String getMaximumText() {
        if (this.historySummary == null || !this.historySummary.hasData()) {
            return "-";
        }

        return formatValue(this.historySummary.getMaximum());
    }

    private String getAverageText() {
        if (this.historySummary == null || !this.historySummary.hasData()) {
            return "-";
        }

        return formatValue(this.historySummary.getAverage());
    }

    private String getLastUpdateText() {
        if (this.historySummary == null || !this.historySummary.hasData()) {
            return "-";
        }

        String lastUpdate = this.historySummary.getLastUpdate();

        if (lastUpdate == null || lastUpdate.isEmpty()) {
            return "-";
        }

        return lastUpdate;
    }

    private String getOptimalMinimumText() {
        if (this.sensorThreshold == null || this.selectedSensorItem == null) {
            return "-";
        }

        return SensorValueFormatter.format(this.selectedSensorItem.getDefinition(), this.sensorThreshold.getOptimalMinimum());
    }

    private String getOptimalMaximumText() {
        if (this.sensorThreshold == null || this.selectedSensorItem == null) {
            return "-";
        }

        return SensorValueFormatter.format(this.selectedSensorItem.getDefinition(), this.sensorThreshold.getOptimalMaximum());
    }

    private String getOptimalUnitText() {
        if (this.selectedSensorItem == null || this.selectedSensorItem.getDefinition() == null) {
            return "";
        }

        String unit = this.selectedSensorItem.getDefinition().getUnit();
        return unit == null ? "" : unit;
    }

    private String formatValue(float value) {
        if (this.selectedSensorItem == null
                || this.selectedSensorItem.getDefinition() == null) {
            return "-";
        }
        return SensorValueFormatter.format(this.selectedSensorItem.getDefinition(), value);
    }

    private String getCurrentUnit() {
        if (this.selectedSensorItem == null || this.selectedSensorItem.getDefinition() == null) {
            return "";
        }

        String unit = this.selectedSensorItem.getDefinition().getUnit();
        return unit == null ? "" : unit;
    }
}