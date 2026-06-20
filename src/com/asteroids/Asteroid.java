package com.asteroids;

import java.awt.Graphics2D;
import java.util.Random;

/** A drifting, spinning rock. Large rocks split into two of the next size down when shot. */
public class Asteroid {
    public static final int LARGE = 3, MEDIUM = 2, SMALL = 1;

    // Collision radius per size.
    private static final double[] RADIUS = {0, 14, 26, 52};

    /** A few jagged outlines (radius multipliers around the perimeter) for variety. */
    private static final double[][] SHAPES = {
            {1.0, 0.7, 0.95, 0.75, 1.05, 0.8, 1.0, 0.65, 0.9, 0.8, 1.05, 0.85},
            {0.9, 1.05, 0.8, 1.0, 0.7, 1.0, 0.85, 1.05, 0.7, 1.0, 0.9, 1.05},
            {1.05, 0.85, 1.0, 0.65, 1.0, 0.85, 1.05, 0.7, 1.0, 0.9, 0.8, 1.0},
            {0.85, 1.0, 0.9, 1.05, 0.7, 0.95, 1.0, 0.75, 1.05, 0.9, 0.75, 1.0},
    };

    public double x, y, vx, vy;
    public int size;
    public double radius;
    private final double[] shape;
    private double rot;
    private final double spin;

    public Asteroid(double x, double y, int size, Random rnd) {
        this.x = x; this.y = y; this.size = size;
        this.radius = RADIUS[size];
        this.shape = SHAPES[rnd.nextInt(SHAPES.length)];
        double speed = (40 + rnd.nextDouble() * 50) * (4 - size) * 0.45 + 18;
        double dir = rnd.nextDouble() * Math.PI * 2;
        this.vx = Math.cos(dir) * speed;
        this.vy = Math.sin(dir) * speed;
        this.rot = rnd.nextDouble() * Math.PI * 2;
        this.spin = (rnd.nextDouble() - 0.5) * 1.6;
    }

    public void update(double dt) {
        x = Game.wrap(x + vx * dt, Game.WORLD_W);
        y = Game.wrap(y + vy * dt, Game.WORLD_H);
        rot += spin * dt;
    }

    public int score() {
        switch (size) {
            case LARGE: return 20;
            case MEDIUM: return 50;
            default: return 100;
        }
    }

    public void draw(Graphics2D g) {
        int n = shape.length;
        double[] xs = new double[n];
        double[] ys = new double[n];
        for (int i = 0; i < n; i++) {
            double a = rot + i * (Math.PI * 2 / n);
            double r = radius * shape[i];
            xs[i] = x + Math.cos(a) * r;
            ys[i] = y + Math.sin(a) * r;
        }
        VectorGraphics.poly(g, xs, ys, n, true);
    }
}
