package com.asteroids;

import java.awt.Graphics2D;

/** Short-lived debris fragment used for explosions and thrust sparks. */
public class Particle {
    public double x, y, vx, vy;
    public double life, maxLife;

    public Particle(double x, double y, double vx, double vy, double life) {
        this.x = x; this.y = y; this.vx = vx; this.vy = vy;
        this.life = life; this.maxLife = life;
    }

    public boolean update(double dt) {
        x = Game.wrap(x + vx * dt, Game.WORLD_W);
        y = Game.wrap(y + vy * dt, Game.WORLD_H);
        life -= dt;
        return life > 0;
    }

    public void draw(Graphics2D g) {
        VectorGraphics.dot(g, x, y, 1.4);
    }
}
