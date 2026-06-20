package com.asteroids;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Persistent top-10 leaderboard, stored as plain text under the user's home directory. */
public class HighScores {
    public static final int MAX = 10;

    public static class Entry {
        public final String name;
        public final int score;
        public Entry(String name, int score) { this.name = name; this.score = score; }
    }

    private final List<Entry> entries = new ArrayList<Entry>();
    private final File file;

    public HighScores() {
        this.file = resolveFile();
        load();
    }

    private static File resolveFile() {
        try {
            File dir = new File(System.getProperty("user.home"), ".asteroids-opus");
            if (dir.exists() || dir.mkdirs()) {
                return new File(dir, "highscores.txt");
            }
        } catch (Throwable ignored) {}
        return new File("highscores.txt"); // fallback: alongside the JAR
    }

    public List<Entry> entries() { return entries; }

    public int highest() { return entries.isEmpty() ? 0 : entries.get(0).score; }

    public boolean qualifies(int score) {
        if (score <= 0) return false;
        return entries.size() < MAX || score > entries.get(entries.size() - 1).score;
    }

    public void add(String name, int score) {
        if (name == null || name.trim().isEmpty()) name = "AAA";
        entries.add(new Entry(name.trim(), score));
        sortAndTrim();
        save();
    }

    private void sortAndTrim() {
        Collections.sort(entries, new Comparator<Entry>() {
            public int compare(Entry a, Entry b) { return Integer.compare(b.score, a.score); }
        });
        while (entries.size() > MAX) entries.remove(entries.size() - 1);
    }

    private void load() {
        entries.clear();
        boolean ok = false;
        if (file.exists()) {
            BufferedReader r = null;
            try {
                r = new BufferedReader(new FileReader(file));
                String line;
                while ((line = r.readLine()) != null) {
                    int comma = line.lastIndexOf(',');
                    if (comma <= 0) continue;
                    String name = line.substring(0, comma).trim();
                    try {
                        int score = Integer.parseInt(line.substring(comma + 1).trim());
                        entries.add(new Entry(name, score));
                        ok = true;
                    } catch (NumberFormatException ignored) {}
                }
            } catch (Throwable ignored) {
            } finally {
                if (r != null) try { r.close(); } catch (Throwable ignored) {}
            }
        }
        if (!ok) seedDefaults();
        sortAndTrim();
    }

    private void seedDefaults() {
        entries.clear();
        String[] names = {"AAA", "BBB", "CCC", "DDD", "EEE", "FFF", "GGG", "HHH", "III", "JJJ"};
        int[] scores = {10000, 9000, 8000, 7000, 6000, 5000, 4000, 3000, 2000, 1000};
        for (int i = 0; i < names.length; i++) entries.add(new Entry(names[i], scores[i]));
    }

    private void save() {
        PrintWriter w = null;
        try {
            w = new PrintWriter(file);
            for (Entry e : entries) w.println(e.name + "," + e.score);
        } catch (Throwable ignored) {
        } finally {
            if (w != null) w.close();
        }
    }
}
