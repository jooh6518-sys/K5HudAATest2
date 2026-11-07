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

    @Override protected void onServiceConnected() {
        super.onServiceConnected();
        OverlayStore.connection(this, true);
    }

    @Override public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event == null || event.getPackageName() == null ||
                !NAVER.contentEquals(event.getPackageName())) return;
        long time = SystemClock.elapsedRealtime();
        if (time - lastCapture < 700L) return;
        lastCapture = time;

        LinkedHashSet<String> found = new LinkedHashSet<>();
        int matchingWindows = 0;
        try {
            // Accessibility events are restricted to the NAVER MAP package in the XML.
            // Even when retrieving all interactive windows, ignore other packages.
            AccessibilityNodeInfo source = event.getSource();
            if (source != null) {
                collect(source, found);
            }
            List<AccessibilityWindowInfo> windows = getWindows();
            if (windows != null) {
                for (AccessibilityWindowInfo window : windows) {
                    if (window == null) continue;
                    AccessibilityNodeInfo root = window.getRoot();
                    if (root == null) continue;
                    CharSequence pkg = root.getPackageName();
                    if (pkg != null && NAVER.contentEquals(pkg)) {
                        matchingWindows++;
                        collect(root, found);
                    }
                }
            }
        } catch (Exception ignored) {
            found.add("(접근성 창 검사 실패)");
        }

        StringBuilder text = new StringBuilder();
        for (String line : found) {
            if (text.length() + line.length() > 4500) break;
            text.append(line).append('\n');
        }
        if (text.length() > 0) text.setLength(text.length() - 1);
        OverlayStore.record(this, Integer.toString(event.getEventType()), matchingWindows, text.toString());
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
    @Override public boolean onUnbind(android.content.Intent intent) {
        OverlayStore.connection(this, false);
        return super.onUnbind(intent);
    }
}
