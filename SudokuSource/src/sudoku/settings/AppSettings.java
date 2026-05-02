package sudoku.settings;

import java.io.*;
import java.util.Properties;

public class AppSettings {

    private static AppSettings instance;
    private static final String FILE =
            System.getProperty("user.home") + "/.sudoku_settings.properties";

    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private String  theme        = "default";
    private int     musicVolume  = 70;
    private int     soundVolume  = 100;
    private String  userName     = "";
    private String  userId       = "";

    public static final int SCALE = 125;

    private AppSettings() {}

    public static AppSettings getInstance() {
        if (instance == null) instance = new AppSettings();
        return instance;
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    public static void load() {
        Properties p = new Properties();
        try (FileReader fr = new FileReader(FILE)) {
            p.load(fr);
            AppSettings s  = getInstance();
            s.soundEnabled = Boolean.parseBoolean(p.getProperty("sound",       "true"));
            s.musicEnabled = Boolean.parseBoolean(p.getProperty("music",       "true"));
            s.theme        = p.getProperty("theme",       "default");
            s.musicVolume  = Integer.parseInt(p.getProperty("musicVolume",     "70"));
            s.soundVolume  = Integer.parseInt(p.getProperty("soundVolume",     "100"));
            s.userName     = p.getProperty("userName",    "");
            s.userId       = p.getProperty("userId",      "");
        } catch (Exception ignored) {}
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    public void save() {
        Properties p = new Properties();
        p.setProperty("sound",       String.valueOf(soundEnabled));
        p.setProperty("music",       String.valueOf(musicEnabled));
        p.setProperty("theme",       theme);
        p.setProperty("musicVolume", String.valueOf(musicVolume));
        p.setProperty("soundVolume", String.valueOf(soundVolume));
        p.setProperty("userName",    userName != null ? userName : "");
        p.setProperty("userId",      userId   != null ? userId   : "");
        try (FileWriter fw = new FileWriter(FILE)) {
            p.store(fw, "Sudoku Settings");
        } catch (Exception ignored) {}
    }

    // ── Sound ─────────────────────────────────────────────────────────────────

    public boolean isSoundEnabled()        { return soundEnabled; }
    public void setSoundEnabled(boolean v) { soundEnabled = v; save(); }

    public boolean isMusicEnabled()        { return musicEnabled; }
    public void setMusicEnabled(boolean v) {
        musicEnabled = v;
        save();
        if (v) sudoku.audio.MusicPlayer.playLoop("resources/music/background.wav");
        else   sudoku.audio.MusicPlayer.stop();
    }

    // ── Theme ─────────────────────────────────────────────────────────────────

    public String getTheme()           { return theme; }
    public void   setTheme(String t)   { theme = t; save(); }

    // ── Volume ────────────────────────────────────────────────────────────────

    public int getMusicVolume()        { return musicVolume; }
    public void setMusicVolume(int v) {
        musicVolume = clamp(v);
        save();
        sudoku.audio.MusicPlayer.setVolume(musicVolume / 100f);
    }

    public int getSoundVolume()        { return soundVolume; }
    public void setSoundVolume(int v) {
        soundVolume = clamp(v);
        save();
        sudoku.audio.SoundManager.setVolume(soundVolume / 100f);
    }

    private int clamp(int v) { return Math.max(0, Math.min(100, v)); }

    // ── Player info ───────────────────────────────────────────────────────────

    public String getUserName()          { return userName != null ? userName : ""; }
    public void   setUserName(String v)  { userName = (v != null ? v : ""); save(); }

    public String getUserId()            { return userId != null ? userId : ""; }
    public void   setUserId(String v)    { userId = (v != null ? v : ""); save(); }

    // ── Scale ─────────────────────────────────────────────────────────────────

    public int getScale() { return SCALE; }
}