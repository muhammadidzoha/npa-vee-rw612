package com.nxp.example.smartgreenhouse.view.linechart;

import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.style.Images;

import ej.annotation.NonNullByDefault;
import ej.drawing.ShapePainter;
import ej.drawing.ShapePainter.Cap;
import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Image;
import ej.microui.display.Painter;
import ej.microui.event.Event;
import ej.microui.event.generator.Buttons;
import ej.microui.event.generator.Pointer;
import ej.mwt.Widget;
import ej.mwt.util.Alignment;
import ej.mwt.util.Rectangle;
import ej.mwt.util.Size;
import ej.widget.render.StringPainter;

public class LineChart extends Widget {

    private static final int DEFAULT_SCALE_COUNT = 5;
    private static final int DECIMALS_LONG_COUNT = 3;
    private static final String DECIMALS_SEPARATOR = ".";

    private static final int CONTENT_MARGIN = 10;

    private static final int PADDING_Y_BAR = 5;
    private static final int PADDING_X_BAR = 5;
    private static final int SCALE_LINE_DOT_LENGTH = 3;
    private static final int SCALE_LINE_DOT_GAP = 3;

    private static final int POINT_RADIUS = 4;
    private static final int LINE_THICKNESS = 1;
    private static final int LINE_FADE = 1;

    private static final int DECIMAL = 10;

    private final Image grafikFrame;
    private final boolean drawCircle;

    private ChartPoint[] points;
    private String title;
    private String unit;
    private int scaleCount;
    private int selectedChartPointIndex;

    public LineChart(String title, ChartPoint[] points) {
        this(title, points, true);
    }

    public LineChart(String title, ChartPoint[] points, boolean drawCircle) {
        super(true);

        this.grafikFrame = Image.getImage(Images.GRAFIK_FRAME);
        this.drawCircle = drawCircle;

        this.title = title;
        this.points = points;
        this.unit = "";
        this.scaleCount = DEFAULT_SCALE_COUNT;
        this.selectedChartPointIndex = -1;
    }

    public int getChartWidth() {
        return this.grafikFrame.getWidth();
    }

    public int getChartHeight() {
        return this.grafikFrame.getHeight();
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPoints(ChartPoint[] points) {
        this.points = points;
        this.selectedChartPointIndex = -1;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public String getUnit() {
        return this.unit;
    }

    public void setScaleCount(int scaleCount) {
        if (scaleCount <= 0) {
            return;
        }

        this.scaleCount = scaleCount;
    }

    public int getScaleCount() {
        return this.scaleCount;
    }

    public void clearPoints() {
        this.points = new ChartPoint[0];
        this.selectedChartPointIndex = -1;
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        size.setSize(getChartWidth(), getChartHeight());
    }

    @Override
    public boolean handleEvent(int event) {
        int type = Event.getType(event);

        if (type == Pointer.EVENT_TYPE) {
            int action = Buttons.getAction(event);

            if (action == Buttons.RELEASED || action == Pointer.DRAGGED) {
                Pointer pointer = (Pointer) Event.getGenerator(event);

                Rectangle bounds = getContentBounds();
                int localX = pointer.getX() - getAbsoluteX() - bounds.getX();

                selectPointAtLocalX(localX);
                return true;
            }
        }

        return super.handleEvent(event);
    }

    public boolean selectPointAtLocalX(int localX) {
        if (this.points == null || this.points.length == 0) {
            return false;
        }

        Font font = Fonts.jetbrainsMonoRegular12px();

        int chartWidth = getChartWidth() - (CONTENT_MARGIN * 2);
        float topValue = getTopScaleValue();

        int yBarWidth = getYBarWidth(font, topValue);
        int xStart = CONTENT_MARGIN + yBarWidth;
        int xEnd = CONTENT_MARGIN + chartWidth;

        if (localX >= xStart && localX < xEnd) {
            int selectedPoint = this.points.length * (localX - xStart) / (xEnd - xStart);
            selectPoint(selectedPoint);
            return true;
        }

        selectPoint(-1);
        return false;
    }

    public void selectPoint(int pointIndex) {
        if (this.points == null) {
            return;
        }

        if (pointIndex >= this.points.length) {
            return;
        }

        int lastIndex = this.selectedChartPointIndex;

        if (pointIndex != lastIndex) {
            if (lastIndex > -1) {
                ChartPoint oldPoint = this.points[lastIndex];
                oldPoint.setSelected(false);
            }

            this.selectedChartPointIndex = pointIndex;

            if (pointIndex > -1) {
                ChartPoint newPoint = this.points[pointIndex];

                if (newPoint.getValue() < 0.0f) {
                    this.selectedChartPointIndex = -1;
                } else {
                    newPoint.setSelected(true);
                }
            }

            requestRender();
        }
    }

    @Override
    @NonNullByDefault
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Painter.drawImage(g, this.grafikFrame, 0, 0);

        if (this.points == null || this.points.length == 0) {
            return;
        }

        Font font = Fonts.jetbrainsMonoRegular12px();

        int chartWidth = contentWidth - (CONTENT_MARGIN * 2);
        int chartHeight = contentHeight - (CONTENT_MARGIN * 2);

        renderChartContent(g, font, chartWidth, chartHeight);
    }

    private void renderChartContent(GraphicsContext g, Font font, int chartWidth, int chartHeight) {
        float topValue = getTopScaleValue();

        int fontHeight = font.getHeight();
        int yBarWidth = getYBarWidth(font, topValue);
        int xBarHeight = fontHeight + PADDING_X_BAR;
        int yBarTopHeight = getTopBarHeight(fontHeight);
        int yBarBottom = LineChart.CONTENT_MARGIN + chartHeight - xBarHeight;
        int innerChartWidth = chartWidth - yBarWidth;

        g.setColor(ApplicationColors.SECONDARY_COLOR);

        StringPainter.drawStringInArea(g, this.unit, font, LineChart.CONTENT_MARGIN, LineChart.CONTENT_MARGIN, yBarWidth - PADDING_Y_BAR, yBarTopHeight, Alignment.RIGHT, Alignment.TOP);

        drawSelectedPointInfo(g, font, chartWidth, yBarTopHeight);

        int numScaleValues = this.scaleCount;

        for (int i = 0; i < numScaleValues + 1; i++) {
            float scaleValue = topValue * i / numScaleValues;
            String scaleString = toStringFloat(scaleValue, 0);

            int yScale = yBarBottom + ((LineChart.CONTENT_MARGIN + yBarTopHeight - yBarBottom) * i) / numScaleValues;

            g.setColor(ApplicationColors.SECONDARY_COLOR);

            StringPainter.drawStringAtPoint(g, scaleString, font, LineChart.CONTENT_MARGIN + yBarWidth - PADDING_Y_BAR, yScale, Alignment.RIGHT, Alignment.VCENTER);

            drawHorizontalDottedLine(g, LineChart.CONTENT_MARGIN + yBarWidth, yScale, innerChartWidth);
        }

        Rectangle chartBounds = new Rectangle(LineChart.CONTENT_MARGIN + yBarWidth, LineChart.CONTENT_MARGIN + yBarTopHeight, innerChartWidth, chartHeight - yBarTopHeight - xBarHeight);

        renderPointsAndLabel(g, font, chartBounds, LineChart.CONTENT_MARGIN + chartHeight, topValue);
    }

    private void drawSelectedPointInfo(GraphicsContext g, Font font, int chartWidth, int height) {
        String text;

        if (this.selectedChartPointIndex > -1) {
            ChartPoint selectedPoint = this.points[this.selectedChartPointIndex];

            text = selectedPoint.getFullName() + ": " + toStringFloat(selectedPoint.getValue(), DECIMALS_LONG_COUNT);
        } else {
            text = this.title;
        }

        g.setColor(ApplicationColors.SECONDARY_COLOR);

        StringPainter.drawStringInArea(g, text, font, LineChart.CONTENT_MARGIN, LineChart.CONTENT_MARGIN, chartWidth, height, Alignment.HCENTER, Alignment.TOP);
    }

    private void renderPointsAndLabel(GraphicsContext g, Font font, Rectangle chartBounds, int contentBottom, float topValue) {
        ChartPoint[] points = this.points;

        int yBottom = chartBounds.getY() + chartBounds.getHeight();
        float xStep = getStepSize(chartBounds.getWidth());
        float xPosStart = chartBounds.getX() + xStep / 2;

        int previousX = -1;
        int previousY = -1;
        float xPos = xPosStart;

        for (ChartPoint chartPoint : points) {
            float value = chartPoint.getValue();
            int currentX = (int) xPos;
            xPos += xStep;

            g.setColor(ApplicationColors.SECONDARY_COLOR);

            StringPainter.drawStringAtPoint(g, chartPoint.getName(), font, currentX, contentBottom, Alignment.HCENTER, Alignment.BOTTOM);

            int finalLength = (int) ((yBottom - chartBounds.getY()) * value / topValue);
            int currentY = yBottom - finalLength;

            if (previousY != -1) {
                g.setColor(ApplicationColors.LINE_COLOR);

                ShapePainter.drawThickFadedLine(g, previousX, previousY, currentX, currentY, LINE_THICKNESS, LINE_FADE, Cap.NONE, Cap.NONE);
            }

            previousX = currentX;
            previousY = currentY;
        }

        if (this.drawCircle) {
            xPos = xPosStart;

            for (int i = 0; i < points.length; i++) {
                ChartPoint chartPoint = points[i];

                float value = chartPoint.getValue();
                int currentX = (int) xPos;
                xPos += xStep;

                int finalLength = (int) ((yBottom - chartBounds.getY()) * value / topValue);
                int currentY = yBottom - finalLength;

                int color;

                if (chartPoint.isSelected()) {
                    color = ApplicationColors.POINT_SELECTED_COLOR;
                } else {
                    color = ApplicationColors.POINT_COLOR;
                }

                g.setColor(color);

                ShapePainter.drawThickFadedPoint(g, currentX, currentY, POINT_RADIUS * 2, LINE_FADE);
            }
        }
    }

    private void drawHorizontalDottedLine(GraphicsContext g, int x, int y, int width) {
        g.setColor(ApplicationColors.GRAPH_LINE_COLOR);

        int endX = x + width;

        for (int currentX = x; currentX < endX; currentX += SCALE_LINE_DOT_LENGTH + SCALE_LINE_DOT_GAP) {
            int lineEndX = currentX + SCALE_LINE_DOT_LENGTH;

            if (lineEndX > endX) {
                lineEndX = endX;
            }

            Painter.drawLine(g, currentX, y, lineEndX, y);
        }
    }

    private int getTopBarHeight(int fontHeight) {
        return fontHeight + fontHeight / 2;
    }

    private int getYBarWidth(Font font, float topValue) {
        String topValueString = toStringFloat(topValue, 0);

        int widthUnit = font.stringWidth(this.unit);
        int widthValue = font.stringWidth(topValueString);

        return max(widthUnit, widthValue) + PADDING_Y_BAR;
    }

    protected float getStepSize(int width) {
        if (this.points == null || this.points.length == 0) {
            return 0;
        }

        return (float) width / this.points.length;
    }

    protected static String toStringFloat(float value, int decimals) {
        StringBuilder builder = new StringBuilder();

        builder.append((int) value);

        if (decimals > 0) {
            builder.append(DECIMALS_SEPARATOR);

            for (int i = 0; i < decimals; i++) {
                value *= DECIMAL;
                builder.append((int) value % DECIMAL);
            }
        }

        return builder.toString();
    }

    private float getMaxPointValue() {
        float maxValue = 0.0f;

        ChartPoint[] points = this.points;

        for (ChartPoint point : points) {
            if (point.getValue() > maxValue) {
                maxValue = point.getValue();
            }
        }

        return maxValue;
    }

    private float getTopScaleValue() {
        int numValues = this.scaleCount;
        float value = getMaxPointValue();
        float multiplier = 1.0f;

        if (value <= 0) {
            return 100;
        }

        while (value < DECIMAL) {
            value *= DECIMAL;
            multiplier /= DECIMAL;
        }

        while (value > DECIMAL * (2 * numValues)) {
            value /= DECIMAL;
            multiplier *= DECIMAL;
        }

        int n = (int) Math.ceil(value);
        int extra = n % numValues;

        if (extra > 0) {
            n += numValues - extra;
        }

        return n * multiplier;
    }

    private int max(int valueA, int valueB) {
        return Math.max(valueA, valueB);
    }
}