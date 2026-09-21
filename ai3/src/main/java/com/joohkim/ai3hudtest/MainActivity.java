package com.joohkim.ai3hudtest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageInfo;
import android.content.pm.ServiceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private static final String NITRO_PACKAGE = "com.nitromirror.homescreen";
    private static final String NITRO_SERVICE = "com.nitromirror.homescreen.services.CarService";

    private TextView status;
    private IBinder nitroBinder;
    private boolean bound;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bound = true;
            nitroBinder = service;
            log("★ Nitro CarService BIND 성공");
            log("component: " + name.flattenToShortString());
            log("binder class: " + service.getClass().getName());
            try {
                log("interface descriptor: " + service.getInterfaceDescriptor());
            } catch (Throwable t) {
                log("descriptor 읽기 실패: " + err(t));
            }
            try {
                log("isBinderAlive=" + service.isBinderAlive() + " · pingBinder=" + service.pingBinder());
            } catch (Throwable t) {
                log("binder 상태 확인 실패: " + err(t));
            }
            try {
                String d = service.getInterfaceDescriptor();
                Object local = service.queryLocalInterface(d);
                log("queryLocalInterface: " + (local == null ? "null (remote binder)" : local.getClass().getName()));
            } catch (Throwable t) {
                log("local interface 확인 실패: " + err(t));
            }
            log("※ 이 descriptor가 다음 단계에서 가장 중요합니다.");
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("Nitro CarService 연결 끊김: " + name.flattenToShortString());
            bound = false;
            nitroBinder = null;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            log("Nitro CarService binding died: " + name.flattenToShortString());
            bound = false;
            nitroBinder = null;
        }

        @Override
        public void onNullBinding(ComponentName name) {
            log("Nitro CarService NULL binding: " + name.flattenToShortString());
            bound = false;
            nitroBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        log("AI3 HUD Binder Probe 0.2");
        log("root/화이트리스트/별도 Car.connect()는 사용하지 않습니다.");
        inspectNitro();
    }

    private void buildUi() {
        ScrollView outer = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        root.setPadding(p, p, p, p);
        outer.addView(root);

        TextView title = new TextView(this);
        title.setText("AI3 · Nitro Binder HUD Probe");
        title.setTextSize(21);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("실행 중인 Nitro Android Auto projection CarService에 직접 bind해 Binder 인터페이스를 확인합니다.");
        desc.setTextSize(13);
        desc.setPadding(0, dp(4), 0, dp(10));
        root.addView(desc);

        addButton(root, "1. Nitro 서비스 정보 확인", v -> inspectNitro());
        addButton(root, "2. Nitro CarService 직접 BIND", v -> bindNitro(false));
        addButton(root, "2-B. AA Projection intent로 BIND", v -> bindNitro(true));
        addButton(root, "3. Binder 정보 다시 읽기", v -> inspectBinder());
        addButton(root, "연결 해제", v -> unbindNitro());

        status = new TextView(this);
        status.setTextSize(12);
        status.setTextIsSelectable(true);
        status.setMovementMethod(new ScrollingMovementMethod());
        status.setPadding(0, dp(12), 0, dp(24));
        root.addView(status);

        setContentView(outer);
    }

    private void addButton(LinearLayout root, String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        root.addView(b, new LinearLayout.LayoutParams(-1, -2));
    }

    private void inspectNitro() {
        log("--- Nitro 검사 ---");
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(NITRO_PACKAGE, 0);
            log("NitroAAutoMirror: 설치됨 · " + pi.versionName + " (" + pi.versionCode + ")");
        } catch (Throwable t) {
            log("Nitro 패키지 확인 실패: " + err(t));
        }

        try {
            ComponentName cn = new ComponentName(NITRO_PACKAGE, NITRO_SERVICE);
            ServiceInfo si = getPackageManager().getServiceInfo(cn, 0);
            log("CarService: " + si.name);
            log("exported=" + si.exported + " · enabled=" + si.enabled + " · permission=" + si.permission);
            log("process=" + (si.processName == null ? NITRO_PACKAGE : si.processName));
        } catch (Throwable t) {
            log("CarService 정보 확인 실패: " + err(t));
        }
    }

    private void bindNitro(boolean projectionIntent) {
        if (bound) {
            log("이미 bind 상태입니다. 먼저 연결 해제 후 다시 시도하세요.");
            return;
        }
        try {
            Intent i = new Intent("android.intent.action.MAIN");
            i.setComponent(new ComponentName(NITRO_PACKAGE, NITRO_SERVICE));
            if (projectionIntent) {
                i.addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION");
                i.addCategory("com.google.android.gms.car.category.CATEGORY_PROJECTION_OEM");
            }
            log("--- BIND 시작 · mode=" + (projectionIntent ? "projection categories" : "explicit only") + " ---");
            boolean ok = bindService(i, conn, Context.BIND_AUTO_CREATE);
            log("bindService() return=" + ok);
            if (!ok) log("서비스가 바인딩 요청을 받지 않았습니다.");
        } catch (Throwable t) {
            log("BIND 예외: " + err(t));
        }
    }

    private void inspectBinder() {
        if (nitroBinder == null) {
            log("Binder 없음 → 먼저 2번 또는 2-B를 누르세요.");
            return;
        }
        try {
            log("--- Binder 재확인 ---");
            log("class=" + nitroBinder.getClass().getName());
            log("descriptor=" + nitroBinder.getInterfaceDescriptor());
            log("alive=" + nitroBinder.isBinderAlive() + " · ping=" + nitroBinder.pingBinder());
        } catch (Throwable t) {
            log("Binder 재확인 실패: " + err(t));
        }
    }

    private void unbindNitro() {
        if (!bound) {
            log("현재 bind 상태가 아닙니다.");
            return;
        }
        try {
            unbindService(conn);
            log("Nitro CarService unbind 완료");
        } catch (Throwable t) {
            log("unbind 실패: " + err(t));
        } finally {
            bound = false;
            nitroBinder = null;
        }
    }

    @Override
    protected void onDestroy() {
        if (bound) {
            try { unbindService(conn); } catch (Throwable ignored) {}
        }
        super.onDestroy();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private void log(String s) {
        runOnUiThread(() -> {
            if (status != null) status.append(s + "\n");
        });
    }

    private static String err(Throwable t) {
        Throwable x = t;
        while (x.getCause() != null && x.getCause() != x) x = x.getCause();
        return x.getClass().getSimpleName() + ": " + String.valueOf(x.getMessage());
    }
}
