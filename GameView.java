package com.santarita.flappybird;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF; // Used for rounded buttons
import android.graphics.Rect;
import android.graphics.Typeface; // For styling
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import androidx.annotation.NonNull;
import android.graphics.Bitmap; // Required for Bitmap
import android.graphics.BitmapFactory; // Required for BitmapFactory
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GameView extends SurfaceView implements SurfaceHolder.Callback {
    // Game Components
    private GameThread thread;
    private BirdEntity bird;
    private BackgroundManager backgroundManager;
    private final List<PipeEntity> pipes = new ArrayList<>();

    // --- 1. EXPANDED GAME STATES ---
    public enum GameState {
        MENU, CREDITS, HIGH_SCORES, READY, PLAYING, PAUSED, GAME_OVER
    }
    // Start in MENU instead of READY
    private GameState gameState = GameState.MENU;

    private int score = 0;
    private int highScore = 0;
    private SharedPreferences prefs;

    private int pipesPassedInTheme = 0;
    private static final int PIPES_PER_THEME_CHANGE = 4;

    // Pipe Spawning
    private long lastPipeTime = 0;
    private static final int PIPE_INTERVAL_MS = 2000;

    // Reference to MainActivity
    private final MainActivity mainActivity;

    // Drawing and Text
    private final Paint scorePaint, messagePaint, pauseButtonPaint, menuButtonPaint, titlePaint, buttonBgPaint;
    private final Rect pauseButtonBounds;

    // --- NEW: MENU BACKGROUND VARIABLE ---
    private Bitmap menuBackground;

    // --- 2. MENU BUTTON BOUNDS ---
    private RectF btnPlay, btnScores, btnCredits, btnExit, btnBack;

    private int screenWidth, screenHeight;
    private float groundHeight;

    public GameView(Context context) {
        super(context);
        getHolder().addCallback(this);
        setFocusable(true);

        this.mainActivity = (MainActivity) context;

        // Load High Score
        prefs = context.getSharedPreferences("FlappyPrefs", Context.MODE_PRIVATE);
        highScore = prefs.getInt("high_score", 0);

        // --- PAINTS SETUP ---
        scorePaint = new Paint();
        scorePaint.setColor(Color.WHITE);
        scorePaint.setTextSize(60);
        scorePaint.setFakeBoldText(true);
        scorePaint.setTextAlign(Paint.Align.RIGHT);
        scorePaint.setShadowLayer(5, 0, 0, Color.BLACK);

        messagePaint = new Paint();
        messagePaint.setColor(Color.WHITE);
        messagePaint.setTextSize(50);
        messagePaint.setTextAlign(Paint.Align.CENTER);
        messagePaint.setShadowLayer(5, 0, 0, Color.BLACK);

        // Title Paint for "Floaty Head"
        titlePaint = new Paint();
        titlePaint.setColor(Color.rgb(255, 215, 0)); // Gold color
        titlePaint.setTextSize(100);
        titlePaint.setTypeface(Typeface.DEFAULT_BOLD);
        titlePaint.setTextAlign(Paint.Align.CENTER);
        titlePaint.setShadowLayer(10, 5, 5, Color.BLACK);

        // Button Background Paint
        buttonBgPaint = new Paint();
        buttonBgPaint.setColor(Color.rgb(70, 130, 180)); // Steel Blue
        buttonBgPaint.setStyle(Paint.Style.FILL);
        buttonBgPaint.setAntiAlias(true);

        // Button Text Paint
        menuButtonPaint = new Paint();
        menuButtonPaint.setColor(Color.WHITE);
        menuButtonPaint.setTextSize(50);
        menuButtonPaint.setTextAlign(Paint.Align.CENTER);
        menuButtonPaint.setFakeBoldText(true);

        pauseButtonPaint = new Paint();
        pauseButtonPaint.setColor(Color.argb(150, 0, 0, 0));
        pauseButtonPaint.setStyle(Paint.Style.FILL);

        pauseButtonBounds = new Rect(0, 0, 0, 0);
    }

    @Override
    public void surfaceCreated(@NonNull SurfaceHolder holder) {
        screenWidth = getWidth();
        screenHeight = getHeight();

        // Pause Button Layout
        int buttonSize = screenWidth / 10;
        int padding = 20;
        pauseButtonBounds.set(padding, padding, buttonSize + padding, buttonSize + padding);

        groundHeight = screenHeight / 10f;

        // --- 3. INITIALIZE MENU BUTTONS ---
        float btnWidth = screenWidth * 0.5f;
        float btnHeight = screenHeight * 0.08f;
        float centerX = screenWidth / 2f;
        float startY = screenHeight * 0.4f;
        float gap = btnHeight * 1.5f;

        btnPlay = new RectF(centerX - btnWidth/2, startY, centerX + btnWidth/2, startY + btnHeight);
        btnScores = new RectF(centerX - btnWidth/2, startY + gap, centerX + btnWidth/2, startY + btnHeight + gap);
        btnCredits = new RectF(centerX - btnWidth/2, startY + gap*2, centerX + btnWidth/2, startY + btnHeight + gap*2);
        btnExit = new RectF(centerX - btnWidth/2, startY + gap*3, centerX + btnWidth/2, startY + btnHeight + gap*3);

        // Back Button (for Credits/Score screens)
        btnBack = new RectF(centerX - btnWidth/2, screenHeight * 0.8f, centerX + btnWidth/2, screenHeight * 0.8f + btnHeight);

        // --- NEW: LOAD MENU BACKGROUND IMAGE ---
        Bitmap rawMenuBg = BitmapFactory.decodeResource(getResources(),R.drawable.menu_bg);
        menuBackground = Bitmap.createScaledBitmap(rawMenuBg, screenWidth, screenHeight, true);
        // -------------------------------------

        backgroundManager = new BackgroundManager(getResources(), screenWidth, screenHeight, groundHeight);
        bird = new BirdEntity(getResources(), screenWidth, screenHeight);

        // Ensure bird matches theme
        bird.setTheme(getResources(), backgroundManager.getCurrentThemeIndex());

        thread = new GameThread(getHolder(), this);
        thread.setRunning(true);
        thread.start();
    }

    @Override
    public void surfaceChanged(@NonNull SurfaceHolder holder, int format, int width, int height) {}

    @Override
    public void surfaceDestroyed(@NonNull SurfaceHolder holder) {
        boolean retry = true;
        thread.setRunning(false);
        while (retry) {
            try {
                thread.join();
                retry = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    public void update() {
        // Scroll background in MENU and READY states for visual appeal
        boolean shouldScroll = (gameState == GameState.PLAYING || gameState == GameState.MENU || gameState == GameState.READY || gameState == GameState.CREDITS || gameState == GameState.HIGH_SCORES);
        backgroundManager.update(!shouldScroll);

        if (gameState == GameState.PLAYING) {
            bird.update();
            updatePipes();
            checkCollisions();
            spawnPipes();
        } else if (gameState == GameState.GAME_OVER) {
            bird.update(); // Let bird fall
        }
        // In MENU/CREDITS, we might want the bird to float in the center
        else if (gameState == GameState.MENU) {
            // Optional: visual "float" logic could go here
        }
    }

    private void updatePipes() {
        Iterator<PipeEntity> iterator = pipes.iterator();
        while (iterator.hasNext()) {
            PipeEntity pipe = iterator.next();
            pipe.update();

            if (pipe.getX() + pipe.getWidth() < 0) {
                iterator.remove();
            }

            if (!pipe.isPassed() && pipe.getX() < bird.x) {
                pipe.setPassed(true);
                score++;
                mainActivity.playScoreSound();
                pipesPassedInTheme++;

                if (score > highScore) {
                    highScore = score;
                    prefs.edit().putInt("high_score", highScore).apply();
                }

                if (pipesPassedInTheme >= PIPES_PER_THEME_CHANGE) {
                    pipesPassedInTheme = 0;
                    backgroundManager.switchTheme();
                    bird.setTheme(getResources(), backgroundManager.getCurrentThemeIndex());
                }
            }
        }
    }

    private void checkCollisions() {
        boolean wasPlaying = gameState == GameState.PLAYING;

        if (bird.y + bird.getBirdHeight() >= screenHeight - groundHeight) {
            bird.y = screenHeight - groundHeight - bird.getBirdHeight();
            bird.setDead(true);
            gameState = GameState.GAME_OVER;
        }

        if (gameState == GameState.PLAYING && !bird.isDead()) {
            Rect birdBounds = bird.getBounds();
            for (PipeEntity pipe : pipes) {
                if (pipe.checkCollision(birdBounds)) {
                    bird.setDead(true);
                    gameState = GameState.GAME_OVER;
                    break;
                }
            }
        }

        if (wasPlaying && gameState == GameState.GAME_OVER) {
            mainActivity.onGameOver();
        }
    }

    private void spawnPipes() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastPipeTime > PIPE_INTERVAL_MS) {
            pipes.add(new PipeEntity(
                    screenWidth,
                    screenHeight,
                    backgroundManager.getTopPipeBitmap(),
                    backgroundManager.getBottomPipeBitmap()));
            lastPipeTime = currentTime;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        if (canvas == null) return;

        // 1. Always Draw Background
        // --- NEW LOGIC: Draw static menu background OR scrolling background ---
        if (gameState == GameState.MENU && menuBackground != null) {
            canvas.drawBitmap(menuBackground, 0, 0, null);
        } else {
            backgroundManager.draw(canvas);
        }
        // ---------------------------------------------------------------------

        // 2. State Specific Drawing
        switch (gameState) {
            case MENU:
                drawMenu(canvas);
                break;
            case CREDITS:
                drawCredits(canvas);
                break;
            case HIGH_SCORES:
                drawHighScores(canvas);
                break;
            case READY:
                bird.draw(canvas);
                drawHUD(canvas);
                drawReadyMessage(canvas);
                break;
            case PLAYING:
            case PAUSED:
                for (PipeEntity pipe : pipes) pipe.draw(canvas);
                bird.draw(canvas);
                drawHUD(canvas);
                if (gameState == GameState.PAUSED) drawPauseMenu(canvas);
                break;
            case GAME_OVER:
                for (PipeEntity pipe : pipes) pipe.draw(canvas);
                bird.draw(canvas);
                drawHUD(canvas);
                drawGameOver(canvas);
                break;
        }
    }

    // --- 4. NEW DRAWING HELPER METHODS ---

    private void drawMenu(Canvas canvas) {
        // Draw Title "Floaty Head"
        canvas.drawText("Floaty Head", screenWidth / 2f, screenHeight * 0.25f, titlePaint);

        // Draw Buttons
        drawButton(canvas, btnPlay, "PLAY");
        drawButton(canvas, btnScores, "BEST SCORE");
        drawButton(canvas, btnCredits, "CREDITS");
        drawButton(canvas, btnExit, "EXIT");
    }

    private void drawCredits(Canvas canvas) {
        canvas.drawColor(Color.argb(150, 0, 0, 0)); // Dim background
        canvas.drawText("CREDITS", screenWidth / 2f, screenHeight * 0.2f, titlePaint);

        float startY = screenHeight * 0.4f;
        float lineHeight = 70;

        canvas.drawText("Created by:", screenWidth / 2f, startY, messagePaint);
        canvas.drawText("John Claire Abatayo", screenWidth / 2f, startY + lineHeight, messagePaint);

        canvas.drawText("Tools:", screenWidth / 2f, startY + lineHeight * 3, messagePaint);
        canvas.drawText("Android Studio", screenWidth / 2f, startY + lineHeight * 4, messagePaint);
        canvas.drawText("Java", screenWidth / 2f, startY + lineHeight * 5, messagePaint);

        drawButton(canvas, btnBack, "BACK");
    }

    private void drawHighScores(Canvas canvas) {
        canvas.drawColor(Color.argb(150, 0, 0, 0));
        canvas.drawText("BEST SCORE", screenWidth / 2f, screenHeight * 0.3f, titlePaint);

        // Draw actual score number huge
        Paint bigScorePaint = new Paint(titlePaint);
        bigScorePaint.setColor(Color.WHITE);
        bigScorePaint.setTextSize(150);
        canvas.drawText(String.valueOf(highScore), screenWidth / 2f, screenHeight * 0.5f, bigScorePaint);

        drawButton(canvas, btnBack, "BACK");
    }

    private void drawButton(Canvas canvas, RectF rect, String text) {
        // Draw rounded rectangle
        canvas.drawRoundRect(rect, 20, 20, buttonBgPaint);

        // Draw centered text
        Paint.FontMetrics fm = menuButtonPaint.getFontMetrics();
        float height = fm.descent - fm.ascent;
        float y = rect.centerY() + (height / 2) - fm.descent;
        canvas.drawText(text, rect.centerX(), y, menuButtonPaint);
    }

    private void drawHUD(Canvas canvas) {
        // Draw Score
        canvas.drawText(String.valueOf(score), screenWidth - 40, 100, scorePaint);

        // Draw Pause Button icon
        if (gameState != GameState.GAME_OVER) {
            canvas.drawRect(pauseButtonBounds, pauseButtonPaint);
            float lineW = pauseButtonBounds.width() * 0.15f;
            float lineH = pauseButtonBounds.height() * 0.4f;
            float centerX = pauseButtonBounds.centerX();
            float centerY = pauseButtonBounds.centerY();
            Paint whitePaint = new Paint();
            whitePaint.setColor(Color.WHITE);
            canvas.drawRect(centerX - lineW * 2, centerY - lineH, centerX - lineW, centerY + lineH, whitePaint);
            canvas.drawRect(centerX + lineW, centerY - lineH, centerX + lineW * 2, centerY + lineH, whitePaint);
        }
    }

    private void drawReadyMessage(Canvas canvas) {
        canvas.drawText("Tap to Jump!", screenWidth / 2f, screenHeight / 2f, messagePaint);
    }

    private void drawPauseMenu(Canvas canvas) {
        canvas.drawColor(Color.argb(100, 0, 0, 0));
        canvas.drawText("PAUSED", screenWidth / 2f, screenHeight * 0.3f, titlePaint);

        // Re-use logic for centered menu text, but simple for now
        float buttonY = screenHeight / 2f;
        float menuLineSpacing = 100;
        canvas.drawText("RESUME", screenWidth / 2f, buttonY + 50, menuButtonPaint);
        canvas.drawText("RESTART", screenWidth / 2f, buttonY + 50 + menuLineSpacing, menuButtonPaint);
        canvas.drawText("MAIN MENU", screenWidth / 2f, buttonY + 50 + 2 * menuLineSpacing, menuButtonPaint);
    }

    private void drawGameOver(Canvas canvas) {
        canvas.drawText("Game Over!", screenWidth / 2f, screenHeight * 0.3f, titlePaint);
        canvas.drawText("Score: " + score, screenWidth / 2f, screenHeight * 0.45f, messagePaint);
        canvas.drawText("Best: " + highScore, screenWidth / 2f, screenHeight * 0.52f, messagePaint);

        // Simple text buttons for Game Over
        float centerY = screenHeight * 0.7f;
        canvas.drawText("PLAY AGAIN", screenWidth / 2f, centerY, menuButtonPaint);
        canvas.drawText("MAIN MENU", screenWidth / 2f, centerY + 120, menuButtonPaint);
    }

    @Override
    public boolean performClick() {
        super.performClick();
        return true;
    }

    // --- 5. TOUCH EVENT HANDLING ---
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            switch (gameState) {
                case MENU:
                    if (btnPlay.contains(x, y)) {
                        restartGame(); // Reset everything
                    } else if (btnScores.contains(x, y)) {
                        gameState = GameState.HIGH_SCORES;
                    } else if (btnCredits.contains(x, y)) {
                        gameState = GameState.CREDITS;
                    } else if (btnExit.contains(x, y)) {
                        mainActivity.onGameExit();
                    }
                    break;

                case CREDITS:
                case HIGH_SCORES:
                    if (btnBack.contains(x, y)) {
                        gameState = GameState.MENU;
                    }
                    break;

                case READY:
                    gameState = GameState.PLAYING;
                    bird.jump();
                    mainActivity.playFlapSound();
                    mainActivity.onGameRestart();
                    break;

                case PLAYING:
                    if (pauseButtonBounds.contains((int) x, (int) y)) {
                        gameState = GameState.PAUSED;
                        thread.pause();
                        mainActivity.onGamePause();
                        return true;
                    }
                    bird.jump();
                    mainActivity.playFlapSound();
                    break;

                case PAUSED:
                    float buttonY = screenHeight / 2f;
                    // Approximate touch zones for text buttons
                    if (y > buttonY && y < buttonY + 100) { // Resume
                        gameState = GameState.PLAYING;
                        thread.resumeGame();
                        mainActivity.onGameRestart();
                    } else if (y > buttonY + 100 && y < buttonY + 200) { // Restart
                        restartGame();
                        thread.resumeGame();
                    } else if (y > buttonY + 200 && y < buttonY + 300) { // Main Menu
                        gameState = GameState.MENU;
                        thread.resumeGame();
                    }
                    break;

                case GAME_OVER:
                    float goCenterY = screenHeight * 0.7f;
                    // Play Again
                    if (y > goCenterY - 60 && y < goCenterY + 60) {
                        restartGame();
                    }
                    // Main Menu
                    else if (y > goCenterY + 60 && y < goCenterY + 180) {
                        gameState = GameState.MENU;
                    }
                    break;
            }
            return true;
        }
        return false;
    }

    private void restartGame() {
        score = 0;
        pipesPassedInTheme = 0;
        pipes.clear();
        bird = new BirdEntity(getResources(), screenWidth, screenHeight);

        // Reset background to theme 1 or keep current? Let's reset for fresh start.
        // We reuse the existing BackgroundManager but trigger a reload if needed
        backgroundManager = new BackgroundManager(getResources(), screenWidth, screenHeight, groundHeight);
        bird.setTheme(getResources(), backgroundManager.getCurrentThemeIndex());

        gameState = GameState.READY;
        mainActivity.onGameRestart();
    }

    public void pause() {
        if (thread != null) thread.pause();
    }

    public void resume() {
        if (thread != null) thread.resumeGame();
    }
}