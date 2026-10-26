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
import android.widget.FrameLayout;
import android.widget.ImageView;

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

    private void init(Intent i) {
        p = GoPrefs.get(this);
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
        GoPrefs.launch(this, "maps");
    }

    /** Only two things are ever asked: display over other apps, and location (for the speed). */
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
        if (Build.VERSION.SDK_INT >= 23 && !p.getBoolean("askedLoc", false)
                && checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            p.edit().putBoolean("askedLoc", true).apply();
            requestPermissions(new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION}, 5);
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
