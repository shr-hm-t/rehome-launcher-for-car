package com.rehome.go;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.LinearGradient;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

/**
 * Rehome: Go overlays and the start-up sequence.
 * Draws the star vignette, the logo, the top frame and the mini bar over the four apps (maps, radio, 360, YouTube),
 * and starts the four apps one after the other behind a cover at boot.
 */
public class GoService extends Service {
    static GoService inst;

    private WindowManager wm;
    private SharedPreferences p;
    private final Handler H = new Handler(Looper.getMainLooper());
    private VigView vigV;
    private TopView topV;
    private View leftB, rightB;
    private LinearLayout barV;
    private View coverV;
    private Bitmap logo;
    private float sc;
    private int W, Hh;
    private String barFor = "?";
    private boolean showL, showR, showBar, showVig;
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
        build();
        startWatcher();
        H.removeCallbacks(watchTick);
        H.postDelayed(watchTick, 4000);
        startLocation();
        if (i != null && i.getBooleanExtra("seq", false)) sequence(i.getBooleanExtra("force", false));
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        H.removeCallbacks(watchTick);
        stopLocation();
        removeAll();
        removeCover();
        AppWatcher.stop();
        if (inst == this) inst = null;
        super.onDestroy();
    }

    private void startWatcher() {
        AppWatcher.start(this, new Runnable() {
            @Override
            public void run() {
                update();
            }
        });
    }

    /** Usage access can be allowed after Go started: keep checking until the watcher runs. */
    private final Runnable watchTick = new Runnable() {
        @Override
        public void run() {
            if (!AppWatcher.running && AppWatcher.granted(GoService.this)) startWatcher();
            H.postDelayed(this, 4000);
        }
    };

    /** Rebuild the overlays after a setting changed. */
    static void refresh() {
        final GoService s = inst;
        if (s == null) return;
        s.H.removeCallbacks(s.rebuild);
        s.H.postDelayed(s.rebuild, 150);
    }

    private final Runnable rebuild = new Runnable() {
        @Override
        public void run() {
            build();
        }
    };

    private boolean canOverlay() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 26
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    // ------------------------------------------------------------------ windows

    private void removeAll() {
        for (View v : new View[]{vigV, topV, leftB, rightB, barV}) {
            if (v != null) {
                try {
                    wm.removeView(v);
                } catch (Exception ignored) {
                }
            }
        }
        vigV = null;
        topV = null;
        leftB = null;
        rightB = null;
        barV = null;
        barFor = "?";
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

    private void add(View v, WindowManager.LayoutParams l) {
        try {
            wm.addView(v, l);
        } catch (Exception ignored) {
        }
    }

    private void build() {
        removeAll();
        if (!canOverlay() || wm == null) return;
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        W = dm.widthPixels;
        Hh = dm.heightPixels;
        sc = GoPrefs.scale(this, p);
        logo = GoWallpaper.loadLogo(this);
        // bottom to top: vignette + logo + speed light, top frame, touch blockers, mini bar
        vigV = new VigView(this);
        add(vigV, lp(-1, -1, false, Gravity.TOP | Gravity.START, 0, 0));
        topV = new TopView(this);
        float sH = p.getInt("statusH", 44) * sc, sTop = p.getInt("statusTop", 0) * sc;
        float[] b = GoDraw.barRect(p, sc, W, Hh);
        int th = (int) Math.ceil(Math.max(sTop + sH, GoDraw.joined(p, sc) ? b[1] + b[3] : 0)) + 2;
        add(topV, lp(-1, th, false, Gravity.TOP | Gravity.START, 0, 0));
        if (p.getBoolean("block", true)) {
            int bh = (int) Math.ceil(sTop + sH);
            leftB = new View(this);
            rightB = new View(this);
            add(leftB, lp((int) (p.getInt("statusL", 200) * sc), bh, true, Gravity.TOP | Gravity.START, 0, 0));
            add(rightB, lp((int) (p.getInt("statusR", 200) * sc), bh, true, Gravity.TOP | Gravity.END, 0, 0));
        }
        barV = new BarLayout(this);
        barV.setOrientation(LinearLayout.HORIZONTAL);
        barV.setGravity(Gravity.CENTER_VERTICAL);
        add(barV, lp((int) b[2], (int) b[3], true, Gravity.TOP | Gravity.START, (int) b[0], (int) b[1]));
        if (coverV != null) {   // keep the cover on top
            try {
                wm.removeView(coverV);
                wm.addView(coverV, lp(-1, -1, true, Gravity.TOP | Gravity.START, 0, 0));
            } catch (Exception ignored) {
            }
        }
        update();
    }

    // ------------------------------------------------------------------ which app is in front

    private String roleOf(String pk) {
        if (pk == null || pk.length() == 0) return null;
        for (String r : GoPrefs.ROLES) {
            if (pk.equals(p.getString("app_" + r, ""))) return r;
        }
        return null;
    }

    private void update() {
        if (vigV == null) return;
        if (GoPrefs.otherHome(this)) {   // another launcher is Home now: stay quiet
            stopSelf();
            return;
        }
        String pk = AppWatcher.pkg;
        String r = roleOf(pk);
        boolean self = getPackageName().equals(pk);
        boolean preview = self && p.getBoolean("preview", false);
        if (r != null) p.edit().putString("lastApp", r).apply();
        boolean four = r != null;
        showVig = four || preview;
        boolean statusRole = "maps".equals(r) || "radio".equals(r) || "yt".equals(r) || preview;
        boolean st = statusRole && p.getBoolean("statusOn", true);
        showL = st;
        showR = st;
        showBar = four || self;
        topV.invalidate();
        vigV.invalidate();
        int vis = st ? View.VISIBLE : View.GONE;
        if (leftB != null) leftB.setVisibility(vis);
        if (rightB != null) rightB.setVisibility(vis);
        if (barV != null) {
            barV.setVisibility(showBar ? View.VISIBLE : View.GONE);
            String key = self ? "" : String.valueOf(r);
            if (!key.equals(barFor)) {
                barFor = key;
                fillBar(self ? "" : r);
            }
        }
    }

    // ------------------------------------------------------------------ the three views

    private class TopView extends View {
        TopView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            if (!showL && !showR && !showBar) return;
            GoDraw.top(c, p, sc, W, Hh, showL, showR, showBar);
        }
    }

    private class VigView extends View {
        VigView(Context c) {
            super(c);
        }

        @Override
        protected void onDraw(Canvas c) {
            if (!showVig) return;
            drawSpeedLight(c, getWidth(), getHeight());
            if (p.getBoolean("vigOn", true)) GoDraw.vignette(c, getWidth(), getHeight(), p.getInt("vig", 200));
            if (logo != null && p.getBoolean("logoOn", true)) {
                boolean sp = p.getBoolean("sparkle", true);
                GoDraw.logo(c, logo, getWidth() / 2f, p.getInt("logoTop", 6) * sc, p.getInt("logoSize", 64) * sc, sp);
                if (sp) postInvalidateOnAnimation();
            }
        }
    }

    private class BarLayout extends LinearLayout {
        BarLayout(Context c) {
            super(c);
            setWillNotDraw(false);
        }

        @Override
        protected void onDraw(Canvas c) {
            if (!GoDraw.joined(p, sc)) GoDraw.bar(c, getWidth(), getHeight());
        }
    }

    // ------------------------------------------------------------------ over-speed light

    /** Soft light along the top third: amber from the first speed, red from the second (like the launcher). */
    private void drawSpeedLight(Canvas c, float w, float h) {
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
        float th = h / 3f;
        pt.setShader(new LinearGradient(0, 0, 0, th, new int[]{strong, mid, none}, new float[]{0f, 0.4f, 1f}, Shader.TileMode.CLAMP));
        c.drawRect(0, 0, w, th, pt);
        pt.setShader(null);
        pt.setColor(0xFF000000 | (col & 0x00FFFFFF));
        c.drawRect(0, 0, w, Math.max(2f, 3f * sc), pt);
    }

    // ------------------------------------------------------------------ GPS speed: over-speed light and slow-speed 360

    private final LocationListener locL = new LocationListener() {
        @Override
        public void onLocationChanged(Location loc) {
            if (loc == null || !LocationManager.GPS_PROVIDER.equals(loc.getProvider())) return;
            float before = curKmh;
            curKmh = loc.hasSpeed() ? loc.getSpeed() * 3.6f : 0f;
            boolean level = (before >= p.getInt("warnAmber", 80)) != (curKmh >= p.getInt("warnAmber", 80))
                    || (before >= p.getInt("warnRed", 120)) != (curKmh >= p.getInt("warnRed", 120));
            if (level && vigV != null) vigV.invalidate();
            check360();
        }

        @Override
        public void onStatusChanged(String provider, int status, android.os.Bundle extras) {
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

    /** Show the 360 when the car creeps along slowly while Maps, the radio or YouTube is in front (upper limit 20 km/h). */
    private void check360() {
        if (!p.getBoolean("auto360", true) || coverV != null) return;
        String r = roleOf(AppWatcher.pkg);
        if (r == null || "cam".equals(r)) return;
        if (p.getString("app_cam", "").length() == 0) return;
        float hi = Math.min(20, p.getInt("auto360Max", 10));
        float lo = Math.min(hi, p.getInt("auto360Min", 5));
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
        launch(this, "cam");
    }

    // ------------------------------------------------------------------ mini bar

    private void fillBar(String current) {
        barV.removeAllViews();
        boolean first = true;
        for (final String r : GoPrefs.ROLES) {
            if (r.equals(current)) continue;
            if (!"yt".equals(r) && p.getString("app_" + r, "").length() == 0) continue;
            if (!first) {
                View d = new View(this);
                d.setBackgroundColor(0x66FFFFFF);
                LinearLayout.LayoutParams dl = new LinearLayout.LayoutParams(Math.max(1, (int) (1.5f * sc)), (int) (p.getInt("barH", 48) * sc * 0.55f));
                barV.addView(d, dl);
            }
            first = false;
            final Glyph g = new Glyph(this, r);
            g.setOnTouchListener(new View.OnTouchListener() {
                private boolean held;
                private Runnable hold;

                @Override
                public boolean onTouch(View v, MotionEvent e) {
                    switch (e.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            held = false;
                            v.setAlpha(0.55f);
                            hold = new Runnable() {
                                @Override
                                public void run() {
                                    held = true;
                                    v.setAlpha(1f);
                                    openFallback(GoService.this);
                                }
                            };
                            H.postDelayed(hold, Math.max(500, p.getInt("holdMs", 1500)));
                            return true;
                        case MotionEvent.ACTION_UP:
                            H.removeCallbacks(hold);
                            v.setAlpha(1f);
                            if (!held) launch(GoService.this, r);
                            return true;
                        case MotionEvent.ACTION_CANCEL:
                            H.removeCallbacks(hold);
                            v.setAlpha(1f);
                            return true;
                        default:
                            return true;
                    }
                }
            });
            barV.addView(g, new LinearLayout.LayoutParams(0, -1, 1f));
        }
    }

    /** Outline icons in the style of the contacts bar: radio tower, "360°", map pin, play button. */
    private static class Glyph extends View {
        private final String role;
        private final Paint pt = new Paint(Paint.ANTI_ALIAS_FLAG);

        Glyph(Context c, String role) {
            super(c);
            this.role = role;
            pt.setColor(Color.WHITE);
        }

        @Override
        protected void onDraw(Canvas c) {
            float w = getWidth(), h = getHeight(), cx = w / 2f, cy = h / 2f, s = Math.min(w, h) * 0.62f;
            float sw = Math.max(2f, s * 0.085f);
            pt.setStrokeWidth(sw);
            pt.setStrokeCap(Paint.Cap.ROUND);
            pt.setStrokeJoin(Paint.Join.ROUND);
            pt.setStyle(Paint.Style.STROKE);
            switch (role) {
                case "radio": {
                    c.drawLine(cx, cy - s * 0.1f, cx - s * 0.32f, cy + s * 0.5f, pt);
                    c.drawLine(cx, cy - s * 0.1f, cx + s * 0.32f, cy + s * 0.5f, pt);
                    c.drawLine(cx - s * 0.2f, cy + s * 0.22f, cx + s * 0.2f, cy + s * 0.22f, pt);
                    pt.setStyle(Paint.Style.FILL);
                    c.drawCircle(cx, cy - s * 0.2f, sw * 0.9f, pt);
                    pt.setStyle(Paint.Style.STROKE);
                    for (int k = 1; k <= 2; k++) {
                        float rr = s * (0.22f + 0.2f * k);
                        c.drawArc(new RectF(cx - rr, cy - s * 0.2f - rr, cx + rr, cy - s * 0.2f + rr), 215, 110, false, pt);
                        c.drawArc(new RectF(cx - rr, cy - s * 0.2f - rr, cx + rr, cy - s * 0.2f + rr), 35, 110, false, pt);
                    }
                    break;
                }
                case "cam": {
                    pt.setStyle(Paint.Style.FILL);
                    pt.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    pt.setTextSize(s * 0.78f);
                    pt.setTextAlign(Paint.Align.CENTER);
                    c.drawText("360°", cx, cy + s * 0.27f, pt);
                    break;
                }
                case "maps": {
                    Path pin = new Path();
                    float rr = s * 0.3f;
                    pin.addCircle(cx, cy - s * 0.12f, rr, Path.Direction.CW);
                    pt.setStyle(Paint.Style.FILL);
                    Path body = new Path();
                    body.moveTo(cx - rr, cy - s * 0.12f);
                    body.lineTo(cx, cy + s * 0.5f);
                    body.lineTo(cx + rr, cy - s * 0.12f);
                    body.close();
                    c.drawPath(body, pt);
                    c.drawCircle(cx, cy - s * 0.12f, rr, pt);
                    pt.setColor(0xFF05070A);
                    c.drawCircle(cx, cy - s * 0.12f, rr * 0.4f, pt);
                    pt.setColor(Color.WHITE);
                    break;
                }
                default: {
                    RectF rc = new RectF(cx - s * 0.5f, cy - s * 0.32f, cx + s * 0.5f, cy + s * 0.32f);
                    c.drawRoundRect(rc, s * 0.2f, s * 0.2f, pt);
                    Path tri = new Path();
                    tri.moveTo(cx - s * 0.12f, cy - s * 0.16f);
                    tri.lineTo(cx + s * 0.2f, cy);
                    tri.lineTo(cx - s * 0.12f, cy + s * 0.16f);
                    tri.close();
                    pt.setStyle(Paint.Style.FILL);
                    c.drawPath(tri, pt);
                }
            }
        }
    }

    // ------------------------------------------------------------------ launching apps

    static void launch(Context c, String role) {
        try {
            SharedPreferences p = GoPrefs.get(c);
            if ("yt".equals(role)) {
                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com"));
                String bp = p.getString("app_yt", "");
                if (bp.length() > 0) i.setPackage(bp);
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                c.startActivity(i);
                return;
            }
            String pk = p.getString("app_" + role, "");
            if (pk.length() == 0) return;
            Intent i = c.getPackageManager().getLaunchIntentForPackage(pk);
            if (i == null) return;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception ignored) {
        }
    }

    static void openFallback(Context c) {
        Intent i = new Intent(c, GoActivity.class);
        i.putExtra("fallback", true);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        try {
            c.startActivity(i);
        } catch (Exception ignored) {
        }
    }

    // ------------------------------------------------------------------ start-up sequence + cover

    private void sequence(boolean force) {
        if (!force && !GoPrefs.freshBoot(this, p)) return;
        GoPrefs.markBoot(this, p);
        showCover();
        long d1 = Math.max(0, p.getInt("d1", 1000)), d2 = Math.max(0, p.getInt("d2", 3000));
        long hold = Math.max(0, p.getInt("hold", 3000));
        launch(this, "maps");
        H.postDelayed(new Runnable() {
            @Override
            public void run() {
                launch(GoService.this, "radio");
            }
        }, d1);
        H.postDelayed(new Runnable() {
            @Override
            public void run() {
                launch(GoService.this, "cam");
            }
        }, d1 + d2);
        H.postDelayed(new Runnable() {
            @Override
            public void run() {
                fadeCover();
            }
        }, d1 + d2 + hold);
    }

    private void showCover() {
        if (coverV != null || !canOverlay() || wm == null) return;
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getRealMetrics(dm);
        final Bitmap bmp = GoWallpaper.current(this, dm.widthPixels, dm.heightPixels);
        View v = new View(this) {
            @Override
            protected void onDraw(Canvas c) {
                c.drawColor(Color.BLACK);
                float s = Math.max(getWidth() / (float) bmp.getWidth(), getHeight() / (float) bmp.getHeight());
                float bw = bmp.getWidth() * s, bh = bmp.getHeight() * s;
                RectF d = new RectF((getWidth() - bw) / 2f, (getHeight() - bh) / 2f, (getWidth() + bw) / 2f, (getHeight() + bh) / 2f);
                c.drawBitmap(bmp, null, d, new Paint(Paint.FILTER_BITMAP_FLAG));
            }
        };
        v.setClickable(true);
        coverV = v;
        add(v, lp(-1, -1, true, Gravity.TOP | Gravity.START, 0, 0));
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
        try {
            wm.removeView(coverV);
        } catch (Exception ignored) {
        }
        coverV = null;
    }
}
