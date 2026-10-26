package com.rehome.go;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Rehome: Go overlays: the black corner frame, the speed light, the three buttons (movable and resizable when held)
 * and the start-up cover. Also the slow-speed 360 launch. Needs only "display over other apps" and location.
 */
public class GoService extends Service {
    static GoService inst;

    private static final float GAP = 0.18f;          // gap between the buttons, in button sizes
    private static final float GROUP = 3f + 2f * GAP; // width of the three buttons, in button sizes

    private WindowManager wm;
    private SharedPreferences p;
    private final Handler H = new Handler(Looper.getMainLooper());
    private int W, Hh;
    private View frameV, speedV, coverV;
    private ButtonsView btnV;
    private final List<View> blockers = new ArrayList<>();
    private LocationManager lm;
    private volatile float curKmh = 0f;
    private long lastAuto360 = 0;
    private boolean armed360 = true;

    // ------------------------------------------------------------------ service

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        inst = this;
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        p = GoPrefs.get(this);
    }

    @Override
    public int onStartCommand(Intent i, int flags, int startId) {
        if (p == null) p = GoPrefs.get(this);
        if (GoPrefs.otherHome(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (frameV == null) build();
        startLocation();
        if (i != null && i.getBooleanExtra("cover", false)) showCover();
        H.removeCallbacks(tick);
        H.postDelayed(tick, 10000);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        H.removeCallbacksAndMessages(null);
        stopLocation();
        removeAll();
        removeCover();
        if (inst == this) inst = null;
        super.onDestroy();
    }

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            if (GoPrefs.otherHome(GoService.this)) {   // another launcher took over
                stopSelf();
                return;
            }
            H.postDelayed(this, 10000);
        }
    };

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private WindowManager.LayoutParams lp(int w, int h, boolean touch, int gravity, int x, int y) {
        int fl = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | (touch ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE);
        WindowManager.LayoutParams l = new WindowManager.LayoutParams(w, h, overlayType(), fl, PixelFormat.TRANSLUCENT);
        l.gravity = gravity;
        l.x = x;
        l.y = y;
        return l;
    }

    private boolean add(View v, WindowManager.LayoutParams l) {
        try {
            wm.addView(v, l);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private void remove(View v) {
        if (v == null) return;
        try {
            wm.removeView(v);
        } catch (Exception ignored) {
        }
    }

    private void removeAll() {
        remove(frameV);
        remove(speedV);
        remove(btnV);
        for (View b : blockers) remove(b);
        blockers.clear();
        frameV = null;
        speedV = null;
        btnV = null;
    }

    // ------------------------------------------------------------------ building the overlays

    private void build() {
        removeAll();
        if (!GoPrefs.canOverlay(this) || wm == null) return;
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        W = dm.widthPixels;
        Hh = dm.heightPixels;
        // bottom to top: speed light, black corner frame (and its touch blockers), buttons, cover
        speedV = new SpeedView(this);
        add(speedV, lp(-1, -1, false, Gravity.TOP | Gravity.START, 0, 0));
        frameV = new FrameView(this);
        add(frameV, lp(-1, -1, false, Gravity.TOP | Gravity.START, 0, 0));
        int m = Math.round(frameMargin());
        if (m > 0) {
            addBlocker(W, m, Gravity.TOP | Gravity.START);
            addBlocker(W, m, Gravity.BOTTOM | Gravity.START);
            addBlocker(m, Hh, Gravity.TOP | Gravity.START);
            addBlocker(m, Hh, Gravity.TOP | Gravity.END);
        }
        btnV = new ButtonsView(this);
        add(btnV, btnV.normalParams());
        if (coverV != null) {   // the cover stays above everything
            remove(coverV);
            add(coverV, lp(-1, -1, true, Gravity.TOP | Gravity.START, 0, 0));
        }
    }

    private void addBlocker(int w, int h, int gravity) {
        View v = new View(this);
        v.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                return true;
            }
        });
        if (add(v, lp(w, h, true, gravity, 0, 0))) blockers.add(v);
    }

    /** Sizes follow the screen height, so they stay the same whatever the "smallest width" is. */
    private float frameMargin() {
        return Hh * 0.011f;
    }

    private float frameRadius() {
        return Hh * 0.083f;
    }

    /** Black frame with rounded inner corners. Fully opaque. */
    private class FrameView extends View {
        private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();

        FrameView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight();
            float m = frameMargin(), r = frameRadius();
            pt.setStyle(Paint.Style.FILL);
            pt.setColor(0xFF000000);
            path.reset();
            path.setFillType(Path.FillType.EVEN_ODD);
            path.addRect(0, 0, w, h, Path.Direction.CW);
            path.addRoundRect(new RectF(m, m, w - m, h - m), r, r, Path.Direction.CW);
            c.drawPath(path, pt);
        }
    }

    // ------------------------------------------------------------------ speed light (over-speed warning)

    private class SpeedView extends View {
        SpeedView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            if (!p.getBoolean("speedLight", true)) return;
            float k = curKmh;
            int col, a;
            if (k >= p.getInt("warnRed", 120)) {
                col = 0xFFFF3B30;
                a = 0xE0;
            } else if (k >= p.getInt("warnAmber", 80)) {
                col = 0xFFFFBE1E;
                a = 0xC8;
            } else {
                return;
            }
            int strong = (a << 24) | (col & 0x00FFFFFF), mid = ((a / 3) << 24) | (col & 0x00FFFFFF), none = col & 0x00FFFFFF;
            Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
            float th = getHeight() / 3f;
            pt.setShader(new LinearGradient(0, 0, 0, th, new int[]{strong, mid, none}, new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
            c.drawRect(0, 0, getWidth(), th, pt);
            pt.setShader(null);
            pt.setColor(0xFF000000 | (col & 0x00FFFFFF));
            c.drawRect(0, 0, getWidth(), Math.max(2f, Hh * 0.004f), pt);
        }
    }

    // ------------------------------------------------------------------ the three buttons

    /**
     * Three bare icons on separate black discs, bottom right. Hold them to move and resize (corner handles, fixed ratio)
     * and to set the transparency. Everything else is fixed.
     */
    private class ButtonsView extends View {
        private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Bitmap[] icon = new Bitmap[4];   // cam, maps, radio, yt
        private boolean edit = false;
        private float left, top, size;                 // screen pixels
        private int pressed = -1;
        private boolean held = false;
        private float downX, downY;
        private final Runnable holdRun = new Runnable() {
            @Override
            public void run() {
                held = true;
                pressed = -1;
                enterEdit();
            }
        };
        // edit mode
        private int drag = 0;                          // 0 none, 1 move, 2 resize
        private float offX, offY, ancX, ancY;
        private final RectF rMinus = new RectF(), rPlus = new RectF(), rDone = new RectF();

        ButtonsView(Context c) {
            super(c);
            icon[0] = load(R.drawable.ic_360);
            icon[1] = load(R.drawable.ic_map);
            icon[2] = load(R.drawable.ic_radio);
            icon[3] = load(R.drawable.ic_yt);
            readGeometry();
        }

        private Bitmap load(int id) {
            return BitmapFactory.decodeResource(getResources(), id);
        }

        private void readGeometry() {
            size = p.getFloat("bs", 0.16f) * Hh;
            float defLeft = W - GROUP * size - 0.07f * Hh, defTop = Hh - size - 0.07f * Hh;
            left = p.contains("bx") ? p.getFloat("bx", 0f) * W : defLeft;
            top = p.contains("by") ? p.getFloat("by", 0f) * Hh : defTop;
            clampGeometry();
        }

        private void clampGeometry() {
            size = Math.max(0.06f * Hh, Math.min(0.40f * Hh, size));
            if (GROUP * size > W) size = W / GROUP;
            left = Math.max(0, Math.min(W - GROUP * size, left));
            top = Math.max(0, Math.min(Hh - size, top));
        }

        WindowManager.LayoutParams normalParams() {
            return lp(Math.round(GROUP * size), Math.round(size), true, Gravity.TOP | Gravity.START, Math.round(left), Math.round(top));
        }

        private void save() {
            p.edit().putFloat("bx", left / W).putFloat("by", top / Hh).putFloat("bs", size / Hh).apply();
        }

        private void enterEdit() {
            edit = true;
            try {
                wm.updateViewLayout(this, lp(-1, -1, true, Gravity.TOP | Gravity.START, 0, 0));
            } catch (Exception ignored) {
            }
            invalidate();
        }

        private void exitEdit() {
            edit = false;
            drag = 0;
            clampGeometry();
            save();
            try {
                wm.updateViewLayout(this, normalParams());
            } catch (Exception ignored) {
            }
            invalidate();
        }

        private String roleAt(int i) {
            return i == 0 ? "cam" : i == 1 ? "maps" : GoPrefs.third(p);
        }

        private Bitmap iconFor(String role) {
            switch (role) {
                case "cam": return icon[0];
                case "maps": return icon[1];
                case "radio": return icon[2];
                default: return icon[3];
            }
        }

        @Override
        protected void onDraw(Canvas c) {
            float ox = edit ? left : 0, oy = edit ? top : 0;
            if (edit) {
                c.drawColor(0x66000000);
            }
            int alpha = Math.round(255 * Math.max(0, Math.min(100, p.getInt("bAlpha", 60))) / 100f);
            for (int i = 0; i < 3; i++) {
                float cx = ox + i * size * (1f + GAP) + size / 2f, cy = oy + size / 2f;
                pt.setStyle(Paint.Style.FILL);
                pt.setColor(Color.argb(alpha, 0, 0, 0));
                c.drawCircle(cx, cy, size / 2f, pt);
                if (pressed == i) {
                    pt.setColor(0x40FFFFFF);
                    c.drawCircle(cx, cy, size / 2f, pt);
                }
                Bitmap ic = iconFor(roleAt(i));
                if (ic != null) {
                    float s = size * 0.8f;
                    pt.setColor(Color.WHITE);
                    c.drawBitmap(ic, null, new RectF(cx - s / 2f, cy - s / 2f, cx + s / 2f, cy + s / 2f), pt);
                }
            }
            if (!edit) return;
            // frame and handles
            float gw = GROUP * size;
            float hr = Hh * 0.035f;
            pt.setStyle(Paint.Style.STROKE);
            pt.setColor(0xCCFFFFFF);
            pt.setStrokeWidth(Math.max(2f, Hh * 0.004f));
            c.drawRect(left, top, left + gw, top + size, pt);
            pt.setStyle(Paint.Style.FILL);
            pt.setColor(0xFFFFFFFF);
            float[][] hs = {{left, top}, {left + gw, top}, {left, top + size}, {left + gw, top + size}};
            for (float[] h : hs) c.drawCircle(h[0], h[1], hr, pt);
            // panel: transparency and done
            float pw = Math.min(W * 0.8f, Hh * 1.6f), ph = Hh * 0.16f;
            float px = (W - pw) / 2f, py = Hh * 0.12f;
            pt.setColor(0xFF111418);
            c.drawRoundRect(new RectF(px, py, px + pw, py + ph), ph / 2f, ph / 2f, pt);
            pt.setStyle(Paint.Style.STROKE);
            pt.setColor(0x88FFFFFF);
            c.drawRoundRect(new RectF(px, py, px + pw, py + ph), ph / 2f, ph / 2f, pt);
            pt.setStyle(Paint.Style.FILL);
            pt.setTextAlign(Paint.Align.CENTER);
            pt.setColor(Color.WHITE);
            pt.setTextSize(ph * 0.34f);
            float bw = ph * 0.9f, by = py + (ph - bw) / 2f;
            rMinus.set(px + pw * 0.33f - bw / 2f, by, px + pw * 0.33f + bw / 2f, by + bw);
            rPlus.set(px + pw * 0.62f - bw / 2f, by, px + pw * 0.62f + bw / 2f, by + bw);
            rDone.set(px + pw * 0.82f, by, px + pw - ph * 0.15f, by + bw);
            c.drawText("Transparency", px + pw * 0.15f, py + ph * 0.6f, labelPaint());
            drawKey(c, rMinus, "−");
            drawKey(c, rPlus, "+");
            c.drawText(p.getInt("bAlpha", 60) + "%", (rMinus.right + rPlus.left) / 2f, py + ph * 0.62f, pt);
            drawKey(c, rDone, "Done");
        }

        private Paint labelPaint() {
            Paint l = new Paint(Paint.ANTI_ALIAS_FLAG);
            l.setColor(0xFFDDE6FF);
            l.setTextSize(Hh * 0.16f * 0.30f);
            l.setTextAlign(Paint.Align.CENTER);
            return l;
        }

        private void drawKey(Canvas c, RectF r, String t) {
            Paint k = new Paint(Paint.ANTI_ALIAS_FLAG);
            k.setColor(0xFF2A313A);
            c.drawRoundRect(r, r.height() / 2f, r.height() / 2f, k);
            k.setColor(Color.WHITE);
            k.setTextAlign(Paint.Align.CENTER);
            k.setTextSize(r.height() * 0.42f);
            c.drawText(t, r.centerX(), r.centerY() + r.height() * 0.15f, k);
        }

        private int hit(float x, float y) {
            float ox = edit ? left : 0, oy = edit ? top : 0;
            for (int i = 0; i < 3; i++) {
                float cx = ox + i * size * (1f + GAP) + size / 2f, cy = oy + size / 2f;
                float dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy <= size * size / 4f * 1.15f) return i;
            }
            return -1;
        }

        @Override
        public boolean onTouchEvent(MotionEvent e) {
            float x = e.getX(), y = e.getY();
            if (edit) return editTouch(e, x, y);
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                    held = false;
                    downX = x;
                    downY = y;
                    pressed = hit(x, y);
                    invalidate();
                    H.postDelayed(holdRun, 700);
                    return true;
                case MotionEvent.ACTION_MOVE:
                    if (Math.abs(x - downX) > size * 0.3f || Math.abs(y - downY) > size * 0.3f) {
                        H.removeCallbacks(holdRun);
                        pressed = -1;
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                    H.removeCallbacks(holdRun);
                    int i = pressed;
                    pressed = -1;
                    invalidate();
                    if (!held && i >= 0) {
                        GoPrefs.launch(GoService.this, roleAt(i));
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_CANCEL:
                    H.removeCallbacks(holdRun);
                    pressed = -1;
                    invalidate();
                    return true;
                default:
                    return true;
            }
        }

        private boolean editTouch(MotionEvent e, float x, float y) {
            float gw = GROUP * size, hr = Hh * 0.035f * 2.2f;
            switch (e.getActionMasked()) {
                case MotionEvent.ACTION_DOWN: {
                    drag = 0;
                    if (rDone.contains(x, y)) {
                        exitEdit();
                        return true;
                    }
                    if (rMinus.contains(x, y) || rPlus.contains(x, y)) {
                        int a = p.getInt("bAlpha", 60) + (rPlus.contains(x, y) ? 5 : -5);
                        p.edit().putInt("bAlpha", Math.max(0, Math.min(100, a))).apply();
                        invalidate();
                        return true;
                    }
                    float[][] hs = {{left, top}, {left + gw, top}, {left, top + size}, {left + gw, top + size}};
                    for (int k = 0; k < 4; k++) {
                        float dx = x - hs[k][0], dy = y - hs[k][1];
                        if (dx * dx + dy * dy <= hr * hr) {
                            float[] an = hs[3 - k];   // the opposite corner stays where it is
                            ancX = an[0];
                            ancY = an[1];
                            drag = 2;
                            return true;
                        }
                    }
                    if (x >= left && x <= left + gw && y >= top && y <= top + size) {
                        drag = 1;
                        offX = x - left;
                        offY = y - top;
                    }
                    return true;
                }
                case MotionEvent.ACTION_MOVE:
                    if (drag == 1) {
                        left = x - offX;
                        top = y - offY;
                        clampGeometry();
                        invalidate();
                    } else if (drag == 2) {
                        float nw = Math.max(Math.abs(x - ancX), 0.06f * Hh * GROUP);
                        size = nw / GROUP;
                        clampGeometry();
                        left = x < ancX ? ancX - GROUP * size : ancX;
                        top = y < ancY ? ancY - size : ancY;
                        clampGeometry();
                        invalidate();
                    }
                    return true;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    if (drag != 0) save();
                    drag = 0;
                    return true;
                default:
                    return true;
            }
        }
    }

    // ------------------------------------------------------------------ start-up cover

    private void showCover() {
        if (coverV != null || !GoPrefs.canOverlay(this) || wm == null) return;
        if (W == 0) {
            DisplayMetrics dm = new DisplayMetrics();
            wm.getDefaultDisplay().getRealMetrics(dm);
            W = dm.widthPixels;
            Hh = dm.heightPixels;
        }
        Bitmap b = null;
        try {
            java.io.File f = GoPrefs.file(this, "wallpaper.png");
            if (f.exists()) b = BitmapFactory.decodeFile(f.getAbsolutePath());
        } catch (Throwable ignored) {
        }
        final Bitmap bmp = b;
        View v = new View(this) {
            @Override
            protected void onDraw(Canvas c) {
                c.drawColor(Color.BLACK);
                if (bmp == null) return;
                float s = Math.max(getWidth() / (float) bmp.getWidth(), getHeight() / (float) bmp.getHeight());
                float bw = bmp.getWidth() * s, bh = bmp.getHeight() * s;
                c.drawBitmap(bmp, null, new RectF((getWidth() - bw) / 2f, (getHeight() - bh) / 2f,
                        (getWidth() + bw) / 2f, (getHeight() + bh) / 2f), new Paint(Paint.FILTER_BITMAP_FLAG));
            }
        };
        v.setClickable(true);
        coverV = v;
        add(v, lp(-1, -1, true, Gravity.TOP | Gravity.START, 0, 0));
        H.postDelayed(new Runnable() {
            @Override
            public void run() {
                fadeCover();
            }
        }, Math.max(1000, p.getInt("coverMs", 4000)));
    }

    private void fadeCover() {
        final View v = coverV;
        if (v == null) return;
        v.animate().alpha(0f).setDuration(450).withEndAction(new Runnable() {
            @Override
            public void run() {
                removeCover();
            }
        }).start();
    }

    private void removeCover() {
        if (coverV == null) return;
        remove(coverV);
        coverV = null;
    }

    // ------------------------------------------------------------------ GPS: speed light and slow-speed 360

    private final LocationListener locL = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            if (loc == null || !LocationManager.GPS_PROVIDER.equals(loc.getProvider())) return;
            float before = curKmh;
            curKmh = loc.hasSpeed() ? loc.getSpeed() * 3.6f : 0f;
            int am = p.getInt("warnAmber", 80), rd = p.getInt("warnRed", 120);
            if (((before >= am) != (curKmh >= am) || (before >= rd) != (curKmh >= rd)) && speedV != null) speedV.invalidate();
            check360();
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        @Override
        public void onProviderEnabled(String provider) {
        }

        @Override
        public void onProviderDisabled(String provider) {
        }
    };

    private void startLocation() {
        if (lm != null) return;
        try {
            if (Build.VERSION.SDK_INT >= 23 && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) return;
            lm = (LocationManager) getSystemService(LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locL, Looper.getMainLooper());
            }
        } catch (Throwable t) {
            lm = null;
        }
    }

    private void stopLocation() {
        try {
            if (lm != null) lm.removeUpdates(locL);
        } catch (Throwable ignored) {
        }
        lm = null;
    }

    /** Show the 360 when the car creeps along slowly (faster than the low limit, slower than the top one, 20 km/h by default). */
    private void check360() {
        if (!p.getBoolean("auto360", true) || coverV != null) return;
        if ("cam".equals(p.getString("cur", "maps"))) return;
        if (btnV != null && btnV.edit) return;
        if (GoPrefs.pkg(this, "cam").length() == 0) return;
        float hi = Math.max(2, p.getInt("auto360Max", 20));
        float lo = Math.min(hi - 1, p.getInt("auto360Min", 5));
        float k = curKmh;
        if (k > Math.max(15f, hi + 5f)) armed360 = true;
        if (!(k >= lo && k < hi)) return;
        long now = System.currentTimeMillis();
        if (p.getBoolean("auto360ModeB", false)) {
            if (!armed360) return;
        } else if (now - lastAuto360 < p.getInt("auto360Gap", 60) * 1000L) {
            return;
        }
        lastAuto360 = now;
        armed360 = false;
        GoPrefs.launch(this, "cam");
        if (btnV != null) btnV.invalidate();
    }
}
