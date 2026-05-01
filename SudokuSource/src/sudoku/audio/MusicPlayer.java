package sudoku.audio;

import javax.sound.sampled.*;
import java.io.InputStream;

public class MusicPlayer {
    private static Clip  clip;
    private static float volume = 0.5f;   // 0.0 – 1.0, matches AppSettings default (70)

    public static void playLoop(String path) {
        try {
            if (clip != null && clip.isRunning()) return;

            InputStream is = MusicPlayer.class.getResourceAsStream(path);
            if (is == null) return;

            AudioInputStream ais = AudioSystem.getAudioInputStream(is);
            clip = AudioSystem.getClip();
            clip.open(ais);
            applyVolume();
            clip.loop(Clip.LOOP_CONTINUOUSLY);
            clip.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void stop() {
        if (clip != null) {
            clip.stop();
            clip.close();
            clip = null;
        }
    }

    /** v — float 0.0 (mute) … 1.0 (full). Called from AppSettings.setMusicVolume(). */
    public static void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
        applyVolume();
    }

    private static void applyVolume() {
        if (clip == null) return;
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        // Convert linear 0-1 to decibels; clamp to control range
        float dB = volume == 0f
                ? gain.getMinimum()
                : Math.max(gain.getMinimum(), (float)(20.0 * Math.log10(volume)));
        gain.setValue(Math.min(dB, gain.getMaximum()));
    }
}