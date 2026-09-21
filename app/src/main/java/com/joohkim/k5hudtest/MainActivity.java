package com.joohkim.k5hudtest;

import android.app.Activity;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.car.app.navigation.model.Maneuver;

public class MainActivity extends Activity {
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
        title.setText("K5 HUD · Android Auto TBT Test");
        title.setTextSize(22);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText(
                "차량을 정차한 상태에서 Android Auto를 연결한 뒤 아래 버튼을 누르세요.\n" +
                "Android Auto의 NavigationManager.updateTrip()으로 테스트 길안내를 보냅니다."
        );
        body.setTextSize(15);
        body.setPadding(0, gap, 0, gap);
        root.addView(body);

        status = new TextView(this);
        status.setText("상태: 대기 중");
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
            status.setText(delivered
                    ? "상태: 안내 종료 명령 전달됨"
                    : "상태: 종료 명령 대기 중 · Android Auto에서 앱을 한 번 열어주세요");
        });

        setContentView(scroll);
    }

    private void send(int maneuverType, int meters, String road, String label) {
        boolean delivered = HudCommandBus.dispatch(
                HudCommandBus.Command.trip(maneuverType, meters, road, label));
        status.setText(delivered
                ? "상태: " + label + " → Android Auto로 전달됨"
                : "상태: " + label + " 대기 중 · Android Auto에서 앱을 한 번 열어주세요");
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
