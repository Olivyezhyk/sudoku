package sudoku.core;

public class ScoreRecord {
    private final String   userName;
    private final String   userId;
    private final int      timeSeconds;
    private final GameMode gameMode;
    private final Difficulty difficulty;
    private final long     timestamp;   // System.currentTimeMillis()

    public ScoreRecord(String userName, String userId,
                       int timeSeconds, GameMode gameMode,
                       Difficulty difficulty) {
        this.userName    = userName;
        this.userId      = userId;
        this.timeSeconds = timeSeconds;
        this.gameMode    = gameMode;
        this.difficulty  = difficulty;
        this.timestamp   = System.currentTimeMillis();
    }

    // ── Getters ───────────────────────────────────────────────────────────────
    public String     getUserName()   { return userName;    }
    public String     getUserId()     { return userId;      }
    public int        getTimeSeconds(){ return timeSeconds; }
    public GameMode   getGameMode()   { return gameMode;    }
    public Difficulty getDifficulty() { return difficulty;  }
    public long       getTimestamp()  { return timestamp;   }

    /** Converts record to CSV line: name,id,seconds,mode,difficulty,timestamp */
    public String toCsv() {
        return String.join(",",
                escapeCsv(userName),
                escapeCsv(userId),
                String.valueOf(timeSeconds),
                gameMode.name(),
                difficulty.name(),
                String.valueOf(timestamp)
        );
    }

    /** Parses a CSV line produced by {@link #toCsv()}. Returns null on error. */
    public static ScoreRecord fromCsv(String line) {
        try {
            String[] p = line.split(",", -1);
            if (p.length < 6) return null;
            String   name  = p[0].replace("\\c", ",").replace("\\n", "\n");
            String   id    = p[1].replace("\\c", ",").replace("\\n", "\n");
            int      secs  = Integer.parseInt(p[2].trim());
            GameMode mode  = GameMode.valueOf(p[3].trim());
            Difficulty diff= Difficulty.valueOf(p[4].trim());
            long     ts    = Long.parseLong(p[5].trim());
            ScoreRecord r  = new ScoreRecord(name, id, secs, mode, diff);
            // timestamp is final — reconstruct via helper
            return new ScoreRecord(name, id, secs, mode, diff, ts);
        } catch (Exception e) {
            return null;
        }
    }

    /** Internal constructor that accepts an existing timestamp (used by fromCsv). */
    private ScoreRecord(String userName, String userId,
                        int timeSeconds, GameMode gameMode,
                        Difficulty difficulty, long timestamp) {
        this.userName    = userName;
        this.userId      = userId;
        this.timeSeconds = timeSeconds;
        this.gameMode    = gameMode;
        this.difficulty  = difficulty;
        this.timestamp   = timestamp;
    }

    private static String escapeCsv(String s) {
        return s.replace(",", "\\c").replace("\n", "\\n");
    }

    @Override
    public String toString() {
        return String.format("ScoreRecord{user='%s', id='%s', time=%ds, mode=%s, diff=%s}",
                userName, userId, timeSeconds, gameMode, difficulty);
    }
}