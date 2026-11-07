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
    private TextView state, events;
    private boolean showing;
    private String old = null;

    private final Runnable update = new Runnable() {
        @Override public void run() {
            if (!showing) return;
            refresh();
            handler.postDelayed(this, 1000L);
        }
    };

    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        handler = new Handler(Looper.getMainLooper());
        LinearLayout page = new LinearLayout(this);
        page.setOrientation(LinearLayout.VERTICAL);
        page.setPadding(dp(16), dp(20), dp(16), dp(16));
        page.setBackgroundColor(Color.rgb(247, 248, 249));
        setContentView(page);

        TextView title = new TextView(this);
        title.setText("네이버지도 알림 필드 테스트");
        title.setTextColor(Color.rgb(29, 38, 45));
        title.setTextSize(21);
        title.setTypeface(null, Typeface.BOLD);
        page.addView(title);

        TextView help = new TextView(this);
        help.setText("네이버지도 길안내 알림만 기록합니다.\n알림 필드의 거리·회전 방향·갱신 간격을 확인합니다.\n인터넷 권한이 없고 로그는 앱 내부에만 저장됩니다.");
        help.setTextSize(13);
        help.setTextColor(Color.DKGRAY);
        help.setPadding(0, dp(8), 0, dp(12));
        page.addView(help);

        state = new TextView(this);
        state.setTextSize(15);
        state.setTypeface(null, Typeface.BOLD);
        state.setPadding(0, 0, 0, dp(8));
        page.addView(state);

        Button access = button("1. 알림 접근 권한 설정");
        access.setOnClickListener(v -> {
            try { startActivity(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)); }
            catch (Exception e) { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        });
        page.addView(access);

        LinearLayout actions = new LinearLayout(this);
        actions.setOrientation(LinearLayout.HORIZONTAL);
        page.addView(actions);
        Button copy = button("로그 복사");
        Button clear = button("기록 비우기");
        actions.addView(copy, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        actions.addView(clear, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        copy.setOnClickListener(v -> {
            String content = ProbeStore.read(this);
            if (content.isEmpty()) {
                Toast.makeText(this, "기록이 없습니다", Toast.LENGTH_SHORT).show();
                return;
            }
            ClipboardManager mgr = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            if (mgr != null) {
                mgr.setPrimaryClip(ClipData.newPlainText("Naver notification fields", content));
                Toast.makeText(this, "복사 완료 · 위치 정보 확인 후 공유하세요", Toast.LENGTH_LONG).show();
            }
        });
        clear.setOnClickListener(v -> {
            ProbeStore.clear(this);
            old = null;
            refresh();
        });

        TextView guide = new TextView(this);
        guide.setText("POSTED: 알림 갱신, ACTIVE: 시작 시 기존 알림\nsince_previous_posted: 직전 POSTED와의 시간 차이");
        guide.setTextSize(12);
        guide.setTextColor(Color.DKGRAY);
        guide.setPadding(0, dp(8), 0, dp(8));
        page.addView(guide);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Color.WHITE);
        page.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        events = new TextView(this);
        events.setTextSize(12);
        events.setTextColor(Color.BLACK);
        events.setTypeface(Typeface.MONOSPACE);
        events.setTextIsSelectable(true);
        events.setPadding(dp(9), dp(9), dp(9), dp(9));
        scroll.addView(events);
    }

    private Button button(String name) {
        Button b = new Button(this);
        b.setText(name);
        b.setTextSize(13);
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
        NotificationManager mgr = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        boolean enabled = mgr != null && mgr.isNotificationListenerAccessGranted(
            new ComponentName(this, NaverNotificationListener.class));
        state.setText(enabled ? "● 알림 접근 허용됨" : "○ 알림 접근 권한 필요");
        state.setTextColor(enabled ? Color.rgb(22, 116, 68) : Color.rgb(170, 64, 35));

        String text = ProbeStore.read(this);
        if (text.equals(old)) return;
        old = text;
        events.setText(text.isEmpty() ? "아직 기록이 없습니다. 네이버지도 길안내를 실행해 보세요." : text);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }
}
