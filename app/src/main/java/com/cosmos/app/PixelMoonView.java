package com.cosmos.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Handler;
import android.util.AttributeSet;
import android.view.View;

public class PixelMoonView extends View {

    private final Paint paintSurface = new Paint();
    private final Paint paintRim = new Paint();
    private final Paint paintShadow = new Paint();
    private final Paint paintCraterFloor = new Paint();
    private final Paint paintCraterDeep = new Paint();
    private final Paint paintMare = new Paint();
    private final Paint paintMareDark = new Paint();
    private final Paint paintStar = new Paint();
    private final Paint paintStarDim = new Paint();

    private final Handler animHandler = new Handler();
    private int animFrame = 0;
    private boolean isAttached = false;

    // 36x36 Grid
    private static final int GRID = 36;
    private final int[][] grid = new int[GRID][GRID];

    // Types
    private static final int TYPE_NONE = 0;
    private static final int TYPE_SURFACE = 1;
    private static final int TYPE_RIM = 2;
    private static final int TYPE_SHADOW = 3;
    private static final int TYPE_CRATER_FLOOR = 4;
    private static final int TYPE_CRATER_DEEP = 5;
    private static final int TYPE_MARE = 6;
    private static final int TYPE_MARE_DARK = 7;

    // Surrounding stars: (x, y)
    private static final int[][] STARS = {
        {3, 4}, {32, 5}, {2, 27}, {33, 28}, {5, 18}, {30, 16}, {17, 2}, {18, 33}, {8, 31}, {28, 32}
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
        paintSurface.setColor(Color.parseColor("#EBEBF0"));
        paintSurface.setStyle(Paint.Style.FILL);

        paintRim.setColor(Color.parseColor("#FFFFFF"));
        paintRim.setStyle(Paint.Style.FILL);

        paintShadow.setColor(Color.parseColor("#A5A5AF"));
        paintShadow.setStyle(Paint.Style.FILL);

        paintCraterFloor.setColor(Color.parseColor("#5F5F69"));
        paintCraterFloor.setStyle(Paint.Style.FILL);

        paintCraterDeep.setColor(Color.parseColor("#32323A"));
        paintCraterDeep.setStyle(Paint.Style.FILL);

        paintMare.setColor(Color.parseColor("#B9B9C3"));
        paintMare.setStyle(Paint.Style.FILL);

        paintMareDark.setColor(Color.parseColor("#9696A0"));
        paintMareDark.setStyle(Paint.Style.FILL);

        paintStar.setColor(Color.parseColor("#F5F5FA"));
        paintStar.setStyle(Paint.Style.FILL);

        paintStarDim.setColor(Color.parseColor("#6E6E78"));
        paintStarDim.setStyle(Paint.Style.FILL);

        // Precompute Full Moon with craters and lunar maria on 36x36 grid
        float cx = 17.5f, cy = 17.5f, r = 14.5f;

        // Craters: (rel_x, rel_y, radius, isMare)
        float[][] craters = {
            {0.0f, 7.0f, 2.8f, 0f},       // Tycho (prominent South impact)
            {-5.0f, -1.0f, 2.3f, 0f},     // Copernicus
            {-8.5f, -2.0f, 1.4f, 0f},     // Kepler
            {-2.0f, -10.5f, 1.7f, 0f},    // Plato
            {5.5f, 7.5f, 1.6f, 0f},       // Stevinus
            {-9.5f, 5.0f, 1.5f, 0f},      // Byrgius
            {7.0f, -8.0f, 1.3f, 0f},      // Endymion
            {-1.0f, 1.0f, 1.5f, 0f},      // Ptolemaeus

            // Lunar Maria (Dark Basalt Plains)
            {4.5f, -4.0f, 4.2f, 1f},      // Mare Serenitatis
            {6.5f, 1.5f, 3.8f, 1f},       // Mare Tranquillitatis
            {10.0f, -2.5f, 2.2f, 1f},     // Mare Crisium
            {-6.5f, -6.5f, 3.5f, 1f},     // Mare Imbrium
            {-11.0f, 2.0f, 2.0f, 1f},     // Mare Grimaldi
            {2.0f, -8.0f, 2.5f, 1f}       // Mare Vaporum
        };

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                float dx = x - cx;
                float dy = y - cy;
                float distSq = dx * dx + dy * dy;

                if (distSq > r * r) {
                    grid[y][x] = TYPE_NONE;
                    continue;
                }

                float dist = (float) Math.sqrt(distSq);
                boolean isLimb = (dist >= r - 0.9f);

                int assignedType = TYPE_SURFACE;
                boolean hitCrater = false;

                // 1. Check distinct impact craters first
                for (float[] c : craters) {
                    if (c[3] == 0f) {
                        float cdx = x - (cx + c[0]);
                        float cdy = y - (cy + c[1]);
                        float cdist = (float) Math.sqrt(cdx * cdx + cdy * cdy);
                        float cr = c[2];

                        if (cdist <= cr * 0.65f) {
                            assignedType = TYPE_CRATER_DEEP;
                            hitCrater = true;
                            break;
                        } else if (cdist <= cr) {
                            assignedType = TYPE_CRATER_FLOOR;
                            hitCrater = true;
                            break;
                        } else if (cdist <= cr + 0.9f) {
                            assignedType = (cdx + cdy < 0) ? TYPE_RIM : TYPE_SHADOW;
                            hitCrater = true;
                            break;
                        }
                    }
                }

                // 2. Check maria plains
                if (!hitCrater) {
                    for (float[] c : craters) {
                        if (c[3] == 1f) {
                            float cdx = x - (cx + c[0]);
                            float cdy = y - (cy + c[1]);
                            float cdist = (float) Math.sqrt(cdx * cdx + cdy * cdy);
                            float cr = c[2];

                            if (cdist <= cr * 0.7f) {
                                assignedType = ((x + y) % 2 == 0) ? TYPE_MARE_DARK : TYPE_MARE;
                                hitCrater = true;
                                break;
                            } else if (cdist <= cr) {
                                assignedType = TYPE_MARE;
                                hitCrater = true;
                                break;
                            }
                        }
                    }
                }

                if (!hitCrater && isLimb) {
                    assignedType = TYPE_SHADOW;
                }

                grid[y][x] = assignedType;
            }
        }
    }

    private final Runnable animRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isAttached) return;
            animFrame = (animFrame + 1) % 14;
            invalidate();
            animHandler.postDelayed(this, 140);
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
        float floatOffset = (float) Math.sin(animFrame * 2.0 * Math.PI / 14.0) * (pixelSize * 0.6f);

        // Draw Moon Pixels
        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                int type = grid[y][x];
                if (type == TYPE_NONE) continue;

                float px = offsetX + x * pixelSize;
                float py = offsetY + y * pixelSize + floatOffset;

                Paint paint;
                switch (type) {
                    case TYPE_CRATER_DEEP:  paint = paintCraterDeep; break;
                    case TYPE_CRATER_FLOOR: paint = paintCraterFloor; break;
                    case TYPE_RIM:          paint = paintRim; break;
                    case TYPE_SHADOW:       paint = paintShadow; break;
                    case TYPE_MARE_DARK:    paint = paintMareDark; break;
                    case TYPE_MARE:         paint = paintMare; break;
                    default:                paint = paintSurface; break;
                }

                canvas.drawRect(px, py, px + pixelSize, py + pixelSize, paint);
            }
        }

        // Draw Twinkling Stars
        for (int i = 0; i < STARS.length; i++) {
            int sx = STARS[i][0];
            int sy = STARS[i][1];
            float px = offsetX + sx * pixelSize;
            float py = offsetY + sy * pixelSize;

            int twinklePhase = (animFrame + i * 2) % 6;
            boolean isBig = (twinklePhase == 0 || twinklePhase == 1);

            // Core pixel
            canvas.drawRect(px, py, px + pixelSize, py + pixelSize, paintStar);

            // Cross sparkle if twinkling
            if (isBig) {
                canvas.drawRect(px + pixelSize, py, px + 2 * pixelSize, py + pixelSize, paintStarDim);
                canvas.drawRect(px - pixelSize, py, px, py + pixelSize, paintStarDim);
                canvas.drawRect(px, py + pixelSize, px + pixelSize, py + 2 * pixelSize, paintStarDim);
                canvas.drawRect(px, py - pixelSize, px + pixelSize, py, paintStarDim);
            }
        }
    }
}
