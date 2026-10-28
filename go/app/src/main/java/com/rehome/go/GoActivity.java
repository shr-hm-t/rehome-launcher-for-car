package com.rehome.go;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.View;
import android.view.Gravity;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Rehome: Go as the Home app. Pressing Home (or opening Rehome: Go from the app list) opens Maps. At boot it does not
 * open anything: the cover from GoService covers the unit's start-up for a few seconds. All screens are overlays (GoService).
 */
public class GoActivity extends Activity {
    private SharedPreferences p;
    private FrameLayout root;
    private boolean locked = false;
    private BroadcastReceiver unlockRx;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);
        p = GoPrefs.get(this);
        if (Build.VERSION.SDK_INT >= 24) {
            android.os.UserManager um = (android.os.UserManager) getSystemService(Context.USER_SERVICE);
            if (um != null && !um.isUserUnlocked()) {
                // storage still locked after a cold start: show the cover picture and carry on once unlocked
                locked = true;
                showCoverPicture();
                unlockRx = new BroadcastReceiver() {
                    @Override
                    public void onReceive(Context c, Intent i) {
                        locked = false;
                        p = GoPrefs.get(GoActivity.this);
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
        // Home screen: nothing to go back to
    }

    private void showCoverPicture() {
        try {
            java.io.File f = GoPrefs.file(this, "wallpaper.png");
            if (!f.exists()) return;
            Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath());
            if (b == null) return;
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(b);
            root.removeAllViews();
            root.addView(iv, new FrameLayout.LayoutParams(-1, -1));
        } catch (Throwable ignored) {
        }
    }

    private TextView welcomeStatus;

    /** First start: explain, ask for "display over other apps", and keep everything off until Start is pressed. */
    private void showWelcome() {
        root.removeAllViews();
        root.setBackgroundColor(Color.BLACK);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int pad = Math.round(getResources().getDisplayMetrics().density * 24);
        col.setPadding(pad, pad, pad, pad);
        col.addView(welcomeText("Rehome: Go", 30, Color.WHITE));
        col.addView(welcomeText("Pressing Home opens Google Maps. A small bar of buttons (360, Maps, radio or YouTube, settings) and a black corner frame sit on top of other apps.", 17, 0xFFCCCCCC));
        col.addView(welcomeText("Step 1: allow \"display over other apps\". Without it Go cannot show anything.", 17, 0xFFCCCCCC));
        Button allow = new Button(this);
        allow.setText("Allow display over other apps");
        allow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    Toast.makeText(GoActivity.this, "Open Android settings, Apps, Rehome: Go, and allow display over other apps.", Toast.LENGTH_LONG).show();
                }
            }
        });
        col.addView(allow);
        welcomeStatus = welcomeText("", 16, 0xFFFFC857);
        col.addView(welcomeStatus);
        col.addView(welcomeText("Step 2: press Start. Location and usage access are optional and can be set later in settings (the gear on the bar).", 17, 0xFFCCCCCC));
        Button start = new Button(this);
        start.setText("Start");
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!GoPrefs.canOverlay(GoActivity.this)) {
                    Toast.makeText(GoActivity.this, "Allow display over other apps first.", Toast.LENGTH_LONG).show();
                    return;
                }
                p.edit().putBoolean("welcomed", true).apply();
                init(getIntent());
            }
        });
        col.addView(start);
        ScrollView sv = new ScrollView(this);
        sv.addView(col);
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        refreshWelcome();
    }

    private TextView welcomeText(String t, int sp, int color) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextSize(sp);
        v.setTextColor(color);
        v.setGravity(Gravity.CENTER);
        v.setPadding(0, 12, 0, 12);
        return v;
    }

    private void refreshWelcome() {
        if (welcomeStatus == null) return;
        welcomeStatus.setText(GoPrefs.canOverlay(this) ? "Allowed. You can press Start." : "Not allowed yet.");
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (p != null && !p.getBoolean("welcomed", false)) refreshWelcome();
    }

    private void init(Intent i) {
        p = GoPrefs.get(this);
        if (!p.getBoolean("welcomed", false) && GoPrefs.canOverlay(this)) {
            p.edit().putBoolean("welcomed", true).apply();   // already set up (an update): no welcome screen
        }
        if (!p.getBoolean("welcomed", false)) {
            showWelcome();   // nothing starts (no service, no Maps) until Start is pressed
            return;
        }
        askPermissionsOnce();
        boolean homeIntent = i != null && i.hasCategory(Intent.CATEGORY_HOME);
        boolean boot = homeIntent && SystemClock.elapsedRealtime() < 300000L && GoPrefs.freshBoot(this, p);
        boolean quiet = GoPrefs.otherHome(this) && homeIntent;
        if (!quiet) {
            Intent s = new Intent(this, GoService.class);
            if (boot) s.putExtra("cover", true);
            try {
                startService(s);
            } catch (Exception ignored) {
            }
        }
        if (boot) {
            GoPrefs.markBoot(this, p);
            showCoverPicture();   // the overlay cover goes on top; nothing is opened
            return;
        }
        root.removeAllViews();
        root.setBackgroundColor(Color.BLACK);
        if (!GoPrefs.launch(this, "maps")) {
            android.widget.Toast.makeText(this, "Google Maps was not found on this unit.", android.widget.Toast.LENGTH_LONG).show();
        }
    }

    /** Only "display over other apps" is asked here. Location and usage access are optional, in Rehome: Boot Pack. */
    private void askPermissionsOnce() {
        if (!GoPrefs.canOverlay(this)) {
            if (!p.getBoolean("askedOverlay", false)) {
                p.edit().putBoolean("askedOverlay", true).apply();
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
                } catch (Exception ignored) {
                }
            }
        } else if (p.getBoolean("askedOverlay", false)) {
            p.edit().putBoolean("askedOverlay", false).apply();
        }
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        super.onRequestPermissionsResult(code, perms, res);
        try {
            startService(new Intent(this, GoService.class));
        } catch (Exception ignored) {
        }
    }
}
