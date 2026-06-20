package com.asteroids;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.Set;

/** Window surface, ~60 FPS game loop, keyboard input, screen state machine and all rendering. */
public class GamePanel extends JPanel {
    private static final int W = Game.WORLD_W;
    private static final int H = Game.WORLD_H;

    private final Sound sound = new Sound();
    private final HighScores highScores = new HighScores();
    private final Game game = new Game(sound);

    private GameState state = GameState.ATTRACT;
    private final Set<Integer> down = new HashSet<Integer>();

    private long lastNanos;
    private double blinkClock;
    private double endTimer;       // banner delay before leaving a finished game
    private String nameBuffer = "";

    private JFrame frame;
    private boolean fullscreen;

    public GamePanel() {
        setPreferredSize(new Dimension(W, H));
        setBackground(Color.BLACK);
        setFocusable(true);

        game.startAttract();

        addKeyListener(new KeyAdapter() {
            public void keyPressed(KeyEvent e) { onKeyPressed(e); }
            public void keyReleased(KeyEvent e) { onKeyReleased(e); }
            public void keyTyped(KeyEvent e) { onKeyTyped(e); }
        });
        addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) { requestFocusInWindow(); }
        });
    }

    public void setFrame(JFrame f) { this.frame = f; }

    public void start() {
        lastNanos = System.nanoTime();
        Timer timer = new Timer(16, new ActionListener() {
            public void actionPerformed(ActionEvent e) { tick(); }
        });
        timer.start();
        requestFocusInWindow();
    }

    // ---- main loop ----------------------------------------------------------

    private void tick() {
        long now = System.nanoTime();
        double dt = (now - lastNanos) / 1_000_000_000.0;
        lastNanos = now;
        if (dt > 0.05) dt = 0.05;
        blinkClock += dt;

        switch (state) {
            case ATTRACT:
                game.update(dt);
                break;
            case PLAYING:
                game.rotateInput = (down.contains(KeyEvent.VK_RIGHT) ? 1 : 0) - (down.contains(KeyEvent.VK_LEFT) ? 1 : 0);
                game.thrustInput = down.contains(KeyEvent.VK_UP);
                game.update(dt);
                if (game.gameOver) {
                    state = GameState.GAME_OVER;
                    endTimer = 2.4;
                    sound.stopAllLoops();
                }
                break;
            case GAME_OVER:
                game.update(dt); // keep the field animating behind the banner
                endTimer -= dt;
                if (endTimer <= 0) finishGame();
                break;
            case PAUSED:
            case ENTER_NAME:
                break; // frozen
        }
        repaint();
    }

    private void finishGame() {
        sound.stopAllLoops();
        if (highScores.qualifies(game.score)) {
            nameBuffer = "";
            state = GameState.ENTER_NAME;
        } else {
            toAttract();
        }
    }

    private void toAttract() {
        sound.stopAllLoops();
        game.startAttract();
        state = GameState.ATTRACT;
    }

    private void startGame() {
        sound.stopAllLoops();
        game.startNewGame();
        state = GameState.PLAYING;
    }

    // ---- input --------------------------------------------------------------

    private void onKeyPressed(KeyEvent e) {
        int code = e.getKeyCode();

        // Global toggles.
        if (code == KeyEvent.VK_F11) { toggleFullscreen(); return; }
        if (code == KeyEvent.VK_ESCAPE) { if (fullscreen) toggleFullscreen(); return; }

        if (down.contains(code)) return; // ignore auto-repeat
        down.add(code);

        if (code == KeyEvent.VK_G) { VectorGraphics.glow = !VectorGraphics.glow; return; }

        switch (state) {
            case ATTRACT:
                if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ENTER) startGame();
                break;
            case PLAYING:
                if (code == KeyEvent.VK_SPACE) game.fire();
                else if (code == KeyEvent.VK_DOWN) game.hyperspace();
                else if (code == KeyEvent.VK_P) pause();
                else if (code == KeyEvent.VK_R) endRun();
                break;
            case PAUSED:
                if (code == KeyEvent.VK_P) resume();
                else if (code == KeyEvent.VK_R) endRun();
                break;
            case GAME_OVER:
                if (code == KeyEvent.VK_SPACE || code == KeyEvent.VK_ENTER || code == KeyEvent.VK_R) finishGame();
                break;
            case ENTER_NAME:
                if (code == KeyEvent.VK_ENTER) confirmName();
                else if (code == KeyEvent.VK_BACK_SPACE && nameBuffer.length() > 0)
                    nameBuffer = nameBuffer.substring(0, nameBuffer.length() - 1);
                break;
        }
    }

    private void onKeyReleased(KeyEvent e) {
        down.remove(e.getKeyCode());
    }

    private void onKeyTyped(KeyEvent e) {
        if (state != GameState.ENTER_NAME) return;
        char c = Character.toUpperCase(e.getKeyChar());
        if ((c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == ' ' || c == '.' || c == '-') {
            if (nameBuffer.length() < 12) nameBuffer += c;
        }
    }

    private void confirmName() {
        String name = nameBuffer.trim();
        if (name.isEmpty()) name = "AAA";
        highScores.add(name, game.score);
        toAttract();
    }

    private void endRun() {
        sound.stopAllLoops();
        if (highScores.qualifies(game.score)) {
            nameBuffer = "";
            state = GameState.ENTER_NAME;
        } else {
            toAttract();
        }
    }

    private void pause() {
        state = GameState.PAUSED;
        sound.stopAllLoops();
    }

    private void resume() {
        state = GameState.PLAYING;
        if (game.saucer != null) sound.saucer(game.saucer.type, true);
        lastNanos = System.nanoTime();
    }

    // ---- fullscreen ---------------------------------------------------------

    private void toggleFullscreen() {
        if (frame == null) return;
        boolean wasVisible = frame.isVisible();
        frame.dispose();
        fullscreen = !fullscreen;
        frame.setUndecorated(fullscreen);
        if (fullscreen) {
            frame.setResizable(false);
            Rectangle b = frame.getGraphicsConfiguration().getBounds();
            frame.setBounds(b);
        } else {
            frame.setResizable(true);
            frame.setSize(W, H);
            frame.setLocationRelativeTo(null);
        }
        if (wasVisible) frame.setVisible(true);
        requestFocusInWindow();
    }

    // ---- rendering ----------------------------------------------------------

    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setColor(Color.BLACK);
        g2.fillRect(0, 0, getWidth(), getHeight());

        double scale = Math.min(getWidth() / (double) W, getHeight() / (double) H);
        double ox = (getWidth() - W * scale) / 2.0;
        double oy = (getHeight() - H * scale) / 2.0;
        g2.translate(ox, oy);
        g2.scale(scale, scale);

        g2.setColor(Color.WHITE);
        drawEntities(g2);

        switch (state) {
            case ATTRACT:   drawAttract(g2); break;
            case PLAYING:   drawHud(g2); break;
            case PAUSED:    drawHud(g2); drawPaused(g2); break;
            case GAME_OVER: drawHud(g2); center(g2, "GAME OVER", H / 2.0 - 30, 60); break;
            case ENTER_NAME: drawEnterName(g2); break;
        }
        g2.dispose();
    }

    private void drawEntities(Graphics2D g) {
        for (Asteroid a : game.asteroids) a.draw(g);
        for (Particle p : game.particles) p.draw(g);
        for (Bullet b : game.playerBullets) b.draw(g);
        for (Bullet b : game.saucerBullets) b.draw(g);
        if (game.saucer != null) game.saucer.draw(g);
        if (game.ship != null) game.ship.draw(g);
    }

    private void drawHud(Graphics2D g) {
        VectorGraphics.text(g, String.valueOf(game.score), 40, 24, 34, false);
        int hi = Math.max(highScores.highest(), game.score);
        VectorGraphics.text(g, String.valueOf(hi), W / 2.0, 18, 22, true);
        for (int i = 0; i < game.lives; i++) {
            Ship.drawShape(g, 52 + i * 26, 92, -Math.PI / 2, 1.0);
        }
    }

    private void drawPaused(Graphics2D g) {
        center(g, "PAUSED", H / 2.0 - 40, 60);
        center(g, "PRESS P TO RESUME", H / 2.0 + 40, 22);
    }

    private void drawAttract(Graphics2D g) {
        center(g, "ASTEROIDS", 70, 86);

        if (blink()) center(g, "PRESS SPACE TO START", 188, 26);

        center(g, "HIGH SCORES", 238, 24);
        double y = 274;
        int rank = 1;
        for (HighScores.Entry e : highScores.entries()) {
            String left = pad2(rank) + "   " + e.name;
            VectorGraphics.text(g, left, W / 2.0 - 150, y, 19, false);
            String sc = String.valueOf(e.score);
            VectorGraphics.text(g, sc, W / 2.0 + 150 - VectorGraphics.textWidth(sc, 19), y, 19, false);
            y += 25;
            rank++;
        }

        center(g, "CONTROLS", 548, 22);
        String[] lines = {
                "LEFT / RIGHT ARROW - ROTATE",
                "UP ARROW - THRUST     SPACE - FIRE",
                "DOWN ARROW - HYPERSPACE",
                "P - PAUSE     R - END GAME",
                "F11 - FULLSCREEN     G - GLOW",
        };
        double ly = 582;
        for (String s : lines) {
            center(g, s, ly, 16);
            ly += 26;
        }
    }

    private void drawEnterName(Graphics2D g) {
        center(g, "GAME OVER", 120, 56);
        center(g, "NEW HIGH SCORE", 240, 40);
        center(g, "SCORE " + game.score, 310, 26);
        center(g, "ENTER YOUR NAME", 400, 24);
        String shown = nameBuffer + (blink() ? "_" : " ");
        center(g, shown, 460, 34);
        center(g, "PRESS ENTER", 560, 20);
    }

    private void center(Graphics2D g, String s, double y, double size) {
        VectorGraphics.text(g, s, W / 2.0, y, size, true);
    }

    private boolean blink() {
        return ((int) (blinkClock * 1.6)) % 2 == 0;
    }

    private static String pad2(int n) {
        return (n < 10 ? " " : "") + n;
    }
}
