package com.asteroids;

import java.awt.Graphics2D;
import java.util.Random;

/** The player ship: rotation, thrust with inertia/friction, screen wrap, firing and hyperspace. */
public class Ship {
    private static final double ROT_SPEED = 3.7;      // rad/s
    private static final double THRUST_ACC = 270;     // px/s^2
    private static final double FRICTION = 0.42;      // velocity damping per second
    private static final double MAX_SPEED = 430;      // px/s
    public static final double RADIUS = 11;

    private static final double BULLET_SPEED = 560;
    private static final double BULLET_LIFE = 0.85;

    public double x, y, vx, vy;
    public double angle = -Math.PI / 2; // pointing up
    public boolean thrusting;
    public boolean alive = true;

    public double invuln;     // seconds of spawn invulnerability remaining
    public double blink;      // visual blink phase while invulnerable
    private double hyperCooldown;
    private double flame;      // thrust flame flicker phase

    private final Random rnd;

    public Ship(Random rnd) {
        this.rnd = rnd;
        reset();
    }

    public void reset() {
        x = Game.WORLD_W / 2.0;
        y = Game.WORLD_H / 2.0;
        vx = vy = 0;
        angle = -Math.PI / 2;
        alive = true;
        thrusting = false;
        invuln = 2.5;
        blink = 0;
        hyperCooldown = 0;
    }

    /** @param rotateInput -1 = left, +1 = right, 0 = none */
    public void update(double dt, int rotateInput) {
        if (!alive) return;
        angle += rotateInput * ROT_SPEED * dt;

        if (thrusting) {
            vx += Math.cos(angle) * THRUST_ACC * dt;
            vy += Math.sin(angle) * THRUST_ACC * dt;
            flame += dt * 40;
        }
        vx -= vx * FRICTION * dt;
        vy -= vy * FRICTION * dt;

        double sp = Math.sqrt(vx * vx + vy * vy);
        if (sp > MAX_SPEED) {
            vx = vx / sp * MAX_SPEED;
            vy = vy / sp * MAX_SPEED;
        }

        x = Game.wrap(x + vx * dt, Game.WORLD_W);
        y = Game.wrap(y + vy * dt, Game.WORLD_H);

        if (invuln > 0) {
            invuln -= dt;
            blink += dt;
        }
        if (hyperCooldown > 0) hyperCooldown -= dt;
    }

    public Bullet fire() {
        double nx = x + Math.cos(angle) * RADIUS;
        double ny = y + Math.sin(angle) * RADIUS;
        return new Bullet(nx, ny,
                vx + Math.cos(angle) * BULLET_SPEED,
                vy + Math.sin(angle) * BULLET_SPEED,
                BULLET_LIFE, true);
    }

    public boolean canHyperspace() {
        return alive && hyperCooldown <= 0;
    }

    /** Jump to a random location. @return true if the jump misfires (ship should explode). */
    public boolean hyperspace() {
        x = 40 + rnd.nextDouble() * (Game.WORLD_W - 80);
        y = 40 + rnd.nextDouble() * (Game.WORLD_H - 80);
        vx = vy = 0;
        hyperCooldown = 0.8;
        invuln = Math.max(invuln, 0.4);
        return rnd.nextDouble() < 0.125;
    }

    public boolean visibleNow() {
        // Blink (disappear briefly) while invulnerable after respawn.
        return invuln <= 0 || ((int) (blink * 12) % 2 == 0);
    }

    public void draw(Graphics2D g) {
        if (!alive || !visibleNow()) return;
        drawShape(g, x, y, angle, 1.0);
        if (thrusting && ((int) flame % 2 == 0)) {
            drawFlame(g, x, y, angle);
        }
    }

    /**
     * Render the ship outline at an arbitrary position/scale (used for the HUD life icons too).
     * The authentic Asteroids ship is an elongated dart whose tail is a concave V-notch
     * (pointing toward the nose) rather than a flat base — that notch is what distinguishes
     * it from a plain triangle.
     */
    public static void drawShape(Graphics2D g, double cx, double cy, double ang, double scale) {
        double nose = 16 * scale, back = 11 * scale, wing = 8 * scale, notch = 4 * scale;
        // nose -> right rear corner -> tail notch -> left rear corner (closed back to nose).
        double[][] pts = {
                {nose, 0},
                {-back, wing},
                {-notch, 0},
                {-back, -wing},
        };
        drawRotated(g, cx, cy, ang, pts, true);
    }

    private static void drawFlame(Graphics2D g, double cx, double cy, double ang) {
        double[][] pts = {{-7, 4}, {-16, 0}, {-7, -4}};
        drawRotated(g, cx, cy, ang, pts, false);
    }

    private static void drawRotated(Graphics2D g, double cx, double cy, double ang, double[][] pts, boolean closed) {
        double cos = Math.cos(ang), sin = Math.sin(ang);
        double[] xs = new double[pts.length];
        double[] ys = new double[pts.length];
        for (int i = 0; i < pts.length; i++) {
            xs[i] = cx + pts[i][0] * cos - pts[i][1] * sin;
            ys[i] = cy + pts[i][0] * sin + pts[i][1] * cos;
        }
        VectorGraphics.poly(g, xs, ys, pts.length, closed);
    }
}
