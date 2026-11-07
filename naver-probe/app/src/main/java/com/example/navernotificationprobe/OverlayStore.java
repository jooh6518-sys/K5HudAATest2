package com.example.navernotificationprobe;

import android.content.Context;
import android.content.SharedPreferences;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

final class OverlayStore {
    private static final String FILE = "overlay_probe_private";
    private static final Object LOCK = new Object();
    private OverlayStore() {}

    static String[] snapshot(Context c) {
        synchronized (LOCK) {
            SharedPreferences p = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
            return new String[]{
                p.getString("state", "연결 전"),
                p.getString("last", "—"),
                p.getString("info", "네이버지도 백그라운드 안내창을 띄워주세요."),
                String.valueOf(p.getInt("events", 0)),
                String.valueOf(p.getInt("changes", 0))
            };
        }
    }

    static String log(Context c) {
        synchronized (LOCK) {
            return c.getSharedPreferences(FILE, Context.MODE_PRIVATE).getString("log", "");
        }
    }

    static void connection(Context c, boolean connected) {
        synchronized (LOCK) {
            c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putString("state", connected ? "접근성 서비스 연결됨" : "접근성 서비스 중지됨").apply();
        }
    }

    static void record(Context c, String eventName, int matchedWindows, String data) {
        synchronized (LOCK) {
            SharedPreferences p = c.getSharedPreferences(FILE, Context.MODE_PRIVATE);
            String now = new SimpleDateFormat("MM-dd HH:mm:ss.SSS", Locale.KOREA).format(new Date());
            String info = data.isEmpty() ?
                "네이버지도 접근성 이벤트 수신 / 텍스트 노드 없음 (매칭 창: " + matchedWindows + ")" :
                "접근성 텍스트 노드 " + data.split("\n").length + "개 / 매칭 창 " + matchedWindows + "개";
            String fingerprint = data;
            boolean changed = !fingerprint.equals(p.getString("fingerprint", "\u0000"));
            SharedPreferences.Editor e = p.edit()
                .putString("last", now).putString("info", info)
                .putString("state", "접근성 서비스 연결됨")
                .putInt("events", p.getInt("events", 0) + 1);
            if (changed) {
                e.putString("fingerprint", fingerprint).putInt("changes", p.getInt("changes", 0) + 1);
                String entry = now + "  [" + eventName + "] windows=" + matchedWindows +
                    (data.isEmpty() ? "\n(접근성에 노출된 네이버지도 텍스트 없음)" : "\n" + data) + "\n---\n";
                String log = p.getString("log", "") + entry;
                if (log.length() > 40000) {
                    int start = log.length() - 27000;
                    int boundary = log.indexOf("\n---\n", start);
                    if (boundary >= 0) start = boundary + 5;
                    log = log.substring(start);
                }
                e.putString("log", log);
            }
            e.apply();
        }
    }
    static void clear(Context c) {
        synchronized (LOCK) {
            c.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .remove("log").remove("fingerprint").remove("last").remove("info")
                .remove("events").remove("changes").commit();
        }
    }
}
