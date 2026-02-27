package com.santarita.flappybird;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;

/**
 * Manages scrolling backgrounds, ground, and theme switching.
 */
public class BackgroundManager {
    // Theme structure: {Background ID, Pipe Top ID, Pipe Bottom ID}
    private static final int[][] THEMES = {
            {R.drawable.bg1, R.drawable.tp3, R.drawable.bp3},    // Day (Green Pipes)
            {R.drawable.bg2, R.drawable.tp1, R.drawable.bp1},    // Sunset (Orange/Brown Pipes)
            {R.drawable.bg3, R.drawable.tp2, R.drawable.bp2}     // Night/Snow (Ice/Blue Pipes)
    };
    private static final float SCROLL_SPEED = 10f; // Must match Pipe.PIPE_SCROLL_SPEED

    private final Resources resources;
    private final int screenWidth;
    private final int screenHeight;

    private int currentThemeIndex = 1;

    // Background scrolling
    private Bitmap currentBackground;
    private float backgroundX1 = 0;
    private float backgroundX2;

    // Ground
    private final Bitmap groundBitmap;
    private final float groundY;
    private float groundX1 = 0;
    private float groundX2;

    /**
     * Constructor for BackgroundManager.
     */
    public BackgroundManager(Resources resources, int screenWidth, int screenHeight, float groundHeight) {
        this.resources = resources;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        // Load ground and scale it to cover the width
        Bitmap rawGround = BitmapFactory.decodeResource(resources, R.drawable.ground);
        groundBitmap = Bitmap.createScaledBitmap(rawGround, screenWidth, (int) groundHeight, true);
        groundY = screenHeight - groundHeight;

        backgroundX2 = screenWidth;
        groundX2 = screenWidth;

        loadBackgroundTheme();
    }

    /**
     * Loads the background image for the current theme and scales it to fit the screen.
     */
    private void loadBackgroundTheme() {
        int bgResId = THEMES[currentThemeIndex][0];
        Bitmap rawBackground = BitmapFactory.decodeResource(resources, bgResId);
        // Scale background to fill the screen
        currentBackground = Bitmap.createScaledBitmap(rawBackground, screenWidth, screenHeight, true);
    }

    /**
     * Changes the theme and reloads the background. (Renamed from changeTheme)
     */
    public void switchTheme() {
        currentThemeIndex = (currentThemeIndex + 1) % THEMES.length;
        loadBackgroundTheme();
    }

    /**
     * The main update method.
     * @param isGameOver If true, scrolling stops.
     */
    public void update(boolean isGameOver) {
        // 1. Stop scrolling if the game is over
        if (isGameOver) {
            return;
        }

        // 2. Background Scrolling
        backgroundX1 -= SCROLL_SPEED;
        backgroundX2 -= SCROLL_SPEED;

        // GAP FIX: Instead of setting to screenWidth, we set it relative to the OTHER background.
        // This ensures they stay "glued" together even if frame rates fluctuate.
        if (backgroundX1 + screenWidth <= 0) {
            backgroundX1 = backgroundX2 + screenWidth;
        }
        if (backgroundX2 + screenWidth <= 0) {
            backgroundX2 = backgroundX1 + screenWidth;
        }

        // 3. Ground Scrolling
        groundX1 -= SCROLL_SPEED;
        groundX2 -= SCROLL_SPEED;

        // GAP FIX for Ground as well
        if (groundX1 + screenWidth <= 0) {
            groundX1 = groundX2 + screenWidth;
        }
        if (groundX2 + screenWidth <= 0) {
            groundX2 = groundX1 + screenWidth;
        }
    }

    /**
     * Draws the backgrounds and the ground.
     * @param canvas The canvas to draw on.
     */
    public void draw(Canvas canvas) {
        if (canvas != null) {
            // Draw background (two instances for seamless scrolling)
            // Use Math.ceil or casting to ensure no sub-pixel gaps in rendering
            canvas.drawBitmap(currentBackground, (int)backgroundX1, 0, null);
            canvas.drawBitmap(currentBackground, (int)backgroundX2, 0, null);

            // Draw ground (two instances for seamless scrolling)
            canvas.drawBitmap(groundBitmap, (int)groundX1, groundY, null);
            canvas.drawBitmap(groundBitmap, (int)groundX2, groundY, null);
        }
    }

    public int getCurrentThemeIndex() {
        return currentThemeIndex;
    }

    /**
     * Gets the bitmap for the top pipe of the current theme.
     */
    public Bitmap getTopPipeBitmap() {
        return BitmapFactory.decodeResource(resources, THEMES[currentThemeIndex][1]);
    }

    /**
     * Gets the bitmap for the bottom pipe of the current theme.
     */
    public Bitmap getBottomPipeBitmap() {
        return BitmapFactory.decodeResource(resources, THEMES[currentThemeIndex][2]);
    }

    public float getGroundY() {
        return groundY;
    }
}