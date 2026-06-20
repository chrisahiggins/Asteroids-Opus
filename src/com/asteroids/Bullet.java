package com.asteroids;

import java.awt.Graphics2D;

/** A shot fired by the player or a saucer. Wraps around the screen and expires after a set range. */
public class Bullet {
    public double x, y, vx, vy;
    public double life;
    public final boolean fromPlayer;

    public Bullet(double x, double y, double vx, double vy, double life, boolean fromPlayer) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.fromPlayer = fromPlayer;
    }

    public boolean update(double dt) {
        x = Game.wrap(x + vx * dt, Game.WORLD_W);
        y = Game.wrap(y + vy * dt, Game.WORLD_H);
        life -= dt;
        return life > 0;
    }

    public void draw(Graphics2D g) {
        VectorGraphics.dot(g, x, y, 1.6);
    }
}
