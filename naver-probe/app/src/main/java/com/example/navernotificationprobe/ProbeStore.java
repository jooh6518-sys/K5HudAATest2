package com.example.navernotificationprobe;

import android.content.Context;
import android.content.SharedPreferences;

final class ProbeStore {
    private static final String PREF = "probe_private";
    private static final String KEY = "events";
    private static final int MAX = 110000;
    private static final Object LOCK = new Object();
    private ProbeStore() {}

    static String read(Context ctx) {
        synchronized (LOCK) {
            return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                    .getString(KEY, "");
        }
    }

    static void append(Context ctx, String value) {
        synchronized (LOCK) {
            SharedPreferences p = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE);
            String previous = p.getString(KEY, "");
            String result = previous + value;
            if (result.length() > MAX) {
                int start = result.length() - 75000;
                int boundary = result.indexOf("\n--- ", start);
                if (boundary >= 0) start = boundary + 1;
                result = result.substring(start);
            }
            p.edit().putString(KEY, result).commit();
        }
    }

    static void clear(Context ctx) {
        synchronized (LOCK) {
            ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().remove(KEY).commit();
        }
    }
}
