package com.nxp.example.smartgreenhouse.views.wifi.scroll;

import ej.annotation.NonNullByDefault;
import ej.annotation.Nullable;
import ej.bon.XMath;
import ej.mwt.Container;
import ej.mwt.Widget;
import ej.mwt.animation.Animator;
import ej.mwt.util.Size;
import ej.widget.swipe.SwipeEventHandler;
import ej.widget.swipe.SwipeListener;
import ej.widget.swipe.Swipeable;

public class Scroll extends Container {

    private @Nullable Widget child;
    private @Nullable Scrollable scrollableChild;

    private @Nullable SwipeEventHandler swipeEventHandler;
    private final ScrollAssistant assistant;
    private int value;
    private final boolean allowExcess;

    public Scroll() {
        super(true);
        this.assistant = new ScrollAssistant();
        this.allowExcess = true;
    }

    public void setChild(Widget child) {
        Widget oldChild = this.child;
        if (child != oldChild) {
            if (oldChild != null) {
                replaceChild(getChildIndex(oldChild), child);
            } else {
                insertChild(child, 0);
            }

            this.child = child;
            if (child instanceof Scrollable) {
                this.scrollableChild = (Scrollable) child;
            } else {
                this.scrollableChild = null;
            }
        }
    }

    @Override
    protected void setShownChildren() {
        Widget child = this.child;
        if (child != null) {
            setShownChild(child);
        }
    }

    @Override
    @NonNullByDefault
    protected void computeContentOptimalSize(Size size) {
        int width = 0;
        int height = 0;

        Widget child = this.child;
        if (child != null) {
            computeChildOptimalSize(child, size.getWidth(), size.getHeight());
            width = child.getWidth();
            height = child.getHeight();
        }

        size.setSize(width, height);
    }

    @Override
    protected void layOutChildren(int contentWidth, int contentHeight) {
        Scrollable scrollableChild = this.scrollableChild;
        if (scrollableChild != null) {
            scrollableChild.initializeViewport(contentWidth, contentHeight);
        }

        Widget child = this.child;
        int childOptimalHeight = 0;
        if (child != null) {
            childOptimalHeight = child.getHeight();
        }

        int excess = childOptimalHeight - contentHeight;
        if (excess > 0) {
            SwipeEventHandler swipeEventHandler = this.swipeEventHandler;
            if (swipeEventHandler != null) {
                swipeEventHandler.stop();
            }

            Animator animator = getDesktop().getAnimator();
            swipeEventHandler = new SwipeEventHandler(this, excess, false, false, this.assistant, animator);
            swipeEventHandler.setSwipeListener(this.assistant);
            swipeEventHandler.moveTo(this.value);
            this.swipeEventHandler = swipeEventHandler;
        }

        if (child != null) {
            layOutChild(child, 0, 0, contentWidth, childOptimalHeight);
        }

        int childCoordinate = -this.value;
        updateViewport(childCoordinate);
    }

    @Override
    protected void onHidden() {
        super.onHidden();
        SwipeEventHandler swipeEventHandler = this.swipeEventHandler;
        if (swipeEventHandler != null) {
            swipeEventHandler.stop();
            swipeEventHandler.moveTo(limit(this.value));
        }
    }

    @Override
    public boolean handleEvent(int event) {
        SwipeEventHandler swipeEventHandler = this.swipeEventHandler;
        if (swipeEventHandler != null && swipeEventHandler.handleEvent(event)) {
            return true;
        }
        return super.handleEvent(event);
    }

    public void reset() {
        this.value = 0;

        SwipeEventHandler handler = this.swipeEventHandler;

        if (handler != null) {
            handler.stop();
            handler.moveTo(0);
        }

        shift();

        requestLayOut();
        requestRender();
    }

    private int limit(int position) {
        int max = 0;
        Widget child = this.child;
        if (child != null) {
            max = Math.max(0, child.getHeight() - getContentHeight());
        }
        return XMath.limit(position, 0, max);
    }

    private void updateViewport(int childCoordinate) {
        Widget child = this.child;
        if (child != null) {
            child.setPosition(child.getX(), childCoordinate);
        }
        Scrollable scrollableChild = this.scrollableChild;
        if (scrollableChild != null) {
            scrollableChild.updateViewport(0, childCoordinate);
        }
    }

    private void shift() {
        if (isShown()) {
            int childCoordinate = -this.value;
            updateViewport(childCoordinate);
        }
    }

    class ScrollAssistant implements Swipeable, SwipeListener {

        @Override
        public void onSwipeStarted() {
        }

        @Override
        public void onSwipeStopped() {
        }

        @Override
        public void onMove(int position) {
            Scroll scroll = Scroll.this;
            if (scroll.value != position) {
                scroll.value = scroll.allowExcess ? position : limit(position);
                scroll.shift();
                requestRender();
            }
        }

    }

}