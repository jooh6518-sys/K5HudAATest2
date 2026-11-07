package com.example.navernotificationprobe;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class MainActivity extends Activity {
    private Handler handler;
    private TextView state, turn, distance, detail, counter, times, events, overlayState, overlayInfo, overlayEvents;
    private boolean showing;
    private String oldSnapshot = "", oldLog = "";
    private final Runnable update = new Runnable() {
        @Override public void run() {
            if (!showing) return;
            refresh();
            handler.postDelayed(this, 500L);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        handler = new Handler(Looper.getMainLooper());
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(16), dp(16), dp(16));
        page.setBackgroundColor(Color.rgb(247, 248, 250));
        setContentView(page);

        TextView title = text("네이버지도 HUD 데이터 테스트 v1.2.1", 20, true);
        page.addView(title);
        TextView help = text("회전 방향·거리 변경 시에만 기록합니다.\n수신은 계속 집계하며 인터넷 전송은 하지 않습니다.", 12, false);
        help.setPadding(0, dp(5), 0, dp(10));
        page.addView(help);

        state = text("알림 권한 확인 중", 14, true);
        page.addView(state);
        Button access = button("알림 접근 권한 설정");
        access.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
        page.addView(access);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackgroundColor(Color.WHITE);
        card.setPadding(dp(14), dp(12), dp(14), dp(12));
        page.addView(card);
        card.addView(text("현재 알림 경로 안내", 12, false));
        turn = text("—", 24, true);
        card.addView(turn);
        distance = text("—", 32, true);
        card.addView(distance);
        detail = text("", 13, false);
        card.addView(detail);
        counter = text("알림 수신 0회 · 안내 변경 0회", 13, false);
        counter.setPadding(0, dp(8), 0, 0);
        card.addView(counter);
        times = text("마지막 알림 — / 마지막 변경 —", 12, false);
        card.addView(times);

        TextView overlayTitle = text("백그라운드 안내창 · 접근성 테스트", 16, true);
        overlayTitle.setPadding(0, dp(13), 0, dp(4));
        page.addView(overlayTitle);

        Button accessibility = button("접근성 권한 설정 (네이버지도 안내창 분석)");
        accessibility.setOnClickListener(v -> startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        page.addView(accessibility);

        overlayState = text("접근성 연결 전", 13, true);
        overlayInfo = text("네이버지도 작은 안내창을 띄우고 접근성 권한을 허용하세요.", 12, false);
        page.addView(overlayState);
        page.addView(overlayInfo);

        LinearLayout overlayActions = new LinearLayout(this);
        overlayActions.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(overlayActions);
        Button overlayCopy = button("안내창 기록 복사");
        Button overlayClear = button("안내창 기록 삭제");
        overlayActions.addView(overlayCopy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        overlayActions.addView(overlayClear, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        overlayCopy.setOnClickListener(v -> {
            String log = OverlayStore.log(this);
            if (log.isEmpty()) {
                Toast.makeText(this, "안내창 기록이 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText("Naver overlay probe", log));
                Toast.makeText(this, "안내창 기록 복사됨 · 경로 정보 확인 후 공유", Toast.LENGTH_LONG).show();
            }
        });
        overlayClear.setOnClickListener(v -> {
            OverlayStore.clear(this);
            refresh();
        });
        overlayEvents = text("접근성 텍스트 변경을 기록합니다.", 12, false);
        page.addView(overlayEvents);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(actions);
        Button copy = button("변경 기록 복사");
        Button clear = button("기록 초기화");
        actions.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(clear, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        copy.setOnClickListener(v -> {
        String log = ProbeStore.readLog(this);
            if (log.isEmpty()) {
                Toast.makeText(this, "기록이 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager manager = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (manager != null) {
                manager.setPrimaryClip(ClipData.newPlainText("Naver turn changes", log));
                Toast.makeText(this, "복사 완료 · 위치 정보 확인 후 공유하세요", Toast.LENGTH_LONG).show();
            }
        });
        clear.setOnClickListener(v -> {
            ProbeStore.clear(this);
            oldSnapshot = "";
            oldLog = "";
            refresh();
        });

        TextView guide = text("아래에는 방향·거리 값이 변경된 기록만 표시됩니다.\nAndroid Auto 연결 전·후 각각 시험해 주세요.", 12, false);
        guide.setPadding(0, dp(8), 0, dp(8));
        page.addView(guide);
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.WHITE);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        events = text("기록 대기 중", 12, false);
        events.setTypeface(Typeface.MONOSPACE);
        events.setTextIsSelectable(true);
        events.setPadding(dp(10), dp(10), dp(10), dp(10));
        scroll.addView(events);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView v = new TextView(this);
        v.setText(value);
        v.setTextSize(size);
        v.setTextColor(Color.rgb(29, 38, 45));
        if (bold) v.setTypeface(null, Typeface.BOLD);
        return v;
    }
    private Button button(String name) {
        Button b = new Button(this);
        b.setText(name);
        b.setTextSize(12);
        b.setAllCaps(false);
        return b;
    }
    @Override protected void onResume() {
        super.onResume();
        showing = true;
        handler.removeCallbacks(update);
        handler.post(update);
    }
    @Override protected void onPause() {
        showing = false;
        handler.removeCallbacks(update);
        super.onPause();
    }
    private void refresh() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        boolean enabled = manager != null && manager.isNotificationListenerAccessGranted(
            new ComponentName(this, NaverNotificationListener.class));
        state.setText(enabled ? "● 알림 접근 허용됨" : "○ 알림 접근 권한 필요");
        state.setTextColor(enabled ? Color.rgb(23, 115, 70) : Color.rgb(167, 64, 38));

        String[] overlay = OverlayStore.snapshot(this);
                boolean accessibilityEnabled = isAccessibilityEnabled();
                overlayState.setText((accessibilityEnabled ? "● 접근성 사용 중" : "○ 접근성 꺼짐") +
                    " · 이벤트 " + overlay[3] + "회 · 변경 " + overlay[4] + "회");
                overlayInfo.setText("마지막: " + overlay[1] + "\n" + overlay[2]);
                String accessLog = OverlayStore.log(this);
                String lastRecord = accessLog.length() > 300 ? accessLog.substring(accessLog.length() - 300) : accessLog;
                overlayEvents.setText(accessLog.isEmpty() ? "아직 접근성 텍스트 기록 없음" : lastRecord);


        String[] s = ProbeStore.snapshot(this);
        String key = String.join("\t", s);
        if (!key.equals(oldSnapshot)) {
            oldSnapshot = key;
            turn.setText(s[7] + " · " + s[0]);
            distance.setText(s[1]);
            detail.setText(s[2]);
            counter.setText("알림 수신 " + s[5] + "회  ·  안내 변경 " + s[6] + "회");
            times.setText("마지막 알림 " + s[3] + "\n마지막 변경 " + s[4]);
        }
        String log = ProbeStore.readLog(this);
        if (!log.equals(oldLog)) {
            oldLog = log;
            events.setText(log.isEmpty() ? "기록 대기 중" : log);
        }
    }

    private boolean isAccessibilityEnabled() {
        String raw = Settings.Secure.getString(getContentResolver(),
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (raw == null || raw.isEmpty()) return false;
        ComponentName expected = new ComponentName(this, OverlayAccessibilityService.class);
        for (String value : raw.split(":")) {
            ComponentName current = ComponentName.unflattenFromString(value);
            if (expected.equals(current)) return true;
        }
        return false;
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
