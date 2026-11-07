package com.example.navernotificationprobe;

import android.app.Notification;
import android.os.Bundle;
import android.os.SystemClock;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

public final class NaverNotificationListener extends NotificationListenerService {
    private static final String PACKAGE = "com.nhn.android.nmap";
    private long previous = -1L;

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        ProbeStore.append(this, "\n=== LISTENER CONNECTED " + stamp() + " ===\n");
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification item : active) {
                    if (isNaver(item)) capture("ACTIVE", item);
                }
            }
        } catch (Exception error) {
            ProbeStore.append(this, "ACTIVE ERROR " + error.getClass().getSimpleName() + "\n");
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification item) {
        if (isNaver(item)) capture("POSTED", item);
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification item) {
        if (isNaver(item)) {
            ProbeStore.append(this, "\n--- REMOVED " + stamp() + " ---\n");
        }
    }

    private static boolean isNaver(StatusBarNotification item) {
        return item != null && PACKAGE.equals(item.getPackageName());
    }

    private synchronized void capture(String kind, StatusBarNotification item) {
        Notification n = item.getNotification();
        if (n == null) return;
        long now = SystemClock.elapsedRealtime();
        String delay = previous < 0 ? "first" : Long.toString(now - previous) + "ms";
        if ("POSTED".equals(kind)) previous = now;

        StringBuilder sb = new StringBuilder(1500);
        sb.append("\n--- ").append(kind).append(" ").append(stamp()).append(" ---\n");
        sb.append("since_previous_posted=").append(delay).append("\n");
        sb.append("pkg=").append(item.getPackageName()).append("\n");
        sb.append("id=").append(item.getId()).append(" category=").append(print(n.category)).append("\n");
        sb.append("ticker=").append(print(n.tickerText)).append("\n");
        sb.append("postTime=").append(item.getPostTime()).append("\n");

        Bundle b = n.extras;
        if (b == null) {
            sb.append("extras=<null>\n");
        } else {
            String[] known = {
                Notification.EXTRA_TITLE, Notification.EXTRA_TEXT,
                Notification.EXTRA_BIG_TEXT, Notification.EXTRA_SUB_TEXT,
                Notification.EXTRA_SUMMARY_TEXT, Notification.EXTRA_TITLE_BIG,
                Notification.EXTRA_TEXT_LINES, Notification.EXTRA_TEMPLATE
            };
            for (String k : known) {
                try { sb.append(k).append("=").append(print(b.get(k))).append("\n"); }
                catch (Exception e) { sb.append(k).append("=<error>\n"); }
            }

            try {
                String[] keys = b.keySet().toArray(new String[0]);
                Arrays.sort(keys);
                for (String k : keys) {
                    boolean already = false;
                    for (String standard : known) {
                        if (standard.equals(k)) { already = true; break; }
                    }
                    if (already) continue;
                    try {
                        Object v = b.get(k);
                        if (v instanceof CharSequence || v instanceof Number ||
                            v instanceof Boolean || v instanceof CharSequence[] ||
                            v instanceof String[]) {
                            sb.append("OTHER[").append(k).append("]=")
                              .append(print(v)).append("\n");
                        }
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
        }
        ProbeStore.append(this, sb.toString());
    }

    private static String print(Object v) {
        if (v == null) return "<null>";
        if (v instanceof CharSequence[]) return shorten(Arrays.toString((CharSequence[])v));
        if (v instanceof String[]) return shorten(Arrays.toString((String[])v));
        return shorten(v.toString());
    }

    private static String shorten(String s) {
        s = s.replace("\r", "\\r").replace("\n", "\\n");
        return s.length() > 500 ? s.substring(0, 500) + "…" : s;
    }

    private static String stamp() {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.KOREA).format(new Date());
    }
}
