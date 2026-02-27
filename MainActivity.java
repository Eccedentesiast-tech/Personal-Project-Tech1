package com.santarita.flappybird;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.WindowInsets;
import android.view.WindowInsetsController;

public class MainActivity extends Activity {
    private GameView gameView;
    private MediaPlayer musicPlayer;
    private MediaPlayer gameOverSoundPlayer;
    private MediaPlayer scoreSoundPlayer;
    // --- 1. Declare the Flap Sound Player ---
    private MediaPlayer flapSoundPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameView = new GameView(this);
        setContentView(gameView);
        gameView.setKeepScreenOn(true);

        // Initialize Game Over Sound
        try {
            gameOverSoundPlayer = MediaPlayer.create(this, R.raw.game_over_sound);
            if (gameOverSoundPlayer != null) gameOverSoundPlayer.setLooping(false);
        } catch (Exception e) {}

        // Initialize Score Sound
        try {
            scoreSoundPlayer = MediaPlayer.create(this, R.raw.point_sound);
        } catch (Exception e) {}

        // --- 2. Initialize Flap Sound ---
        try {
            // Make sure your file is named 'flap_sound' in res/raw/
            flapSoundPlayer = MediaPlayer.create(this, R.raw.flap_sound);
        } catch (Exception e) {
            // Handle missing file
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            setImmersiveMode();
        }
    }

    private void setImmersiveMode() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        }
    }

    private void startMusic() {
        musicPlayer = MediaPlayer.create(this, R.raw.background_music);
        if (musicPlayer != null) {
            musicPlayer.setLooping(true);
            musicPlayer.start();
        }
    }

    public void playScoreSound() {
        if (scoreSoundPlayer != null) {
            if (scoreSoundPlayer.isPlaying()) {
                scoreSoundPlayer.seekTo(0);
            }
            scoreSoundPlayer.start();
        }
    }

    // --- 3. Add Method to Play Flap Sound ---
    public void playFlapSound() {
        if (flapSoundPlayer != null) {
            // Reset to start if the user taps quickly
            if (flapSoundPlayer.isPlaying()) {
                flapSoundPlayer.seekTo(0);
            }
            flapSoundPlayer.start();
        }
    }

    public void onGameOver() {
        if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
        if (gameOverSoundPlayer != null) {
            gameOverSoundPlayer.seekTo(0);
            gameOverSoundPlayer.start();
        }
    }

    public void onGamePause() {
        if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
    }

    public void onGameRestart() {
        if (gameOverSoundPlayer != null && gameOverSoundPlayer.isPlaying()) {
            gameOverSoundPlayer.pause();
            gameOverSoundPlayer.seekTo(0);
        }
        if (musicPlayer == null) startMusic();
        else if (!musicPlayer.isPlaying()) musicPlayer.start();
    }

    public void onGameExit() {
        if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
        finish();
    }

    @Override
    protected void onPause() {
        super.onPause();
        gameView.pause();
        if (musicPlayer != null && musicPlayer.isPlaying()) musicPlayer.pause();
        if (gameOverSoundPlayer != null && gameOverSoundPlayer.isPlaying()) gameOverSoundPlayer.pause();
        if (scoreSoundPlayer != null && scoreSoundPlayer.isPlaying()) scoreSoundPlayer.pause();
        // --- Pause Flap Player ---
        if (flapSoundPlayer != null && flapSoundPlayer.isPlaying()) flapSoundPlayer.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        gameView.resume();
        if (musicPlayer != null && !musicPlayer.isPlaying() && (gameOverSoundPlayer == null || !gameOverSoundPlayer.isPlaying())) {
            musicPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (musicPlayer != null) { musicPlayer.stop(); musicPlayer.release(); musicPlayer = null; }
        if (gameOverSoundPlayer != null) { gameOverSoundPlayer.stop(); gameOverSoundPlayer.release(); gameOverSoundPlayer = null; }
        if (scoreSoundPlayer != null) { scoreSoundPlayer.release(); scoreSoundPlayer = null; }

        // --- 4. Release Flap Player ---
        if (flapSoundPlayer != null) {
            flapSoundPlayer.release();
            flapSoundPlayer = null;
        }
    }
}