package com.rehome.go;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
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
import android.util.TypedValue;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Rehome: Boot Pack. Makes the null boot logo, null boot animation and the wallpaper (the start-up cover) from your emblem,
 * and holds the few settings that have no screen of their own: cover time, slow-speed 360 and the speed light.
 */
public class BootPackActivity extends Activity {
    private static final int REQ_EMBLEM = 81;
    private SharedPreferences p;
    private final Handler H = new Handler(Looper.getMainLooper());
    private ImageView preview;
    private TextView status;
    private float d;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        p = GoPrefs.get(this);
        NullPack.cfg = p;
        d = getResources().getDisplayMetrics().heightPixels / 720f;
        ScrollView sv = new ScrollView(this);
        sv.setBackgroundColor(0xFF05070A);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(px(40), px(30), px(40), px(40));
        sv.addView(col);
        setContentView(sv);

        col.addView(tv("Rehome: Boot Pack", 28, Color.WHITE, true));
        col.addView(tv("Makes a boot logo, a boot animation and the start-up cover wallpaper from your emblem. Without an emblem they are plain black.", 15, 0xFFB0B8C0, false));
        preview = new ImageView(this);
        preview.setAdjustViewBounds(true);
        LinearLayout.LayoutParams pl = new LinearLayout.LayoutParams(px(560), px(315));
        pl.topMargin = px(14);
        col.addView(preview, pl);
        col.addView(btn("Choose emblem (PNG)", this::pickEmblem), wrap(10));
        toggle(col, "Make a black background transparent", "cutBlack", true, this::regen);
        col.addView(btn("Remove the emblem", () -> {
            GoPrefs.file(this, "emblem.png").delete();
            regen();
        }), wrap(8));
        col.addView(tv("Glow colour behind the emblem", 16, Color.WHITE, false), wrap(14));
        LinearLayout sw = new LinearLayout(this);
        final int[] cols = {0xFFFF1E1E, 0xFFFF7A1E, 0xFFFFC21E, 0xFF3DDC84, 0xFF3D8BFF, 0xFFA98BFF, 0xFFFFFFFF};
        for (final int cc : cols) {
            View v = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(cc);
            g.setStroke(px(2), 0x88FFFFFF);
            v.setBackground(g);
            v.setOnClickListener(x -> {
                p.edit().putInt("glow", cc).apply();
                regen();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(44), px(44));
            lp.rightMargin = px(12);
            sw.addView(v, lp);
        }
        col.addView(sw, wrap(6));
        col.addView(btn("Make the boot pack", this::makePack), wrap(16));
        status = tv("", 14, 0xFF8FB8FF, false);
        col.addView(status, wrap(6));

        col.addView(btn("Set the wallpaper as this device's wallpaper", this::setWallpaper), wrap(8));
        section(col, "Optional permissions");
        col.addView(tv("Both are optional. Go works without them, with less.", 14, 0xFFB0B8C0, false));
        final TextView perm = tv("", 15, 0xFFFFC27A, false);
        col.addView(perm, wrap(6));
        col.addView(tv("Location gives Go your speed: it is used for the speed light and for showing the 360 when you creep along slowly. Without it those two do nothing.", 14, 0xFFB0B8C0, false), wrap(6));
        col.addView(btn("Allow location", () -> {
            if (Build.VERSION.SDK_INT >= 23) requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 6);
        }), wrap(6));
        col.addView(tv("Usage access lets Go see which app is in front, so the third button can switch between the radio and YouTube. Without it the third button is the radio on a tap and YouTube (Brave) on a hold. Only the app name is read, nothing leaves the device.", 14, 0xFFB0B8C0, false), wrap(10));
        col.addView(btn("Allow usage access", () -> {
            try {
                startActivity(new Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS));
            } catch (Exception e) {
                Toast.makeText(this, "Could not open usage access on this unit.", Toast.LENGTH_LONG).show();
            }
        }), wrap(6));
        permView = perm;
        refreshPerms();
        section(col, "Picture (boot pack and wallpaper)");
        stepper(col, "Colour strength", "picVig", 100, 0, 100, 5, "%");
        stepper(col, "Fade to dark (right and bottom)", "picDark", 100, 0, 100, 5, "%");
        stepper(col, "Emblem across (x)", "picX", 50, 0, 100, 1, "%");
        stepper(col, "Emblem down (y)", "picY", 41, 0, 100, 1, "%");
        stepper(col, "Emblem size (height)", "picSize", 60, 10, 100, 1, "%");
        section(col, "Testing");
        toggle(col, "Show the overlays even when Go is not the Home app", "overOther", true);
        section(col, "Start-up cover");
        stepper(col, "Cover stays for", "coverMs", 12000, 1000, 60000, 500, "ms");
        section(col, "Driving");
        toggle(col, "Show the 360 when driving slowly (test while parked first)", "auto360", true);
        stepper(col, "...faster than", "auto360Min", 5, 0, 60, 1, "km/h");
        stepper(col, "...and slower than", "auto360Max", 20, 2, 60, 1, "km/h");
        toggle(col, "Repeat only after going faster again (off = after the time gap)", "auto360ModeB", false);
        stepper(col, "Time gap between shows", "auto360Gap", 60, 10, 600, 5, "s");
        toggle(col, "Speed light along the top (over-speed warning)", "speedLight", true);
        stepper(col, "Amber light from", "warnAmber", 80, 20, 250, 5, "km/h");
        stepper(col, "Red light from", "warnRed", 120, 20, 250, 5, "km/h");
        regen();
    }

    // ------------------------------------------------------------------ emblem and files

    private TextView permView;

    private void refreshPerms() {
        if (permView == null) return;
        boolean loc = Build.VERSION.SDK_INT < 23 || checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED;
        permView.setText((loc ? "\u2713 Location" : "\u2717 Location") + "     " + (AppWatcher.granted(this) ? "\u2713 Usage access" : "\u2717 Usage access"));
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        refreshPerms();
        try {
            startService(new Intent(this, GoService.class));
        } catch (Exception ignored) {
        }
    }

    private void setWallpaper() {
        try {
            Bitmap b = NullPack.scene(emblem(), p.getInt("glow", 0xFFFF1E1E), false);
            android.app.WallpaperManager.getInstance(this).setBitmap(b);
            say("Wallpaper set.");
        } catch (Throwable t) {
            say("Could not set the wallpaper: " + t);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshPerms();
        GoService.suspend(true);    // Go's frame and buttons would sit on top of this screen
    }

    @Override
    protected void onPause() {
        GoService.suspend(false);
        super.onPause();
    }

    private Bitmap emblem() {
        try {
            File f = GoPrefs.file(this, "emblem.png");
            if (!f.exists()) return null;
            Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (b != null && p.getBoolean("cutBlack", true)) b = NullPack.removeBlack(b);   // a black background becomes transparent
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Rebuild the preview and the cover wallpaper. */
    private void regen() {
        try {
            Bitmap s = NullPack.scene(emblem(), p.getInt("glow", 0xFFFF1E1E), false);
            NullPack.png(s, GoPrefs.file(this, "wallpaper.png"));
            preview.setImageBitmap(s);
        } catch (Throwable t) {
            say("Could not update the wallpaper: " + t);
        }
    }

    private void say(String s) {
        status.setText(s);
    }

    private void pickEmblem() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT).setType("image/*");
        try {
            startActivityForResult(Intent.createChooser(i, "Choose your emblem"), REQ_EMBLEM);
        } catch (Exception e) {
            Toast.makeText(this, "No gallery or file app found.", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQ_EMBLEM || res != RESULT_OK || data == null || data.getData() == null) return;
        try {
            Bitmap b = decode(data.getData(), 1024);
            if (b == null) {
                say("Could not read that image.");
                return;
            }
            NullPack.png(b, GoPrefs.file(this, "emblem.png"));   // the original; the background cut is applied when it is used
            regen();
        } catch (Throwable t) {
            say("Could not use that image: " + t);
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
        return b == null ? null : b.copy(Bitmap.Config.ARGB_8888, true);
    }

    private void makePack() {
        say("Making the boot pack...");
        new Thread(() -> {
            String msg;
            try {
                File dir = new File(getCacheDir(), "bootpack");
                NullPack.write(dir, emblem(), p.getInt("glow", 0xFFFF1E1E));
                String where = copyToDownloads(dir);
                if (where.length() == 0) {   // Downloads was not possible: keep the files in the app's own folder instead
                    File own = new File(getExternalFilesDir(null), "bootpack");
                    NullPack.write(own, emblem(), p.getInt("glow", 0xFFFF1E1E));
                    where = own.getAbsolutePath();
                }
                msg = "Done. Saved in " + where + ": bootanimation.zip, logo.png, logo.bmp, bkg.png, keyboard.png, desc.txt.";
            } catch (Throwable e) {
                msg = "Could not make the boot pack: " + e;
            }
            final String m = msg;
            H.post(() -> say(m));
        }).start();
    }

    /** Best-effort copy of the files into Download/Rehome so a file manager finds them. */
    private String copyToDownloads(File dir) {
        String[] names = {"bootanimation.zip", "logo.bmp", "logo.png", "bkg.png", "keyboard.png", "desc.txt"};
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                for (String n : names) {
                    ContentValues cv = new ContentValues();
                    cv.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, n);
                    cv.put(android.provider.MediaStore.MediaColumns.MIME_TYPE, n.endsWith(".zip") ? "application/zip"
                            : n.endsWith(".png") ? "image/png" : n.endsWith(".bmp") ? "image/bmp" : "text/plain");
                    cv.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download/Rehome");
                    Uri u = getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (u == null) return "";
                    OutputStream os = getContentResolver().openOutputStream(u);
                    copy(new File(dir, n), os);
                }
                return "Download/Rehome";
            }
            File dd = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS), "Rehome");
            dd.mkdirs();
            for (String n : names) copy(new File(dir, n), new FileOutputStream(new File(dd, n)));
            return dd.getAbsolutePath();
        } catch (Exception e) {
            return "";
        }
    }

    private void copy(File f, OutputStream os) throws Exception {
        InputStream in = new java.io.FileInputStream(f);
        byte[] buf = new byte[1 << 16];
        int k;
        while ((k = in.read(buf)) > 0) os.write(buf, 0, k);
        in.close();
        os.close();
    }

    // ------------------------------------------------------------------ small UI helpers

    private int px(float v) {
        return Math.round(v * d * 1.0f);
    }

    private TextView tv(String s, float sz, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(TypedValue.COMPLEX_UNIT_PX, sz * d);
        if (bold) t.setTypeface(Typeface.DEFAULT_BOLD);
        return t;
    }

    private TextView btn(String s, final Runnable r) {
        TextView t = tv(s, 16, Color.WHITE, true);
        t.setGravity(Gravity.CENTER);
        t.setPadding(px(18), px(10), px(18), px(10));
        GradientDrawable g = new GradientDrawable();
        g.setColor(0xCC10161C);
        g.setCornerRadius(px(16));
        g.setStroke(Math.max(1, px(1)), 0x66FFFFFF);
        t.setBackground(g);
        t.setClickable(true);
        t.setOnClickListener(v -> r.run());
        return t;
    }

    private LinearLayout.LayoutParams wrap(int top) {
        LinearLayout.LayoutParams l = new LinearLayout.LayoutParams(-2, -2);
        l.topMargin = px(top);
        return l;
    }

    private void section(LinearLayout col, String t) {
        TextView s = tv(t, 20, 0xFF8FB8FF, true);
        s.setPadding(0, px(24), 0, px(4));
        col.addView(s);
    }

    private String fmt(int v, String unit) {
        if ("ms".equals(unit)) return String.format(java.util.Locale.US, "%.1f s", v / 1000.0);
        return v + " " + unit;
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
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, px(4), 0, px(4));
        r.addView(tv(label, 16, Color.WHITE, false), new LinearLayout.LayoutParams(0, -2, 1f));
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
            if (key.startsWith("pic")) regen();
        });
        repeat(plus, () -> {
            int v = Math.min(max, p.getInt(key, def) + step);
            p.edit().putInt(key, v).apply();
            val.setText(fmt(v, unit));
            if (key.startsWith("pic")) regen();
        });
        r.addView(minus, new LinearLayout.LayoutParams(px(52), -2));
        r.addView(val, new LinearLayout.LayoutParams(px(150), -2));
        r.addView(plus, new LinearLayout.LayoutParams(px(52), -2));
        col.addView(r);
    }

    private void toggle(LinearLayout col, String label, final String key, final boolean def) {
        toggle(col, label, key, def, null);
    }

    private void toggle(LinearLayout col, String label, final String key, final boolean def, final Runnable after) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(0, px(4), 0, px(4));
        r.addView(tv(label, 16, Color.WHITE, false), new LinearLayout.LayoutParams(0, -2, 1f));
        final TextView b = btn(p.getBoolean(key, def) ? "On" : "Off", () -> {
        });
        b.setOnClickListener(v -> {
            boolean nv = !p.getBoolean(key, def);
            p.edit().putBoolean(key, nv).apply();
            b.setText(nv ? "On" : "Off");
            if (after != null) after.run();
        });
        r.addView(b, new LinearLayout.LayoutParams(px(120), -2));
        col.addView(r);
    }
}
