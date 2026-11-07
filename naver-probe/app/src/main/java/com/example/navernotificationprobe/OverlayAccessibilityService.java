package com.example.navernotificationprobe;

import android.accessibilityservice.AccessibilityService;
import android.os.SystemClock;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityWindowInfo;
import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.List;

public final class OverlayAccessibilityService extends AccessibilityService {
    private static final String NAVER = "com.nhn.android.nmap";
    private long lastCapture = 0L;
    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
    private boolean listening = false;
    private final Runnable poll = new Runnable() {
        @Override public void run() {
            if (!listening) return;
            scan("POLL", null);
            handler.postDelayed(this, 1000L);
        }
    };

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        listening = true;
        OverlayStore.connection(this, true);
        handler.removeCallbacks(poll);
        handler.post(poll);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null ||
                !NAVER.contentEquals(event.getPackageName())) return;
        long time = SystemClock.elapsedRealtime();
        if (time - lastCapture < 700L) return;
        lastCapture = time;
        scan("EVENT " + event.getEventType(), event.getSource());
    }

    private void scan(String kind, AccessibilityNodeInfo eventRoot) {
        LinkedHashSet<String> found = new LinkedHashSet<>();
        int matched = 0;
        try {
            if (eventRoot != null && NAVER.contentEquals(eventRoot.getPackageName())) {
                collect(eventRoot, found);
            }
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    if (window == null) continue;
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root == null) continue;
                    CharSequence pkg = root.getPackageName();
                    if (pkg != null && NAVER.contentEquals(pkg)) {
                        matched++;
                        android.graphics.Rect bounds = new android.graphics.Rect();
                        window.getBoundsInScreen(bounds);
                        found.add("WINDOW: type=" + window.getType() + " bounds=" + bounds.toShortString());
                        collect(root, found);
                    }
                }
            }
        } catch (Exception e) {
            found.add("SCAN_ERROR: " + e.getClass().getSimpleName());
        }

        StringBuilder text = new StringBuilder();
        for (String item : found) {
            if (text.length() + item.length() > 4500) break;
            text.append(item).append('\n');
        }
        if (text.length() > 0) text.setLength(text.length() - 1);
        // Save only changed text snapshots, while still counting every accessibility poll.
        OverlayStore.record(this, kind, matched, text.toString());
    }

    private void collect(AccessibilityNodeInfo root, LinkedHashSet<String> output) {
        ArrayDeque<AccessibilityNodeInfo> queue = new ArrayDeque<>();
        queue.add(root);
        int visited = 0;
        while (!queue.isEmpty() && visited++ < 300 && output.size() < 70) {
            AccessibilityNodeInfo node = queue.removeFirst();
            try {
                CharSequence pkg = node.getPackageName();
                // A NULL package may occur on container nodes; do not record text from them.
                if (pkg != null && NAVER.contentEquals(pkg)) {
                    addText(output, "text", node.getText());
                    addText(output, "desc", node.getContentDescription());
                }
                for (int i = 0, size = node.getChildCount(); i < size; i++) {
                    AccessibilityNodeInfo child = node.getChild(i);
                    if (child != null) queue.addLast(child);
                }
            } catch (Exception ignored) {}
        }
    }

    private static void addText(LinkedHashSet<String> output, String label, CharSequence value) {
        if (value == null) return;
        String s = value.toString().trim().replace("\r", " ").replace("\n", " / ");
        if (s.isEmpty()) return;
        if (s.length() > 250) s = s.substring(0, 250);
        output.add(label + ": " + s);
    }

    @Override public void onInterrupt() {}
    @Override public void onDestroy() {
        listening = false;
        handler.removeCallbacks(poll);
        OverlayStore.connection(this, false);
        super.onDestroy();
    }
    @Override public boolean onUnbind(android.content.Intent intent) {
        listening = false;
        handler.removeCallbacks(poll);
        OverlayStore.connection(this, false);
        return super.onUnbind(intent);
    }
}
