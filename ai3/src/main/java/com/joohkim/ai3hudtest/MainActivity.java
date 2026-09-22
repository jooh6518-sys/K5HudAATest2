package com.joohkim.ai3hudtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String TAG = "AI3HUD095";
    private static final String VERSION = "0.95";
    private static final String GEARHEAD = "com.google.android.projection.gearhead";
    private static final String GMS = "com.google.android.gms";
    private static final String STARTUP_SERVICE =
            "com.google.android.apps.auto.carservice.service.impl.GearheadCarStartupService";

    private TextView logView;
    private ScrollView logScroll;

    private Context gearheadContext;
    private ClassLoader gearheadLoader;
    private ClassLoader gmsLoader;
    private ClassLoader carLoader;
    private Class<?> dynamicApiFactory;

    private boolean startupBound;
    private IBinder startupBinder;

    private final ServiceConnection startupConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            startupBound = true;
            startupBinder = service;
            section("IStartup BINDER CONNECTED");
            log("component=" + name.flattenToShortString());
            inspectStartupBinder(service);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            log("IStartup disconnected=" + name.flattenToShortString());
            startupBinder = null;
            startupBound = false;
        }

        @Override
        public void onBindingDied(ComponentName name) {
            log("IStartup bindingDied=" + name.flattenToShortString());
            startupBinder = null;
            startupBound = false;
        }

        @Override
        public void onNullBinding(ComponentName name) {
            log("IStartup nullBinding=" + name.flattenToShortString());
            startupBinder = null;
            startupBound = false;
        }
    };

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        log("AI3 HUD Probe " + VERSION);
        log("0.94에서 확인된 GearheadCarStartupService/IStartup 경로 전용");
    }

    @Override
    protected void onDestroy() {
        unbindStartup();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10), dp(10), dp(10), dp(10));
        root.setBackgroundColor(0xff101114);

        TextView title = new TextView(this);
        title.setText("AI3 HUD Probe v" + VERSION);
        title.setTextColor(0xfff1f3f4);
        title.setTextSize(18f);
        root.addView(title);

        LinearLayout r1 = new LinearLayout(this);
        r1.setOrientation(LinearLayout.HORIZONTAL);
        addButton(r1, "전체 검사", v -> runFullTest());
        addButton(r1, "IStartup 재연결", v -> bindStartup());
        root.addView(r1);

        LinearLayout r2 = new LinearLayout(this);
        r2.setOrientation(LinearLayout.HORIZONTAL);
        addButton(r2, "로그 복사", v -> copyLog());
        addButton(r2, "지우기", v -> logView.setText(""));
        root.addView(r2);

        logView = new TextView(this);
        logView.setTextColor(0xffe8eaed);
        logView.setTextSize(11f);
        logView.setTextIsSelectable(true);
        logView.setPadding(0, dp(8), 0, dp(24));

        logScroll = new ScrollView(this);
        logScroll.addView(logView);
        root.addView(logScroll, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f));
        setContentView(root);
    }

    private void addButton(LinearLayout row, String text, View.OnClickListener listener) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(listener);
        row.addView(b, new LinearLayout.LayoutParams(0, dp(48), 1f));
    }

    private void runFullTest() {
        unbindStartup();
        logView.setText("");
        section("AI3 HUD Probe " + VERSION + " 전체 검사");
        log("sdk=" + android.os.Build.VERSION.SDK_INT
                + " device=" + android.os.Build.MANUFACTURER + "/" + android.os.Build.MODEL);
        prepareDynamicCarApi();
        inspectApiInterfaces();
        dumpTargetClasses();
        bindStartup();
    }

    private void prepareDynamicCarApi() {
        section("DynamicApiFactory initialize");
        carLoader = null;
        dynamicApiFactory = null;

        try {
            gearheadContext = createPackageContext(
                    GEARHEAD, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            gearheadLoader = gearheadContext.getClassLoader();
            log("Gearhead Context OK=" + gearheadContext);
            log("Gearhead ClassLoader=" + gearheadLoader);
        } catch (Throwable t) {
            log("Gearhead Context ERROR=" + err(t));
            return;
        }

        try {
            Context gmsContext = createPackageContext(
                    GMS, Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            gmsLoader = gmsContext.getClassLoader();
            log("GMS ClassLoader=" + gmsLoader);
        } catch (Throwable t) {
            log("GMS ClassLoader ERROR=" + err(t));
        }

        try {
            dynamicApiFactory = Class.forName(
                    "com.google.android.gms.car.DynamicApiFactory", false, gearheadLoader);
            log("★ DynamicApiFactory=" + dynamicApiFactory
                    + " loader=" + dynamicApiFactory.getClassLoader());
            dumpClass(dynamicApiFactory, "DynamicApiFactory", true);
        } catch (Throwable t) {
            log("DynamicApiFactory LOAD ERROR=" + err(t));
            return;
        }

        Context initializedWith = null;
        Method initialize = findMethod(dynamicApiFactory, "initialize", Context.class);
        if (initialize == null) {
            log("initialize(Context) 메서드 없음");
        } else {
            for (Context ctx : new Context[]{gearheadContext, getApplicationContext(), this}) {
                if (ctx == null) continue;
                try {
                    initialize.setAccessible(true);
                    initialize.invoke(null, ctx);
                    initializedWith = ctx;
                    log("★ initialize SUCCESS context=" + describeContext(ctx));
                    break;
                } catch (Throwable t) {
                    log("initialize FAIL context=" + describeContext(ctx) + " -> " + err(t));
                }
            }
        }

        Method getter = findMethod(dynamicApiFactory, "getCarApiClassLoader", Context.class);
        if (getter == null) {
            log("getCarApiClassLoader(Context) 메서드 없음");
            return;
        }

        Set<Context> contexts = new LinkedHashSet<>();
        if (initializedWith != null) contexts.add(initializedWith);
        contexts.add(gearheadContext);
        contexts.add(getApplicationContext());
        contexts.add(this);

        for (Context ctx : contexts) {
            if (ctx == null) continue;
            try {
                getter.setAccessible(true);
                Object result = getter.invoke(null, ctx);
                log("getCarApiClassLoader(" + describeContext(ctx) + ")=" + result);
                if (result instanceof ClassLoader) {
                    carLoader = (ClassLoader) result;
                    log("★ CAR API CLASSLOADER SUCCESS=" + carLoader);
                    break;
                }
            } catch (Throwable t) {
                log("getCarApiClassLoader FAIL context="
                        + describeContext(ctx) + " -> " + err(t));
            }
        }

        if (carLoader == null) {
            log("!! CAR API CLASSLOADER 획득 실패");
        }
    }

    private void inspectApiInterfaces() {
        section("DynamicApiFactory.isApiInterface");
        if (dynamicApiFactory == null) {
            log("DynamicApiFactory 없음");
            return;
        }
        Method isApi = findMethod(dynamicApiFactory, "isApiInterface", String.class);
        if (isApi == null) {
            log("isApiInterface(String) 없음");
            return;
        }
        String[] names = targetNames();
        for (String name : names) {
            try {
                isApi.setAccessible(true);
                Object r = isApi.invoke(null, name);
                log("isApiInterface " + name + " -> " + r);
            } catch (Throwable t) {
                log("isApiInterface ERROR " + name + " -> " + err(t));
            }
        }
    }

    private void dumpTargetClasses() {
        section("CAR API CLASS 검사");
        for (String name : targetNames()) {
            Class<?> c = loadAny(name);
            if (c == null) {
                log("CLASS X " + name);
            } else {
                log("★ CLASS OK " + name + " loader=" + c.getClassLoader());
                dumpClass(c, name, true);
            }
        }
    }

    private String[] targetNames() {
        return new String[] {
                "com.google.android.gms.car.startup.IStartup",
                "com.google.android.gms.car.startup.IStartup$Stub",
                "com.google.android.gms.car.ICarProjection",
                "com.google.android.gms.car.ICarProjection$Stub",
                "com.google.android.gms.car.ICarNavigationStatus",
                "com.google.android.gms.car.ICarNavigationStatus$Stub",
                "com.google.android.gms.car.ICarNavigationStatusEventListener",
                "com.google.android.gms.car.ICarNavigationStatusEventListener$Stub",
                "com.google.android.gms.car.CarNavigationStatusManager"
        };
    }

    private void bindStartup() {
        unbindStartup();
        section("GearheadCarStartupService BIND");
        Intent i = new Intent();
        i.setComponent(new ComponentName(GEARHEAD, STARTUP_SERVICE));
        try {
            boolean ok = bindService(i, startupConnection, Context.BIND_AUTO_CREATE);
            log("bindService=" + ok);
            if (!ok) log("!! IStartup bind 실패");
        } catch (Throwable t) {
            log("IStartup BIND ERROR=" + err(t));
        }
    }

    private void unbindStartup() {
        if (!startupBound) return;
        try {
            unbindService(startupConnection);
        } catch (Throwable ignored) {}
        startupBound = false;
        startupBinder = null;
    }

    private void inspectStartupBinder(IBinder binder) {
        if (binder == null) {
            log("startup binder=null");
            return;
        }

        String descriptor = null;
        try {
            descriptor = binder.getInterfaceDescriptor();
            log("binderClass=" + binder.getClass().getName());
            log("alive=" + binder.isBinderAlive() + " ping=" + binder.pingBinder());
            log("★ descriptor=" + descriptor);
        } catch (Throwable t) {
            log("Binder metadata ERROR=" + err(t));
        }

        if (!"com.google.android.gms.car.startup.IStartup".equals(descriptor)) {
            log("!! 예상 IStartup descriptor와 다름");
            return;
        }

        Class<?> iface = loadAny("com.google.android.gms.car.startup.IStartup");
        Class<?> stub = loadAny("com.google.android.gms.car.startup.IStartup$Stub");
        log("IStartup class=" + iface);
        log("IStartup Stub=" + stub);

        if (iface != null) dumpClass(iface, "IStartup(interface)", true);
        if (stub == null) {
            log("!! IStartup$Stub 로드 실패 - Car API loader 로그 확인");
            return;
        }

        try {
            Method asInterface = stub.getDeclaredMethod("asInterface", IBinder.class);
            asInterface.setAccessible(true);
            Object api = asInterface.invoke(null, binder);
            log("★ IStartup asInterface SUCCESS proxy="
                    + (api == null ? "null" : api.getClass().getName()));
            if (api != null) {
                dumpClass(api.getClass(), "IStartup proxy", true);
                Class<?>[] interfaces = api.getClass().getInterfaces();
                for (Class<?> c : interfaces) {
                    log("proxy interface=" + c.getName());
                    dumpClass(c, "proxy interface " + c.getName(), true);
                }
            }
        } catch (Throwable t) {
            log("IStartup asInterface ERROR=" + err(t));
        }
    }

    private Class<?> loadAny(String name) {
        ClassLoader[] loaders = {carLoader, gearheadLoader, gmsLoader};
        Set<ClassLoader> seen = new LinkedHashSet<>();
        for (ClassLoader loader : loaders) {
            if (loader == null || !seen.add(loader)) continue;
            try {
                return Class.forName(name, false, loader);
            } catch (Throwable ignored) {}
        }
        return null;
    }

    private Method findMethod(Class<?> c, String name, Class<?>... params) {
        if (c == null) return null;
        try {
            return c.getDeclaredMethod(name, params);
        } catch (Throwable ignored) {
            try {
                return c.getMethod(name, params);
            } catch (Throwable ignored2) {
                return null;
            }
        }
    }

    private void dumpClass(Class<?> c, String label, boolean dumpFields) {
        if (c == null) return;
        log("--- " + label + " ---");
        try {
            Class<?> parent = c.getSuperclass();
            if (parent != null) log(" superclass=" + parent.getName());
        } catch (Throwable ignored) {}
        try {
            for (Class<?> i : c.getInterfaces()) log(" interface=" + i.getName());
        } catch (Throwable ignored) {}
        try {
            for (Class<?> n : c.getDeclaredClasses()) log(" nested=" + n.getName());
        } catch (Throwable ignored) {}
        try {
            Constructor<?>[] cs = c.getDeclaredConstructors();
            for (int i = 0; i < Math.min(cs.length, 30); i++) {
                log(" C " + cs[i].toGenericString());
            }
        } catch (Throwable t) {
            log(" constructors ERROR=" + err(t));
        }
        try {
            Method[] ms = c.getDeclaredMethods();
            for (int i = 0; i < Math.min(ms.length, 160); i++) {
                log(" M " + methodSig(ms[i]));
            }
            if (ms.length > 160) log(" ... methods=" + ms.length);
        } catch (Throwable t) {
            log(" methods ERROR=" + err(t));
        }
        if (!dumpFields) return;
        try {
            Field[] fs = c.getDeclaredFields();
            int printed = 0;
            for (Field f : fs) {
                if (printed >= 100) break;
                String name = f.getName();
                boolean interesting = name.startsWith("TRANSACTION_")
                        || name.toLowerCase().contains("navigation")
                        || name.toLowerCase().contains("service")
                        || name.toLowerCase().contains("startup")
                        || name.toLowerCase().contains("status")
                        || name.toLowerCase().contains("turn")
                        || f.getType().isPrimitive()
                        || f.getType() == String.class;
                if (!interesting) continue;
                String value = "(instance)";
                if (Modifier.isStatic(f.getModifiers())) {
                    try {
                        f.setAccessible(true);
                        value = String.valueOf(f.get(null));
                    } catch (Throwable t) {
                        value = "(unreadable)";
                    }
                }
                log(" F " + f.getType().getSimpleName() + " " + name + "=" + value);
                printed++;
            }
        } catch (Throwable t) {
            log(" fields ERROR=" + err(t));
        }
    }

    private String methodSig(Method m) {
        try {
            return m.toGenericString();
        } catch (Throwable ignored) {
            return m.toString();
        }
    }

    private String describeContext(Context c) {
        if (c == null) return "null";
        try {
            return c.getPackageName() + "/" + c.getClass().getName();
        } catch (Throwable t) {
            return c.getClass().getName();
        }
    }

    private String err(Throwable t) {
        if (t == null) return "null";
        Throwable x = t;
        int guard = 0;
        while (x.getCause() != null && x.getCause() != x && guard++ < 12) {
            x = x.getCause();
        }
        String msg = x.getMessage();
        return x.getClass().getName() + (msg == null ? "" : ": " + msg);
    }

    private void copyLog() {
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText(
                    "AI3 HUD Probe " + VERSION, logView.getText()));
            Toast.makeText(this, "로그 복사 완료", Toast.LENGTH_SHORT).show();
        } catch (Throwable t) {
            log("로그 복사 ERROR=" + err(t));
        }
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private void section(String title) {
        log("\n=== " + title + " ===");
    }

    private void log(String s) {
        Log.d(TAG, s);
        if (logView == null) return;
        runOnUiThread(() -> {
            logView.append(s + "\n");
            if (logScroll != null) {
                logScroll.post(() -> logScroll.fullScroll(View.FOCUS_DOWN));
            }
        });
    }
}
