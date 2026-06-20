package com.asteroids;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Toolkit;

/** Application entry point: builds the window (sized to fit the display) and starts the game loop. */
public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                JFrame frame = new JFrame("Asteroids");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setResizable(true);

                GamePanel panel = new GamePanel();
                panel.setFrame(frame);

                // Size the content to fit the screen while preserving the 4:3 world.
                // Use Java's own screen dimensions so the render scale-to-fit matches exactly,
                // regardless of Windows DPI virtualisation.
                Dimension scr = Toolkit.getDefaultToolkit().getScreenSize();
                int availW = scr.width - 60;
                int availH = scr.height - 100;
                double s = Math.min(availW / (double) Game.WORLD_W, availH / (double) Game.WORLD_H);
                if (s > 1.0) s = 1.0;
                int cw = (int) Math.round(Game.WORLD_W * s);
                int ch = (int) Math.round(Game.WORLD_H * s);
                panel.setPreferredSize(new Dimension(cw, ch));

                frame.setContentPane(panel);
                frame.setMinimumSize(new Dimension(512, 432));
                frame.pack();
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);

                System.out.println("screen=" + scr.width + "x" + scr.height
                        + " content=" + cw + "x" + ch);

                panel.start();
            }
        });
    }
}
