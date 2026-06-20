package com.asteroids;

/** The high-level screen the game is currently showing. */
public enum GameState {
    ATTRACT,    // welcome / startup screen (title, leaderboard, controls, drifting rocks)
    PLAYING,    // active game
    PAUSED,     // game frozen via the P key
    GAME_OVER,  // brief "game over" banner before returning to attract
    ENTER_NAME  // entering initials/name for a qualifying high score
}
