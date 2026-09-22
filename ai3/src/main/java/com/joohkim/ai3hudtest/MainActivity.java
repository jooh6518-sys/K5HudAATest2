package com.joohkim.ai3hudtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.car.CarApi;
import com.google.android.gms.car.CarApiConnection;
import com.google.android.gms.car.CarMessageManager;
import com.google.android.gms.car.CarNavigationStatusManager;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String TAG="AI3HUD098";
    private static final String VERSION="0.98";
    private static final String GEARHEAD="com.google.android.projection.gearhead";

    private TextView logView;
    private ScrollView logScroll;

    private Context gearheadContext;
    private ClassLoader gearheadLoader;
    private Class<?> dynamicApiFactory;

    private CarApiConnection connection;
    private CarApi carApi;
    private CarNavigationStatusManager nav;
    private CarMessageManager focus;
    private boolean focusOwned;

    private final IdentityHashMap<Object,Boolean> visited = new IdentityHashMap<>();

    private final CarApiConnection.ApiConnectionCallback connectionCallback =
            new CarApiConnection.ApiConnectionCallback() {
        @Override public void onConnected() {
            log("★ CarApiConnection.onConnected()");
            dumpConnectionDeep("CONNECTED");
            onCarApiConnected();
        }
        @Override public void onConnectionFailed() {
            log("!! CarApiConnection.onConnectionFailed()");
            dumpConnectionDeep("FAILED");
            tryGetCarApiAfterFailure();
        }
        @Override public void onConnectionSuspended() {
            log("!! CarApiConnection.onConnectionSuspended()");
            dumpConnectionDeep("SUSPENDED");
        }
    };

    private final CarMessageManager.CarMessageListener messageListener =
            new CarMessageManager.CarMessageListener() {
        @Override public void onIntegerMessage(int category,int key,int value) {
            log("CarMessage category="+category+" key="+key+" value="+value);
        }
        @Override public void onOwnershipLost(int category) {
            log("!! CarMessage ownership lost category="+category);
            if(category==1) focusOwned=false;
        }
    };

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
        log("AI3 HUD Test "+VERSION);
        log("AA navigation manifest + CarApi handshake deep diagnostic");
    }

    @Override protected void onDestroy() {
        stopNavigationSafe();
        disconnectSafe();
        super.onDestroy();
    }

    private void buildUi() {
        LinearLayout root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(10),dp(10),dp(10),dp(10));
        root.setBackgroundColor(0xff101114);

        TextView title=new TextView(this);
        title.setText("AI3 HUD Test v"+VERSION);
        title.setTextColor(0xfff1f3f4);
        title.setTextSize(18f);
        root.addView(title);

        LinearLayout r1=new LinearLayout(this); r1.setOrientation(LinearLayout.HORIZONTAL);
        addButton(r1,"1. Car API 연결",v->connectCarApi());
        addButton(r1,"2. 포커스 획득",v->requestNavigationFocus());
        root.addView(r1);

        LinearLayout r2=new LinearLayout(this); r2.setOrientation(LinearLayout.HORIZONTAL);
        addButton(r2,"3. HUD 테스트",v->sendHudTest());
        addButton(r2,"4. HUD 종료",v->stopNavigationSafe());
        root.addView(r2);

        LinearLayout r3=new LinearLayout(this); r3.setOrientation(LinearLayout.HORIZONTAL);
        addButton(r3,"로그 복사",v->copyLog());
        addButton(r3,"초기화",v->{ stopNavigationSafe(); disconnectSafe(); logView.setText(""); });
        root.addView(r3);

        logView=new TextView(this);
        logView.setTextColor(0xffe8eaed);
        logView.setTextSize(10.5f);
        logView.setTextIsSelectable(true);
        logView.setPadding(0,dp(8),0,dp(24));

        logScroll=new ScrollView(this);
        logScroll.addView(logView);
        root.addView(logScroll,new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,0,1f));
        setContentView(root);
    }

    private void addButton(LinearLayout row,String text,View.OnClickListener l) {
        Button b=new Button(this);
        b.setText(text); b.setAllCaps(false); b.setOnClickListener(l);
        row.addView(b,new LinearLayout.LayoutParams(0,dp(48),1f));
    }

    private void connectCarApi() {
        disconnectSafe();
        visited.clear();
        section("Manifest / Car API 연결");
        dumpManifest();

        try {
            gearheadContext=createPackageContext(
                    GEARHEAD,Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
            gearheadLoader=gearheadContext.getClassLoader();
            dynamicApiFactory=Class.forName(
                    "com.google.android.gms.car.DynamicApiFactory",false,gearheadLoader);
            log("DynamicApiFactory="+dynamicApiFactory);

            Method init=dynamicApiFactory.getDeclaredMethod("initialize",Context.class);
            init.setAccessible(true);
            init.invoke(null,gearheadContext);
            log("★ DynamicApiFactory.initialize SUCCESS");
            log("Client Version.getVersion()="+com.google.android.gms.car.Version.getVersion());

            Method isApi=dynamicApiFactory.getDeclaredMethod("isApiInterface",String.class);
            isApi.setAccessible(true);
            for(String n:new String[]{
                    "com.google.android.gms.car.CarApiConnection",
                    "com.google.android.gms.car.CarApiConnection$ApiConnectionCallback",
                    "com.google.android.gms.car.CarApi",
                    "com.google.android.gms.car.CarMessageManager",
                    "com.google.android.gms.car.CarNavigationStatusManager"}) {
                try { log("isApiInterface "+n+" -> "+isApi.invoke(null,n)); }
                catch(Throwable t) { log("isApiInterface ERR "+n+" -> "+err(t)); }
            }

            Method make=dynamicApiFactory.getDeclaredMethod(
                    "newCarApiConnection",Context.class,Object.class,Looper.class);
            make.setAccessible(true);
            Object obj=make.invoke(null,this,connectionCallback,Looper.getMainLooper());
            log("newCarApiConnection result="+obj);

            if(obj!=null) {
                log("connection class="+obj.getClass().getName());
                dumpClassShape(obj.getClass(),"connection-class");
                visited.clear();
                dumpDeep(obj,"connection-created",0,3);
            }

            if(!(obj instanceof CarApiConnection)) {
                log("!! returned object is NOT CarApiConnection");
                return;
            }

            connection=(CarApiConnection)obj;
            log("★ CarApiConnection interface match");
            connection.connect();
            log("connect() 호출 완료 - callback 대기");
        } catch(Throwable t) {
            log("!! Car API 연결 ERROR="+err(t));
        }
    }

    private void dumpManifest() {
        try {
            ApplicationInfo ai=getPackageManager().getApplicationInfo(
                    getPackageName(),PackageManager.GET_META_DATA);
            Bundle m=ai.metaData;
            log("appPackage="+getPackageName());
            log("metaData="+m);
            if(m!=null) {
                log("com.google.android.gms.car.application="
                        +m.get("com.google.android.gms.car.application"));
                log("androidx.car.app.minCarApiLevel="
                        +m.get("androidx.car.app.minCarApiLevel"));
            }
        } catch(Throwable t) {
            log("manifest inspect ERROR="+err(t));
        }
    }

    private void onCarApiConnected() {
        try {
            if(connection==null) { log("connection=null"); return; }
            carApi=connection.getCarApi();
            log("getCarApi="+carApi);
            if(carApi==null) return;

            log("CarApi class="+carApi.getClass().getName());
            dumpClassShape(carApi.getClass(),"CarApiImpl");
            visited.clear();
            dumpDeep(carApi,"carApi",0,2);

            log("isConnectedToCar="+carApi.isConnectedToCar());
            try { log("connectionType="+carApi.getCarConnectionType()); }
            catch(Throwable t) { log("getCarConnectionType ERROR="+err(t)); }

            Object f=carApi.getCarManager("app_focus");
            log("getCarManager(app_focus)="+f);
            if(f instanceof CarMessageManager) {
                focus=(CarMessageManager)f;
                log("★ CarMessageManager 획득");
                try { focus.registerMessageListener(messageListener); log("focus listener 등록"); }
                catch(Throwable t) { log("focus listener ERROR="+err(t)); }
            } else if(f!=null) {
                log("app_focus class="+f.getClass().getName());
                dumpClassShape(f.getClass(),"app_focus");
            }

            Object n=carApi.getCarManager("car_navigation_service");
            log("getCarManager(car_navigation_service)="+n);
            if(n instanceof CarNavigationStatusManager) {
                nav=(CarNavigationStatusManager)n;
                log("★ CarNavigationStatusManager 획득 성공");
                log("★ 실제 HUD 송신 준비 완료");
            } else if(n!=null) {
                log("navigation class="+n.getClass().getName());
                dumpClassShape(n.getClass(),"navigation");
            } else {
                log("!! navigation manager=null");
            }
        } catch(Throwable t) {
            log("!! onCarApiConnected ERROR="+err(t));
        }
    }

    private void tryGetCarApiAfterFailure() {
        if(connection==null) return;
        try {
            CarApi a=connection.getCarApi();
            log("getCarApi after failure="+a);
            if(a!=null) {
                visited.clear();
                dumpDeep(a,"carApi-after-failure",0,2);
                try { log("after-failure isConnectedToCar="+a.isConnectedToCar()); }
                catch(Throwable t) { log("after-failure isConnectedToCar ERROR="+err(t)); }
            }
        } catch(Throwable t) {
            log("getCarApi after failure ERROR="+err(t));
        }
    }

    private void dumpConnectionDeep(String reason) {
        if(connection==null) {
            log("["+reason+"] connection=null");
            return;
        }
        section("Connection deep dump: "+reason);
        dumpClassShape(connection.getClass(),"CarApiConnectionImpl");
        visited.clear();
        dumpDeep(connection,"connection",0,3);
    }

    private void dumpClassShape(Class<?> c,String label) {
        if(c==null) return;
        log("--- CLASS "+label+" "+c.getName()+" loader="+c.getClassLoader()+" ---");
        try {
            Class<?>[] ifs=c.getInterfaces();
            for(Class<?> i:ifs) log(" I "+i.getName());
        } catch(Throwable ignored) {}
        try {
            Method[] ms=c.getDeclaredMethods();
            int lim=Math.min(ms.length,120);
            for(int i=0;i<lim;i++) log(" M "+safeMethod(ms[i]));
        } catch(Throwable t) {
            log(" method dump ERROR="+err(t));
        }
        try {
            Field[] fs=c.getDeclaredFields();
            int lim=Math.min(fs.length,120);
            for(int i=0;i<lim;i++) {
                Field f=fs[i];
                String val="";
                if(Modifier.isStatic(f.getModifiers())) {
                    try { f.setAccessible(true); val="="+String.valueOf(f.get(null)); }
                    catch(Throwable ignored) {}
                }
                log(" F "+f.getType().getName()+" "+f.getName()+val);
            }
        } catch(Throwable t) {
            log(" field shape ERROR="+err(t));
        }
    }

    private void dumpDeep(Object obj,String path,int depth,int maxDepth) {
        if(obj==null) { log(" D "+path+"=null"); return; }
        if(depth>maxDepth) return;
        if(visited.containsKey(obj)) { log(" D "+path+"=<visited "+obj.getClass().getName()+">"); return; }
        visited.put(obj,Boolean.TRUE);

        Class<?> cls=obj.getClass();
        String cn=cls.getName();

        if(isSimple(obj)) {
            log(" D "+path+"="+stringValue(obj));
            return;
        }

        if(obj instanceof ComponentName || obj instanceof Intent || obj instanceof Bundle) {
            log(" D "+path+"="+stringValue(obj));
            return;
        }

        log(" D "+path+" <"+cn+"> "+safeToString(obj));

        if(depth==maxDepth) return;

        if(!cn.startsWith("com.google.android.gms.car")
                && !cn.startsWith("java.lang.ref")
                && !cn.startsWith("android.content")) {
            return;
        }

        try {
            Class<?> c=cls;
            while(c!=null && c!=Object.class) {
                for(Field f:c.getDeclaredFields()) {
                    if(Modifier.isStatic(f.getModifiers())) continue;
                    try {
                        f.setAccessible(true);
                        Object v=f.get(obj);
                        String p=path+"."+c.getSimpleName()+"."+f.getName();
                        if(v==null || isSimple(v) || v instanceof ComponentName
                                || v instanceof Intent || v instanceof Bundle) {
                            log(" D "+p+"="+stringValue(v));
                        } else {
                            dumpDeep(v,p,depth+1,maxDepth);
                        }
                    } catch(Throwable ignored) {}
                }
                c=c.getSuperclass();
            }
        } catch(Throwable t) {
            log(" deep dump ERROR "+path+"="+err(t));
        }
    }

    private boolean isSimple(Object o) {
        return o==null || o instanceof String || o instanceof Number
                || o instanceof Boolean || o instanceof Character
                || o.getClass().isEnum() || o instanceof Class;
    }

    private String stringValue(Object o) {
        if(o==null) return "null";
        String s=safeToString(o);
        if(s.length()>500) s=s.substring(0,500)+"...";
        return s;
    }

    private String safeToString(Object o) {
        try { return String.valueOf(o); }
        catch(Throwable t) { return "<toString error "+t.getClass().getSimpleName()+">"; }
    }

    private String safeMethod(Method m) {
        try { return m.toGenericString(); }
        catch(Throwable t) { return m.toString(); }
    }

    private void requestNavigationFocus() {
        section("Navigation Focus");
        if(focus==null) {
            log("focus manager 없음 → 1. Car API 연결 먼저");
            return;
        }
        try {
            boolean ok=focus.acquireCategory(1);
            log("acquireCategory(1)="+ok);
            if(ok) {
                focusOwned=true;
                focus.sendIntegerMessage(1,0,1);
                log("★ navigation focus 활성 메시지 전송 (1,0,1)");
            }
        } catch(Throwable t) {
            log("!! focus ERROR="+err(t));
        }
    }

    private void sendHudTest() {
        section("HUD 고정값 테스트");
        if(nav==null) {
            log("navigation manager 없음 → 1. Car API 연결 먼저");
            return;
        }
        if(!focusOwned) {
            log("focus 미획득 → 먼저 2. 포커스 획득");
            return;
        }
        try {
            boolean s=nav.sendNavigationStatus(1);
            log("sendNavigationStatus(ACTIVE=1)="+s);

            boolean t=nav.sendNavigationTurnEvent(4,"HUD TEST",-1,-1,null,2);
            log("sendNavigationTurnEvent(TURN=4, RIGHT=2)="+t);

            boolean d=nav.sendNavigationTurnDistanceEvent(300,30,300000,1);
            log("sendNavigationTurnDistanceEvent(300m,30s,meters)="+d);
            log("★ HUD 테스트 데이터 전송 완료");
        } catch(Throwable t) {
            log("!! HUD TEST ERROR="+err(t));
        }
    }

    private void stopNavigationSafe() {
        if(nav!=null) {
            try { log("sendNavigationStatus(INACTIVE=2)="+nav.sendNavigationStatus(2)); }
            catch(Throwable t) { log("navigation stop ERROR="+err(t)); }
        }
        if(focus!=null) {
            try { focus.sendIntegerMessage(1,0,0); } catch(Throwable ignored) {}
            try { focus.releaseCategory(1); } catch(Throwable ignored) {}
        }
        focusOwned=false;
    }

    private void disconnectSafe() {
        nav=null;
        carApi=null;
        if(focus!=null) {
            try { focus.unregisterMessageListener(); } catch(Throwable ignored) {}
            try { focus.releaseCategory(1); } catch(Throwable ignored) {}
        }
        focus=null;
        focusOwned=false;
        if(connection!=null) {
            try { connection.disconnect(); } catch(Throwable ignored) {}
        }
        connection=null;
    }

    private String err(Throwable t) {
        Throwable x=t; int n=0;
        while(x!=null && x.getCause()!=null && x.getCause()!=x && n++<12) x=x.getCause();
        if(x==null) return "null";
        return x.getClass().getName()+": "+String.valueOf(x.getMessage());
    }

    private void copyLog() {
        try {
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("AI3 "+VERSION,logView.getText()));
            Toast.makeText(this,"로그 복사 완료",Toast.LENGTH_SHORT).show();
        } catch(Throwable t) {
            log("copy ERROR="+err(t));
        }
    }

    private void section(String s) { log("\n=== "+s+" ==="); }

    private void log(String s) {
        Log.d(TAG,s);
        if(logView==null) return;
        runOnUiThread(()->{
            logView.append(s+"\n");
            if(logScroll!=null) logScroll.post(()->logScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private int dp(int n) {
        return Math.round(n*getResources().getDisplayMetrics().density);
    }
}
