package com.joohkim.k5hudtest;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.car.app.navigation.model.Maneuver;

public class MainActivity extends Activity implements HudCommandBus.ResultListener {
    private TextView status;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        float d = getResources().getDisplayMetrics().density;
        int pad = (int) (20 * d);
        int gap = (int) (10 * d);

        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);
        scroll.addView(root);

        TextView title = new TextView(this);
        title.setText("K5 HUD · Android Auto TBT Test v0.3");
        title.setTextSize(22);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText(
                "Android Auto에서 'K5 HUD AA Test'를 먼저 연 뒤 휴대폰의 버튼을 누르세요.\n" +
                "이번 버전은 NavigationManager.navigationStarted()와 updateTrip()의 실제 결과를 휴대폰에 표시합니다."
        );
        body.setTextSize(15);
        body.setPadding(0, gap, 0, gap);
        root.addView(body);

        status = new TextView(this);
        status.setText("상태: " + HudCommandBus.getLastResult());
        status.setTextSize(16);
        status.setPadding(0, gap, 0, gap);
        root.addView(status);

        addButton(root, "↱ 우회전 · 300 m",
                () -> send(Maneuver.TYPE_TURN_NORMAL_RIGHT, 300, "테스트 우회전 도로", "우회전 300m"));
        addButton(root, "↰ 좌회전 · 500 m",
                () -> send(Maneuver.TYPE_TURN_NORMAL_LEFT, 500, "테스트 좌회전 도로", "좌회전 500m"));
        addButton(root, "↑ 직진 · 1 km",
                () -> send(Maneuver.TYPE_STRAIGHT, 1000, "테스트 직진 도로", "직진 1km"));
        addButton(root, "↶ 유턴 · 200 m",
                () -> send(Maneuver.TYPE_U_TURN_LEFT, 200, "테스트 유턴 도로", "유턴 200m"));
        addButton(root, "■ 안내 종료", () -> {
            boolean delivered = HudCommandBus.dispatch(HudCommandBus.Command.end());
            if (!delivered) {
                status.setText("상태: AA 세션 미연결 · Android Auto에서 K5 HUD AA Test를 먼저 열어주세요");
            } else {
                status.setText("상태: 종료 명령 전달 중...");
            }
        });

        setContentView(scroll);
    }

    @Override
    protected void onStart() {
        super.onStart();
        HudCommandBus.setResultListener(this);
        status.setText("상태: " + HudCommandBus.getLastResult());
    }

    @Override
    protected void onStop() {
        HudCommandBus.clearResultListener(this);
        super.onStop();
    }

    @Override
    public void onResult(String result) {
        runOnUiThread(() -> status.setText("상태: " + result));
    }

    private void send(int maneuverType, int meters, String road, String label) {
        boolean delivered = HudCommandBus.dispatch(
                HudCommandBus.Command.trip(maneuverType, meters, road, label));
        status.setText(delivered
                ? "상태: " + label + " · AA 호출 실행 중..."
                : "상태: AA 세션 미연결 · Android Auto에서 K5 HUD AA Test를 먼저 열어주세요");
    }

    private void addButton(LinearLayout root, String text, Runnable action) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(18);
        b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = (int) (8 * getResources().getDisplayMetrics().density);
        root.addView(b, lp);
    }
}
