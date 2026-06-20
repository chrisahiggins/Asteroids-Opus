package com.asteroids;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.Stroke;
import java.awt.geom.GeneralPath;
import java.util.HashMap;
import java.util.Map;

/**
 * Vector-display rendering helpers: crisp white strokes with an optional soft
 * phosphor "glow", plus an angular all-caps stroke font in the spirit of the
 * original Asteroids vector character set.
 *
 * Glyphs are defined on a 4 (wide) x 6 (tall) grid as one or more polylines.
 */
public final class VectorGraphics {

    private VectorGraphics() {}

    /** Toggled in-game with the G key. */
    public static boolean glow = true;

    private static final Stroke THIN = new BasicStroke(1.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);
    private static final Stroke GLOW = new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND);

    /** Stroke a shape with the current colour, adding a translucent glow pass underneath when enabled. */
    public static void stroke(Graphics2D g, Shape s) {
        Color c = g.getColor();
        if (glow) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 60));
            g.setStroke(GLOW);
            g.draw(s);
            g.setColor(c);
        }
        g.setStroke(THIN);
        g.draw(s);
    }

    /** Draw a polyline through the given world points; optionally close it into a polygon. */
    public static void poly(Graphics2D g, double[] xs, double[] ys, int n, boolean closed) {
        GeneralPath p = new GeneralPath();
        p.moveTo(xs[0], ys[0]);
        for (int i = 1; i < n; i++) p.lineTo(xs[i], ys[i]);
        if (closed) p.closePath();
        stroke(g, p);
    }

    public static void line(Graphics2D g, double x1, double y1, double x2, double y2) {
        GeneralPath p = new GeneralPath();
        p.moveTo(x1, y1);
        p.lineTo(x2, y2);
        stroke(g, p);
    }

    /** A small filled dot with a glow halo (bullets, particles). */
    public static void dot(Graphics2D g, double x, double y, double r) {
        Color c = g.getColor();
        if (glow) {
            g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), 70));
            g.fillOval((int) Math.round(x - r * 2), (int) Math.round(y - r * 2),
                    (int) Math.round(r * 4), (int) Math.round(r * 4));
            g.setColor(c);
        }
        g.fillOval((int) Math.round(x - r), (int) Math.round(y - r),
                (int) Math.round(r * 2), (int) Math.round(r * 2));
    }

    // ---- Text ---------------------------------------------------------------

    private static final double CELL_W = 0.60;  // glyph width as fraction of size
    private static final double SPACING = 0.32; // gap between glyphs as fraction of size

    public static double textWidth(String s, double size) {
        double adv = (CELL_W + SPACING) * size;
        return s.length() * adv - SPACING * size;
    }

    /** Draw text with its top-left at (x, y), or horizontally centred on x when {@code center}. */
    public static void text(Graphics2D g, String s, double x, double y, double size, boolean center) {
        double cw = CELL_W * size;
        double ch = size;
        double adv = (CELL_W + SPACING) * size;
        double startX = center ? x - textWidth(s, size) / 2.0 : x;

        GeneralPath path = new GeneralPath();
        double cx = startX;
        for (int k = 0; k < s.length(); k++) {
            int[][] glyph = FONT.get(Character.toUpperCase(s.charAt(k)));
            if (glyph != null) {
                for (int[] pl : glyph) {
                    for (int i = 0; i + 1 < pl.length; i += 2) {
                        double px = cx + pl[i] / 4.0 * cw;
                        double py = y + pl[i + 1] / 6.0 * ch;
                        if (i == 0) path.moveTo(px, py);
                        else path.lineTo(px, py);
                    }
                }
            }
            cx += adv;
        }
        stroke(g, path);
    }

    // ---- Stroke font --------------------------------------------------------

    private static final Map<Character, int[][]> FONT = new HashMap<Character, int[][]>();

    private static void f(char c, int[]... polylines) {
        FONT.put(c, polylines);
    }

    static {
        f('A', new int[]{0, 6, 0, 2, 2, 0, 4, 2, 4, 6}, new int[]{0, 4, 4, 4});
        f('B', new int[]{0, 6, 0, 0, 3, 0, 4, 1, 4, 2, 3, 3, 0, 3},
                new int[]{0, 3, 3, 3, 4, 4, 4, 5, 3, 6, 0, 6});
        f('C', new int[]{4, 1, 3, 0, 1, 0, 0, 1, 0, 5, 1, 6, 3, 6, 4, 5});
        f('D', new int[]{0, 0, 0, 6, 3, 6, 4, 5, 4, 1, 3, 0, 0, 0});
        f('E', new int[]{4, 0, 0, 0, 0, 6, 4, 6}, new int[]{0, 3, 3, 3});
        f('F', new int[]{4, 0, 0, 0, 0, 6}, new int[]{0, 3, 3, 3});
        f('G', new int[]{4, 1, 3, 0, 1, 0, 0, 1, 0, 5, 1, 6, 3, 6, 4, 5, 4, 3, 2, 3});
        f('H', new int[]{0, 0, 0, 6}, new int[]{4, 0, 4, 6}, new int[]{0, 3, 4, 3});
        f('I', new int[]{0, 0, 4, 0}, new int[]{2, 0, 2, 6}, new int[]{0, 6, 4, 6});
        f('J', new int[]{4, 0, 4, 5, 3, 6, 1, 6, 0, 5});
        f('K', new int[]{0, 0, 0, 6}, new int[]{4, 0, 0, 3, 4, 6});
        f('L', new int[]{0, 0, 0, 6, 4, 6});
        f('M', new int[]{0, 6, 0, 0, 2, 2, 4, 0, 4, 6});
        f('N', new int[]{0, 6, 0, 0, 4, 6, 4, 0});
        f('O', new int[]{1, 0, 3, 0, 4, 1, 4, 5, 3, 6, 1, 6, 0, 5, 0, 1, 1, 0});
        f('P', new int[]{0, 6, 0, 0, 3, 0, 4, 1, 4, 2, 3, 3, 0, 3});
        f('Q', new int[]{1, 0, 3, 0, 4, 1, 4, 5, 3, 6, 1, 6, 0, 5, 0, 1, 1, 0},
                new int[]{2, 4, 4, 6});
        f('R', new int[]{0, 6, 0, 0, 3, 0, 4, 1, 4, 2, 3, 3, 0, 3}, new int[]{2, 3, 4, 6});
        f('S', new int[]{4, 1, 3, 0, 1, 0, 0, 1, 0, 2, 1, 3, 3, 3, 4, 4, 4, 5, 3, 6, 1, 6, 0, 5});
        f('T', new int[]{0, 0, 4, 0}, new int[]{2, 0, 2, 6});
        f('U', new int[]{0, 0, 0, 5, 1, 6, 3, 6, 4, 5, 4, 0});
        f('V', new int[]{0, 0, 2, 6, 4, 0});
        f('W', new int[]{0, 0, 1, 6, 2, 3, 3, 6, 4, 0});
        f('X', new int[]{0, 0, 4, 6}, new int[]{4, 0, 0, 6});
        f('Y', new int[]{0, 0, 2, 3, 4, 0}, new int[]{2, 3, 2, 6});
        f('Z', new int[]{0, 0, 4, 0, 0, 6, 4, 6});

        f('0', new int[]{1, 0, 3, 0, 4, 1, 4, 5, 3, 6, 1, 6, 0, 5, 0, 1, 1, 0}, new int[]{4, 1, 0, 5});
        f('1', new int[]{1, 1, 2, 0, 2, 6}, new int[]{1, 6, 3, 6});
        f('2', new int[]{0, 1, 1, 0, 3, 0, 4, 1, 4, 2, 0, 6, 4, 6});
        f('3', new int[]{0, 1, 1, 0, 3, 0, 4, 1, 4, 2, 3, 3, 2, 3},
                new int[]{3, 3, 4, 4, 4, 5, 3, 6, 1, 6, 0, 5});
        f('4', new int[]{3, 6, 3, 0, 0, 4, 4, 4});
        f('5', new int[]{4, 0, 0, 0, 0, 3, 3, 3, 4, 4, 4, 5, 3, 6, 1, 6, 0, 5});
        f('6', new int[]{4, 1, 3, 0, 1, 0, 0, 1, 0, 5, 1, 6, 3, 6, 4, 5, 4, 4, 3, 3, 0, 3});
        f('7', new int[]{0, 0, 4, 0, 1, 6});
        f('8', new int[]{1, 3, 0, 2, 0, 1, 1, 0, 3, 0, 4, 1, 4, 2, 3, 3, 1, 3,
                0, 4, 0, 5, 1, 6, 3, 6, 4, 5, 4, 4, 3, 3});
        f('9', new int[]{4, 3, 1, 3, 0, 2, 0, 1, 1, 0, 3, 0, 4, 1, 4, 5, 3, 6, 1, 6, 0, 5});

        f(' ');
        f('.', new int[]{2, 5, 2, 6});
        f(',', new int[]{2, 5, 1, 6});
        f('-', new int[]{0, 3, 4, 3});
        f('\'', new int[]{2, 0, 2, 2});
        f('!', new int[]{2, 0, 2, 4}, new int[]{2, 5, 2, 6});
        f('?', new int[]{0, 1, 1, 0, 3, 0, 4, 1, 4, 2, 2, 3, 2, 4}, new int[]{2, 5, 2, 6});
        f(':', new int[]{2, 1, 2, 2}, new int[]{2, 4, 2, 5});
        f('/', new int[]{4, 0, 0, 6});
        f('<', new int[]{4, 0, 0, 3, 4, 6});
        f('>', new int[]{0, 0, 4, 3, 0, 6});
        f('(', new int[]{3, 0, 1, 2, 1, 4, 3, 6});
        f(')', new int[]{1, 0, 3, 2, 3, 4, 1, 6});
        f('=', new int[]{0, 2, 4, 2}, new int[]{0, 4, 4, 4});
    }
}
