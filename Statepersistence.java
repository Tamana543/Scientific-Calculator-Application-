import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Properties;


// Logic 
/**
 * Saves and restores the calculator's memory value, A-F variables, and calculation history
 * between runs, using a small .properties file in the user's home directory (works no matter
 * where the app is installed, unlike writing next to the jar - Program Files, for instance,
 * often isn't writable by a normal user).
 * Saving happens via a JVM shutdown hook , which fires on
 * any normal exit path - closing the window, closing from the taskbar, etc. It does NOT cover
 * a hard crash or the process being killed; that's an accepted gap for "save on close", not an
 * attempt at continuous autosave.
 */
public class Statepersistence {
      private static final File STATE_FILE =
        new File(System.getProperty("user.home"), ".graphing_calculator_state.properties");
 
    private StatePersistence() {} // static utility only
     public static void save(Map<Character, Double> variables, double memory, List<String> history) {
        Properties p = new Properties();
        p.setProperty("memory", String.valueOf(memory));
        for (Map.Entry<Character, Double> e : variables.entrySet()) {
            p.setProperty("variable." + e.getKey(), String.valueOf(e.getValue()));
        }
        p.setProperty("history.count", String.valueOf(history.size()));
        for (int i = 0; i < history.size(); i++) {
            p.setProperty("history." + i, history.get(i));
        }
        try (Writer w = new OutputStreamWriter(new FileOutputStream(STATE_FILE), StandardCharsets.UTF_8)) {
            p.store(w, "Scientific Graphing Calculator - saved state");
        } catch (IOException ignored) {
            // best-effort, see class comment
        }
    }
}
