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

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

public class MainActivity extends Activity {
    private static final String NITRO_PACKAGE = "com.nitromirror.homescreen";
    private static final String NITRO_SERVICE = "com.nitromirror.homescreen.services.CarService";
    private static final String GMS_PACKAGE = "com.google.android.gms";
    private static final String PROJECTION_IFACE = "com.google.android.gms.car.ICarProjection";

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
            log("→ 이제 3번 'ICarProjection 구조 읽기'를 누르세요.");
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
        log("AI3 HUD Binder Probe 0.3");
        log("Binder 연결 후 ICarProjection의 메서드/transaction code만 읽습니다.");
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
        title.setText("AI3 · Nitro Binder HUD Probe v0.3");
        title.setTextSize(21);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("Nitro CarService Binder에 접근한 뒤 ICarProjection AIDL 구조와 transaction code를 안전하게 읽습니다.");
        desc.setTextSize(13);
        desc.setPadding(0, dp(4), 0, dp(10));
        root.addView(desc);

        addButton(root, "1. Nitro/GMS 정보 확인", v -> inspectNitro());
        addButton(root, "2. Nitro CarService 직접 BIND", v -> bindNitro(false));
        addButton(root, "2-B. AA Projection intent로 BIND", v -> bindNitro(true));
        addButton(root, "3. ICarProjection 구조 읽기", v -> inspectProjectionInterface());
        addButton(root, "4. Binder 정보 다시 읽기", v -> inspectBinder());
        addButton(root, "연결 해제", v -> unbindNitro());

        status = new TextView(this);
        status.setTextSize(11);
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
        log("--- 환경 검사 ---");
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(NITRO_PACKAGE, 0);
            log("NitroAAutoMirror: " + pi.versionName + " (" + pi.versionCode + ")");
        } catch (Throwable t) {
            log("Nitro 패키지 확인 실패: " + err(t));
        }

        try {
            PackageInfo pi = getPackageManager().getPackageInfo(GMS_PACKAGE, 0);
            log("Google Play services: " + pi.versionName + " (" + pi.versionCode + ")");
        } catch (Throwable t) {
            log("Google Play services 패키지 확인 실패: " + err(t));
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
        } catch (Throwable t) {
            log("BIND 예외: " + err(t));
        }
    }

    private void inspectProjectionInterface() {
        if (nitroBinder == null) {
            log("Binder 없음 → 먼저 2번 또는 2-B를 누르세요.");
            return;
        }

        log("--- ICarProjection 구조 탐색 ---");
        ClassLoader[] loaders = new ClassLoader[3];
        String[] names = new String[]{"내 앱", "Nitro", "GMS"};

        loaders[0] = getClassLoader();

        try {
            Context c = createPackageContext(NITRO_PACKAGE,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            loaders[1] = c.getClassLoader();
            log("Nitro ClassLoader 준비 성공");
        } catch (Throwable t) {
            log("Nitro ClassLoader 실패: " + err(t));
        }

        try {
            Context c = createPackageContext(GMS_PACKAGE,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            loaders[2] = c.getClassLoader();
            log("GMS ClassLoader 준비 성공");
        } catch (Throwable t) {
            log("GMS ClassLoader 실패: " + err(t));
        }

        boolean found = false;
        for (int i = 0; i < loaders.length; i++) {
            ClassLoader cl = loaders[i];
            if (cl == null) continue;
            try {
                Class<?> iface = Class.forName(PROJECTION_IFACE, false, cl);
                found = true;
                log("★ " + names[i] + " loader에서 ICarProjection 발견");
                dumpClass(iface, "IFACE");

                try {
                    Class<?> stub = Class.forName(PROJECTION_IFACE + "$Stub", false, cl);
                    dumpTransactions(stub);
                    dumpClass(stub, "STUB");

                    Method asInterface = stub.getDeclaredMethod("asInterface", IBinder.class);
                    asInterface.setAccessible(true);
                    Object proxy = asInterface.invoke(null, nitroBinder);
                    log("asInterface 성공: " + (proxy == null ? "null" : proxy.getClass().getName()));
                    if (proxy != null) dumpClass(proxy.getClass(), "PROXY");
                } catch (Throwable t) {
                    log("Stub/asInterface 탐색 실패: " + err(t));
                }
            } catch (Throwable t) {
                log(names[i] + " loader: ICarProjection 없음 · " + err(t));
            }
        }

        if (!found) {
            log("ICarProjection Java class는 어느 loader에서도 직접 노출되지 않았습니다.");
            log("그래도 Binder 자체는 정상 연결되어 있으므로 다음 단계는 Nitro 내부 Stub 구현을 역추적합니다.");
        }
    }

    private void dumpTransactions(Class<?> stub) {
        log("[TRANSACTION fields]");
        int count = 0;
        for (Field f : stub.getDeclaredFields()) {
            if (!f.getName().startsWith("TRANSACTION_")) continue;
            try {
                f.setAccessible(true);
                log("  " + f.getName() + "=" + String.valueOf(f.get(null)));
                if (++count >= 80) {
                    log("  ... transaction field 출력 제한");
                    break;
                }
            } catch (Throwable t) {
                log("  " + f.getName() + "=<읽기 실패>");
            }
        }
        if (count == 0) log("  TRANSACTION_* 필드 없음/숨김");
    }

    private void dumpClass(Class<?> c, String tag) {
        log("[" + tag + "] " + c.getName());
        Method[] ms;
        try {
            ms = c.getDeclaredMethods();
        } catch (Throwable t) {
            log("  메서드 읽기 실패: " + err(t));
            return;
        }
        int count = 0;
        for (Method m : ms) {
            StringBuilder sb = new StringBuilder();
            sb.append("  ");
            if (Modifier.isStatic(m.getModifiers())) sb.append("static ");
            sb.append(m.getReturnType().getSimpleName()).append(" ");
            sb.append(m.getName()).append("(");
            Class<?>[] ps = m.getParameterTypes();
            for (int j = 0; j < ps.length; j++) {
                if (j > 0) sb.append(", ");
                sb.append(ps[j].getSimpleName());
            }
            sb.append(")");
            log(sb.toString());
            if (++count >= 100) {
                log("  ... method 출력 제한");
                break;
            }
        }
        if (count == 0) log("  declared method 없음");
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
