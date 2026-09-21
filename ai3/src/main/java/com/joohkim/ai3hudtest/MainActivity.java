package com.joohkim.ai3hudtest;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ApplicationInfo;
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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class MainActivity extends Activity {
    private static final String NITRO_PACKAGE = "com.nitromirror.homescreen";
    private static final String NITRO_SERVICE = "com.nitromirror.homescreen.services.CarService";
    private static final String[] PROJECTION_PACKAGES = new String[] {
            "com.google.android.projection.bumblebee",
            "com.google.android.projection.gearhead"
    };

    private TextView status;
    private IBinder nitroBinder;
    private boolean bound;

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            bound = true;
            nitroBinder = service;
            log("★ Nitro CarService BIND 성공");
            log("component=" + name.flattenToShortString());
            try { log("descriptor=" + service.getInterfaceDescriptor()); }
            catch (Throwable t) { log("descriptor 실패: " + err(t)); }
            log("alive=" + service.isBinderAlive() + " · ping=" + service.pingBinder());
            log("→ 3번 Projection 패키지 AIDL 분석을 누르세요.");
        }

        @Override public void onServiceDisconnected(ComponentName name) {
            log("Nitro CarService 연결 끊김");
            bound = false; nitroBinder = null;
        }
        @Override public void onBindingDied(ComponentName name) {
            log("Nitro CarService binding died");
            bound = false; nitroBinder = null;
        }
        @Override public void onNullBinding(ComponentName name) {
            log("Nitro CarService NULL binding");
            bound = false; nitroBinder = null;
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        log("AI3 HUD Projection Probe 0.4");
        log("Nitro가 실제로 로드하는 Android Auto projection 패키지에서 AIDL 구조를 읽습니다.");
        inspectEnvironment();
    }

    private void buildUi() {
        ScrollView outer = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        root.setPadding(p, p, p, p);
        outer.addView(root);

        TextView title = new TextView(this);
        title.setText("AI3 · Projection AIDL Probe v0.4");
        title.setTextSize(21);
        root.addView(title);

        addButton(root, "1. 환경 / Projection 패키지 확인", v -> inspectEnvironment());
        addButton(root, "2. Nitro CarService BIND", v -> bindNitro());
        addButton(root, "3. Projection 패키지 AIDL 분석", v -> inspectProjectionPackages());
        addButton(root, "4. Binder 재확인", v -> inspectBinder());
        addButton(root, "연결 해제", v -> unbindNitro());

        status = new TextView(this);
        status.setTextSize(10);
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

    private void inspectEnvironment() {
        log("--- 환경 검사 ---");
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(NITRO_PACKAGE, 0);
            log("NitroAAutoMirror=" + pi.versionName + " (" + pi.versionCode + ")");
            ServiceInfo si = getPackageManager().getServiceInfo(
                    new ComponentName(NITRO_PACKAGE, NITRO_SERVICE), 0);
            log("CarService exported=" + si.exported + " permission=" + si.permission);
        } catch (Throwable t) {
            log("Nitro 확인 실패: " + err(t));
        }

        for (String pkg : PROJECTION_PACKAGES) {
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(pkg, 0);
                ApplicationInfo ai = pi.applicationInfo;
                log("★ Projection 설치됨: " + pkg);
                log("  version=" + pi.versionName + " (" + pi.versionCode + ")");
                log("  sourceDir=" + ai.sourceDir);
                if (ai.splitSourceDirs != null) {
                    for (String s : ai.splitSourceDirs) log("  split=" + s);
                }
            } catch (Throwable t) {
                log("Projection 없음: " + pkg + " · " + t.getClass().getSimpleName());
            }
        }
    }

    private void bindNitro() {
        if (bound) {
            log("이미 BIND 상태입니다.");
            return;
        }
        try {
            Intent i = new Intent("android.intent.action.MAIN");
            i.setComponent(new ComponentName(NITRO_PACKAGE, NITRO_SERVICE));
            log("--- Nitro BIND 시작 ---");
            boolean ok = bindService(i, conn, Context.BIND_AUTO_CREATE);
            log("bindService()=" + ok);
        } catch (Throwable t) {
            log("BIND 예외: " + err(t));
        }
    }

    private void inspectProjectionPackages() {
        log("--- Projection 패키지 AIDL 분석 ---");
        for (String pkg : PROJECTION_PACKAGES) {
            try {
                PackageInfo pi = getPackageManager().getPackageInfo(pkg, 0);
                Context pc = createPackageContext(pkg,
                        Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
                ClassLoader cl = pc.getClassLoader();

                log("====== " + pkg + " " + pi.versionName + " ======");
                log("ClassLoader=" + cl);

                inspectClass(cl, "com.google.android.gms.car.DynamicApiFactory", true);
                inspectClass(cl, "com.google.android.gms.car.ICarProjection", true);
                inspectClass(cl, "com.google.android.gms.car.ICarProjection$Stub", true);
                inspectClass(cl, "com.google.android.gms.car.ICarNavigationStatus", true);
                inspectClass(cl, "com.google.android.gms.car.ICarNavigationStatus$Stub", true);
                inspectClass(cl, "com.google.android.gms.car.ICarNavigationStatusEventListener", false);
                inspectClass(cl, "com.google.android.gms.car.CarNavigationStatusManager", false);

            } catch (Throwable t) {
                log(pkg + " 분석 실패: " + fullErr(t));
            }
        }
    }

    private void inspectClass(ClassLoader cl, String name, boolean dumpTransactions) {
        try {
            Class<?> c = Class.forName(name, false, cl);
            log("★ CLASS " + name);
            dumpMethods(c);
            if (dumpTransactions) dumpTransactionFields(c);

            if (name.endsWith("$Stub") && nitroBinder != null &&
                    name.equals("com.google.android.gms.car.ICarProjection$Stub")) {
                try {
                    Method m = c.getDeclaredMethod("asInterface", IBinder.class);
                    m.setAccessible(true);
                    Object proxy = m.invoke(null, nitroBinder);
                    log("  asInterface(nitroBinder)=" +
                            (proxy == null ? "null" : proxy.getClass().getName()));
                    if (proxy != null) dumpMethods(proxy.getClass());
                } catch (Throwable t) {
                    log("  asInterface 실패: " + fullErr(t));
                }
            }
        } catch (Throwable t) {
            log("CLASS 없음: " + name + " · " + err(t));
        }
    }

    private void dumpMethods(Class<?> c) {
        Method[] ms;
        try { ms = c.getDeclaredMethods(); }
        catch (Throwable t) { log("  methods 읽기 실패: " + err(t)); return; }

        int n = 0;
        for (Method m : ms) {
            StringBuilder s = new StringBuilder("  ");
            if (Modifier.isStatic(m.getModifiers())) s.append("static ");
            s.append(m.getReturnType().getSimpleName()).append(" ");
            s.append(m.getName()).append("(");
            Class<?>[] ps = m.getParameterTypes();
            for (int i = 0; i < ps.length; i++) {
                if (i > 0) s.append(",");
                s.append(ps[i].getSimpleName());
            }
            s.append(")");
            log(s.toString());
            if (++n >= 120) {
                log("  ... method 출력 제한");
                break;
            }
        }
    }

    private void dumpTransactionFields(Class<?> c) {
        int n = 0;
        for (Field f : c.getDeclaredFields()) {
            if (!f.getName().startsWith("TRANSACTION_")) continue;
            try {
                f.setAccessible(true);
                log("  " + f.getName() + "=" + f.get(null));
                n++;
            } catch (Throwable ignored) {}
        }
        if (n == 0) log("  TRANSACTION_* 필드 없음/난독화");
    }

    private void inspectBinder() {
        if (nitroBinder == null) {
            log("Binder 없음 → 2번 먼저 실행");
            return;
        }
        try {
            log("descriptor=" + nitroBinder.getInterfaceDescriptor());
            log("alive=" + nitroBinder.isBinderAlive() + " ping=" + nitroBinder.pingBinder());
        } catch (Throwable t) {
            log("Binder 확인 실패: " + err(t));
        }
    }

    private void unbindNitro() {
        if (!bound) return;
        try { unbindService(conn); } catch (Throwable ignored) {}
        bound = false; nitroBinder = null;
        log("Nitro CarService unbind 완료");
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

    private static String fullErr(Throwable t) {
        StringBuilder sb = new StringBuilder();
        Throwable x = t;
        int depth = 0;
        while (x != null && depth < 8) {
            if (depth > 0) sb.append(" <- ");
            sb.append(x.getClass().getSimpleName()).append(":").append(String.valueOf(x.getMessage()));
            x = x.getCause();
            depth++;
        }
        return sb.toString();
    }
}
