// Small stateless helpers shared between calculator.java and GraphCanvas.java.
 
public final class CalcUtils {
    private CalcUtils() {} // static helpers only
 
    /** Swaps ASCII * and / for the nicer × and ÷ glyphs used on-screen. */
    public static String prettyPrint(String s) {
        return s.replace('*', '\u00D7').replace('/', '\u00F7');
    }
 
    /** Formats a double for display, dropping a trailing ".0". */
    public static String trimZero(double v) {
        String s = String.valueOf(Math.abs(v) < 1e-12 ? 0 : v);
        return s.endsWith(".0") ? s.substring(0, s.length() - 2) : s;
    }
    // fix the running main issue
}
