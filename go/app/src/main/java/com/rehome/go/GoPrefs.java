package com.rehome.go;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;
import android.provider.Settings;

import java.io.File;
import java.util.List;

/** Settings, files and app lookup of Rehome: Go. Kept in device-protected storage so it can be read before the first unlock. */
final class GoPrefs {
    private GoPrefs() {
    }

    static final String[] ROLES = {"cam", "maps", "radio", "yt"};

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

    static boolean canOverlay(Context c) {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(c);
    }

    /** True when another launcher (for example Rehome: Scroll) is the chosen Home app, so Go must stay quiet. */
    static boolean otherHome(Context c) {
        try {
            Intent h = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
            ResolveInfo ri = c.getPackageManager().resolveActivity(h, PackageManager.MATCH_DEFAULT_ONLY);
            if (ri == null || ri.activityInfo == null) return false;
            if ("android".equals(ri.activityInfo.packageName)) return false;   // no default chosen yet
            return !c.getPackageName().equals(ri.activityInfo.packageName);
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------ once per boot

    private static int bootCount(Context c) {
        try {
            return Settings.Global.getInt(c.getContentResolver(), "boot_count", -1);
        } catch (Throwable t) {
            return -1;
        }
    }

    /** True once per boot: the start-up cover has not run since the unit last started. */
    static boolean freshBoot(Context c, SharedPreferences p) {
        long now = SystemClock.elapsedRealtime();
        long mark = p.getLong("seqMark", -1);
        int cnt = bootCount(c);
        return mark < 0 || now < mark || (cnt >= 0 && cnt != p.getInt("seqBoot", -2));
    }

    static void markBoot(Context c, SharedPreferences p) {
        p.edit().putLong("seqMark", SystemClock.elapsedRealtime()).putInt("seqBoot", bootCount(c)).apply();
    }

    // ------------------------------------------------------------ finding the apps by name and package

    private static int score(String role, String name, String pk) {
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

    /** The package for a role, found by name and package and remembered. Empty if nothing matches. */
    static String pkg(Context c, String role) {
        SharedPreferences p = get(c);
        PackageManager pm = c.getPackageManager();
        String cached = p.getString("app_" + role, "");
        if (cached.length() > 0 && pm.getLaunchIntentForPackage(cached) != null) return cached;
        String best = "";
        int bs = 0;
        try {
            Intent li = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> l = pm.queryIntentActivities(li, 0);
            for (ResolveInfo ri : l) {
                String pk = ri.activityInfo.packageName;
                if (pk.equals(c.getPackageName())) continue;
                int sc = score(role, String.valueOf(ri.loadLabel(pm)).toLowerCase(), pk.toLowerCase());
                if (sc > bs) {
                    bs = sc;
                    best = pk;
                }
            }
        } catch (Throwable ignored) {
        }
        if (best.length() > 0) p.edit().putString("app_" + role, best).apply();
        return best;
    }

    /** The third button: the radio when you are in the 360 or YouTube, YouTube when you are in Maps or the radio. */
    static String third(Context c, SharedPreferences p) {
        if (!AppWatcher.running) return "radio";   // no usage access: radio on a tap, YouTube (Brave) on a hold
        String cur = p.getString("cur", "maps");
        return "cam".equals(cur) || "yt".equals(cur) ? "radio" : "yt";
    }

    static String otherThird(Context c, SharedPreferences p) {
        return "radio".equals(third(c, p)) ? "yt" : "radio";
    }

    /** The package of the 360 player (car360-player): any app whose package starts with "bvack.". */
    static String playerPkg(Context c) {
        PackageManager pm = c.getPackageManager();
        try {
            pm.getPackageInfo("bvack.camera360", 0);
            return "bvack.camera360";
        } catch (Exception ignored) {
        }
        try {
            Intent li = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER);
            for (ResolveInfo ri : pm.queryIntentActivities(li, 0)) {
                if (ri.activityInfo.packageName.startsWith("bvack.")) return ri.activityInfo.packageName;
            }
        } catch (Throwable ignored) {
        }
        return "";
    }

    /** Opens the 360 player. */
    static boolean launchPlayer(Context c) {
        try {
            String pk = playerPkg(c);
            if (pk.length() == 0) return false;
            Intent i = c.getPackageManager().getLaunchIntentForPackage(pk);
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    static boolean launch(Context c, String role) {
        try {
            SharedPreferences p = get(c);
            String pk = pkg(c, role);
            if (pk.length() == 0) return false;
            Intent i = c.getPackageManager().getLaunchIntentForPackage(pk);
            if (i == null) return false;
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
            p.edit().putString("cur", role).apply();
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }
}
