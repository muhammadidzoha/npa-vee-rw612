package com.nxp.example.smartgreenhouse.views.wifi;

import com.nxp.example.smartgreenhouse.views.keyboard.ControlCharacters;
import com.nxp.example.smartgreenhouse.views.keyboard.KeyboardEventGenerator;

import ej.microui.display.Font;
import ej.microui.display.GraphicsContext;
import ej.microui.display.Painter;
import ej.microui.event.Event;
import ej.microui.event.EventHandler;
import ej.microui.event.generator.Buttons;
import ej.microui.event.generator.Pointer;
import ej.mwt.Widget;
import ej.mwt.style.Style;
import ej.mwt.util.Size;

public final class WifiPasswordField extends Widget implements EventHandler {

    private static final int DEFAULT_WIDTH = 220;
    private static final int DEFAULT_HEIGHT = 30;

    private static final int MAX_PASSWORD_LENGTH = 63;

    private static final char MASK_CHARACTER = '*';

    private static final int CARET_WIDTH = 1;

    private final StringBuilder buffer;

    private int caret;

    public WifiPasswordField() {
        super(true);

        this.buffer = new StringBuilder();

        this.caret = 0;
    }

    public String getPassword() {
        return this.buffer.toString();
    }

    public int getPasswordLength() {
        return this.buffer.length();
    }

    public boolean isEmpty() {
        return this.buffer.length() == 0;
    }

    public void clear() {
        this.buffer.setLength(0);
        this.caret = 0;

        requestRender();
    }

    @Override
    public boolean handleEvent(int event) {
        int eventType = Event.getType(event);

        if (eventType == KeyboardEventGenerator.EVENTGENERATOR_ID) {
            KeyboardEventGenerator generator = (KeyboardEventGenerator) Event.getGenerator(event);

            char character = generator.getChar(event);
            handleKeyboardCharacter(character);

            return true;
        }

        if (eventType == Pointer.EVENT_TYPE && Buttons.isPressed(event)) {
            this.caret = this.buffer.length();

            requestRender();

            return true;
        }

        return super.handleEvent(event);
    }

    private void handleKeyboardCharacter(char character) {
        switch (character) {
            case ControlCharacters.BACK_SPACE:
                removePreviousCharacter();
                break;
            case ControlCharacters.VK_LEFT:
                moveCaretLeft();
                break;
            case ControlCharacters.VK_RIGHT:
                moveCaretRight();
                break;
            default:
                insertCharacter(character);
                break;
        }

        requestRender();
    }

    private void insertCharacter(char character) {
        if (this.buffer.length() >= MAX_PASSWORD_LENGTH) {
            return;
        }

        this.buffer.insert(this.caret, character);

        this.caret++;
    }

    private void removePreviousCharacter() {
        if (this.caret <= 0 || this.buffer.length() == 0) {
            return;
        }

        int removeIndex = this.caret - 1;

        this.buffer.deleteCharAt(removeIndex
        );

        this.caret = removeIndex;
    }

    private void moveCaretLeft() {
        if (this.caret > 0) {
            this.caret--;
        }
    }

    private void moveCaretRight() {
        if (this.caret < this.buffer.length()) {
            this.caret++;
        }
    }

    @Override
    protected void computeContentOptimalSize(Size size) {
        size.setSize(DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    @Override
    protected void renderContent(GraphicsContext g, int contentWidth, int contentHeight) {
        Style style = getStyle();

        Font font = style.getFont();

        int characterWidth = Math.max(1, font.charWidth(MASK_CHARACTER));

        int maximumVisibleCharacters = Math.max(1, contentWidth / characterWidth);

        int visibleStart = calculateVisibleStart(maximumVisibleCharacters);

        int visibleEnd = Math.min(this.buffer.length(), visibleStart + maximumVisibleCharacters);

        String maskedText = createMaskedText(visibleEnd - visibleStart);

        int textY = (contentHeight - font.getHeight()) / 2;

        g.setColor(style.getColor());
        Painter.drawString(g, maskedText, font, 0, textY);

        int visibleCaret = this.caret - visibleStart;

        if (visibleCaret < 0) {
            visibleCaret = 0;
        }

        if (visibleCaret > maskedText.length()) {

            visibleCaret = maskedText.length();
        }

        int caretX = font.substringWidth(maskedText, 0, visibleCaret);

        Painter.fillRectangle(g, caretX, textY, CARET_WIDTH, font.getHeight());
    }

    private int calculateVisibleStart(int maximumVisibleCharacters) {
        int length = this.buffer.length();

        if (length <= maximumVisibleCharacters) {
            return 0;
        }

        int start = length - maximumVisibleCharacters;
        if (this.caret < start) {
            start = this.caret;
        } else if (this.caret > start + maximumVisibleCharacters) {
            start = this.caret - maximumVisibleCharacters;
        }

        return Math.max(0, start);
    }

    private static String createMaskedText(int length) {
        if (length <= 0) {
            return "";
        }

        char[] characters = new char[length];

        for (int i = 0; i < characters.length; i++) {
            characters[i] = MASK_CHARACTER;
        }

        return new String(characters);
    }
}