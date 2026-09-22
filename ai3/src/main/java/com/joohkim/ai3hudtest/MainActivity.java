package com.joohkim.ai3hudtest;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
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

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends Activity {
    private static final String TAG="AI3HUD096";
    private static final String VERSION="0.96";
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

    private final CarApiConnection.ApiConnectionCallback connectionCallback =
            new CarApiConnection.ApiConnectionCallback() {
        @Override public void onConnected() {
            log("★ CarApiConnection.onConnected()");
            onCarApiConnected();
        }
        @Override public void onConnectionFailed() {
            log("!! CarApiConnection.onConnectionFailed()");
        }
        @Override public void onConnectionSuspended() {
            log("!! CarApiConnection.onConnectionSuspended()");
        }
    };

    private final CarMessageManager.CarMessageListener messageListener =
            new CarMessageManager.CarMessageListener() {
        @Override public void onIntegerMessage(int category, int key, int value) {
            log("CarMessage onIntegerMessage category="+category+" key="+key+" value="+value);
        }
        @Override public void onOwnershipLost(int category) {
            log("!! CarMessage ownership lost category="+category);
            if(category==1) focusOwned=false;
        }
    };

    @Override protected void onCreate(Bundle state){
        super.onCreate(state);
        buildUi();
        log("AI3 HUD Test "+VERSION);
        log("실제 CarApiConnection / navigation manager 연결 테스트");
    }

    @Override protected void onDestroy(){
        stopNavigationSafe();
        disconnectSafe();
        super.onDestroy();
    }

    private void buildUi(){
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
        logView.setTextSize(11f);
        logView.setTextIsSelectable(true);
        logView.setPadding(0,dp(8),0,dp(24));

        logScroll=new ScrollView(this);
        logScroll.addView(logView);
        root.addView(logScroll,new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,0,1f));
        setContentView(root);
    }

    private void addButton(LinearLayout row,String text,View.OnClickListener l){
        Button b=new Button(this);
        b.setText(text); b.setAllCaps(false); b.setOnClickListener(l);
        row.addView(b,new LinearLayout.LayoutParams(0,dp(48),1f));
    }

    private void connectCarApi(){
        disconnectSafe();
        section("Car API 연결");
        try{
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

            Method isApi=dynamicApiFactory.getDeclaredMethod("isApiInterface",String.class);
            isApi.setAccessible(true);
            for(String n:new String[]{
                    "com.google.android.gms.car.CarApiConnection",
                    "com.google.android.gms.car.CarApiConnection$ApiConnectionCallback",
                    "com.google.android.gms.car.CarApi",
                    "com.google.android.gms.car.CarMessageManager",
                    "com.google.android.gms.car.CarNavigationStatusManager"}){
                try{ log("isApiInterface "+n+" -> "+isApi.invoke(null,n)); }
                catch(Throwable t){ log("isApiInterface ERR "+n+" -> "+err(t)); }
            }

            Method make=dynamicApiFactory.getDeclaredMethod(
                    "newCarApiConnection",Context.class,Object.class,Looper.class);
            make.setAccessible(true);
            Object obj=make.invoke(null,this,connectionCallback,Looper.getMainLooper());
            log("newCarApiConnection result="+obj);
            if(obj!=null){
                log("connection class="+obj.getClass().getName());
                dumpInterfaces(obj.getClass());
            }
            if(!(obj instanceof CarApiConnection)){
                log("!! returned object is NOT CarApiConnection");
                return;
            }
            connection=(CarApiConnection)obj;
            log("★ CarApiConnection interface match");
            connection.connect();
            log("connect() 호출 완료 - callback 대기");
        }catch(Throwable t){
            log("!! Car API 연결 ERROR="+err(t));
        }
    }

    private void onCarApiConnected(){
        try{
            if(connection==null){ log("connection=null"); return; }
            carApi=connection.getCarApi();
            log("getCarApi="+carApi);
            if(carApi==null) return;
            log("CarApi class="+carApi.getClass().getName());
            dumpInterfaces(carApi.getClass());
            log("isConnectedToCar="+carApi.isConnectedToCar());
            try{ log("connectionType="+carApi.getCarConnectionType()); }
            catch(Throwable t){ log("getCarConnectionType ERROR="+err(t)); }

            Object f=carApi.getCarManager("app_focus");
            log("getCarManager(app_focus)="+f);
            if(f instanceof CarMessageManager){
                focus=(CarMessageManager)f;
                log("★ CarMessageManager 획득");
                try{ focus.registerMessageListener(messageListener); log("focus listener 등록"); }
                catch(Throwable t){ log("focus listener ERROR="+err(t)); }
            }else if(f!=null){
                log("app_focus class="+f.getClass().getName());
                dumpInterfaces(f.getClass());
            }

            Object n=carApi.getCarManager("car_navigation_service");
            log("getCarManager(car_navigation_service)="+n);
            if(n instanceof CarNavigationStatusManager){
                nav=(CarNavigationStatusManager)n;
                log("★ CarNavigationStatusManager 획득 성공");
                log("★ 실제 HUD 송신 준비 완료");
            }else if(n!=null){
                log("navigation class="+n.getClass().getName());
                dumpInterfaces(n.getClass());
            }else{
                log("!! navigation manager=null");
            }
        }catch(Throwable t){
            log("!! onCarApiConnected ERROR="+err(t));
        }
    }

    private void requestNavigationFocus(){
        section("Navigation Focus");
        if(focus==null){
            log("focus manager 없음 → 1. Car API 연결 먼저");
            return;
        }
        try{
            boolean ok=focus.acquireCategory(1);
            log("acquireCategory(1)="+ok);
            if(ok){
                focusOwned=true;
                focus.sendIntegerMessage(1,0,1);
                log("★ navigation focus 활성 메시지 전송 (1,0,1)");
            }
        }catch(Throwable t){
            log("!! focus ERROR="+err(t));
        }
    }

    private void sendHudTest(){
        section("HUD 고정값 테스트");
        if(nav==null){
            log("navigation manager 없음 → 1. Car API 연결 먼저");
            return;
        }
        if(!focusOwned){
            log("focus 미획득 → 먼저 2. 포커스 획득");
            return;
        }
        try{
            boolean s=nav.sendNavigationStatus(1);
            log("sendNavigationStatus(ACTIVE=1)="+s);

            boolean t=nav.sendNavigationTurnEvent(
                    4,
                    "HUD TEST",
                    -1,
                    -1,
                    null,
                    2);
            log("sendNavigationTurnEvent(TURN=4, RIGHT=2)="+t);

            boolean d=nav.sendNavigationTurnDistanceEvent(
                    300,
                    30,
                    300000,
                    1);
            log("sendNavigationTurnDistanceEvent(300m,30s,meters)="+d);
            log("★ HUD 테스트 데이터 전송 완료");
        }catch(Throwable x){
            log("!! HUD TEST ERROR="+err(x));
        }
    }

    private void stopNavigationSafe(){
        section("HUD / Navigation 종료");
        if(nav!=null){
            try{
                boolean r=nav.sendNavigationStatus(2);
                log("sendNavigationStatus(INACTIVE=2)="+r);
            }catch(Throwable t){
                log("navigation stop ERROR="+err(t));
            }
        }
        if(focus!=null){
            try{ focus.sendIntegerMessage(1,0,0); log("focus inactive message (1,0,0)"); }
            catch(Throwable t){ log("focus inactive msg ERROR="+err(t)); }
            try{ focus.releaseCategory(1); log("releaseCategory(1)"); }
            catch(Throwable t){ log("releaseCategory ERROR="+err(t)); }
        }
        focusOwned=false;
    }

    private void disconnectSafe(){
        nav=null;
        carApi=null;
        if(focus!=null){
            try{ focus.unregisterMessageListener(); }catch(Throwable ignored){}
            try{ focus.releaseCategory(1); }catch(Throwable ignored){}
        }
        focus=null;
        focusOwned=false;
        if(connection!=null){
            try{ connection.disconnect(); }catch(Throwable t){ log("disconnect ERROR="+err(t)); }
        }
        connection=null;
    }

    private void dumpInterfaces(Class<?> c){
        try{
            Set<String> seen=new LinkedHashSet<>();
            Class<?> x=c;
            while(x!=null){
                for(Class<?> i:x.getInterfaces()){
                    if(seen.add(i.getName())) log(" interface="+i.getName()+" loader="+i.getClassLoader());
                }
                x=x.getSuperclass();
            }
        }catch(Throwable t){ log("dumpInterfaces ERROR="+err(t)); }
    }

    private String err(Throwable t){
        Throwable x=t; int n=0;
        while(x!=null && x.getCause()!=null && x.getCause()!=x && n++<12) x=x.getCause();
        if(x==null) return "null";
        return x.getClass().getName()+": "+String.valueOf(x.getMessage());
    }

    private void copyLog(){
        try{
            ClipboardManager cm=(ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
            cm.setPrimaryClip(ClipData.newPlainText("AI3 "+VERSION,logView.getText()));
            Toast.makeText(this,"로그 복사 완료",Toast.LENGTH_SHORT).show();
        }catch(Throwable t){ log("copy ERROR="+err(t)); }
    }

    private void section(String s){ log("\n=== "+s+" ==="); }
    private void log(String s){
        Log.d(TAG,s);
        if(logView==null) return;
        runOnUiThread(()->{
            logView.append(s+"\n");
            if(logScroll!=null) logScroll.post(()->logScroll.fullScroll(View.FOCUS_DOWN));
        });
    }
    private int dp(int n){ return Math.round(n*getResources().getDisplayMetrics().density); }
}
