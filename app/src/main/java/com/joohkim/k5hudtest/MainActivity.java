package com.joohkim.k5hudtest;

import android.app.Activity;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int pad = (int) (20 * getResources().getDisplayMetrics().density);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(pad, pad, pad, pad);

        TextView title = new TextView(this);
        title.setText("K5 HUD · Android Auto TBT Test");
        title.setTextSize(22);
        root.addView(title);

        TextView body = new TextView(this);
        body.setText(
                "이 앱은 Android Auto의 NavigationManager.updateTrip()을 이용해 " +
                "가짜 턴바이턴 정보를 차량 호스트로 전송합니다.\n\n" +
                "휴대폰 화면에서는 테스트를 실행할 수 없습니다. Android Auto에서 " +
                "'K5 HUD AA Test'를 열고 원하는 안내를 선택하세요.\n\n" +
                "테스트 항목: 우회전 300m / 좌회전 500m / 직진 1km / 유턴 200m / 안내 종료"
        );
        body.setTextSize(16);
        body.setPadding(0, pad, 0, 0);
        root.addView(body);

        setContentView(root);
    }
}
