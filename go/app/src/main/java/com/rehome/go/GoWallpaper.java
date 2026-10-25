package com.rehome.go;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;

import java.io.File;
import java.io.FileOutputStream;

/**
 * The Go wallpaper: black, a horizon, and a bell-shaped glow that is brightest in the middle and fades out to both sides.
 * The top frame and the logo are painted in, so the fallback screen and the cover look like the real screen.
 */
final class GoWallpaper {
    private GoWallpaper() {
    }

    static final String FILE = "wallpaper.png";
    static final String CUSTOM = "wallpaper_custom.png";
    static final String LOGO = "logo.png";

    static Bitmap loadLogo(Context c) {
        try {
            File f = GoPrefs.file(c, LOGO);
            return f.exists() ? BitmapFactory.decodeFile(f.getAbsolutePath()) : null;
        } catch (Throwable t) {
            return null;
        }
    }

    /** The picture to show: your own image, the made wallpaper, or one made on the spot. */
    static Bitmap current(Context c, int W, int H) {
        SharedPreferences p = GoPrefs.get(c);
        try {
            File f = GoPrefs.file(c, p.getBoolean("wpCustom", false) ? CUSTOM : FILE);
            if (f.exists()) {
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
                if (b != null) return b;
            }
        } catch (Throwable ignored) {
        }
        return make(c, W, H);
    }

    static void save(Context c, Bitmap b, String name) {
        try {
            File f = GoPrefs.file(c, name);
            FileOutputStream o = new FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.PNG, 100, o);
            o.close();
        } catch (Throwable ignored) {
        }
    }

    static Bitmap make(Context c, int W, int H) {
        SharedPreferences p = GoPrefs.get(c);
        int glow = p.getInt("glow", 0xFF6FA8FF);
        float gr = Color.red(glow) / 255f, gg = Color.green(glow) / 255f, gb = Color.blue(glow) / 255f;
        int bw = 320, bh = Math.max(90, Math.round(bw * H / (float) W));
        int[] px = new int[bw * bh];
        float hy = 0.56f;   // horizon height
        for (int y = 0; y < bh; y++) {
            float dy = (y + 0.5f) / bh - hy;
            for (int x = 0; x < bw; x++) {
                float nx = (x + 0.5f) / bw * 2f - 1f;
                float bell = (float) Math.exp(-(nx * nx) / (2 * 0.42f * 0.42f));          // wide bell, both sides fade out
                float line = (float) Math.exp(-(dy * dy) / (2 * 0.010f * 0.010f)) * bell;   // thin horizon
                float haze = (float) Math.exp(-(dy * dy) / (2 * 0.075f * 0.075f)) * bell * 0.42f;
                float peakH = 0.30f * (float) Math.exp(-(nx * nx) / (2 * 0.20f * 0.20f));
                float peak = 0f;
                if (dy < 0 && -dy < peakH) {
                    float u = 1f - (-dy) / peakH;
                    peak = u * u * 0.34f * (float) Math.exp(-(nx * nx) / (2 * 0.28f * 0.28f));
                }
                float core = (float) Math.exp(-(nx * nx) / (2 * 0.09f * 0.09f) - (dy * dy) / (2 * 0.035f * 0.035f));
                float i = Math.min(1.25f, line * 0.85f + haze + peak + core * 0.95f);
                float t = Math.min(1f, i), w = Math.max(0f, i - 0.85f) * 1.6f;   // very bright parts turn white
                int r = Math.min(255, Math.round(255 * (6 / 255f + gr * t * 0.9f + w)));
                int g = Math.min(255, Math.round(255 * (7 / 255f + gg * t * 0.9f + w)));
                int b = Math.min(255, Math.round(255 * (9 / 255f + gb * t * 0.9f + w)));
                px[y * bw + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
            }
        }
        Bitmap small = Bitmap.createBitmap(px, bw, bh, Bitmap.Config.ARGB_8888);
        Bitmap big = Bitmap.createScaledBitmap(small, W, H, true);
        if (!big.isMutable()) big = big.copy(Bitmap.Config.ARGB_8888, true);
        Canvas cv = new Canvas(big);
        // sizes are set for the real screen; follow the bitmap if it is a different size
        float sc = GoPrefs.scale(c, p) * W / (float) Math.max(1, c.getResources().getDisplayMetrics().widthPixels);
        // paint in the frame and the logo, the same way the overlays draw them
        if (p.getBoolean("statusOn", true)) GoDraw.top(cv, p, sc, W, H, true, true, true);
        Bitmap logo = loadLogo(c);
        if (logo != null && p.getBoolean("logoOn", true)) {
            GoDraw.logo(cv, logo, W / 2f, p.getInt("logoTop", 6) * sc, p.getInt("logoSize", 64) * sc, false);
        }
        return big;
    }
}
