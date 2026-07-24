package com.nxp.example.smartgreenhouse.view.detail;

import com.nxp.example.smartgreenhouse.model.sensor.SensorDisplayItem;
import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Icons;
import com.nxp.example.smartgreenhouse.style.Images;
import com.nxp.example.smartgreenhouse.view.gauge.GaugeWidget;
import com.nxp.example.smartgreenhouse.view.linechart.ChartPoint;
import com.nxp.example.smartgreenhouse.view.linechart.LineChart;
import ej.microui.display.*;
import ej.mwt.Container;
import ej.mwt.util.Size;
import ej.widget.basic.ImageButton;
import ej.widget.basic.OnClickListener;

public class SensorDetail extends Container {

    public interface onBackListener {
        void onBack();
    }

    private final Image headerFrame;
    private final ImageButton backButton;
    private final GaugeWidget gauge;
    private final LineChart lineChart;

    private SensorDisplayItem selectedSensorItem;

    private onBackListener onBackListener;

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

    private String detailTitle;
    private final Font detailTitleFont;

    public SensorDetail() {
        setEnabled(true);

        this.headerFrame = Image.getImage(Images.HEADER_DETAIL_FRAME);
        this.backButton = new ImageButton(Icons.BACK_ICON_24);
        this.gauge = new GaugeWidget();
        this.lineChart = new LineChart("RIWAYAT (24H)", new ChartPoint[0], true);

        this.selectedSensorItem = null;
        this.detailTitle = "";
        this.detailTitleFont = Fonts.jetbrainsMonoBold12px();

        this.backButton.setOnClickListener(new OnClickListener() {
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
        this.detailTitle = detailTitle == null ? "" : detailTitle;
        requestRender();
    }

    public void setSensorItem(SensorDisplayItem item, double minValue, double maxValue, ChartPoint[] historyPoints) {
        if (item == null) {
            clearSensorItem();
            return;
        }

        this.selectedSensorItem = item;

        this.gauge.setData(item, minValue, maxValue);

        this.lineChart.setTitle("RIWAYAT");
        this.lineChart.setUnit(item.getDefinition().getUnit());

        this.lineChart.setPoints(historyPoints == null ? new ChartPoint[0] : historyPoints);

        requestLayOut();
        requestRender();
    }

    public void clearSensorItem() {
        this.selectedSensorItem = null;

        this.gauge.clearData();
        this.lineChart.clearPoints();
        this.lineChart.setUnit("");

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

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {
        if (this.selectedSensorItem != null) {
            int gaugeY = getGaugeY();

            layOutChild(this.gauge, GAUGE_LEFT, gaugeY, this.gauge.getGaugeWidth(), this.gauge.getGaugeHeight());

            layOutChild(this.lineChart, getLineChartX(), getLineChartY(), this.lineChart.getChartWidth(), this.lineChart.getChartHeight());
        } else {
            layOutChild(this.gauge, 0, contentHeight, this.gauge.getGaugeWidth(), this.gauge.getGaugeHeight());
            layOutChild(this.lineChart, 0, contentHeight, this.lineChart.getChartWidth(), this.lineChart.getChartHeight());
        }

        layOutChild(
                this.backButton,
                PADDING_LEFT_ICON,
                PADDING_TOP_ICON,
                BACK_ICON_SIZE,
                BACK_ICON_SIZE
        );
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

        if (!this.detailTitle.isEmpty()) {
            int titleWidth = this.detailTitleFont.stringWidth(this.detailTitle);
            int titleX = (contentWidth - titleWidth) / 2;
            int titleY = PADDING_TOP_TITLE - TITLE_OFFSET_Y;
            g.setColor(ApplicationColors.PRIMARY_COLOR);
            Painter.drawString(g, this.detailTitle, this.detailTitleFont, titleX, titleY);
        }

        super.renderContent(g, contentWidth, contentHeight);
    }
}
