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
// local stoarage 
public class StatePersistence {
    private static final File STATE_FILE = resolveStateFile();

    private static File resolveStateFile() {
        String localAppData = System.getenv("LOCALAPPDATA");
        File baseDir = (localAppData != null && !localAppData.isBlank())
            ? new File(localAppData, "ScientificGraphingCalculator")
            : new File(System.getProperty("user.home"), ".scientific_graphing_calculator");
        if (!baseDir.exists()) baseDir.mkdirs();
        return new File(baseDir, "state.properties");
    }

    private StatePersistence() {}

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
        }
    }

    public static double load(Map<Character, Double> variables, List<String> history) {
        if (!STATE_FILE.exists()) return 0.0;

        Properties p = new Properties();
        try (Reader r = new InputStreamReader(new FileInputStream(STATE_FILE), StandardCharsets.UTF_8)) {
            p.load(r);
        } catch (IOException ex) {
            return 0.0;
        }

        double memory = 0.0;
        try {
            memory = Double.parseDouble(p.getProperty("memory", "0"));
        } catch (NumberFormatException ignored) {
        }

        for (char c = 'A'; c <= 'F'; c++) {
            String v = p.getProperty("variable." + c);
            if (v == null) continue;
            try {
                variables.put(c, Double.parseDouble(v));
            } catch (NumberFormatException ignored) {
            }
        }

        try {
            int count = Integer.parseInt(p.getProperty("history.count", "0"));
            for (int i = 0; i < count; i++) {
                String entry = p.getProperty("history." + i);
                if (entry != null) history.add(entry);
            }
        } catch (NumberFormatException ignored) {
        }

        return memory;
    }
}