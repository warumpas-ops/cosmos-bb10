package com.cosmos.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class PixelMoonView extends View {

    private final Paint paintMoon = new Paint();
    private final Paint paintCrater = new Paint();
    private final Paint paintRim = new Paint();
    private final Paint paintStar = new Paint();
    private final Paint paintStarDim = new Paint();

    private final Handler animHandler = new Handler();
    private int animFrame = 0;
    private boolean isAttached = false;

    // 32x32 Grid
    private static final int GRID = 32;
    private final boolean[][] moonGrid = new boolean[GRID][GRID];
    private final boolean[][] craterGrid = new boolean[GRID][GRID];
    private final boolean[][] rimGrid = new boolean[GRID][GRID];

    // Star coordinates: (x, y)
    private static final int[][] STARS = {
        {5, 6}, {4, 23}, {24, 6}, {27, 20}, {21, 26}, {12, 29}
    };

    public PixelMoonView(Context context) {
        super(context);
        init();
    }

    public PixelMoonView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        paintMoon.setColor(Color.parseColor("#F2F2F2"));
        paintMoon.setStyle(Paint.Style.FILL);

        paintCrater.setColor(Color.parseColor("#505058"));
        paintCrater.setStyle(Paint.Style.FILL);

        paintRim.setColor(Color.parseColor("#FFFFFF"));
        paintRim.setStyle(Paint.Style.FILL);

        paintStar.setColor(Color.parseColor("#F2F2F2"));
        paintStar.setStyle(Paint.Style.FILL);

        paintStarDim.setColor(Color.parseColor("#707076"));
        paintStarDim.setStyle(Paint.Style.FILL);

        // Pre-compute crescent moon with craters on 32x32 grid
        float cx = 15f, cy = 16f, r = 11f;
        float cutX = cx + 6f, cutY = cy - 2f, cutR = 9f;

        float[][] craters = {
            {-4f, -2f, 1.8f},
            {-6f, 2f, 1.5f},
            {-2f, 5f, 1.3f},
            {-1f, -6f, 1.0f}
        };

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                float dMain = (x - cx) * (x - cx) + (y - cy) * (y - cy);
                float dCut = (x - cutX) * (x - cutX) + (y - cutY) * (y - cutY);

                if (dMain <= r * r && dCut > cutR * cutR) {
                    moonGrid[y][x] = true;

                    for (float[] c : craters) {
                        float dist = (float) Math.sqrt((x - (cx + c[0])) * (x - (cx + c[0])) + (y - (cy + c[1])) * (y - (cy + c[1])));
                        if (dist <= c[2]) {
                            craterGrid[y][x] = true;
                            break;
                        } else if (dist <= c[2] + 0.8f) {
                            rimGrid[y][x] = true;
                        }
                    }
                }
            }
        }
    }

    private final Runnable animRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAttached) return;
            animFrame = (animFrame + 1) % 6;
            invalidate();
            animHandler.postDelayed(this, 350);
        }
    };

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        isAttached = true;
        animHandler.post(animRunnable);
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        isAttached = false;
        animHandler.removeCallbacks(animRunnable);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        int size = Math.min(w, h);
        if (size <= 0) return;

        float pixelSize = (float) size / GRID;
        float offsetX = (w - size) / 2f;
        float offsetY = (h - size) / 2f;

        // Subtle floating bobbing effect
        float floatOffset = (float) Math.sin(animFrame * Math.PI / 3.0) * (pixelSize * 0.4f);

        // Draw Moon
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (moonGrid[y][x]) {
                    float px = offsetX + x * pixelSize;
                    float py = offsetY + y * pixelSize + floatOffset;

                    if (craterGrid[y][x]) {
                        canvas.drawRect(px, py, px + pixelSize, py + pixelSize, paintCrater);
                    } else if (rimGrid[y][x]) {
                        canvas.drawRect(px, py, px + pixelSize, py + pixelSize, paintRim);
                    } else {
                        canvas.drawRect(px, py, px + pixelSize, py + pixelSize, paintMoon);
                    }
                }
            }
        }

        // Draw Twinkling Stars
        for (int i = 0; i < STARS.length; i++) {
            int sx = STARS[i][0];
            int sy = STARS[i][1];
            float px = offsetX + sx * pixelSize;
            float py = offsetY + sy * pixelSize;

            boolean isTwinkle = ((animFrame + i) % 3 == 0);

            // Center star pixel
            canvas.drawRect(px, py, px + pixelSize, py + pixelSize, paintStar);

            // Cross sparkle if twinkling
            if (isTwinkle) {
                canvas.drawRect(px + pixelSize, py, px + 2 * pixelSize, py + pixelSize, paintStarDim);
                canvas.drawRect(px - pixelSize, py, px, py + pixelSize, paintStarDim);
                canvas.drawRect(px, py + pixelSize, px + pixelSize, py + 2 * pixelSize, paintStarDim);
                canvas.drawRect(px, py - pixelSize, px + pixelSize, py, paintStarDim);
            }
        }
    }
}
