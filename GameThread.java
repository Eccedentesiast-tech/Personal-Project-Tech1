package com.santarita.flappybird;

import android.graphics.Canvas;
import android.view.SurfaceHolder;

/**
 * GameThread is responsible for managing the main game loop.
 * It continuously calls the update and draw methods on the GameView.
 */
public class GameThread extends Thread {
    private final SurfaceHolder surfaceHolder;
    private final GameView gameView;
    private boolean isRunning;
    private boolean isPaused;

    // Target FPS and frame time calculation
    private static final int MAX_FPS = 60;
    private static final int FRAME_PERIOD = 1000 / MAX_FPS;

    /**
     * Constructor for the GameThread.
     * @param surfaceHolder The holder for the surface.
     * @param gameView The view containing the game logic and drawing.
     */
    public GameThread(SurfaceHolder surfaceHolder, GameView gameView) {
        this.surfaceHolder = surfaceHolder;
        this.gameView = gameView;
        this.isPaused = false;
    }

    /**
     * Sets the state of the game loop (running or paused).
     * @param running True to run the loop, false to stop.
     */
    public void setRunning(boolean running) {
        isRunning = running;
    }

    /**
     * Pauses the game loop.
     */
    public void pause() {
        isPaused = true;
    }

    /**
     * Resumes the game loop.
     */
    public void resumeGame() {
        isPaused = false;
    }

    /**
     * The main method for the thread, executing the game loop.
     */
    @Override
    public void run() {
        long startTime;
        long timeMillis;
        long waitTime;

        while (isRunning) {
            if (!isPaused) {
                startTime = System.currentTimeMillis();
                Canvas canvas = null;

                try {
                    // Get the canvas to draw on, locking the surface
                    canvas = this.surfaceHolder.lockCanvas();
                    synchronized (surfaceHolder) {
                        // Update game state
                        this.gameView.update();
                        // Draw the game state onto the canvas
                        this.gameView.draw(canvas);
                    }
                } catch (Exception e) {
                    // Handle exceptions during drawing/locking
                } finally {
                    if (canvas != null) {
                        // Unlock the surface and post the canvas contents
                        surfaceHolder.unlockCanvasAndPost(canvas);
                    }
                }

                // Calculate time taken for the loop
                timeMillis = System.currentTimeMillis() - startTime;
                waitTime = FRAME_PERIOD - timeMillis;

                try {
                    // Pause the thread to meet the target FPS
                    if (waitTime > 0) {
                        //noinspection BusyWait
                        sleep(waitTime);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }
}