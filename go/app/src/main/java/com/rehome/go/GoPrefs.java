package com.rehome.go;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ResolveInfo;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

import java.io.File;

/** Settings and files of Rehome: Go. Kept in device-protected storage so they can be read before the first unlock. */
final class GoPrefs {
    private GoPrefs() {
    }

    static final String[] ROLES = {"maps", "radio", "cam", "yt"};

    static Context dc(Context c) {
        Context a = c.getApplicationContext() != null ? c.getApplicationContext() : c;
        return Build.VERSION.SDK_INT >= 24 ? a.createDeviceProtectedStorageContext() : a;
    }

    static SharedPreferences get(Context c) {
        return dc(c).getSharedPreferences("go", Context.MODE_PRIVATE);
    }

    static File file(Context c, String name) {
        return new File(dc(c).getFilesDir(), name);
    }

    /** dp to pixels, including the "UI size" setting of Go. */
    static float scale(Context c, SharedPreferences p) {
        float ui = Math.max(60, Math.min(160, p.getInt("ui", 100))) / 100f;
        return c.getResources().getDisplayMetrics().density * ui;
    }

    static String label(String role) {
        switch (role) {
            case "maps": return "Maps";
            case "radio": return "Radio";
            case "cam": return "360";
            default: return "YouTube (browser)";
        }
    }

    /** True when another launcher (for example Rehome: Scroll) is the chosen Home app, so Go must stay quiet. */
    static boolean otherHome(Context c) {
        try {
            Intent h = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri = c.getPackageManager().resolveActivity(h, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
            if (ri == null || ri.activityInfo == null) return false;
            if ("android".equals(ri.activityInfo.packageName)) return false;   // no default chosen yet
            return !c.getPackageName().equals(ri.activityInfo.packageName);
        } catch (Throwable t) {
            return false;
        }
    }

    private static int bootCount(Context c) {
        try {
            return Settings.Global.getInt(c.getContentResolver(), "boot_count", -1);
        } catch (Throwable t) {
            return -1;
        }
    }

    /** True once per boot: the start-up sequence has not run since the unit last started. */
    static boolean freshBoot(Context c, SharedPreferences p) {
        long now = SystemClock.elapsedRealtime();
        long mark = p.getLong("seqMark", -1);
        int cnt = bootCount(c);
        return mark < 0 || now < mark || (cnt >= 0 && cnt != p.getInt("seqBoot", -2));
    }

    static void markBoot(Context c, SharedPreferences p) {
        p.edit().putLong("seqMark", SystemClock.elapsedRealtime()).putInt("seqBoot", bootCount(c)).apply();
    }
}
