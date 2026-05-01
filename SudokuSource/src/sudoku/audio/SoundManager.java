package sudoku.audio;

import sudoku.settings.AppSettings;
import javax.sound.sampled.*;

public class SoundManager {
    public enum SoundType { CLICK, SUCCESS, ERROR, WIN, LOSE }

    private static float volume = 1.0f;   // 0.0 – 1.0, matches AppSettings default (100)

    /** v — float 0.0 (mute) … 1.0 (full). Called from AppSettings.setSoundVolume(). */
    public static void setVolume(float v) {
        volume = Math.max(0f, Math.min(1f, v));
    }

    public static void play(SoundType type) {
        if (!AppSettings.getInstance().isSoundEnabled()) return;
        try {
            AudioFormat fmt = new AudioFormat(44100, 16, 1, true, false);
            byte[] data = generate(type, fmt);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt);

            // Apply volume via MASTER_GAIN if supported
            if (line.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gain = (FloatControl) line.getControl(FloatControl.Type.MASTER_GAIN);
                float dB = volume == 0f
                        ? gain.getMinimum()
                        : Math.max(gain.getMinimum(), (float)(20.0 * Math.log10(volume)));
                gain.setValue(Math.min(dB, gain.getMaximum()));
            }

            line.start();
            line.write(data, 0, data.length);
            line.drain();
            line.close();
        } catch (Exception ignored) {}
    }

    public static void playAsync(SoundType type) {
        new Thread(() -> play(type), "sound-thread").start();
    }

    private static byte[] generate(SoundType type, AudioFormat fmt) {
        float sr = fmt.getSampleRate();
        return switch (type) {
            case CLICK   -> tone(sr, 800,  0.06f, 0.15f);
            case SUCCESS -> arpeggio(sr, new float[]{523, 659, 784}, 0.08f);
            case ERROR   -> tone(sr, 200,  0.3f,  0.2f);
            case WIN     -> arpeggio(sr, new float[]{523, 659, 784, 1047}, 0.12f);
            case LOSE    -> descend(sr);
        };
    }

    private static byte[] tone(float sr, float freq, float vol, float dur) {
        int samples = (int)(sr * dur);
        byte[] buf = new byte[samples * 2];
        for (int i = 0; i < samples; i++) {
            double env = Math.min(1.0, Math.min(i / (sr * 0.01), (samples - i) / (sr * 0.05)));
            short v = (short)(Math.sin(2 * Math.PI * freq * i / sr) * vol * env * Short.MAX_VALUE);
            buf[i * 2]     = (byte)(v & 0xFF);
            buf[i * 2 + 1] = (byte)((v >> 8) & 0xFF);
        }
        return buf;
    }

    private static byte[] arpeggio(float sr, float[] freqs, float dur) {
        byte[][] parts = new byte[freqs.length][];
        int total = 0;
        for (int i = 0; i < freqs.length; i++) {
            parts[i] = tone(sr, freqs[i], 0.3f, dur);
            total += parts[i].length;
        }
        byte[] all = new byte[total];
        int pos = 0;
        for (byte[] p : parts) { System.arraycopy(p, 0, all, pos, p.length); pos += p.length; }
        return all;
    }

    private static byte[] descend(float sr) {
        return arpeggio(sr, new float[]{400, 320, 250, 180}, 0.15f);
    }
}