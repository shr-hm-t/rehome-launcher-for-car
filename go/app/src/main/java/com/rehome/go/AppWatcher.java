package com.rehome.go;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import java.util.List;

/**
 * Finds out which app is in front using "Usage access" (optional). Only the package name is read, once a second,
 * and only to let the third button follow where you are. Without the permission nothing here runs.
 */
final class AppWatcher {
    static volatile String pkg = "";
    static volatile boolean running = false;
    private static Runnable listener;
    private static Context ctx;
    private static final Handler H = new Handler(Looper.getMainLooper());
    private static long last = 0;
    private static boolean first = true;

    private static final Runnable TICK = new Runnable() {
        @Override
        public void run() {
            if (ctx == null) return;
            poll();
            H.postDelayed(this, 1000);
        }
    };

    static boolean granted(Context c) {
        try {
            AppOpsManager a = (AppOpsManager) c.getSystemService(Context.APP_OPS_SERVICE);
            int m = a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), c.getPackageName());
            if (m == AppOpsManager.MODE_ALLOWED) return true;
            if (m == AppOpsManager.MODE_DEFAULT) {
                // some units grant it automatically and then report "default": trust the permission, or ask for real data
                if (c.checkCallingOrSelfPermission("android.permission.PACKAGE_USAGE_STATS") == PackageManager.PERMISSION_GRANTED) return true;
                UsageStatsManager u = (UsageStatsManager) c.getSystemService(Context.USAGE_STATS_SERVICE);
                long now = System.currentTimeMillis();
                List<UsageStats> l = u.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, now - 86400000L, now);
                return l != null && !l.isEmpty();
            }
            return false;
        } catch (Throwable t) {
            return false;
        }
    }

    static void start(Context c, Runnable onChange) {
        stop();
        ctx = c.getApplicationContext();
        listener = onChange;
        running = granted(ctx);
        if (!running) return;
        first = true;
        H.post(TICK);
    }

    static void stop() {
        H.removeCallbacks(TICK);
        ctx = null;
        listener = null;
        running = false;
        pkg = "";
    }

    private static void poll() {
        try {
            UsageStatsManager u = (UsageStatsManager) ctx.getSystemService(Context.USAGE_STATS_SERVICE);
            long now = System.currentTimeMillis();
            long from = first ? now - 10 * 60 * 1000L : Math.max(last - 1000, now - 15000);
            UsageEvents ev = u.queryEvents(from, now);
            UsageEvents.Event e = new UsageEvents.Event();
            String found = null;
            while (ev.hasNextEvent()) {
                ev.getNextEvent(e);
                if (e.getEventType() == UsageEvents.Event.MOVE_TO_FOREGROUND
                        && e.getPackageName() != null && !"com.android.systemui".equals(e.getPackageName())) {
                    found = e.getPackageName();
                }
            }
            last = now;
            first = false;
            if (found != null && !found.equals(pkg)) {
                pkg = found;
                if (listener != null) listener.run();
            }
        } catch (Throwable ignored) {
        }
    }
}
