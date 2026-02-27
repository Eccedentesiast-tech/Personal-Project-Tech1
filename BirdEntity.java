package com.santarita.flappybird;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;

public class BirdEntity {
    private static final float JUMP_VELOCITY = -15f;
    private static final float GRAVITY = 1f;

    public float x, y;
    private float velocityY = 0;
    private final float birdWidth;
    private final float birdHeight;
    private boolean isDead = false;

    private Bitmap[] currentBirdFrames;
    private int frameIndex = 0;
    private long lastFrameTime = 0;
    private static final int FRAME_DURATION = 200;
    private final int screenHeight;

    private static final int[][] BIRD_THEMES = {
            {R.drawable.bird1, R.drawable.bird1, R.drawable.bird1},
            {R.drawable.bird2, R.drawable.bird2, R.drawable.bird2},
            {R.drawable.bird3, R.drawable.bird3, R.drawable.bird3}
    };

    public BirdEntity(Resources resources, int screenWidth, int screenHeight) {
        this.screenHeight = screenHeight;

        Bitmap tempFrame = BitmapFactory.decodeResource(resources, BIRD_THEMES[0][0]);
        birdHeight = screenHeight / 12f;
        birdWidth = tempFrame.getWidth() * (birdHeight / tempFrame.getHeight());

        setTheme(resources, 0);

        x = screenWidth / 4f - birdWidth / 2f;
        y = screenHeight / 2f - birdHeight / 2f;
    }

    public void setTheme(Resources resources, int themeIndex) {
        if (themeIndex < 0 || themeIndex >= BIRD_THEMES.length) themeIndex = 0;
        int[] themeDrawables = BIRD_THEMES[themeIndex];
        currentBirdFrames = new Bitmap[themeDrawables.length];

        for (int i = 0; i < themeDrawables.length; i++) {
            Bitmap rawFrame = BitmapFactory.decodeResource(resources, themeDrawables[i]);
            Bitmap scaledFrame = Bitmap.createScaledBitmap(rawFrame, (int) birdWidth, (int) birdHeight, true);
            currentBirdFrames[i] = makeTransparent(scaledFrame);
        }
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
            if (r < 15 && g < 15 && b < 15) pixels[i] = Color.TRANSPARENT;
        }
        newBitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return newBitmap;
    }

    public void update() {
        if (isDead) {
            if (y + birdHeight < screenHeight) {
                velocityY += GRAVITY * 0.5f;
                y += velocityY;
            } else {
                y = screenHeight - birdHeight;
                velocityY = 0;
            }
            return;
        }
        velocityY += GRAVITY;
        y += velocityY;
        if (y < 0) {
            y = 0;
            velocityY = 0;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastFrameTime > FRAME_DURATION) {
            frameIndex = (frameIndex + 1) % currentBirdFrames.length;
            lastFrameTime = currentTime;
        }
    }

    public void draw(Canvas canvas) {
        if (canvas != null && currentBirdFrames != null) {
            Bitmap currentFrame = currentBirdFrames[frameIndex];
            canvas.drawBitmap(currentFrame, x, y, null);
        }
    }

    public void jump() {
        if (!isDead) velocityY = JUMP_VELOCITY;
    }

    public Rect getBounds() {
        // --- FIX: INCREASED PADDING ---
        // Changed from 0.1 (10%) to 0.20 (20%).
        // This makes the invisible hitbox smaller than the bird image.
        // It prevents dying when you hit "empty corners" of the bird image.
        int paddingX = (int) (birdWidth * 0.20);
        int paddingY = (int) (birdHeight * 0.20);

        return new Rect(
                (int) x + paddingX,
                (int) y + paddingY,
                (int) (x + birdWidth) - paddingX,
                (int) (y + birdHeight) - paddingY
        );
    }

    public boolean isDead() { return isDead; }
    public void setDead(boolean dead) { isDead = dead; }
    public float getBirdHeight() { return birdHeight; }
    public float getBirdWidth() { return birdWidth; }
}