package com.rehome.go;

import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;

/**
 * Finds out which app is in front using "Usage access" (no accessibility service).
 * Only the package name is read. It polls once a second, and only while a per-app rule is in use.
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
            return a.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), c.getPackageName())
                    == AppOpsManager.MODE_ALLOWED;
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

    private static boolean shellBackFailed = false;

    /** Back without accessibility: only works on units that allow the shell command; false = did nothing. */
    static boolean back() {
        if (shellBackFailed) return false;
        try {
            java.lang.Process p = Runtime.getRuntime().exec(new String[]{"input", "keyevent", "4"});
            boolean ok = p.waitFor() == 0;
            if (!ok) shellBackFailed = true;
            return ok;
        } catch (Throwable t) {
            shellBackFailed = true;
            return false;
        }
    }
}
