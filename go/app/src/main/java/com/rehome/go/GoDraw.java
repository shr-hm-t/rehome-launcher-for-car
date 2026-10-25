package com.rehome.go;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;

/** Drawing shared by the Go overlays and the Go wallpaper: the top frame, the star vignette and the logo. */
final class GoDraw {
    private GoDraw() {
    }

    // ------------------------------------------------------------ top frame (status frame blended with the mini bar)

    /** True when the mini bar sits at the bottom as its own pill (otherwise it is joined to the top frame). */
    static boolean barAtBottom(SharedPreferences p) {
        return p.getBoolean("barBottomMode", false);
    }

    /** Mini bar rectangle in pixels: {left, top, width, height}. */
    static float[] barRect(SharedPreferences p, float sc, float screenW, float screenH) {
        if (barAtBottom(p)) {
            float w = p.getInt("bbW", 700) * sc, h = p.getInt("bbH", 56) * sc;
            float cx = screenW / 2f + p.getInt("bbX", 0) * sc;
            float bottom = screenH - p.getInt("bbBottom", 10) * sc;
            return new float[]{cx - w / 2f, bottom - h, w, h};
        }
        float w = p.getInt("barW", 340) * sc, h = p.getInt("barH", 48) * sc;
        float cx = screenW / 2f + p.getInt("barX", 0) * sc;
        return new float[]{cx - w / 2f, p.getInt("barTop", 0) * sc, w, h};
    }

    /**
     * Dark glass along the top: a left piece, a right piece and (mini bar at the top) the slab behind the bar,
     * joined by a thin strip so they read as one bar. Solid at the top, fading softly at the lower edge.
     */
    static void top(Canvas c, SharedPreferences p, float sc, float W, float H, boolean left, boolean right, boolean mid) {
        float sH = p.getInt("statusH", 44) * sc;
        float sTop = p.getInt("statusTop", 0) * sc;
        float sl = p.getInt("statusL", 200) * sc, sr = p.getInt("statusR", 200) * sc;
        float link = p.getInt("topLink", 8) * sc;
        mid = mid && !barAtBottom(p);
        float[] b = barRect(p, sc, W, H);
        Path path = new Path();
        float r = Math.max(4f, sH * 0.35f);
        float[] rr = {0, 0, 0, 0, r, r, r, r};
        if (left) path.addRoundRect(new RectF(-r, sTop - 1, sl, sTop + sH), rr, Path.Direction.CW);
        if (right) path.addRoundRect(new RectF(W - sr, sTop - 1, W + r, sTop + sH), rr, Path.Direction.CW);
        if (mid) {
            float mr = Math.max(4f, b[3] * 0.35f);
            path.addRoundRect(new RectF(b[0], b[1] - 1, b[0] + b[2], b[1] + b[3]),
                    new float[]{0, 0, 0, 0, mr, mr, mr, mr}, Path.Direction.CW);
            if (link > 0 && left) path.addRect(sl - 1, sTop - 1, b[0] + 1, sTop + link, Path.Direction.CW);
            if (link > 0 && right) path.addRect(b[0] + b[2] - 1, sTop - 1, W - sr + 1, sTop + link, Path.Direction.CW);
        } else if (link > 0 && left && right) {
            path.addRect(sl - 1, sTop - 1, W - sr + 1, sTop + link, Path.Direction.CW);
        }
        float bottom = Math.max(sTop + sH, mid ? b[1] + b[3] : 0);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setShader(new LinearGradient(0, 0, 0, bottom,
                new int[]{0xFF000000, 0xFF000000, 0xB0000000, 0x00000000},
                new float[]{0f, 0.6f, 0.88f, 1f}, Shader.TileMode.CLAMP));
        c.drawPath(path, paint);
    }

    /** The rounded glass bar of the contacts bar style: dark, with a thin light rim. */
    static void bar(Canvas c, float w, float h) {
        Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        float in = 1.5f;
        RectF rc = new RectF(in, in, w - in, h - in);
        pt.setStyle(Paint.Style.FILL);
        pt.setColor(0xFF05070A);
        c.drawRoundRect(rc, h / 2f, h / 2f, pt);
        pt.setStyle(Paint.Style.STROKE);
        pt.setStrokeWidth(Math.max(2f, h * 0.03f));
        pt.setColor(0x88FFFFFF);
        c.drawRoundRect(rc, h / 2f, h / 2f, pt);
    }

    // ------------------------------------------------------------ star vignette

    private static Bitmap vigBmp;
    private static String vigKey = "";

    /** The star vignette: dark bands along the four edges, thickest in the middle of each edge, corners lightest. */
    static void vignette(Canvas c, float w, float h, int strength) {
        if (strength <= 0 || w <= 0 || h <= 0) return;
        int a = (int) (Math.min(100, strength) / 100f * 255f);   // fully black at the darkest, not see-through
        float extra = Math.max(0, Math.min(200, strength) - 100) / 100f;
        String key = (int) w + "x" + (int) h + ":" + a + ":" + extra;
        if (vigBmp == null || !key.equals(vigKey)) {
            int bw = 160, bh = Math.max(24, Math.round(bw * h / w));
            int[] px = new int[bw * bh];
            float band0 = Math.min(0.95f, 0.55f + 0.0f - 0.18f * extra + 0.0f);
            for (int y = 0; y < bh; y++) {
                float ny = (y + 0.5f) / bh * 2f - 1f;
                for (int x = 0; x < bw; x++) {
                    float nx = (x + 0.5f) / bw * 2f - 1f;
                    float ax = Math.abs(nx), ay = Math.abs(ny);
                    float mx = Math.max(ax, ay), mn = Math.min(ax, ay);
                    float k = mx <= 0f ? 0f : mn / mx;
                    float e = 1f - mx;
                    float band = Math.max(0.02f, band0 * (1f - 0.92f * k * k));
                    float t = Math.max(0f, Math.min(1f, e / band));
                    t = 1f - t * t * (3f - 2f * t);
                    px[y * bw + x] = Math.round(a * t) << 24;
                }
            }
            vigBmp = Bitmap.createBitmap(px, bw, bh, Bitmap.Config.ARGB_8888);
            vigKey = key;
        }
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG);
        c.drawBitmap(vigBmp, null, new RectF(0, 0, w, h), p);
    }

    // ------------------------------------------------------------ logo

    /** Logo centred at the top, {@code heightPx} tall. With sparkle on, small stars blink around it. */
    static void logo(Canvas c, Bitmap logo, float cx, float top, float heightPx, boolean sparkle) {
        if (logo == null) return;
        float asp = logo.getWidth() / (float) Math.max(1, logo.getHeight());
        float w = heightPx * asp;
        RectF dst = new RectF(cx - w / 2f, top, cx + w / 2f, top + heightPx);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        c.drawBitmap(logo, null, dst, p);
        if (!sparkle) return;
        float[] fx = {0.18f, 0.80f, 0.52f, 0.30f, 0.72f, 0.90f};
        float[] fy = {0.30f, 0.22f, 0.12f, 0.78f, 0.66f, 0.50f};
        float[] ph = {0f, 1.1f, 2.3f, 3.4f, 4.4f, 5.5f};
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        double t = System.currentTimeMillis() / 1000.0;
        for (int i = 0; i < fx.length; i++) {
            float k = (float) Math.max(0, Math.sin(t * 1.3 + ph[i]));
            k = k * k * k;
            if (k < 0.03f) continue;
            float r = Math.min(w, heightPx) * 0.11f * k;
            float x = dst.left + fx[i] * w, y = dst.top + fy[i] * heightPx;
            sp.setColor(((int) (210 * k) << 24) | 0xFFFFFF);
            Path st = new Path();
            st.moveTo(x, y - r);
            st.quadTo(x, y, x + r, y);
            st.quadTo(x, y, x, y + r);
            st.quadTo(x, y, x - r, y);
            st.quadTo(x, y, x, y - r);
            c.drawPath(st, sp);
        }
    }
}
