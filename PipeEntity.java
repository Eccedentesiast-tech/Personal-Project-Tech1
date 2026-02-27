package com.santarita.flappybird;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import java.util.Random;

public class PipeEntity {
    // REMOVED fixed pixel constants (400, 600) to fix difficulty on different screens
    private static final float PIPE_SCROLL_SPEED = 10f;

    public float x;
    private final float topPipeHeight;
    private final float pipeGap;
    private final float pipeWidth;

    private final Bitmap topPipeBitmap;
    private final Bitmap bottomPipeBitmap;

    private boolean passed = false;
    private final int screenHeight;

    public PipeEntity(int screenWidth, int screenHeight, Bitmap topBitmap, Bitmap bottomBitmap) {
        this.screenHeight = screenHeight;

        // Define a fixed pipe width
        pipeWidth = screenWidth / 6f;

        // --- FIX 1: DYNAMIC GAP SIZE ---
        // Make the gap 25% of the screen height.
        // This ensures the gap is always passable regardless of screen resolution.
        this.pipeGap = screenHeight * 0.25f;

        // 1. Scale the bitmaps
        float scaleFactor = pipeWidth / topBitmap.getWidth();
        Bitmap scaledTop = Bitmap.createScaledBitmap(topBitmap, (int) pipeWidth, (int) (topBitmap.getHeight() * scaleFactor), true);
        Bitmap scaledBottom = Bitmap.createScaledBitmap(bottomBitmap, (int) pipeWidth, (int) (bottomBitmap.getHeight() * scaleFactor), true);

        // 2. FORCE TRANSPARENCY
        this.topPipeBitmap = makeTransparent(scaledTop);
        this.bottomPipeBitmap = makeTransparent(scaledBottom);

        x = screenWidth;

        Random random = new Random();

        // --- FIX 2: SAFER RANDOM RANGES ---
        // Ensure the gap doesn't spawn too high or too low
        int minPipeHeight = (int) (screenHeight * 0.15f); // Minimum pipe length
        int maxPipeHeight = (int) (screenHeight * 0.6f);  // Maximum pipe length

        // Randomly choose the bottom of the top pipe
        topPipeHeight = minPipeHeight + random.nextInt(maxPipeHeight - minPipeHeight);
    }

    private Bitmap makeTransparent(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        int[] pixels = new int[width * height];
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height);

        for (int i = 0; i < pixels.length; i++) {
            int pixel = pixels[i];
            int r = Color.red(pixel);
            int g = Color.green(pixel);
            int b = Color.blue(pixel);
            if (r < 15 && g < 15 && b < 15) {
                pixels[i] = Color.TRANSPARENT;
            }
        }
        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBitmap;
    }

    public void update() {
        x -= PIPE_SCROLL_SPEED;
    }

    public void draw(Canvas canvas) {
        if (canvas != null) {
            // Draw Top Pipe
            Rect destTop = new Rect((int) x, 0, (int) (x + pipeWidth), (int) topPipeHeight);
            canvas.drawBitmap(topPipeBitmap, null, destTop, null);

            // Draw Bottom Pipe
            float bottomPipeY = topPipeHeight + pipeGap;
            Rect destBottom = new Rect((int) x, (int) bottomPipeY, (int) (x + pipeWidth), screenHeight);
            canvas.drawBitmap(bottomPipeBitmap, null, destBottom, null);
        }
    }

    public boolean checkCollision(Rect birdBounds) {
        Rect topPipeBounds = new Rect((int) x, 0, (int) (x + pipeWidth), (int) topPipeHeight);
        float bottomPipeY = topPipeHeight + pipeGap;
        Rect bottomPipeBounds = new Rect((int) x, (int) bottomPipeY, (int) (x + pipeWidth), screenHeight);

        return Rect.intersects(birdBounds, topPipeBounds) || Rect.intersects(birdBounds, bottomPipeBounds);
    }

    public float getX() { return x; }
    public float getWidth() { return pipeWidth; }
    public boolean isPassed() { return passed; }
    public void setPassed(boolean passed) { this.passed = passed; }
}