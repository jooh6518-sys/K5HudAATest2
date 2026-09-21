package com.joohkim.k5hudtest;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.car.app.CarContext;
import androidx.car.app.Screen;
import androidx.car.app.model.DateTimeWithZone;
import androidx.car.app.model.Distance;
import androidx.car.app.model.ItemList;
import androidx.car.app.model.ListTemplate;
import androidx.car.app.model.Row;
import androidx.car.app.model.Template;
import androidx.car.app.navigation.NavigationManager;
import androidx.car.app.navigation.NavigationManagerCallback;
import androidx.car.app.navigation.model.Destination;
import androidx.car.app.navigation.model.Maneuver;
import androidx.car.app.navigation.model.Step;
import androidx.car.app.navigation.model.TravelEstimate;
import androidx.car.app.navigation.model.Trip;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import java.util.TimeZone;

public class HudTestScreen extends Screen implements HudCommandBus.Listener {
    private final NavigationManager navigationManager;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private String status = "대기 중";
    private boolean navigating = false;

    HudTestScreen(@NonNull CarContext carContext) {
        super(carContext);

        navigationManager = carContext.getCarService(NavigationManager.class);
        navigationManager.setNavigationManagerCallback(new NavigationManagerCallback() {
            @Override
            public void onStopNavigation() {
                navigating = false;
                status = "호스트가 안내 중지 요청";
                HudCommandBus.report("호스트가 onStopNavigation() 호출 → 안내 중지");
                invalidate();
            }
        });

        getLifecycle().addObserver(new DefaultLifecycleObserver() {
            @Override
            public void onStart(@NonNull LifecycleOwner owner) {
                HudCommandBus.register(HudTestScreen.this);
            }

            @Override
            public void onStop(@NonNull LifecycleOwner owner) {
                HudCommandBus.unregister(HudTestScreen.this);
            }
        });
    }

    @Override
    public void onCommand(HudCommandBus.Command command) {
        mainHandler.post(() -> {
            if (command.end) {
                endNavigation();
            } else {
                sendTrip(command.maneuverType, command.meters, command.road, command.label);
            }
        });
    }

    @NonNull
    @Override
    public Template onGetTemplate() {
        ItemList list = new ItemList.Builder()
                .addItem(testRow("↱ 우회전 · 300 m", Maneuver.TYPE_TURN_NORMAL_RIGHT, 300, "테스트 우회전 도로"))
                .addItem(testRow("↰ 좌회전 · 500 m", Maneuver.TYPE_TURN_NORMAL_LEFT, 500, "테스트 좌회전 도로"))
                .addItem(testRow("↑ 직진 · 1 km", Maneuver.TYPE_STRAIGHT, 1000, "테스트 직진 도로"))
                .addItem(testRow("↶ 유턴 · 200 m", Maneuver.TYPE_U_TURN_LEFT, 200, "테스트 유턴 도로"))
                .addItem(new Row.Builder()
                        .setTitle("■ 안내 종료")
                        .addText("NavigationManager.navigationEnded()")
                        .setOnClickListener(this::endNavigation)
                        .build())
                .build();

        return new ListTemplate.Builder()
                .setTitle("K5 HUD TEST · " + status)
                .setSingleList(list)
                .build();
    }

    private Row testRow(String title, int maneuverType, int meters, String road) {
        return new Row.Builder()
                .setTitle(title)
                .addText("navigationStarted + updateTrip 진단")
                .setOnClickListener(() -> sendTrip(maneuverType, meters, road, title))
                .build();
    }

    private Trip buildTrip(int maneuverType, int meters, String road) {
        Maneuver maneuver = new Maneuver.Builder(maneuverType).build();

        Step step = new Step.Builder(meters + "m 앞 안내")
                .setRoad(road)
                .setManeuver(maneuver)
                .build();

        long now = System.currentTimeMillis();
        long secondsToStep = Math.max(15, meters / 10);
        TravelEstimate stepEstimate = new TravelEstimate.Builder(
                Distance.create(meters, Distance.UNIT_METERS),
                DateTimeWithZone.create(now + secondsToStep * 1000L, TimeZone.getDefault()))
                .build();

        Destination destination = new Destination.Builder()
                .setName("HUD 테스트 목적지")
                .setAddress("Android Auto 테스트")
                .build();

        double destinationMeters = Math.max(5000, meters + 3000);
        TravelEstimate destinationEstimate = new TravelEstimate.Builder(
                Distance.create(destinationMeters, Distance.UNIT_METERS),
                DateTimeWithZone.create(now + 10 * 60 * 1000L, TimeZone.getDefault()))
                .build();

        return new Trip.Builder()
                .setCurrentRoad("K5 HUD 테스트 중")
                .addStep(step, stepEstimate)
                .addDestination(destination, destinationEstimate)
                .build();
    }

    private void sendTrip(int maneuverType, int meters, String road, String label) {
        try {
            if (!navigating) {
                navigationManager.navigationStarted();
                navigating = true;
                HudCommandBus.report("navigationStarted() 성공 · updateTrip() 호출 중...");
            }

            Trip trip = buildTrip(maneuverType, meters, road);
            navigationManager.updateTrip(trip);

            status = label + " 전송됨";
            HudCommandBus.report("updateTrip() 성공 · " + label + " · HUD/계기판 확인");
            invalidate();

            // 일부 호스트의 표시 갱신을 확인하기 위해 동일 TBT를 몇 차례 재전송.
            for (int i = 1; i <= 3; i++) {
                mainHandler.postDelayed(() -> {
                    try {
                        if (navigating) {
                            navigationManager.updateTrip(buildTrip(maneuverType, meters, road));
                        }
                    } catch (Exception e) {
                        HudCommandBus.report("재전송 오류: " + e.getClass().getSimpleName() + " · " + safeMessage(e));
                    }
                }, i * 1000L);
            }
        } catch (Exception e) {
            status = "오류: " + e.getClass().getSimpleName();
            HudCommandBus.report("AA 호출 실패: " + e.getClass().getSimpleName() + " · " + safeMessage(e));
            invalidate();
        }
    }

    private void endNavigation() {
        try {
            if (navigating) navigationManager.navigationEnded();
            navigating = false;
            status = "안내 종료됨";
            HudCommandBus.report("navigationEnded() 성공");
        } catch (Exception e) {
            status = "종료 오류";
            HudCommandBus.report("종료 실패: " + e.getClass().getSimpleName() + " · " + safeMessage(e));
        }
        invalidate();
    }

    private String safeMessage(Exception e) {
        String m = e.getMessage();
        return m == null ? "메시지 없음" : m;
    }
}
