package com.example.navernotificationprobe;

import android.app.Notification;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class NaverNotificationListener extends NotificationListenerService {
    private static final String NAVER = "com.nhn.android.nmap";
    private static final String PRIMARY = "android.ongoingActivityNoti.primaryInfo";
    private static final String SECONDARY = "android.ongoingActivityNoti.secondaryInfo";
    private static final String CHIP = "android.ongoingActivityNoti.chipExpandedText";
    private static final Pattern DISTANCE_PATTERN =
        Pattern.compile("([0-9][0-9,]*(?:\\.[0-9]+)?)\\s*(km|m)", Pattern.CASE_INSENSITIVE);

    @Override public void onListenerConnected() {
        super.onListenerConnected();
        try {
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification n : active) if (isNaver(n)) capture(n, false);
            }
        } catch (Exception ignored) {}
    }

    @Override public void onNotificationPosted(StatusBarNotification n) {
        if (isNaver(n)) capture(n, true);
    }

    @Override public void onNotificationRemoved(StatusBarNotification n) {
        if (isNaver(n) && hasNaviFields(n)) ProbeStore.removed(this);
    }

    private boolean isNaver(StatusBarNotification n) {
        return n != null && NAVER.equals(n.getPackageName());
    }
    private boolean hasNaviFields(StatusBarNotification n) {
        if (n == null || n.getNotification() == null) return false;
        Bundle b = n.getNotification().extras;
        return b != null && (b.containsKey(PRIMARY) || b.containsKey(SECONDARY));
    }
    private void capture(StatusBarNotification n, boolean posted) {
        if (n.getNotification() == null) return;
        Bundle b = n.getNotification().extras;
        if (b == null || (!b.containsKey(PRIMARY) && !b.containsKey(SECONDARY))) return;

        String rawDistance = value(b, PRIMARY);
        if (rawDistance.isEmpty()) rawDistance = value(b, CHIP);
        String secondary = value(b, SECONDARY);
        String[] lines = secondary.split("\\r?\\n", 2);
        String direction = lines.length > 0 ? lines[0].trim() : "";
        String detail = lines.length > 1 ? lines[1].trim() : "";
        if (direction.isEmpty() && rawDistance.isEmpty()) return;

        String distance = normalizeDistance(rawDistance);
        if (distance.isEmpty()) distance = rawDistance.isEmpty() ? "—" : rawDistance;
        if (direction.isEmpty()) direction = "알 수 없음";
        ProbeStore.record(this, direction, distance, detail, posted);
    }
    private static String value(Bundle b, String key) {
        try {
            Object v = b.get(key);
            return v == null ? "" : v.toString().trim();
        } catch (Exception e) { return ""; }
    }
    private static String normalizeDistance(String text) {
        Matcher m = DISTANCE_PATTERN.matcher(text);
        if (!m.find()) return text;
        try {
            double number = Double.parseDouble(m.group(1).replace(",", ""));
            double meters = "km".equalsIgnoreCase(m.group(2)) ? number * 1000d : number;
            int rounded = (int) Math.round(meters);
            return String.format(Locale.US, "%dm", rounded);
        } catch (Exception ex) { return text; }
    }
}
