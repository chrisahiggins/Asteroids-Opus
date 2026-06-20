package com.asteroids;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Core game model: holds all entities and advances the simulation each tick
 * (movement, collisions, wave progression, scoring, lives, saucer spawning and
 * the accelerating "heartbeat" beat). Rendering and input live in {@link GamePanel}.
 */
public class Game {
    public static final int WORLD_W = 1024;
    public static final int WORLD_H = 768;

    public static double wrap(double v, double max) {
        v %= max;
        if (v < 0) v += max;
        return v;
    }

    private final Random rnd = new Random();
    private final Sound sound;

    public Ship ship;
    public final List<Asteroid> asteroids = new ArrayList<Asteroid>();
    public final List<Bullet> playerBullets = new ArrayList<Bullet>();
    public final List<Bullet> saucerBullets = new ArrayList<Bullet>();
    public final List<Particle> particles = new ArrayList<Particle>();
    public Saucer saucer;

    public int score;
    public int lives;
    public int level;
    public boolean gameOver;
    public boolean attract;

    private int nextExtraLife;
    private double respawnTimer;
    private double saucerTimer;
    private int activeSaucerType = -1;

    // Heartbeat beat state.
    private double beatTimer;
    private double beatInterval;
    private int beatCount;
    private boolean beatHigh;

    // Input state set by GamePanel.
    public int rotateInput;
    public boolean thrustInput;

    public Game(Sound sound) {
        this.sound = sound;
    }

    // ---- lifecycle ----------------------------------------------------------

    public void startNewGame() {
        attract = false;
        gameOver = false;
        score = 0;
        lives = 3;
        level = 0;
        nextExtraLife = 10000;
        clearEntities();
        ship = new Ship(rnd);
        ship.invuln = 2.0;
        spawnWave();
        saucerTimer = 12 + rnd.nextDouble() * 8;
    }

    /** A non-interactive backdrop of drifting rocks for the welcome screen. */
    public void startAttract() {
        attract = true;
        gameOver = false;
        ship = null;
        saucer = null;
        activeSaucerType = -1;
        clearEntities();
        for (int i = 0; i < 7; i++) {
            int size = Asteroid.LARGE - rnd.nextInt(2);
            asteroids.add(new Asteroid(rnd.nextDouble() * WORLD_W, rnd.nextDouble() * WORLD_H, size, rnd));
        }
    }

    private void clearEntities() {
        asteroids.clear();
        playerBullets.clear();
        saucerBullets.clear();
        particles.clear();
        saucer = null;
        activeSaucerType = -1;
        if (sound != null) sound.stopAllLoops();
    }

    private void spawnWave() {
        level++;
        int count = Math.min(4 + (level - 1) * 2, 11);
        for (int i = 0; i < count; i++) {
            // Spawn away from the centre so the player isn't hit on arrival.
            double x, y;
            do {
                x = rnd.nextDouble() * WORLD_W;
                y = rnd.nextDouble() * WORLD_H;
            } while (Math.hypot(x - WORLD_W / 2.0, y - WORLD_H / 2.0) < 180);
            asteroids.add(new Asteroid(x, y, Asteroid.LARGE, rnd));
        }
        beatCount = 0;
        beatInterval = 0.62;
        beatTimer = beatInterval;
        beatHigh = false;
    }

    // ---- input actions ------------------------------------------------------

    public void fire() {
        if (attract || gameOver || ship == null || !ship.alive) return;
        if (playerBullets.size() >= 4) return;
        playerBullets.add(ship.fire());
        if (sound != null) sound.fire();
    }

    public void hyperspace() {
        if (ship == null || !ship.canHyperspace()) return;
        boolean misfire = ship.hyperspace();
        if (misfire) killShip();
    }

    public void setThrust(boolean on) {
        if (ship != null && ship.alive) {
            ship.thrusting = on;
            if (sound != null) sound.thrust(on && !attract);
        }
    }

    // ---- main update --------------------------------------------------------

    public void update(double dt) {
        for (int i = asteroids.size() - 1; i >= 0; i--) asteroids.get(i).update(dt);
        for (int i = particles.size() - 1; i >= 0; i--) {
            if (!particles.get(i).update(dt)) particles.remove(i);
        }

        if (attract) return; // backdrop only

        if (ship != null) {
            ship.thrusting = thrustInput && ship.alive;
            if (sound != null) sound.thrust(ship.thrusting);
            ship.update(dt, rotateInput);
        }

        for (int i = playerBullets.size() - 1; i >= 0; i--) {
            if (!playerBullets.get(i).update(dt)) playerBullets.remove(i);
        }
        for (int i = saucerBullets.size() - 1; i >= 0; i--) {
            if (!saucerBullets.get(i).update(dt)) saucerBullets.remove(i);
        }

        updateSaucer(dt);
        handleCollisions();

        if (asteroids.isEmpty() && !gameOver) {
            spawnWave();
        }

        // Respawn the ship after death once the area is clear.
        if (ship != null && !ship.alive && !gameOver) {
            respawnTimer -= dt;
            if (respawnTimer <= 0 && centerClear()) {
                ship.reset();
            }
        }

        updateBeat(dt);
        checkExtraLife();
    }

    private boolean centerClear() {
        for (Asteroid a : asteroids) {
            if (Math.hypot(a.x - WORLD_W / 2.0, a.y - WORLD_H / 2.0) < a.radius + 90) return false;
        }
        if (saucer != null && Math.hypot(saucer.x - WORLD_W / 2.0, saucer.y - WORLD_H / 2.0) < 160) return false;
        return true;
    }

    // ---- saucer -------------------------------------------------------------

    private void updateSaucer(double dt) {
        if (saucer == null) {
            saucerTimer -= dt;
            if (saucerTimer <= 0 && !gameOver) spawnSaucer();
            return;
        }
        saucer.update(dt);
        if (saucer.offscreen()) { removeSaucer(); return; }

        saucer.fireTimer -= dt;
        if (saucer.fireTimer <= 0) {
            fireSaucerBullet();
            saucer.fireTimer = 0.7 + rnd.nextDouble() * 1.0;
        }
    }

    private void spawnSaucer() {
        double smallProb = Math.min(0.85, 0.20 + score / 40000.0);
        int type = (rnd.nextDouble() < smallProb) ? Saucer.SMALL : Saucer.LARGE;
        saucer = new Saucer(type, rnd);
        activeSaucerType = type;
        if (sound != null) sound.saucer(type, true);
    }

    private void removeSaucer() {
        if (activeSaucerType >= 0 && sound != null) sound.saucer(activeSaucerType, false);
        saucer = null;
        activeSaucerType = -1;
        saucerTimer = Math.max(5, 13 - level * 0.5) + rnd.nextDouble() * 6;
    }

    private void fireSaucerBullet() {
        double angle;
        if (saucer.type == Saucer.SMALL && ship != null && ship.alive) {
            angle = Math.atan2(ship.y - saucer.y, ship.x - saucer.x);
            double sigma = Math.max(0.04, 0.45 - score / 30000.0);
            angle += (rnd.nextGaussian() * sigma);
        } else {
            angle = rnd.nextDouble() * Math.PI * 2;
        }
        double speed = 320;
        saucerBullets.add(new Bullet(saucer.x, saucer.y,
                Math.cos(angle) * speed, Math.sin(angle) * speed, 1.4, false));
        if (sound != null) sound.saucerShoot();
    }

    // ---- collisions ---------------------------------------------------------

    private void handleCollisions() {
        // Player bullets vs asteroids.
        for (int b = playerBullets.size() - 1; b >= 0; b--) {
            Bullet bullet = playerBullets.get(b);
            boolean consumed = false;
            for (int a = asteroids.size() - 1; a >= 0; a--) {
                Asteroid ast = asteroids.get(a);
                if (Math.hypot(bullet.x - ast.x, bullet.y - ast.y) < ast.radius) {
                    score += ast.score();
                    splitAsteroid(a);
                    playerBullets.remove(b);
                    consumed = true;
                    break;
                }
            }
            if (consumed) continue;
            // Player bullet vs saucer.
            if (saucer != null && Math.hypot(bullet.x - saucer.x, bullet.y - saucer.y) < saucer.radius) {
                score += saucer.score();
                explode(saucer.x, saucer.y, 16);
                if (sound != null) sound.bangLarge();
                removeSaucer();
                playerBullets.remove(b);
            }
        }

        // Saucer bullets vs asteroids (authentic: they break rocks too).
        for (int b = saucerBullets.size() - 1; b >= 0; b--) {
            Bullet bullet = saucerBullets.get(b);
            for (int a = asteroids.size() - 1; a >= 0; a--) {
                Asteroid ast = asteroids.get(a);
                if (Math.hypot(bullet.x - ast.x, bullet.y - ast.y) < ast.radius) {
                    splitAsteroid(a);
                    saucerBullets.remove(b);
                    break;
                }
            }
        }

        if (ship == null || !ship.alive) return;

        // Ship vs asteroids.
        for (int a = asteroids.size() - 1; a >= 0; a--) {
            Asteroid ast = asteroids.get(a);
            if (Math.hypot(ship.x - ast.x, ship.y - ast.y) < ast.radius + Ship.RADIUS) {
                if (ship.invuln <= 0) {
                    splitAsteroid(a);
                    killShip();
                }
                return;
            }
        }

        // Ship vs saucer body and saucer bullets.
        if (ship.invuln <= 0) {
            if (saucer != null && Math.hypot(ship.x - saucer.x, ship.y - saucer.y) < saucer.radius + Ship.RADIUS) {
                explode(saucer.x, saucer.y, 16);
                removeSaucer();
                killShip();
                return;
            }
            for (int b = saucerBullets.size() - 1; b >= 0; b--) {
                Bullet bullet = saucerBullets.get(b);
                if (Math.hypot(ship.x - bullet.x, ship.y - bullet.y) < Ship.RADIUS) {
                    saucerBullets.remove(b);
                    killShip();
                    return;
                }
            }
        }
    }

    private void splitAsteroid(int index) {
        Asteroid a = asteroids.get(index);
        if (sound != null) sound.bang(a.size);
        explode(a.x, a.y, a.size * 4 + 4);
        asteroids.remove(index);
        if (a.size > Asteroid.SMALL) {
            asteroids.add(new Asteroid(a.x, a.y, a.size - 1, rnd));
            asteroids.add(new Asteroid(a.x, a.y, a.size - 1, rnd));
        }
    }

    private void killShip() {
        if (ship == null || !ship.alive) return;
        explode(ship.x, ship.y, 22);
        if (sound != null) { sound.bangLarge(); sound.thrust(false); }
        ship.alive = false;
        ship.thrusting = false;
        lives--;
        if (lives <= 0) {
            gameOver = true;
            if (sound != null) sound.stopAllLoops();
        } else {
            respawnTimer = 1.6;
        }
    }

    private void explode(double x, double y, int count) {
        for (int i = 0; i < count; i++) {
            double a = rnd.nextDouble() * Math.PI * 2;
            double sp = 40 + rnd.nextDouble() * 140;
            particles.add(new Particle(x, y, Math.cos(a) * sp, Math.sin(a) * sp,
                    0.4 + rnd.nextDouble() * 0.5));
        }
    }

    // ---- beat & bonus -------------------------------------------------------

    private void updateBeat(double dt) {
        if (gameOver || (ship != null && !ship.alive)) return;
        beatTimer -= dt;
        if (beatTimer <= 0) {
            if (sound != null) sound.beat(beatHigh);
            beatHigh = !beatHigh;
            beatCount++;
            beatInterval = Math.max(0.26, 0.62 - beatCount * 0.012);
            beatTimer = beatInterval;
        }
    }

    private void checkExtraLife() {
        while (score >= nextExtraLife) {
            lives++;
            nextExtraLife += 10000;
            if (sound != null) sound.extraLife();
        }
    }
}
