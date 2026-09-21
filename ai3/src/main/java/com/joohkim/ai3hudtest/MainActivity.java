package com.joohkim.ai3hudtest;

import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Locale;

public class MainActivity extends Activity {
    private static final String NITRO_PACKAGE = "com.nitromirror.homescreen";

    // Values verified from the exact legacy android.support.car classes bundled in NitroAAutoMirror.
    private static final int APP_FOCUS_TYPE_NAVIGATION = 1;
    private static final int STATUS_ACTIVE = 1;
    private static final int STATUS_INACTIVE = 2;
    private static final int TURN_TURN = 4;
    private static final int TURN_U_TURN = 6;
    private static final int TURN_STRAIGHT = 14;
    private static final int TURN_SIDE_LEFT = 1;
    private static final int TURN_SIDE_RIGHT = 2;
    private static final int TURN_SIDE_UNSPECIFIED = 3;
    private static final int DISTANCE_METERS = 1;

    private final Handler main = new Handler(Looper.getMainLooper());
    private TextView status;

    private Context nitroContext;
    private ClassLoader nitroLoader;
    private Object audioHandler;
    private Object car;
    private Object focusManager;
    private Object navManager;
    private Object focusCallback;
    private Object navCallback;

    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        log("AI3 HUD Test 0.1");
        log("목표: Nitro에 포함된 legacy Car API를 직접 연결해 K5 계기판/HUD TBT 경로를 시험합니다.");
        runAsync(this::initialDiagnostics);
    }

    private void buildUi() {
        ScrollView outer = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        int p = dp(12);
        root.setPadding(p, p, p, p);
        outer.addView(root);

        TextView title = new TextView(this);
        title.setText("AI3 · K5 HUD 직접 테스트");
        title.setTextSize(21);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title);

        TextView desc = new TextView(this);
        desc.setText("NitroAAutoMirror에 들어있는 구형 Android Auto Car API를 로드해 navigation focus + cluster TBT를 직접 보냅니다.");
        desc.setTextSize(13);
        desc.setPadding(0, dp(4), 0, dp(10));
        root.addView(desc);

        addButton(root, "1. 환경 다시 검사", v -> runAsync(this::initialDiagnostics));
        addButton(root, "2. 내 패키지 AA 화이트리스트 패치 (root)", v -> patchWhitelist());
        addButton(root, "3. Nitro SDK 연결 · 내 패키지", v -> connectCar(false));
        addButton(root, "3-B. Nitro 컨텍스트로 연결 · 진단용", v -> connectCar(true));

        LinearLayout row1 = row();
        addSmallButton(row1, "↱ 우회전 300m", v -> sendTurn(TURN_TURN, TURN_SIDE_RIGHT, "테스트 우회전", 300));
        addSmallButton(row1, "↰ 좌회전 500m", v -> sendTurn(TURN_TURN, TURN_SIDE_LEFT, "테스트 좌회전", 500));
        root.addView(row1);

        LinearLayout row2 = row();
        addSmallButton(row2, "↑ 직진 1km", v -> sendTurn(TURN_STRAIGHT, TURN_SIDE_UNSPECIFIED, "테스트 직진", 1000));
        addSmallButton(row2, "↶ 유턴 200m", v -> sendTurn(TURN_U_TURN, TURN_SIDE_LEFT, "테스트 유턴", 200));
        root.addView(row2);

        addButton(root, "■ 내비 안내 종료", v -> stopNavigation());
        addButton(root, "연결 해제", v -> disconnectCar());

        status = new TextView(this);
        status.setTextSize(12);
        status.setTypeface(Typeface.MONOSPACE);
        status.setTextIsSelectable(true);
        status.setMovementMethod(new ScrollingMovementMethod());
        status.setPadding(0, dp(12), 0, dp(24));
        root.addView(status);

        setContentView(outer);
    }

    private LinearLayout row() {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        return r;
    }

    private void addButton(LinearLayout root, String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        root.addView(b, new LinearLayout.LayoutParams(-1, -2));
    }

    private void addSmallButton(LinearLayout root, String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setAllCaps(false);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        root.addView(b, lp);
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private void initialDiagnostics() {
        log("--- 환경 검사 ---");
        try {
            PackageInfo pi = getPackageManager().getPackageInfo(NITRO_PACKAGE, 0);
            log("NitroAAutoMirror: 설치됨 · " + pi.versionName + " (" + pi.versionCode + ")");
        } catch (Throwable t) {
            log("NitroAAutoMirror: 없음 → " + shortError(t));
            return;
        }

        try {
            nitroContext = createPackageContext(
                    NITRO_PACKAGE,
                    Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
            nitroLoader = nitroContext.getClassLoader();
            Class<?> carClass = Class.forName("android.support.car.Car", true, nitroLoader);
            Class<?> navClass = Class.forName(
                    "android.support.car.navigation.CarNavigationStatusManager", true, nitroLoader);
            Class<?> focusClass = Class.forName(
                    "android.support.car.CarAppFocusManager", true, nitroLoader);
            log("Nitro code loader: 성공");
            log("legacy Car API: " + carClass.getName());
            log("navigation manager: " + navClass.getName());
            log("focus manager: " + focusClass.getName());
        } catch (Throwable t) {
            log("Nitro SDK 로드 실패: " + shortError(t));
        }

        try {
            Process p = Runtime.getRuntime().exec(new String[]{"su", "-c", "id"});
            String out = readFirst(p);
            int rc = p.waitFor();
            log("root(su): rc=" + rc + " · " + out);
        } catch (Throwable t) {
            log("root(su): 사용 불가/확인 실패 · " + shortError(t));
        }
    }

    private void patchWhitelist() {
        runAsync(() -> {
            try {
                ensureNitroLoaded();
                log("--- Phenotype app_white_list 패치 시작 ---");

                Class<?> patcherClass = Class.forName(
                        "com.nitromirror.homescreen.utils.PhenotypePatcher", true, nitroLoader);
                Class<?> cbClass = Class.forName(
                        "com.nitromirror.homescreen.interfaces.OnPatchStatusCallback", true, nitroLoader);

                Context fake = new PatchContext(nitroContext, getApplicationContext());
                Constructor<?> ctor = patcherClass.getDeclaredConstructor(Context.class);
                ctor.setAccessible(true);
                Object patcher = ctor.newInstance(fake);

                Object cb = Proxy.newProxyInstance(nitroLoader, new Class[]{cbClass}, (proxy, method, args) -> {
                    String n = method.getName();
                    if ("onPatchSuccessful".equals(n)) {
                        log("화이트리스트 패치 성공: " + getPackageName());
                        log("Android Auto 연결을 끊었다가 다시 연결한 뒤 3번을 실행하세요.");
                    } else if ("onPatchFailed".equals(n)) {
                        log("화이트리스트 패치 실패");
                    }
                    return objectMethod(proxy, method, args);
                });

                Method patch = patcherClass.getMethod("patch", cbClass);
                patch.invoke(patcher, cb);
                log("Nitro PhenotypePatcher 호출 완료 · 결과 콜백 대기");
            } catch (Throwable t) {
                log("화이트리스트 패치 예외: " + fullError(t));
            }
        });
    }

    private void connectCar(boolean useNitroContext) {
        runAsync(() -> {
            try {
                ensureNitroLoaded();
                disconnectCarInternal();

                log("--- legacy Car 연결 시작 ---");
                log("Context 모드: " + (useNitroContext ? "Nitro package context" : "내 package context"));

                Class<?> prefClass = Class.forName(
                        "com.nitromirror.homescreen.handlers.PreferenceHandler", true, nitroLoader);
                Constructor<?> prefCtor = prefClass.getDeclaredConstructor(android.app.Application.class);
                prefCtor.setAccessible(true);
                Object prefs = prefCtor.newInstance(getApplication());

                Class<?> audioClass = Class.forName(
                        "com.nitromirror.homescreen.handlers.AudioHandler", true, nitroLoader);
                Constructor<?> audioCtor = audioClass.getDeclaredConstructor(Context.class, prefClass);
                audioCtor.setAccessible(true);
                Context carContext = useNitroContext ? nitroContext : getApplicationContext();
                audioHandler = audioCtor.newInstance(carContext, prefs);

                Field carField = audioClass.getDeclaredField("car");
                carField.setAccessible(true);
                car = carField.get(audioHandler);

                Method start = audioClass.getMethod("start");
                start.invoke(audioHandler);

                log("Car.connect() 호출됨");
                waitForCarConnection(0);
            } catch (Throwable t) {
                log("legacy Car 연결 예외: " + fullError(t));
            }
        });
    }

    private void waitForCarConnection(int attempt) {
        main.postDelayed(() -> runAsync(() -> {
            try {
                if (car == null) {
                    log("Car 객체 없음");
                    return;
                }
                Method isConnected = car.getClass().getMethod("isConnected");
                boolean connected = Boolean.TRUE.equals(isConnected.invoke(car));
                if (connected) {
                    log("Car API 연결 성공");
                    setupManagers();
                    return;
                }
                if (attempt >= 30) {
                    log("Car API 연결 시간초과 (약 15초)");
                    return;
                }
                if (attempt % 4 == 0) log("연결 대기중... " + (attempt / 2) + "초");
                waitForCarConnection(attempt + 1);
            } catch (Throwable t) {
                log("연결 확인 예외: " + fullError(t));
            }
        }), 500);
    }

    private void setupManagers() throws Exception {
        Method getManager = car.getClass().getMethod("getCarManager", String.class);
        focusManager = getManager.invoke(car, "app_focus");
        navManager = getManager.invoke(car, "car_navigation_service");
        log("app_focus manager: " + className(focusManager));
        log("car_navigation_service: " + className(navManager));

        if (focusManager == null || navManager == null) {
            log("필수 manager가 null → 이 AA host/API 경로에서는 HUD TBT 사용 불가");
            return;
        }

        Class<?> focusCbClass = Class.forName(
                "android.support.car.CarAppFocusManager$OnAppFocusOwnershipCallback", true, nitroLoader);
        focusCallback = Proxy.newProxyInstance(nitroLoader, new Class[]{focusCbClass}, (proxy, method, args) -> {
            if ("onAppFocusOwnershipGranted".equals(method.getName())) {
                log("NAVIGATION focus 획득");
            } else if ("onAppFocusOwnershipLost".equals(method.getName())) {
                log("NAVIGATION focus 상실");
            }
            return objectMethod(proxy, method, args);
        });

        Method request = findMethod(focusManager.getClass(), "requestAppFocus", 2);
        Object result = request.invoke(focusManager, APP_FOCUS_TYPE_NAVIGATION, focusCallback);
        log("requestAppFocus(NAVIGATION) 결과: " + result);

        Class<?> navCbClass = Class.forName(
                "android.support.car.navigation.CarNavigationStatusManager$CarNavigationCallback",
                true, nitroLoader);
        navCallback = Proxy.newProxyInstance(nitroLoader, new Class[]{navCbClass}, (proxy, method, args) -> {
            String n = method.getName();
            if ("onInstrumentClusterStarted".equals(n)) {
                Object cluster = args != null && args.length > 1 ? args[1] : null;
                log("★ Instrument Cluster STARTED: " + clusterDescription(cluster));
            } else if ("onInstrumentClusterStopped".equals(n)) {
                log("Instrument Cluster STOPPED");
            }
            return objectMethod(proxy, method, args);
        });

        try {
            Method addListener = findMethod(navManager.getClass(), "addListener", 1);
            addListener.invoke(navManager, navCallback);
            log("cluster callback 등록 성공");
        } catch (Throwable t) {
            log("cluster callback 등록 실패: " + shortError(t));
        }

        invokeNamed(navManager, "sendNavigationStatus", 1, STATUS_ACTIVE);
        log("sendNavigationStatus(ACTIVE) 전송 완료");
        log("이제 아래 우/좌/직진/U턴 버튼을 누르고 K5 HUD를 확인하세요.");
    }

    private String clusterDescription(Object cluster) {
        if (cluster == null) return "null (host가 cluster 정보를 주지 않음)";
        StringBuilder sb = new StringBuilder(cluster.toString());
        for (String name : new String[]{
                "getType", "getMinIntervalMillis", "getImageWidth",
                "getImageHeight", "getImageColorDepthBits", "supportsCustomImages"}) {
            try {
                Method m = cluster.getClass().getMethod(name);
                sb.append(" · ").append(name).append("=").append(m.invoke(cluster));
            } catch (Throwable ignored) {}
        }
        return sb.toString();
    }

    private void sendTurn(int turnType, int side, String name, int meters) {
        runAsync(() -> {
            try {
                if (navManager == null) {
                    log("먼저 3번으로 Car API를 연결하세요.");
                    return;
                }
                invokeNamed(navManager, "sendNavigationStatus", 1, STATUS_ACTIVE);
                invokeNamed(navManager, "sendNavigationTurnEvent", 5,
                        turnType, name, -1, -1, side);

                int seconds = Math.max(10, meters / 10);
                invokeNamed(navManager, "sendNavigationTurnDistanceEvent", 4,
                        meters, seconds, meters * 1000, DISTANCE_METERS);

                log(String.format(Locale.KOREA,
                        "TBT 전송: %s · %dm · type=%d side=%d", name, meters, turnType, side));

                // Some head units update more reliably when the countdown is refreshed.
                main.postDelayed(() -> resendDistance(meters), 1000);
                main.postDelayed(() -> resendDistance(Math.max(1, meters - 10)), 2000);
                main.postDelayed(() -> resendDistance(Math.max(1, meters - 20)), 3000);
            } catch (Throwable t) {
                log("TBT 전송 실패: " + fullError(t));
            }
        });
    }

    private void resendDistance(int meters) {
        runAsync(() -> {
            try {
                if (navManager != null) {
                    invokeNamed(navManager, "sendNavigationTurnDistanceEvent", 4,
                            meters, Math.max(10, meters / 10), meters * 1000, DISTANCE_METERS);
                }
            } catch (Throwable t) {
                log("거리 갱신 실패: " + shortError(t));
            }
        });
    }

    private void stopNavigation() {
        runAsync(() -> {
            try {
                if (navManager != null) {
                    invokeNamed(navManager, "sendNavigationStatus", 1, STATUS_INACTIVE);
                    log("sendNavigationStatus(INACTIVE) 전송");
                }
                abandonFocus();
            } catch (Throwable t) {
                log("안내 종료 실패: " + fullError(t));
            }
        });
    }

    private void abandonFocus() {
        if (focusManager == null || focusCallback == null) return;
        try {
            Method m = findMethod(focusManager.getClass(), "abandonAppFocus", 2);
            m.invoke(focusManager, focusCallback, APP_FOCUS_TYPE_NAVIGATION);
            log("NAVIGATION focus 반납");
        } catch (Throwable ignored) {
            try {
                Method m = findMethod(focusManager.getClass(), "abandonAppFocus", 1);
                m.invoke(focusManager, focusCallback);
                log("NAVIGATION focus 반납");
            } catch (Throwable t) {
                log("focus 반납 생략: " + shortError(t));
            }
        }
    }

    private void disconnectCar() {
        runAsync(() -> {
            disconnectCarInternal();
            log("Car API 연결 해제");
        });
    }

    private void disconnectCarInternal() {
        try { abandonFocus(); } catch (Throwable ignored) {}
        try {
            if (audioHandler != null) {
                Method stop = audioHandler.getClass().getMethod("stop");
                stop.invoke(audioHandler);
            } else if (car != null) {
                Method d = car.getClass().getMethod("disconnect");
                d.invoke(car);
            }
        } catch (Throwable ignored) {}
        audioHandler = null;
        car = null;
        focusManager = null;
        navManager = null;
        focusCallback = null;
        navCallback = null;
    }

    private void ensureNitroLoaded() throws Exception {
        if (nitroContext != null && nitroLoader != null) return;
        nitroContext = createPackageContext(
                NITRO_PACKAGE,
                Context.CONTEXT_INCLUDE_CODE | Context.CONTEXT_IGNORE_SECURITY);
        nitroLoader = nitroContext.getClassLoader();
        Class.forName("android.support.car.Car", true, nitroLoader);
    }

    private static Method findMethod(Class<?> cls, String name, int parameterCount)
            throws NoSuchMethodException {
        for (Method m : cls.getMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == parameterCount) {
                m.setAccessible(true);
                return m;
            }
        }
        for (Method m : cls.getDeclaredMethods()) {
            if (m.getName().equals(name) && m.getParameterTypes().length == parameterCount) {
                m.setAccessible(true);
                return m;
            }
        }
        throw new NoSuchMethodException(cls.getName() + "." + name + "/" + parameterCount);
    }

    private static Object invokeNamed(Object target, String name, int count, Object... args)
            throws Exception {
        Method m = findMethod(target.getClass(), name, count);
        return m.invoke(target, args);
    }

    private Object objectMethod(Object proxy, Method method, Object[] args) {
        if (method.getDeclaringClass() != Object.class) return null;
        switch (method.getName()) {
            case "toString": return "AI3HudProxy(" + proxy.getClass().getInterfaces()[0].getSimpleName() + ")";
            case "hashCode": return System.identityHashCode(proxy);
            case "equals": return args != null && args.length == 1 && proxy == args[0];
            default: return null;
        }
    }

    private String className(Object o) {
        return o == null ? "null" : o.getClass().getName();
    }

    private String readFirst(Process p) {
        try {
            BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String s = r.readLine();
            return s == null ? "(no stdout)" : s;
        } catch (Throwable t) {
            return "(read failed)";
        }
    }

    private void runAsync(Runnable r) {
        new Thread(r, "AI3HudWorker").start();
    }

    private void log(String s) {
        main.post(() -> {
            if (status != null) {
                status.append(s + "\n");
            }
        });
    }

    private static String shortError(Throwable t) {
        Throwable x = unwrap(t);
        return x.getClass().getSimpleName() + ": " + String.valueOf(x.getMessage());
    }

    private static String fullError(Throwable t) {
        Throwable x = unwrap(t);
        StringBuilder s = new StringBuilder(shortError(x));
        StackTraceElement[] st = x.getStackTrace();
        for (int i = 0; i < Math.min(5, st.length); i++) {
            s.append("\n  at ").append(st[i]);
        }
        return s.toString();
    }

    private static Throwable unwrap(Throwable t) {
        Throwable x = t;
        while (x.getCause() != null &&
                (x instanceof java.lang.reflect.InvocationTargetException ||
                 x instanceof java.lang.ExceptionInInitializerError)) {
            x = x.getCause();
        }
        return x;
    }

    /**
     * Nitro's PhenotypePatcher needs Nitro's raw sqlite3 resource, but we want it to add
     * OUR package name and copy sqlite3 into OUR writable data directory.
     */
    private static final class PatchContext extends ContextWrapper {
        private final ApplicationInfo ownInfo;

        PatchContext(Context nitroBase, Context ownApp) {
            super(nitroBase);
            ownInfo = new ApplicationInfo(ownApp.getApplicationInfo());
        }

        @Override
        public ApplicationInfo getApplicationInfo() {
            return ownInfo;
        }

        @Override
        public String getPackageName() {
            return ownInfo.packageName;
        }
    }
}
