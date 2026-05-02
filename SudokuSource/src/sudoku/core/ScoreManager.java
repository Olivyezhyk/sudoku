package sudoku.core;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Manages high-score persistence.
 *
 * Records are stored in a plain CSV file (one line per record).
 * Only the top {@value #MAX_RECORDS} records (sorted by time ascending)
 * are kept per (mode, difficulty) pair.
 */
public class ScoreManager {

    public static final int MAX_RECORDS = 10;

    private static final String FILE =
            System.getProperty("user.home") + "/.sudoku_scores.csv";

    private final List<ScoreRecord> records;

    // ── Constructor ───────────────────────────────────────────────────────────

    public ScoreManager() {
        records = new ArrayList<>(loadFromDisk());
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Adds a new record, trims each (mode+difficulty) bucket to
     * {@value #MAX_RECORDS} entries, then persists to disk.
     */
    public void addRecord(ScoreRecord record) {
        records.add(record);
        trimAndSort();
        saveToDisk();
    }

    /**
     * Returns an unmodifiable view of ALL stored records,
     * sorted by time ascending (fastest first).
     */
    public List<ScoreRecord> getAll() {
        return Collections.unmodifiableList(records);
    }

    /**
     * Returns records filtered by mode and difficulty,
     * sorted by time ascending.
     */
    public List<ScoreRecord> getFiltered(GameMode mode, Difficulty difficulty) {
        return records.stream()
                .filter(r -> r.getGameMode() == mode && r.getDifficulty() == difficulty)
                .sorted(Comparator.comparingInt(ScoreRecord::getTimeSeconds))
                .collect(Collectors.toList());
    }

    /** Clears all stored records from memory and disk. */
    public void clearAll() {
        records.clear();
        saveToDisk();
    }

    // ── Persistence ───────────────────────────────────────────────────────────

    private List<ScoreRecord> loadFromDisk() {
        List<ScoreRecord> loaded = new ArrayList<>();
        Path path = Path.of(FILE);
        if (!Files.exists(path)) return loaded;

        try (BufferedReader br = Files.newBufferedReader(path)) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                ScoreRecord r = ScoreRecord.fromCsv(line);
                if (r != null) loaded.add(r);
            }
        } catch (IOException e) {
            System.err.println("ScoreManager: could not read scores — " + e.getMessage());
        }
        return loaded;
    }

    private void saveToDisk() {
        try (PrintWriter pw = new PrintWriter(new FileWriter(FILE, false))) {
            pw.println("# Sudoku high scores — name,id,seconds,mode,difficulty,timestamp");
            for (ScoreRecord r : records) pw.println(r.toCsv());
        } catch (IOException e) {
            System.err.println("ScoreManager: could not save scores — " + e.getMessage());
        }
    }

    /**
     * Sorts all records by time and keeps only the best
     * {@value #MAX_RECORDS} per (mode, difficulty) bucket.
     */
    private void trimAndSort() {
        records.sort(Comparator.comparingInt(ScoreRecord::getTimeSeconds));

        // Count per bucket and remove records that exceed the limit
        Map<String, Integer> counts = new HashMap<>();
        Iterator<ScoreRecord> it = records.iterator();
        while (it.hasNext()) {
            ScoreRecord r = it.next();
            String key = r.getGameMode().name() + "_" + r.getDifficulty().name();
            int cnt = counts.getOrDefault(key, 0);
            if (cnt >= MAX_RECORDS) {
                it.remove();
            } else {
                counts.put(key, cnt + 1);
            }
        }
    }
}