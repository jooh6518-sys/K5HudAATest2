package com.example.navernotificationprobe;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class ProbeStore {
    private static final String FILE = "probe_private";
    private static final String LOG = "change_log";
    private static final String COUNT = "received_count";
    private static final String CHANGED = "changed_count";
    private static final String DIRECTION = "direction";
    private static final String DISTANCE = "distance";
    private static final String DETAIL = "detail";
    private static final String LAST_SEEN = "last_seen";
    private static final String LAST_CHANGE = "last_change";
    private static final String ACTIVE = "active";
    private static final Object LOCK = new Object();

    private ProbeStore() {}

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE);
    }
    static String readLog(Context ctx) {
        synchronized (LOCK) { return prefs(ctx).getString(LOG, ""); }
    }
    static String[] snapshot(Context ctx) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(ctx);
            return new String[] {
                p.getString(DIRECTION, "—"), p.getString(DISTANCE, "—"),
                p.getString(DETAIL, ""), p.getString(LAST_SEEN, "—"),
                p.getString(LAST_CHANGE, "—"),
                String.valueOf(p.getInt(COUNT, 0)),
                String.valueOf(p.getInt(CHANGED, 0)),
                p.getBoolean(ACTIVE, false) ? "안내 수신 중" : "알림 대기 중"
            };
        }
    }
    static void record(Context ctx, String direction, String distance, String detail, boolean posted) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(ctx);
            String oldDirection = p.getString(DIRECTION, "");
            String oldDistance = p.getString(DISTANCE, "");
            boolean wasActive = p.getBoolean(ACTIVE, false);
            boolean changed = !wasActive || !oldDirection.equals(direction) || !oldDistance.equals(distance);
            String time = stamp();
            SharedPreferences.Editor e = p.edit()
                .putString(DIRECTION, direction).putString(DISTANCE, distance)
                .putString(DETAIL, detail).putString(LAST_SEEN, time).putBoolean(ACTIVE, true);
            if (posted) e.putInt(COUNT, p.getInt(COUNT, 0) + 1);
            if (changed) {
                e.putInt(CHANGED, p.getInt(CHANGED, 0) + 1).putString(LAST_CHANGE, time);
                String line = time + " | " + direction + " | " + distance
                    + (detail.isEmpty() ? "" : " | " + detail.replace("\n", " / ")) + "\n";
                e.putString(LOG, trim(p.getString(LOG, "") + line));
            }
            e.apply();
        }
    }
    static void removed(Context ctx) {
        synchronized (LOCK) {
            SharedPreferences p = prefs(ctx);
            if (p.getBoolean(ACTIVE, false)) {
                p.edit().putBoolean(ACTIVE, false)
                 .putString(LOG, trim(p.getString(LOG, "") + stamp() + " | 알림 종료\n")).apply();
            }
        }
    }
    static void clear(Context ctx) {
        synchronized (LOCK) { prefs(ctx).edit().clear().commit(); }
    }
    private static String trim(String text) {
        return text.length() > 40000 ? text.substring(text.length() - 26000) : text;
    }
    private static String stamp() {
        return new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.KOREA).format(new Date());
    }
}
