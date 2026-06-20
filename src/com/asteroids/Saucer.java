package com.asteroids;

import java.awt.Graphics2D;
import java.util.Random;

/** Alien flying saucer. Large saucers fire wildly; small ones aim at the player. */
public class Saucer {
    public static final int LARGE = 0, SMALL = 1;

    public final int type;
    public double x, y, vx, vy;
    public double radius;
    public double fireTimer;
    public double dirTimer;

    private final Random rnd;

    public Saucer(int type, Random rnd) {
        this.type = type;
        this.rnd = rnd;
        this.radius = (type == SMALL) ? 13 : 22;

        boolean fromLeft = rnd.nextBoolean();
        double speed = (type == SMALL) ? 150 : 120;
        x = fromLeft ? -radius : Game.WORLD_W + radius;
        vx = fromLeft ? speed : -speed;
        y = 60 + rnd.nextDouble() * (Game.WORLD_H - 120);
        vy = 0;
        fireTimer = 1.0 + rnd.nextDouble();
        dirTimer = 0.6 + rnd.nextDouble();
    }

    public void update(double dt) {
        dirTimer -= dt;
        if (dirTimer <= 0) {
            double v = Math.abs(vx);
            int pick = rnd.nextInt(3); // straight, up, or down
            vy = (pick == 0) ? 0 : (pick == 1 ? -v : v);
            dirTimer = 0.6 + rnd.nextDouble() * 1.0;
        }
        x += vx * dt;
        y = Game.wrap(y + vy * dt, Game.WORLD_H);
    }

    /** True once the saucer has traversed the screen and should despawn. */
    public boolean offscreen() {
        return x < -radius - 10 || x > Game.WORLD_W + radius + 10;
    }

    public int score() {
        return (type == SMALL) ? 1000 : 200;
    }

    public void draw(Graphics2D g) {
        double w = radius, h = radius * 0.42, dome = radius * 0.5;
        // Hull: a flattened hexagon.
        double[] hx = {-w, -w * 0.45, w * 0.45, w, w * 0.45, -w * 0.45};
        double[] hy = {0, h, h, 0, -h, -h};
        double[] xs = new double[6];
        double[] ys = new double[6];
        for (int i = 0; i < 6; i++) { xs[i] = x + hx[i]; ys[i] = y + hy[i]; }
        VectorGraphics.poly(g, xs, ys, 6, true);
        // Mid line and cockpit dome.
        VectorGraphics.line(g, x - w, y, x + w, y);
        double[] dx = {-w * 0.45, -w * 0.22, w * 0.22, w * 0.45};
        double[] dy = {-h, -h - dome, -h - dome, -h};
        double[] dxs = new double[4];
        double[] dys = new double[4];
        for (int i = 0; i < 4; i++) { dxs[i] = x + dx[i]; dys[i] = y + dy[i]; }
        VectorGraphics.poly(g, dxs, dys, 4, false);
    }
}
