package com.nxp.example.smartgreenhouse.views.wifi;

import com.nxp.example.smartgreenhouse.style.ApplicationColors;
import com.nxp.example.smartgreenhouse.style.Fonts;
import com.nxp.example.smartgreenhouse.views.keyboard.Key;
import com.nxp.example.smartgreenhouse.views.keyboard.Keyboard;

import ej.microui.display.Font;
import ej.mwt.style.EditableStyle;
import ej.mwt.style.background.NoBackground;
import ej.mwt.style.background.RoundedBackground;
import ej.mwt.style.outline.FlexibleOutline;
import ej.mwt.style.outline.border.RoundedBorder;
import ej.mwt.stylesheet.cascading.CascadingStylesheet;
import ej.mwt.stylesheet.selector.ClassSelector;
import ej.mwt.stylesheet.selector.StateSelector;
import ej.mwt.stylesheet.selector.TypeSelector;
import ej.mwt.stylesheet.selector.combinator.AndCombinator;
import ej.mwt.util.Alignment;

public final class WifiKeyboardStyles {

    public static final int SPACE_KEY_SELECTOR = 2400;
    public static final int SHIFT_KEY_INACTIVE_SELECTOR = 2401;
    public static final int SHIFT_KEY_ACTIVE_SELECTOR = 2402;
    public static final int SWITCH_MAPPING_KEY_SELECTOR = 2403;

    private static final int KEYBOARD_BACKGROUND = 0x20282A;
    private static final int KEY_TEXT_COLOR = 0xE2E6E8;
    private static final int KEY_ACTIVE_BACKGROUND = 0x394448;

    private static final int PASSWORD_BORDER_COLOR = 0xBFD9FC;

    private static final int KEY_CORNER_RADIUS = 5;
    private static final int KEY_BORDER_SIZE = 1;

    private static final int PASSWORD_CORNER_RADIUS = 5;
    private static final int PASSWORD_BORDER_SIZE = 2;

    private WifiKeyboardStyles() {}

    public static void populate(CascadingStylesheet stylesheet) {
        Font keyFont = Fonts.jetbrainsMonoBold10px();

        TypeSelector keyboardSelector = new TypeSelector(Keyboard.class);
        EditableStyle keyboardStyle = stylesheet.getSelectorStyle(keyboardSelector);

        keyboardStyle.setBackground(new RoundedBackground(KEYBOARD_BACKGROUND, 8, 1));

        TypeSelector keySelector = new TypeSelector(Key.class);
        EditableStyle keyStyle = stylesheet.getSelectorStyle(keySelector);

        keyStyle.setFont(keyFont);
        keyStyle.setColor(KEY_TEXT_COLOR);
        keyStyle.setBackground(NoBackground.NO_BACKGROUND);

        keyStyle.setHorizontalAlignment(Alignment.HCENTER);
        keyStyle.setVerticalAlignment(Alignment.VCENTER);
        keyStyle.setMargin(new FlexibleOutline(1, 1, 1, 1));

        StateSelector activeKeyState = new StateSelector(Key.ACTIVE);
        EditableStyle activeKeyStyle = stylesheet.getSelectorStyle(new AndCombinator(keySelector, activeKeyState));

        activeKeyStyle.setColor(ApplicationColors.BACKGROUND);

        activeKeyStyle.setBackground(new RoundedBackground(KEY_ACTIVE_BACKGROUND, KEY_CORNER_RADIUS, KEY_BORDER_SIZE));
        activeKeyStyle.setBorder(new RoundedBorder(KEY_ACTIVE_BACKGROUND, KEY_CORNER_RADIUS, KEY_BORDER_SIZE));

        ClassSelector spaceSelector = new ClassSelector(SPACE_KEY_SELECTOR);
        EditableStyle spaceStyle = stylesheet.getSelectorStyle(spaceSelector);

        spaceStyle.setBackground(new RoundedBackground(ApplicationColors.BACKGROUND, KEY_CORNER_RADIUS, KEY_BORDER_SIZE));
        spaceStyle.setBorder(new RoundedBorder(ApplicationColors.BACKGROUND, KEY_CORNER_RADIUS, KEY_BORDER_SIZE));

        ClassSelector activeShiftSelector = new ClassSelector(SHIFT_KEY_ACTIVE_SELECTOR);
        EditableStyle activeShiftStyle = stylesheet.getSelectorStyle(activeShiftSelector);

        activeShiftStyle.setColor(ApplicationColors.BACKGROUND);
        activeShiftStyle.setBackground(new RoundedBackground(ApplicationColors.PRIMARY_COLOR, KEY_CORNER_RADIUS, KEY_BORDER_SIZE));
        activeShiftStyle.setBorder(new RoundedBorder(ApplicationColors.PRIMARY_COLOR, KEY_CORNER_RADIUS, KEY_BORDER_SIZE));

        ClassSelector inactiveShiftSelector = new ClassSelector(SHIFT_KEY_INACTIVE_SELECTOR);
        EditableStyle inactiveShiftStyle = stylesheet.getSelectorStyle(inactiveShiftSelector);

        inactiveShiftStyle.setColor(KEY_TEXT_COLOR);

        ClassSelector mappingSelector = new ClassSelector(SWITCH_MAPPING_KEY_SELECTOR);
        EditableStyle mappingStyle = stylesheet.getSelectorStyle(mappingSelector);

        mappingStyle.setFont(Fonts.jetbrainsMonoBold8px());
        mappingStyle.setColor(KEY_TEXT_COLOR);

        TypeSelector passwordSelector = new TypeSelector(WifiPasswordField.class);
        EditableStyle passwordStyle = stylesheet.getSelectorStyle(passwordSelector);

        passwordStyle.setFont(Fonts.jetbrainsMonoBold12px());
        passwordStyle.setColor(ApplicationColors.SECONDARY_COLOR);
        passwordStyle.setBackground(new RoundedBackground(ApplicationColors.BACKGROUND, PASSWORD_CORNER_RADIUS, PASSWORD_BORDER_SIZE));
        passwordStyle.setBorder(new RoundedBorder(PASSWORD_BORDER_COLOR, PASSWORD_CORNER_RADIUS, PASSWORD_BORDER_SIZE));
        passwordStyle.setPadding(new FlexibleOutline(4, 8, 4, 8));
    }
}