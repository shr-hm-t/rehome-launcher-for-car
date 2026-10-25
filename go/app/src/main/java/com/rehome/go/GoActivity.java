package com.rehome.go;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Rehome: Go. Set as Home it starts the four apps behind a cover, keeps the overlays going (GoService)
 * and otherwise stays out of the way. This screen is the fallback: first-run welcome, settings and app choice.
 */
public class GoActivity extends Activity {
    private static final int REQ_LOGO = 71, REQ_WP = 72;

    private SharedPreferences p;
    private float sc;
    private final Handler H = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private boolean locked = false;
    private BroadcastReceiver unlockRx;
    private Runnable onResumeHook;
    private int wStep = 0;
    private boolean inSub = false;   // a page above the fallback is open (settings or welcome)

    // ------------------------------------------------------------------ lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(android.content.pm.ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        p = GoPrefs.get(this);
        sc = GoPrefs.scale(this, p);
        if (Build.VERSION.SDK_INT >= 24) {
            android.os.UserManager um = (android.os.UserManager) getSystemService(Context.USER_SERVICE);
            if (um != null && !um.isUserUnlocked()) {
                // storage still locked after a cold start: show the wallpaper and carry on once unlocked
                locked = true;
                showWallpaperOnly();
                unlockRx = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context c, Intent i) {
                        locked = false;
                        init(getIntent());
                    }
                };
                registerReceiver(unlockRx, new IntentFilter(Intent.ACTION_USER_UNLOCKED));
                return;
            }
        }
        init(getIntent());
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        if (!locked) init(intent);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (onResumeHook != null) onResumeHook.run();
    }

    @Override
    protected void onDestroy() {
        if (unlockRx != null) {
            try {
                unregisterReceiver(unlockRx);
            } catch (Exception ignored) {
            }
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if (inSub && p.getBoolean("welcomed", false)) {
            showFallback();
        }
        // as a Home screen there is nothing to go back to
    }

    private void init(Intent i) {
        p = GoPrefs.get(this);
        sc = GoPrefs.scale(this, p);
        boolean homeIntent = i != null && i.hasCategory(Intent.CATEGORY_HOME);
        boolean fb = i != null && i.getBooleanExtra("fallback", false);
        boolean welcomed = p.getBoolean("welcomed", false);
        boolean goHome = homeIntent && !GoPrefs.otherHome(this);
        boolean fresh = goHome && welcomed && !fb && GoPrefs.freshBoot(this, p);
        if (goHome) {
            Intent s = new Intent(this, GoService.class);
            if (fresh) s.putExtra("seq", true);
            try {
                startService(s);
            } catch (Exception ignored) {
            }
        }
        if (!welcomed) {
            showWelcome();
            return;
        }
        if (fresh) {
            showWallpaperOnly();   // the cover and the start-up sequence run in GoService
            return;
        }
        if (goHome && !fb && !p.getBoolean("homeFallback", false)) {
            String last = p.getString("lastApp", "cam");
            if (p.getString("app_" + last, "").length() == 0 && !"yt".equals(last)) last = "cam";
            if (p.getString("app_" + last, "").length() > 0 || "yt".equals(last)) {
                root.removeAllViews();
                root.setBackgroundColor(Color.BLACK);
                GoService.launch(this, last);
                return;
            }
        }
        showFallback();
    }

    // ------------------------------------------------------------------ small UI helpers

    private int px(float dp) {
        return Math.round(dp * sc);
    }

    private TextView tv(String s, float dp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, dp * sc);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private GradientDrawable pill(int fill, int stroke, float rDp) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(fill);
        g.setCornerRadius(px(rDp));
        g.setStroke(Math.max(1, px(1)), stroke);
        return g;
    }

    private TextView btn(String s, final Runnable r) {
        TextView t = tv(s, 16, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(px(18), px(10), px(18), px(10));
        t.setBackground(pill(0xCC10161C, 0x66FFFFFF, 16));
        t.setClickable(true);
        t.setOnClickListener(v -> r.run());
        return t;
    }

    private LinearLayout.LayoutParams wrap(int top) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = px(top);
        return l;
    }

    private void changed() {
        GoService.refresh();
    }

    private void showWallpaperOnly() {
        root.removeAllViews();
        inSub = false;
        onResumeHook = null;
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        iv.setImageBitmap(GoWallpaper.current(this, dm.widthPixels, dm.heightPixels));
        root.addView(iv, new FrameLayout.LayoutParams(-1, -1));
    }

    // ------------------------------------------------------------------ fallback screen

    private void showFallback() {
        showWallpaperOnly();
        inSub = false;
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER);
        row.addView(btn("Settings", this::showSettings));
        View sp = new View(this);
        row.addView(sp, new LinearLayout.LayoutParams(px(14), 1));
        row.addView(btn("Start the four apps", () -> {
            Intent s = new Intent(this, GoService.class).putExtra("seq", true).putExtra("force", true);
            try {
                startService(s);
            } catch (Exception ignored) {
            }
        }));
        col.addView(row);
        final TextView warn = tv("", 14, 0xFFFFC27A, false);
        warn.setGravity(Gravity.CENTER);
        col.addView(warn, wrap(10));
        FrameLayout.LayoutParams fl = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER_HORIZONTAL | Gravity.BOTTOM);
        fl.bottomMargin = px(70);
        root.addView(col, fl);
        onResumeHook = () -> {
            String w = "";
            if (!overlayOk()) w += "Overlay permission is off. ";
            if (!AppWatcher.granted(this)) w += "Usage access is off. ";
            if (GoPrefs.otherHome(this)) w += "Another launcher is the Home app, so Go stays quiet.";
            warn.setText(w);
        };
        onResumeHook.run();
    }

    private boolean overlayOk() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    // ------------------------------------------------------------------ settings

    private ScrollView page(LinearLayout[] colOut, String title, final Runnable closeAction) {
        FrameLayout f = new FrameLayout(this);
        f.setBackgroundColor(0xF2000000);
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(60), px(70), px(60), px(40));
        sv.addView(col);
        f.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        col.addView(tv(title, 24, Color.WHITE, true));
        if (closeAction != null) {
            TextView c = btn("Close", closeAction);
            FrameLayout.LayoutParams cl = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            cl.topMargin = px(70);
            cl.rightMargin = px(60);
            f.addView(c, cl);
        }
        root.addView(f, new FrameLayout.LayoutParams(-1, -1));
        colOut[0] = col;
        return sv;
    }

    private void section(LinearLayout col, String t) {
        TextView s = tv(t, 18, 0xFF8FB8FF, true);
        s.setPadding(0, px(22), 0, px(4));
        col.addView(s);
    }

    private void note(LinearLayout col, String t) {
        TextView n = tv(t, 13, 0xFFB0B8C0, false);
        n.setPadding(0, px(2), 0, px(4));
        col.addView(n);
    }

    private String fmt(int v, String unit) {
        if ("ms".equals(unit)) return String.format(java.util.Locale.US, "%.1f s (%d ms)", v / 1000.0, v);
        return v + (unit.length() > 0 ? " " + unit : "");
    }

    private void repeat(View b, final Runnable action) {
        b.setOnTouchListener(new View.OnTouchListener() {
            private Runnable rep;

            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        action.run();
                        rep = new Runnable() {
                            @Override
                            public void run() {
                                action.run();
                                H.postDelayed(this, 90);
                            }
                        };
                        H.postDelayed(rep, 450);
                        v.setAlpha(0.6f);
                        return true;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        H.removeCallbacks(rep);
                        v.setAlpha(1f);
                        return true;
                    default:
                        return true;
                }
            }
        });
    }

    private void stepper(LinearLayout col, String label, final String key, final int def, final int min, final int max, final int step, final String unit) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, px(4), 0, px(4));
        TextView l = tv(label, 16, Color.WHITE, false);
        r.addView(l, new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView val = tv(fmt(p.getInt(key, def), unit), 16, 0xFFDDE6FF, true);
        val.setGravity(Gravity.CENTER);
        TextView minus = btn("−", () -> {
        });
        TextView plus = btn("+", () -> {
        });
        repeat(minus, () -> {
            int v = Math.max(min, p.getInt(key, def) - step);
            p.edit().putInt(key, v).apply();
            val.setText(fmt(v, unit));
            changed();
        });
        repeat(plus, () -> {
            int v = Math.min(max, p.getInt(key, def) + step);
            p.edit().putInt(key, v).apply();
            val.setText(fmt(v, unit));
            changed();
        });
        r.addView(minus, new LinearLayout.LayoutParams(px(52), -2));
        r.addView(val, new LinearLayout.LayoutParams(px(190), -2));
        r.addView(plus, new LinearLayout.LayoutParams(px(52), -2));
        col.addView(r);
    }

    private void toggle(LinearLayout col, String label, final String key, final boolean def) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, px(4), 0, px(4));
        r.addView(tv(label, 16, Color.WHITE, false), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView b = btn(p.getBoolean(key, def) ? "On" : "Off", () -> {
        });
        b.setOnClickListener(v -> {
            boolean nv = !p.getBoolean(key, def);
            p.edit().putBoolean(key, nv).apply();
            b.setText(nv ? "On" : "Off");
            changed();
        });
        r.addView(b, new LinearLayout.LayoutParams(px(120), -2));
        col.addView(r);
    }

    private void action(LinearLayout col, String label, Runnable run) {
        col.addView(btn(label, run), wrap(8));
    }

    private final java.util.Map<String, TextView> appLabels = new java.util.HashMap<>();

    private void appRow(LinearLayout col, final String role) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, px(4), 0, px(4));
        final TextView t = tv("", 15, Color.WHITE, false);
        appLabels.put(role, t);
        refreshAppLabel(role);
        r.addView(t, new LinearLayout.LayoutParams(0, -2, 1f));
        r.addView(btn("Choose", () -> chooseApp(role)));
        col.addView(r);
    }

    private void refreshAppLabel(String role) {
        TextView t = appLabels.get(role);
        if (t == null) return;
        String pk = p.getString("app_" + role, "");
        t.setText(GoPrefs.label(role) + ":  " + (pk.length() == 0 ? "(not set)" : appName(pk) + "  —  " + pk));
    }

    private String appName(String pk) {
        try {
            PackageManager pm = getPackageManager();
            return String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(pk, 0)));
        } catch (Exception e) {
            return pk;
        }
    }

    private void showSettings() {
        showFallbackBackdrop();
        inSub = true;
        LinearLayout[] c = new LinearLayout[1];
        page(c, "Rehome: Go settings", this::showFallback);
        LinearLayout col = c[0];
        section(col, "Start-up");
        note(col, "On boot: the cover goes up, Maps opens at once, then the radio, then 360. 360 stays in front.");
        stepper(col, "Radio starts after Maps", "d1", 1000, 0, 30000, 100, "ms");
        stepper(col, "360 starts after the radio", "d2", 3000, 0, 30000, 100, "ms");
        stepper(col, "Cover stays after 360 starts", "hold", 3000, 0, 30000, 100, "ms");
        action(col, "Run the start-up now (test)", () -> {
            Intent s = new Intent(this, GoService.class).putExtra("seq", true).putExtra("force", true);
            try {
                startService(s);
            } catch (Exception ignored) {
            }
        });
        section(col, "Home button");
        toggle(col, "Home shows this screen (off = back to the last app)", "homeFallback", false);
        stepper(col, "Hold a mini bar button for this screen", "holdMs", 1500, 500, 5000, 100, "ms");
        section(col, "Apps");
        note(col, "Picked by name and package. Hold the mini bar buttons any time to come back here.");
        for (String r : GoPrefs.ROLES) appRow(col, r);
        action(col, "Auto-pick the empty ones", () -> {
            autoPick();
            for (String r : GoPrefs.ROLES) refreshAppLabel(r);
            changed();
        });
        section(col, "Driving");
        toggle(col, "Show the 360 when driving slowly (test while parked first)", "auto360", true);
        stepper(col, "...faster than", "auto360Min", 5, 0, 20, 1, "km/h");
        stepper(col, "...and slower than (20 km/h at most)", "auto360Max", 10, 1, 20, 1, "km/h");
        toggle(col, "Repeat only after going faster again (off = after the time gap)", "auto360ModeB", false);
        stepper(col, "Time gap between shows", "auto360Gap", 60, 10, 600, 5, "s");
        toggle(col, "Speed light along the top (over-speed warning)", "speedLight", true);
        stepper(col, "Amber light from", "warnAmber", 80, 20, 250, 5, "km/h");
        stepper(col, "Red light from", "warnRed", 120, 20, 250, 5, "km/h");
        section(col, "Logo and wallpaper");
        toggle(col, "Show the logo", "logoOn", true);
        action(col, "Choose logo image", () -> pickImage(REQ_LOGO));
        stepper(col, "Logo size", "logoSize", 64, 16, 240, 2, "dp");
        stepper(col, "Logo distance from top", "logoTop", 52, 0, 300, 2, "dp");
        toggle(col, "Sparkle on the logo", "sparkle", true);
        glowSwatches(col);
        action(col, "Make the wallpaper", () -> makeWallpaper(true));
        action(col, "Use my own wallpaper image", () -> pickImage(REQ_WP));
        toggle(col, "Use my own image instead of the made wallpaper", "wpCustom", false);
        section(col, "Vignette");
        toggle(col, "Vignette on", "vigOn", true);
        stepper(col, "Strength", "vig", 200, 0, 200, 10, "");
        section(col, "Status frame (Maps, radio, YouTube)");
        toggle(col, "Status frame on", "statusOn", true);
        stepper(col, "Height", "statusH", 44, 24, 160, 2, "dp");
        stepper(col, "Distance from top", "statusTop", 0, 0, 200, 2, "dp");
        stepper(col, "Left piece width", "statusL", 200, 40, 700, 10, "dp");
        stepper(col, "Right piece width", "statusR", 200, 40, 700, 10, "dp");
        stepper(col, "Link strip between the pieces (0 = none)", "topLink", 8, 0, 60, 2, "dp");
        toggle(col, "Pieces block touches", "block", true);
        section(col, "Mini bar");
        toggle(col, "Put the mini bar at the bottom (off = top, joined to the frame)", "barBottomMode", false);
        note(col, "At the top (joined to the frame):");
        stepper(col, "Width", "barW", 340, 120, 1000, 10, "dp");
        stepper(col, "Height", "barH", 48, 30, 160, 2, "dp");
        stepper(col, "Left / right position (0 = centre)", "barX", 0, -700, 700, 10, "dp");
        stepper(col, "Distance from the top", "barTop", 0, 0, 300, 2, "dp");
        note(col, "At the bottom (own rounded bar):");
        stepper(col, "Width", "bbW", 700, 200, 1400, 10, "dp");
        stepper(col, "Height", "bbH", 56, 30, 160, 2, "dp");
        stepper(col, "Left / right position (0 = centre)", "bbX", 0, -700, 700, 10, "dp");
        stepper(col, "Distance from the bottom", "bbBottom", 10, 0, 300, 2, "dp");
        section(col, "Screen");
        stepper(col, "UI size", "ui", 100, 60, 160, 5, "%");
        note(col, "UI size scales every size above. The system \"smallest width\" (for example 480) is an Android developer setting and cannot be changed from an app.");
        toggle(col, "Preview the overlays on this screen too", "preview", false);
        section(col, "Permissions");
        final TextView perm = tv("", 14, 0xFFFFC27A, false);
        col.addView(perm);
        action(col, "Allow draw over other apps", this::openOverlaySettings);
        action(col, "Allow usage access", this::openUsageSettings);
        action(col, "Allow location (speed for the 360 and the speed light)", this::askLocation);
        onResumeHook = () -> perm.setText(permText());
        onResumeHook.run();
        section(col, "Home app");
        action(col, "Choose Rehome: Go as the Home app", this::openHomeChooser);
        action(col, "Run the welcome again", () -> {
            wStep = 0;
            showWelcome();
        });
        TextView pad = tv("", 10, 0, false);
        col.addView(pad, new LinearLayout.LayoutParams(1, px(60)));
    }

    private void showFallbackBackdrop() {
        showWallpaperOnly();
    }

    private void glowSwatches(LinearLayout col) {
        TextView l = tv("Glow colour of the wallpaper", 16, Color.WHITE, false);
        l.setPadding(0, px(8), 0, px(4));
        col.addView(l);
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        final int[] cols = {0xFF6FA8FF, 0xFF3DDCC4, 0xFFFFB35A, 0xFFFF7A9C, 0xFFA98BFF, 0xFFDDE6FF};
        for (final int cc : cols) {
            View v = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(cc);
            g.setStroke(px(2), 0x88FFFFFF);
            v.setBackground(g);
            v.setOnClickListener(x -> {
                p.edit().putInt("glow", cc).apply();
                Toast.makeText(this, "Glow colour set. Press \"Make the wallpaper\" to apply it.", Toast.LENGTH_SHORT).show();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(44), px(44));
            lp.rightMargin = px(12);
            r.addView(v, lp);
        }
        col.addView(r);
    }

    // ------------------------------------------------------------------ images

    private void pickImage(int req) {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(i, "Choose an image"), req);
        } catch (Exception e) {
            Toast.makeText(this, "No gallery or file app found.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Uri u = data.getData();
            DisplayMetrics dm = getResources().getDisplayMetrics();
            int max = req == REQ_LOGO ? 600 : Math.max(dm.widthPixels, dm.heightPixels);
            Bitmap b = decode(u, max);
            if (b == null) {
                Toast.makeText(this, "Could not read that image.", Toast.LENGTH_LONG).show();
                return;
            }
            if (req == REQ_LOGO) {
                GoWallpaper.save(this, b, GoWallpaper.LOGO);
                p.edit().putBoolean("logoOn", true).apply();
                Toast.makeText(this, "Logo set.", Toast.LENGTH_SHORT).show();
            } else {
                GoWallpaper.save(this, b, GoWallpaper.CUSTOM);
                p.edit().putBoolean("wpCustom", true).apply();
                Toast.makeText(this, "Wallpaper set.", Toast.LENGTH_SHORT).show();
            }
            changed();
            if (onResumeHook != null) onResumeHook.run();
        } catch (Throwable t) {
            Toast.makeText(this, "Could not use that image.", Toast.LENGTH_LONG).show();
        }
    }

    private Bitmap decode(Uri u, int maxSide) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        InputStream in = getContentResolver().openInputStream(u);
        BitmapFactory.decodeStream(in, null, o);
        in.close();
        int s = 1;
        while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= maxSide) s *= 2;
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = s;
        in = getContentResolver().openInputStream(u);
        Bitmap b = BitmapFactory.decodeStream(in, null, o2);
        in.close();
        return b;
    }

    private void makeWallpaper(boolean toast) {
        DisplayMetrics dm = getResources().getDisplayMetrics();
        Bitmap b = GoWallpaper.make(this, dm.widthPixels, dm.heightPixels);
        GoWallpaper.save(this, b, GoWallpaper.FILE);
        p.edit().putBoolean("wpCustom", false).apply();
        if (toast) Toast.makeText(this, "Wallpaper made.", Toast.LENGTH_SHORT).show();
        changed();
    }

    // ------------------------------------------------------------------ apps

    private List<String[]> installed() {
        PackageManager pm = getPackageManager();
        Intent li = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> l = pm.queryIntentActivities(li, 0);
        List<String[]> out = new ArrayList<>();
        java.util.HashSet<String> seen = new java.util.HashSet<>();
        for (ResolveInfo ri : l) {
            String pk = ri.activityInfo.packageName;
            if (pk.equals(getPackageName()) || !seen.add(pk)) continue;
            out.add(new String[]{String.valueOf(ri.loadLabel(pm)), pk});
        }
        Collections.sort(out, new Comparator<String[]>() {
            @Override
            public int compare(String[] a, String[] b) {
                return a[0].compareToIgnoreCase(b[0]);
            }
        });
        return out;
    }

    private void chooseApp(final String role) {
        final List<String[]> all = installed();
        final List<String[]> shown = new ArrayList<>(all);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(px(16), px(8), px(16), px(8));
        final EditText q = new EditText(this);
        q.setHint("Search by name or package");
        q.setSingleLine(true);
        box.addView(q);
        final ListView lv = new ListView(this);
        final ArrayAdapter<String> ad = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, new ArrayList<String>());
        lv.setAdapter(ad);
        box.addView(lv, new LinearLayout.LayoutParams(-1, px(300)));
        final Runnable fill = () -> {
            String f = q.getText().toString().trim().toLowerCase();
            shown.clear();
            ad.clear();
            for (String[] a : all) {
                if (f.length() == 0 || a[0].toLowerCase().contains(f) || a[1].toLowerCase().contains(f)) {
                    shown.add(a);
                    ad.add(a[0] + "  —  " + a[1]);
                }
            }
            ad.notifyDataSetChanged();
        };
        q.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            @Override
            public void onTextChanged(CharSequence s, int a, int b, int c) {
                fill.run();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
        fill.run();
        final AlertDialog d = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK)
                .setTitle("Choose: " + GoPrefs.label(role)).setView(box)
                .setNegativeButton("Cancel", null)
                .setNeutralButton("Clear", (x, y) -> {
                    p.edit().putString("app_" + role, "").apply();
                    refreshAppLabel(role);
                    changed();
                }).create();
        lv.setOnItemClickListener((parent, view, pos, id) -> {
            p.edit().putString("app_" + role, shown.get(pos)[1]).apply();
            refreshAppLabel(role);
            changed();
            d.dismiss();
        });
        d.show();
    }

    /** Fill the empty picks by matching app names and package names. */
    private void autoPick() {
        List<String[]> apps = installed();
        SharedPreferences.Editor ed = p.edit();
        for (String role : GoPrefs.ROLES) {
            if (p.getString("app_" + role, "").length() > 0) continue;
            int best = 0;
            String bp = "";
            for (String[] a : apps) {
                int s = score(role, a[0].toLowerCase(), a[1].toLowerCase());
                if (s > best) {
                    best = s;
                    bp = a[1];
                }
            }
            if (bp.length() > 0) ed.putString("app_" + role, bp);
        }
        ed.apply();
    }

    private int score(String role, String name, String pk) {
        String s = name + " " + pk;
        switch (role) {
            case "maps":
                if (pk.equals("com.google.android.apps.maps")) return 10;
                return s.contains("maps") || s.contains("navigation") ? 3 : 0;
            case "radio":
                if (s.contains("internet radio") || s.contains("podcast")) return 0;
                return s.contains("radio") ? 5 : (pk.endsWith(".fm") || s.contains(" fm") ? 3 : 0);
            case "cam": {
                int v = 0;
                if (s.contains("360")) v += 5;
                if (s.contains("avm") || s.contains("around") || s.contains("panor") || s.contains("surround") || s.contains("bird")) v += 4;
                if (v == 0 && s.contains("camera") && !pk.startsWith("com.google") && !pk.equals("com.android.camera")) v = 1;
                return v;
            }
            default:
                if (pk.equals("com.brave.browser")) return 10;
                if (pk.equals("com.android.chrome")) return 4;
                return s.contains("browser") ? 2 : 0;
        }
    }

    // ------------------------------------------------------------------ permissions and Home

    private boolean locationOk() {
        return Build.VERSION.SDK_INT < 23 || checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private String permText() {
        return (overlayOk() ? "\u2713 Overlay" : "\u2717 Overlay") + "     "
                + (AppWatcher.granted(this) ? "\u2713 Usage access" : "\u2717 Usage access") + "     "
                + (locationOk() ? "\u2713 Location" : "\u2717 Location");
    }

    private void askLocation() {
        if (Build.VERSION.SDK_INT >= 23) requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 5);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        if (onResumeHook != null) onResumeHook.run();
        try {
            startService(new Intent(this, GoService.class));
        } catch (Exception ignored) {
        }
    }

    private void openOverlaySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open the overlay setting on this unit.", Toast.LENGTH_LONG).show();
        }
    }

    private void openUsageSettings() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "Could not open usage access on this unit.", Toast.LENGTH_LONG).show();
        }
    }

    private void openHomeChooser() {
        try {
            startActivity(new Intent(Settings.ACTION_HOME_SETTINGS));
            return;
        } catch (Exception ignored) {
        }
        try {
            startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            Toast.makeText(this, "Open Android settings > Apps > Default apps > Home app.", Toast.LENGTH_LONG).show();
        }
    }

    // ------------------------------------------------------------------ welcome (first run)

    private void showWelcome() {
        showWallpaperOnly();
        inSub = true;
        LinearLayout[] c = new LinearLayout[1];
        page(c, "Welcome to Rehome: Go", null);
        LinearLayout col = c[0];
        final int last = 5;
        switch (wStep) {
            case 0:
                note(col, "Rehome: Go starts Maps, the radio and 360 one after the other when the unit boots, and puts a slim frame, a vignette, your logo and a mini bar over them.");
                note(col, "This takes a minute. Everything can be changed later in Settings.");
                break;
            case 1: {
                section(col, "1. Three permissions");
                final TextView perm = tv("", 15, 0xFFFFC27A, false);
                col.addView(perm);
                action(col, "Allow draw over other apps", this::openOverlaySettings);
                action(col, "Allow usage access (to know which app is in front)", this::openUsageSettings);
                action(col, "Allow location (speed for the 360 and the speed light)", this::askLocation);
                onResumeHook = () -> perm.setText(permText());
                onResumeHook.run();
                break;
            }
            case 2:
                section(col, "2. Your logo");
                note(col, "Choose a picture to show at the top centre. A transparent PNG looks best. You can skip this.");
                action(col, "Choose logo image", () -> pickImage(REQ_LOGO));
                final TextView st = tv(GoPrefs.file(this, GoWallpaper.LOGO).exists() ? "✓ Logo chosen" : "No logo yet", 15, 0xFFDDE6FF, false);
                col.addView(st, wrap(10));
                onResumeHook = () -> st.setText(GoPrefs.file(this, GoWallpaper.LOGO).exists() ? "✓ Logo chosen" : "No logo yet");
                break;
            case 3:
                section(col, "3. Your apps");
                note(col, "Found by name. Change any that look wrong.");
                autoPick();
                for (String r : GoPrefs.ROLES) appRow(col, r);
                break;
            case 4:
                section(col, "4. Wallpaper");
                note(col, "The wallpaper is made for you with your logo and the frame painted in. Pick a glow colour, then make it.");
                glowSwatches(col);
                action(col, "Make the wallpaper", () -> {
                    makeWallpaper(true);
                    showWelcomeKeep();
                });
                action(col, "Use my own wallpaper image instead", () -> pickImage(REQ_WP));
                break;
            default:
                section(col, "5. Make Rehome: Go your Home app");
                note(col, "Android asks you to choose. Pick \"Rehome: Go\" and \"Always\".");
                action(col, "Choose the Home app", this::openHomeChooser);
        }
        LinearLayout nav = new LinearLayout(this);
        nav.setOrientation(LinearLayout.HORIZONTAL);
        if (wStep > 0) nav.addView(btn("Back", () -> {
            wStep--;
            showWelcome();
        }));
        View gap = new View(this);
        nav.addView(gap, new LinearLayout.LayoutParams(px(14), 1));
        nav.addView(btn(wStep == last ? "Finish" : "Next", () -> {
            if (wStep == last) {
                p.edit().putBoolean("welcomed", true).apply();
                if (!GoPrefs.file(this, GoWallpaper.FILE).exists()) makeWallpaper(false);
                GoService.refresh();
                onResumeHook = null;
                showFallback();
            } else {
                wStep++;
                showWelcome();
            }
        }));
        col.addView(nav, wrap(26));
    }

    private void showWelcomeKeep() {
        showWelcome();
    }
}
