package com.asteroids;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.DataLine;
import java.util.Random;

/**
 * Procedurally synthesised arcade sound effects. The original Asteroids generated
 * its audio from discrete circuitry, so we recreate the effects from raw waveforms
 * rather than shipping (copyrighted) ROM samples. All audio is generated at startup;
 * the game owns no external asset files.
 *
 * One-shot effects use a small pool of pre-opened {@link Clip}s so they can overlap;
 * looped effects (thrust, saucer warble) use a single dedicated clip.
 */
public class Sound {
    private static final float SR = 44100f;
    private static final AudioFormat FORMAT = new AudioFormat(SR, 16, 1, true, false);
    private static final int SINE = 0, SQUARE = 1, TRI = 2, NOISE = 3;

    private boolean enabled = true;
    private final Random rnd = new Random();

    private Voice fire, saucerFire, bangSmall, bangMed, bangLarge, beatLo, beatHi, extra;
    private Clip thrustClip, saucerBigClip, saucerSmallClip;

    public Sound() {
        try {
            fire = new Voice(blip(0.12, 1050, 480, 0.32, SQUARE), 4);
            saucerFire = new Voice(blip(0.14, 700, 300, 0.30, SQUARE), 3);
            bangSmall = new Voice(noiseBurst(0.20, 0.40, 0.30), 3);
            bangMed = new Voice(noiseBurst(0.32, 0.45, 0.45), 3);
            bangLarge = new Voice(noiseBurst(0.48, 0.50, 0.60), 2);
            beatLo = new Voice(thump(80), 2);
            beatHi = new Voice(thump(102), 2);
            extra = new Voice(blip(0.45, 380, 920, 0.30, SINE), 1);

            thrustClip = openLoop(rumble(0.40, 0.28));
            saucerBigClip = openLoop(warble(235, 196, 0.20, 0.26, SQUARE));
            saucerSmallClip = openLoop(warble(500, 410, 0.13, 0.24, SQUARE));
        } catch (Throwable t) {
            // No mixer / headless audio: run silently rather than failing.
            enabled = false;
        }
    }

    public boolean isEnabled() { return enabled; }

    // ---- public effect triggers --------------------------------------------

    public void fire()        { if (enabled) fire.play(); }
    public void saucerShoot() { if (enabled) saucerFire.play(); }
    public void extraLife()   { if (enabled) extra.play(); }

    public void bang(int asteroidSize) {
        if (!enabled) return;
        if (asteroidSize >= Asteroid.LARGE) bangLarge.play();
        else if (asteroidSize == Asteroid.MEDIUM) bangMed.play();
        else bangSmall.play();
    }

    public void bangLarge() { if (enabled) bangLarge.play(); }

    public void beat(boolean high) {
        if (!enabled) return;
        (high ? beatHi : beatLo).play();
    }

    public void thrust(boolean on) { loop(thrustClip, on); }

    public void saucer(int type, boolean on) {
        if (type == Saucer.SMALL) loop(saucerSmallClip, on);
        else loop(saucerBigClip, on);
    }

    public void stopAllLoops() {
        loop(thrustClip, false);
        loop(saucerBigClip, false);
        loop(saucerSmallClip, false);
    }

    // ---- clip plumbing ------------------------------------------------------

    private void loop(Clip c, boolean on) {
        if (!enabled || c == null) return;
        try {
            if (on) {
                if (!c.isRunning()) { c.setFramePosition(0); c.loop(Clip.LOOP_CONTINUOUSLY); }
            } else if (c.isRunning()) {
                c.stop();
            }
        } catch (Throwable ignored) {}
    }

    private Clip openLoop(byte[] data) throws Exception {
        DataLine.Info info = new DataLine.Info(Clip.class, FORMAT);
        Clip c = (Clip) AudioSystem.getLine(info);
        c.open(FORMAT, data, 0, data.length);
        return c;
    }

    /** A round-robin pool of identical one-shot clips so the effect can overlap with itself. */
    private static class Voice {
        private final Clip[] clips;
        private int idx;

        Voice(byte[] data, int n) throws Exception {
            clips = new Clip[n];
            for (int i = 0; i < n; i++) {
                DataLine.Info info = new DataLine.Info(Clip.class, FORMAT);
                Clip c = (Clip) AudioSystem.getLine(info);
                c.open(FORMAT, data, 0, data.length);
                clips[i] = c;
            }
        }

        void play() {
            Clip c = clips[idx];
            idx = (idx + 1) % clips.length;
            try {
                if (c.isRunning()) c.stop();
                c.setFramePosition(0);
                c.start();
            } catch (Throwable ignored) {}
        }
    }

    // ---- waveform generators ------------------------------------------------

    private static byte[] pack(double[] samples) {
        byte[] out = new byte[samples.length * 2];
        for (int i = 0; i < samples.length; i++) {
            double v = samples[i];
            if (v > 1) v = 1; else if (v < -1) v = -1;
            int s = (int) Math.round(v * 32767);
            out[i * 2] = (byte) (s & 0xff);
            out[i * 2 + 1] = (byte) ((s >> 8) & 0xff);
        }
        return out;
    }

    private double osc(int wave, double phase) {
        switch (wave) {
            case SQUARE: return Math.sin(phase) >= 0 ? 1 : -1;
            case TRI:    return 2.0 / Math.PI * Math.asin(Math.sin(phase));
            case NOISE:  return rnd.nextDouble() * 2 - 1;
            default:     return Math.sin(phase);
        }
    }

    /** A short tone gliding from f0 to f1 with an exponential decay envelope. */
    private byte[] blip(double dur, double f0, double f1, double vol, int wave) {
        int n = (int) (dur * SR);
        double[] s = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double frac = (double) i / n;
            double freq = f0 + (f1 - f0) * frac;
            phase += 2 * Math.PI * freq / SR;
            double env = Math.pow(1 - frac, 1.6);
            s[i] = osc(wave, phase) * vol * env;
        }
        return pack(s);
    }

    /** A low percussive "thump" for the heartbeat beat. */
    private byte[] thump(double freq) {
        double dur = 0.16;
        int n = (int) (dur * SR);
        double[] s = new double[n];
        double phase = 0;
        for (int i = 0; i < n; i++) {
            double frac = (double) i / n;
            double f = freq * (1.0 - 0.3 * frac); // slight downward pitch drop
            phase += 2 * Math.PI * f / SR;
            double attack = Math.min(1.0, frac / 0.05);
            double env = attack * Math.pow(1 - frac, 1.4);
            s[i] = (Math.sin(phase) * 0.7 + (Math.sin(phase) >= 0 ? 0.3 : -0.3)) * 0.55 * env;
        }
        return pack(s);
    }

    /** Filtered noise burst for explosions. */
    private byte[] noiseBurst(double dur, double vol, double decayPow) {
        int n = (int) (dur * SR);
        double[] s = new double[n];
        double prev = 0;
        for (int i = 0; i < n; i++) {
            double frac = (double) i / n;
            double white = rnd.nextDouble() * 2 - 1;
            prev = prev + 0.35 * (white - prev); // low-pass for a deeper "boom"
            double env = Math.pow(1 - frac, 1.0 / (decayPow + 0.001)) * Math.pow(1 - frac, 0.5);
            s[i] = prev * vol * env;
        }
        return pack(s);
    }

    /** Looped low rumble for the thrust. */
    private byte[] rumble(double dur, double vol) {
        int n = (int) (dur * SR);
        double[] s = new double[n];
        double prev = 0;
        for (int i = 0; i < n; i++) {
            double white = rnd.nextDouble() * 2 - 1;
            prev = prev + 0.08 * (white - prev); // heavy low-pass
            s[i] = prev * vol * 3.0;
        }
        return pack(s);
    }

    /** A seamless two-tone warble loop for the saucer siren. */
    private byte[] warble(double f1, double f2, double seg, double vol, int wave) {
        int segN = (int) (seg * SR);
        double[] s = new double[segN * 2];
        double phase = 0;
        for (int i = 0; i < s.length; i++) {
            double freq = (i < segN) ? f1 : f2;
            phase += 2 * Math.PI * freq / SR;
            s[i] = osc(wave, phase) * vol;
        }
        return pack(s);
    }
}
