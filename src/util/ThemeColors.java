package util;

import java.awt.Color;
import java.awt.Font;

public class ThemeColors {
    // Primary palette
    public static final Color PRIMARY = new Color(59, 130, 246);
    public static final Color PRIMARY_HOVER = new Color(37, 99, 235);
    public static final Color PRIMARY_LIGHT = new Color(219, 234, 254);

    // Backgrounds
    public static final Color BG_MAIN = new Color(243, 244, 246);
    public static final Color BG_CARD = Color.WHITE;
    public static final Color BG_SIDEBAR = Color.WHITE;
    public static final Color BG_HEADER = new Color(243, 244, 246);

    // Borders
    public static final Color BORDER = new Color(226, 232, 240);
    public static final Color BORDER_DARK = new Color(203, 213, 225);

    // Text
    public static final Color TEXT_PRIMARY = new Color(30, 41, 59);
    public static final Color TEXT_SECONDARY = new Color(100, 116, 139);
    public static final Color TEXT_MUTED = new Color(148, 163, 184);

    // Status colors
    public static final Color SUCCESS = new Color(34, 197, 94);
    public static final Color SUCCESS_BG = new Color(220, 252, 231);
    public static final Color WARNING = new Color(245, 158, 11);
    public static final Color WARNING_BG = new Color(254, 243, 199);
    public static final Color DANGER = new Color(239, 68, 68);
    public static final Color DANGER_BG = new Color(254, 226, 226);
    public static final Color INFO = new Color(59, 130, 246);
    public static final Color INFO_BG = new Color(219, 234, 254);

    // Expiry priority colors
    public static final Color EXPIRY_CRITICAL_BG = new Color(254, 226, 226);   // <30 days - red tint
    public static final Color EXPIRY_WARNING_BG = new Color(255, 237, 213);    // <90 days - orange tint

    // Category badge colors
    public static final Color PURPLE = new Color(147, 51, 234);
    public static final Color PURPLE_BG = new Color(243, 232, 255);
    public static final Color ORANGE = new Color(234, 88, 12);
    public static final Color ORANGE_BG = new Color(255, 237, 213);
    public static final Color INDIGO = new Color(79, 70, 229);
    public static final Color INDIGO_BG = new Color(224, 231, 255);
    public static final Color TEAL = new Color(20, 184, 166);
    public static final Color TEAL_BG = new Color(204, 251, 241);

    // Fonts
    public static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 24);
    public static final Font FONT_SUBTITLE = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SECTION = new Font("Segoe UI", Font.BOLD, 18);
    public static final Font FONT_CARD_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    public static final Font FONT_BODY = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_SMALL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_TABLE_HEADER = new Font("Segoe UI", Font.BOLD, 12);
    public static final Font FONT_TABLE = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_BUTTON = new Font("Segoe UI", Font.BOLD, 13);
    public static final Font FONT_STAT_VALUE = new Font("Segoe UI", Font.BOLD, 32);
    public static final Font FONT_STAT_LABEL = new Font("Segoe UI", Font.PLAIN, 12);
    public static final Font FONT_MENU = new Font("Segoe UI", Font.PLAIN, 13);
    public static final Font FONT_NAV = new Font("Segoe UI", Font.PLAIN, 14);
    public static final Font FONT_NAV_ACTIVE = new Font("Segoe UI", Font.BOLD, 14);

    // Dimensions
    public static final int SIDEBAR_WIDTH = 240;
    public static final int HEADER_HEIGHT = 56;
    public static final int STATUSBAR_HEIGHT = 32;
    public static final int TOOLBAR_HEIGHT = 40;
    public static final int TABLE_ROW_HEIGHT = 48;
    public static final int BUTTON_HEIGHT = 36;
    public static final int INPUT_HEIGHT = 36;
}
