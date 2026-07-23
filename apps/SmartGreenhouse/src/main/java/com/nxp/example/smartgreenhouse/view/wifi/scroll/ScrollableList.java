package com.nxp.example.smartgreenhouse.view.wifi.scroll;

import ej.annotation.NonNull;
import ej.microui.display.GraphicsContext;
import ej.mwt.Widget;
import ej.widget.container.LayoutOrientation;
import ej.widget.container.List;

public class ScrollableList extends List implements Scrollable {

    private int viewportHeight;
    private int firstVisibleChildIndex;
    private int lastVisibleChildIndex;

    public ScrollableList() {
        super(LayoutOrientation.VERTICAL);
        this.lastVisibleChildIndex = -1;
    }

    @Override
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        int translateX = g.getTranslationX();
        int translateY = g.getTranslationY();
        int x = g.getClipX();
        int y = g.getClipY();
        int width = g.getClipWidth();
        int height = g.getClipHeight();

        Widget[] children = getChildren();
        int first = this.firstVisibleChildIndex;
        int last = this.lastVisibleChildIndex;
        for (int i = first; i <= last; i++) {
            Widget child = children[i];
            assert child != null;
            renderChild(child, g);
            if (i < last) {
                g.setTranslation(translateX, translateY);
                g.setClip(x, y, width, height);
            }
        }
    }

    @Override
    protected void setShownChildren() {
        int first = this.firstVisibleChildIndex;
        int last = this.lastVisibleChildIndex;
        if (last < first) {
            last = getChildrenCount() - 1;
        }
        for (int i = first; i <= last; i++) {
            setShownChild(getChild(i));
        }
    }

    @Override
    public void initializeViewport(int width, int height) {
        this.viewportHeight = height;
    }

    @Override
    public void updateViewport(int x, int y) {
        if (isShown()) {
            removeNoLongerVisibleItems(y);
        }
        addNewlyVisibleItems(y);
    }

    @Override
    public @NonNull int[] getItemSizes() {
        Widget[] children = getChildren();
        int length = children.length;
        int[] sizes = new int[length];
        for (int i = 0; i < length; i++) {
            Widget child = children[i];
            assert child != null;
            sizes[i] = child.getHeight();
        }
        return sizes;
    }

    private void addNewlyVisibleItems(int y) {
        Widget[] children = getChildren();
        int size = children.length;
        if (size > 0) {
            boolean shown = isShown();
            int i = getFirstVisible(y);
            for (; i < size; i++) {
                Widget child = children[i];
                int childY = child.getY();
                if (childY + y >= this.viewportHeight) {
                    break;
                } else if (shown) {
                    setShownChild(child);
                }
            }
            this.lastVisibleChildIndex = i - 1;
        }
    }

    private void removeNoLongerVisibleItems(int y) {
        Widget[] children = getChildren();
        int size = children.length;
        if (size == 0) {
            return;
        }
        for (int i = this.firstVisibleChildIndex; i < size; i++) {
            Widget child = children[i];
            if (!child.isShown()) {
                break;
            }
            int childY = child.getY();
            if (childY + y >= this.viewportHeight || childY + child.getHeight() <= -y) {
                setHiddenChild(child);
            }
        }
    }

    private int getFirstVisible(int y) {
        int index = this.firstVisibleChildIndex;
        Widget prev = getChild(index);
        int childY = prev.getY();
        int childHeight = prev.getHeight();
        boolean stillFirst = childY < -y && childY + childHeight > -y;
        if (!stillFirst) {
            boolean searchForward = childY + childHeight <= -y;
            Widget[] children = getChildren();
            int size = children.length;
            int firstCandidate = searchForward ? size - 1 : 0;
            for (int i = this.firstVisibleChildIndex; i >= 0 && i < size; i += (searchForward ? 1 : -1)) {
                Widget child = children[i];
                int candidateY = child.getY();
                int candidateHeight = child.getHeight();
                if (candidateY <= -y && candidateY + candidateHeight > -y) {
                    firstCandidate = i;
                    break;
                }
            }
            this.firstVisibleChildIndex = firstCandidate;
        }
        return this.firstVisibleChildIndex;
    }

}