package com.rehome.go;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.RectF;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Random;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * The "null" boot pack: black screens that only carry your emblem (60% of the height, a little above the middle)
 * with a smooth round glow behind it. Used for the boot logo, the boot animation (one frame, 1 fps, looping) and the wallpaper.
 */
final class NullPack {
    private NullPack() {
    }

    static final int W = 1280, H = 720;

    // ------------------------------------------------------------ emblem: black background becomes transparent

    /** If the picture has a black background (and no transparency yet), make that background transparent. */
    static Bitmap removeBlack(Bitmap src) {
        int w = src.getWidth(), h = src.getHeight();
        int[] a = new int[w * h];
        src.getPixels(a, 0, w, 0, 0, w, h);
        int border = 0, darkBorder = 0;
        for (int x = 0; x < w; x++) {
            for (int y : new int[]{0, h - 1}) {
                border++;
                int c = a[y * w + x];
                if (Color.alpha(c) < 250) return src;
                if (mx(c) < 56) darkBorder++;
            }
        }
        for (int y = 1; y < h - 1; y++) {
            for (int x : new int[]{0, w - 1}) {
                border++;
                int c = a[y * w + x];
                if (Color.alpha(c) < 250) return src;
                if (mx(c) < 56) darkBorder++;
            }
        }
        if (darkBorder < border * 0.85f) return src;
        boolean[] gone = new boolean[w * h];
        int[] q = new int[w * h];
        int qh = 0, qt = 0;
        for (int x = 0; x < w; x++) {
            for (int y : new int[]{0, h - 1}) {
                int i = y * w + x;
                if (!gone[i] && mx(a[i]) < 56) {
                    gone[i] = true;
                    q[qt++] = i;
                }
            }
        }
        for (int y = 0; y < h; y++) {
            for (int x : new int[]{0, w - 1}) {
                int i = y * w + x;
                if (!gone[i] && mx(a[i]) < 56) {
                    gone[i] = true;
                    q[qt++] = i;
                }
            }
        }
        while (qh < qt) {
            int i = q[qh++], x = i % w, y = i / w;
            if (x > 0) qt = push(a, gone, q, qt, i - 1);
            if (x < w - 1) qt = push(a, gone, q, qt, i + 1);
            if (y > 0) qt = push(a, gone, q, qt, i - w);
            if (y < h - 1) qt = push(a, gone, q, qt, i + w);
        }
        int[] out = new int[w * h];
        for (int i = 0; i < w * h; i++) {
            int c = a[i];
            if (gone[i]) {
                out[i] = 0;
                continue;
            }
            int x = i % w, y = i / w;
            boolean edge = (x > 0 && gone[i - 1]) || (x < w - 1 && gone[i + 1]) || (y > 0 && gone[i - w]) || (y < h - 1 && gone[i + w]);
            if (edge) {
                float t = Math.max(0f, Math.min(1f, (mx(c) - 56f) / 54f));
                out[i] = (Math.round(255 * t) << 24) | (c & 0x00FFFFFF);
            } else {
                out[i] = c | 0xFF000000;
            }
        }
        return Bitmap.createBitmap(out, w, h, Bitmap.Config.ARGB_8888);
    }

    private static int push(int[] a, boolean[] gone, int[] q, int qt, int i) {
        if (!gone[i] && mx(a[i]) < 56) {
            gone[i] = true;
            q[qt++] = i;
        }
        return qt;
    }

    private static int mx(int c) {
        return Math.max(Color.red(c), Math.max(Color.green(c), Color.blue(c)));
    }

    // ------------------------------------------------------------ the scene

    /**
     * Black, with a colour vignette: dark in the middle of the screen, the colour rising smoothly (a bell curve of the
     * distance from the centre) towards the edges. With an emblem it sits on the right at 60% of the height.
     * Rounded black corners, like Go's frame, are cut on top. Without an emblem (and not for the keyboard) it is plain black.
     */
    static Bitmap scene(Bitmap emblem, int glow, boolean glowWithoutEmblem) {
        int[] px = new int[W * H];
        java.util.Arrays.fill(px, 0xFF000000);
        if (emblem != null || glowWithoutEmblem) {
            float gr = Color.red(glow), gg = Color.green(glow), gb = Color.blue(glow);
            Random rnd = new Random(7);
            float peak = 0.95f, sigma = 0.40f;
            float i0 = (float) Math.exp(-(peak * peak) / (2 * sigma * sigma));   // so the very middle is exactly black
            for (int y = 0; y < H; y++) {
                float ny = (y + 0.5f) / H * 2f - 1f;
                for (int x = 0; x < W; x++) {
                    float nx = (x + 0.5f) / W * 2f - 1f;
                    float d = (float) Math.sqrt(nx * nx + ny * ny) / 1.2f;
                    float k = (float) Math.exp(-((d - peak) * (d - peak)) / (2 * sigma * sigma));
                    k = Math.max(0f, (k - i0) / (1f - i0)) * 0.7f;
                    float dither = rnd.nextFloat() - 0.5f;   // stops visible bands in the soft glow
                    int r = clamp(Math.round(gr * k + dither));
                    int g = clamp(Math.round(gg * k + dither));
                    int b = clamp(Math.round(gb * k + dither));
                    px[y * W + x] = 0xFF000000 | (r << 16) | (g << 8) | b;
                }
            }
        }
        Bitmap bm = Bitmap.createBitmap(W, H, Bitmap.Config.ARGB_8888);   // mutable, so a Canvas can draw on it
        bm.setPixels(px, 0, W, 0, 0, W, H);
        Canvas c = new Canvas(bm);
        if (emblem != null) {
            float cx = W * 0.75f, cy = H * 0.41f;
            float eh = H * 0.60f;
            float ew = eh * emblem.getWidth() / (float) emblem.getHeight();
            if (ew > W * 0.44f) {
                ew = W * 0.44f;
                eh = ew * emblem.getHeight() / (float) emblem.getWidth();
            }
            Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
            c.drawBitmap(emblem, null, new RectF(cx - ew / 2f, cy - eh / 2f, cx + ew / 2f, cy + eh / 2f), p);
        }
        // rounded black corners (the same margin and radius as the corner frame)
        float m = H * 0.011f, r = H * 0.083f;
        android.graphics.Path path = new android.graphics.Path();
        path.setFillType(android.graphics.Path.FillType.EVEN_ODD);
        path.addRect(0, 0, W, H, android.graphics.Path.Direction.CW);
        path.addRoundRect(new RectF(m, m, W - m, H - m), r, r, android.graphics.Path.Direction.CW);
        Paint fp = new Paint(Paint.ANTI_ALIAS_FLAG);
        fp.setColor(0xFF000000);
        c.drawPath(path, fp);
        return bm;
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(255, v);
    }

    // ------------------------------------------------------------ files

    /**
     * Writes bkg.png (wallpaper), logo.png and logo.bmp (turned 90 degrees clockwise, 720x1280), bootanimation.zip
     * and keyboard.png (a keyboard background: the same vignette, no emblem).
     */
    static void write(File dir, Bitmap emblem, int glow) throws Exception {
        dir.mkdirs();
        Bitmap s = scene(emblem, glow, false);
        png(s, new File(dir, "bkg.png"));
        png(scene(null, glow, true), new File(dir, "keyboard.png"));
        Matrix m = new Matrix();
        m.postRotate(90);
        Bitmap rot = Bitmap.createBitmap(s, 0, 0, W, H, m, true);   // 720 x 1280
        png(rot, new File(dir, "logo.png"));
        bmp24(rot, new File(dir, "logo.bmp"));
        byte[] frame = pngBytes(s);
        String desc = W + " " + H + " 1\np 0 0 part0\n";
        File zip = new File(dir, "bootanimation.zip");
        ZipOutputStream z = new ZipOutputStream(new FileOutputStream(zip));
        z.setMethod(ZipOutputStream.STORED);
        stored(z, "desc.txt", desc.getBytes("UTF-8"));
        stored(z, "part0/0000.png", frame);
        z.close();
        FileOutputStream d = new FileOutputStream(new File(dir, "desc.txt"));
        d.write(desc.getBytes("UTF-8"));
        d.close();
    }

    private static void stored(ZipOutputStream z, String name, byte[] data) throws Exception {
        ZipEntry e = new ZipEntry(name);
        e.setMethod(ZipEntry.STORED);
        e.setSize(data.length);
        e.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        e.setCrc(crc.getValue());
        z.putNextEntry(e);
        z.write(data);
        z.closeEntry();
    }

    static byte[] pngBytes(Bitmap b) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        b.compress(Bitmap.CompressFormat.PNG, 100, o);
        return o.toByteArray();
    }

    static void png(Bitmap b, File f) throws Exception {
        FileOutputStream o = new FileOutputStream(f);
        o.write(pngBytes(b));
        o.close();
    }

    private static void bmp24(Bitmap b, File f) throws Exception {
        int w = b.getWidth(), h = b.getHeight();
        int row = ((w * 3 + 3) / 4) * 4;
        int size = 54 + row * h;
        byte[] out = new byte[size];
        java.nio.ByteBuffer bb = java.nio.ByteBuffer.wrap(out).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        bb.put((byte) 'B').put((byte) 'M').putInt(size).putInt(0).putInt(54);
        bb.putInt(40).putInt(w).putInt(h).putShort((short) 1).putShort((short) 24).putInt(0).putInt(row * h).putInt(2835).putInt(2835).putInt(0).putInt(0);
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        for (int y = 0; y < h; y++) {
            int base = 54 + (h - 1 - y) * row;
            for (int x = 0; x < w; x++) {
                int c = px[y * w + x];
                out[base + x * 3] = (byte) Color.blue(c);
                out[base + x * 3 + 1] = (byte) Color.green(c);
                out[base + x * 3 + 2] = (byte) Color.red(c);
            }
        }
        FileOutputStream o = new FileOutputStream(f);
        o.write(out);
        o.close();
    }
}
