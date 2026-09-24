import java.awt.Color;

/**
 * Central place for every color used across the UI.
 * Extracted from the color constants that used to live at the top of calculator.java,
 * so every other file (RoundedButton, GraphCanvas, calculator) can share them via Theme.X
 * instead of each needing its own copy.
 */
public final class Theme {
    private Theme() {} // no instances - constants only

    public static final Color APP_BG = new Color(34, 36, 42);
    public static final Color DISPLAY_BG = new Color(11, 12, 16);
    public static final Color KEY_BG = new Color(40, 42, 50);
    public static final Color PILL_BG = new Color(35, 37, 44);
    public static final Color PILL_BORDER = new Color(70, 72, 80);
    public static final Color GOLD = new Color(224, 196, 106);
    public static final Color GOLD_BRIGHT = new Color(240, 215, 126);
    public static final Color OP_FILL = new Color(60, 53, 33);
    public static final Color AC_RED = new Color(234, 67, 53);
    public static final Color DEL_BORDER = new Color(160, 60, 55);
    public static final Color HISTORY_BG = new Color(24, 26, 32);
    public static final Color GRAD_LEFT = new Color(213, 176, 57);
    public static final Color GRAD_RIGHT = new Color(242, 228, 169);
    public static final Color TEXT_DIM = new Color(120, 122, 132);
    public static final Color DIGIT_FG = new Color(228, 230, 235);
    public static final Color ALPHA_LABEL = new Color(232, 120, 110); // reddish, distinct from GOLD
    public static final Color SHIFT_LABEL = GOLD;
    public static final Color[] GRAPH_COLORS = {
        GOLD_BRIGHT,
        new Color(110, 200, 250), 
        new Color(240, 120, 190), 
        new Color(140, 230, 140), 
        new Color(250, 170, 90), 
        new Color(190, 150, 250)  
    };
}