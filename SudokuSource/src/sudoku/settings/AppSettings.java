package sudoku.settings;

import java.io.*;
import java.util.Properties;

public class AppSettings {
    private static AppSettings instance;
    private static final String FILE = System.getProperty("user.home") + "/.sudoku_settings.properties";

    private boolean soundEnabled = true;
    private boolean musicEnabled = true;
    private String  theme        = "default";
    private int     musicVolume  = 70;   // 0-100
    private int     soundVolume  = 100;  // 0-100

    // Scale is permanently fixed at 125 — not stored, not changeable
    public static final int SCALE = 125;

    private AppSettings() {}

    public static AppSettings getInstance() {
        if (instance == null) instance = new AppSettings();
        return instance;
    }

    public static void load() {
        Properties p = new Properties();
        try (FileReader fr = new FileReader(FILE)) {
            p.load(fr);
            getInstance().soundEnabled = Boolean.parseBoolean(p.getProperty("sound", "true"));
            getInstance().musicEnabled = Boolean.parseBoolean(p.getProperty("music", "true"));
            getInstance().theme        = p.getProperty("theme", "default");
            getInstance().musicVolume  = Integer.parseInt(p.getProperty("musicVolume", "70"));
            getInstance().soundVolume  = Integer.parseInt(p.getProperty("soundVolume", "100"));
        } catch (Exception ignored) {}
    }

    public void save() {
        Properties p = new Properties();
        p.setProperty("sound",       String.valueOf(soundEnabled));
        p.setProperty("music",       String.valueOf(musicEnabled));
        p.setProperty("theme",       theme);
        p.setProperty("musicVolume", String.valueOf(musicVolume));
        p.setProperty("soundVolume", String.valueOf(soundVolume));
        try (FileWriter fw = new FileWriter(FILE)) {
            p.store(fw, "Sudoku Settings");
        } catch (Exception ignored) {}
    }

    public boolean isSoundEnabled()        { return soundEnabled; }
    public void setSoundEnabled(boolean v) { soundEnabled = v; save(); }

    public boolean isMusicEnabled()        { return musicEnabled; }
    public void setMusicEnabled(boolean v) {
        musicEnabled = v;
        save();
        if (v) sudoku.audio.MusicPlayer.playLoop("resources/music/background.wav");
        else   sudoku.audio.MusicPlayer.stop();
    }

    public String getTheme()               { return theme; }
    public void setTheme(String t)         { theme = t; save(); }

    public int getMusicVolume()            { return musicVolume; }
    public void setMusicVolume(int v) {
        musicVolume = Math.max(0, Math.min(100, v));
        save();
        sudoku.audio.MusicPlayer.setVolume(musicVolume / 100f);
    }

    public int getSoundVolume()            { return soundVolume; }
    public void setSoundVolume(int v) {
        soundVolume = Math.max(0, Math.min(100, v));
        save();
        sudoku.audio.SoundManager.setVolume(soundVolume / 100f);
    }

    // Scale is fixed — no setter
    public int getScale() { return SCALE; }
}