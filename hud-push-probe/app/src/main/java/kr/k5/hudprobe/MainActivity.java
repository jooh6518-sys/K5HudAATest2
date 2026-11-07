package kr.k5.hudprobe;

import android.app.*;
import android.content.*;
import android.database.Cursor;
import android.net.Uri;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.lang.reflect.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.io.*;
import java.util.ArrayDeque;

public final class MainActivity extends Activity {
  private static final int TURN_RIGHT = 0x43;
  private final Handler ui = new Handler(Looper.getMainLooper());
  private TextView output;
  private TextView aaState;
  private CheckBox aaPark;
  private boolean resumed=false;
  private final Runnable aaStatusLoop = new Runnable() {
    @Override public void run() {
      if (!resumed) return;
      String value=readSmall(new File(getFilesDir(), "aa.status"), 260);
      if (aaState!=null) aaState.setText("AA 수집기 상태: " + (value.isEmpty()?"서비스 대기/미설치":value));
      ui.postDelayed(this, 1500);
    }
  };
  private CheckBox confirm;
  private boolean active = false;
  private boolean busy = false;
  private final StringBuilder history = new StringBuilder();
  private final Runnable timeout = new Runnable() {
    @Override public void run() { clear("5초 자동 해제"); }
  };

  @Override public void onCreate(Bundle b) {
    super.onCreate(b);
    ScrollView sc = new ScrollView(this);
    LinearLayout body = new LinearLayout(this);
    body.setOrientation(LinearLayout.VERTICAL);
    body.setPadding(20,20,20,20);
    body.setBackgroundColor(Color.rgb(246,247,249));
    sc.addView(body);
    setContentView(sc);
    body.addView(text("K5 Android Auto 메서드 실행 추적 v1.4",22));
    body.addView(text("DL3HEV.KOR.0000.V039.001.260902 전용\nNEMESIS · Android 4.4.2",14));
    body.addView(text("반드시 정차(P) + 주차브레이크. 순정 길안내 종료. Android Auto 연결 해제. 주행 중 사용 금지.",16));
    body.addView(text("Android Auto 네이버지도 수신 분석",19));
    body.addView(text("Android Auto 수신 프로세스의 메서드 실행을 90초간 샘플링합니다. 네이버지도 안내 중 콜백 실행 여부를 확인합니다. 순정 GPM 바이너리는 변경하지 않습니다.",14));
    aaState=text("AA 수집기 연결 상태 확인 중",14);
    body.addView(aaState);
    aaPark=new CheckBox(this);
    aaPark.setText("차량을 P 기어 + 주차브레이크로 안전하게 정차했습니다.");
    aaPark.setTextSize(15);
    body.addView(aaPark);
    Button aaStart=button("AA 메서드 추적 90초 시작");
    aaStart.setOnClickListener(new View.OnClickListener(){
      @Override public void onClick(View v){requestAaCapture();}
    });
    body.addView(aaStart);
    Button aaStop=button("AA 메서드 추적 중지");
    aaStop.setOnClickListener(new View.OnClickListener(){
      @Override public void onClick(View v){writeRequest("aa-request.stop");add("AA 수집 중지 요청");}
    });
    body.addView(aaStop);
    Button aaCopy=button("추적 상태 복사");
    aaCopy.setOnClickListener(new View.OnClickListener(){
      @Override public void onClick(View v){
        String logs=aaSummary();
        android.content.ClipboardManager cm=(android.content.ClipboardManager)getSystemService(CLIPBOARD_SERVICE);
        if(cm!=null) {
          cm.setPrimaryClip(ClipData.newPlainText("K5 Android Auto navigation probe",logs));
          add("추적 상태가 복사됐습니다. 실제 메서드 기록은 USB 내보내기 도구로 저장합니다.");
        }
      }
    });
    body.addView(aaCopy);
    body.addView(text("HUD 수동 출력 기능 (v1.0과 동일)",18));
    Button check=button("① HUD API 검사 (읽기 전용)");
    body.addView(check);
    check.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) { inspect(); }
    });
    confirm = new CheckBox(this);
    confirm.setText("P 기어·주차브레이크·순정 길안내 종료를 확인했습니다.");
    confirm.setTextSize(15);
    body.addView(confirm);
    Button send=button("② 우회전 300m 시험 출력 (5초)");
    body.addView(send);
    send.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) { requestSend(); }
    });
    Button stop=button("③ 시험 안내 즉시 지우기");
    body.addView(stop);
    stop.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) { clear("수동 중지"); }
    });
    output=text("",13);
    output.setTextIsSelectable(true);
    output.setBackgroundColor(Color.WHITE);
    body.addView(output);
    add("AA 수신 로그 분석 준비 완료. HUD 직접 출력은 누르지 않으면 수행하지 않습니다.");
  }

  @Override protected void onResume(){
    super.onResume();
    resumed=true;
    ui.removeCallbacks(aaStatusLoop);
    ui.post(aaStatusLoop);
  }
  @Override protected void onPause(){
    resumed=false;
    ui.removeCallbacks(aaStatusLoop);
    super.onPause();
  }
  private void writeRequest(String name) {
    try {
      File f=new File(getFilesDir(),name);
      FileOutputStream os=new FileOutputStream(f);
      os.write((String.valueOf(System.currentTimeMillis())+"\n").getBytes("UTF-8"));
      os.close();
    } catch(Exception e){ add("파일 요청 실패: "+e.getClass().getSimpleName()); }
  }
  private void requestAaCapture() {
    if(!aaPark.isChecked()){add("차단: P 기어·주차브레이크 확인 필요.");return;}
    if(parkState()==0){add("차단: 기어가 P가 아닙니다.");return;}
    String status=readSmall(new File(getFilesDir(),"aa.status"),180);
    if(status.startsWith("CAPTURING")){add("이미 메서드 실행 추적 중입니다.");return;}
    new File(getFilesDir(),"aa-request.stop").delete();
    writeRequest("aa-request.start");
    add("메서드 추적 시작 요청. Android Auto에서 네이버지도 안내를 실행하고 90초 뒤 USB 도구로 기록을 내보내세요.");
  }
  private static String readSmall(File f,int max) {
    if(!f.isFile())return "";
    StringBuilder result=new StringBuilder();
    try {
      BufferedReader b=new BufferedReader(new InputStreamReader(new FileInputStream(f),"UTF-8"));
      String line; while((line=b.readLine())!=null&&result.length()<max)
        result.append(line).append(" ");
      b.close();
    }catch(Exception e) {return "읽기 실패:"+e.getClass().getSimpleName();}
    return result.toString().trim();
  }
  private String aaSummary() {
    StringBuilder out=new StringBuilder();
    out.append("K5 AA METHOD TRACE PROBE v1.4\n");
    out.append("Firmware: DL3HEV.KOR.0000.V039.001.260902\n");
    out.append("Capture: ").append(readSmall(new File(getFilesDir(),"aa.status"),360)).append("\n");
    out.append("Head unit GPM process:\n");
    File ps=new File(getFilesDir(),"aa.processes");
    try{
      BufferedReader br=new BufferedReader(new InputStreamReader(new FileInputStream(ps),"UTF-8"));
      String line;int n=0;while((line=br.readLine())!=null&&n<12){
        if(line.contains("com.google.android.projection.sink")||
           line.contains("com.telechips.android.gpm")||line.contains("hmgapi")){
          out.append(line).append("\n");n++;
        }
      }br.close();
    }catch(Exception e){out.append("(process snapshot unavailable)\n");}
    File trace=new File(getFilesDir(),"aa-method.trace");
    out.append("Method trace: ");
    if (trace.isFile()) {
      out.append("saved, ").append(trace.length()).append(" bytes\n");
    } else {
      out.append("not yet saved (capture incomplete or profile failed)\n");
    }
    out.append("Note: the full method trace is a binary file. Use the USB export tool.\n");
    return out.toString();
  }

  private TextView text(String label,int sp) {
    TextView t=new TextView(this);
    t.setText(label); t.setTextSize(sp);
    t.setTextColor(Color.rgb(35,40,46));
    t.setPadding(8,8,8,8);
    return t;
  }
  private Button button(String label) {
    Button b=new Button(this); b.setText(label); b.setTextSize(15); b.setAllCaps(false);
    return b;
  }
  private void add(String msg) {
    String time=new SimpleDateFormat("HH:mm:ss",Locale.KOREA).format(new Date());
    history.append(time).append(" ").append(msg).append("\n");
    if (history.length()>11000) history.delete(0,history.length()-8000);
    if (output!=null) output.setText(history.toString());
  }
  private void post(final String s) {
    runOnUiThread(new Runnable() { @Override public void run() { add(s); } });
  }
  private static String error(Throwable e) {
    if(e instanceof InvocationTargetException && ((InvocationTargetException)e).getCause()!=null)
      e=((InvocationTargetException)e).getCause();
    return e.getClass().getSimpleName()+": "+e.getMessage();
  }
  private Class<?> navClass() throws Exception {
    return Class.forName("com.hkmc.cluster.NavigationCluster");
  }
  private void inspect() {
    new Thread(new Runnable() {
      @Override public void run() {
        try {
          Class<?> cls=navClass();
          cls.getConstructor(Context.class);
          cls.getMethod("updateTBTInfo",int.class,int.class,float.class,float.class,
            int.class,int.class,int.class,int.class,int.class);
          cls.getMethod("updateTPTurnListNew",byte[].class);
          post("HUD API 발견: 클래스/메서드 시그니처 일치. 호출 권한은 아직 미검증.");
        } catch(Throwable e) { post("HUD API 검사 실패: "+error(e)); }
        post("기어 P="+parkState()+" (1=P, -1=조회 불가)");
        post("순정 안내="+stockGuidance()+" (0=종료, 1=진행, -1=조회 불가)");
      }
    },"hud-api-inspect").start();
  }
  private int parkState() {
    try {
      Class<?> names=Class.forName("com.hkmc.Service");
      String service=(String)names.getField("SYSTEM_SERVICE").get(null);
      Class<?> sm=Class.forName("android.os.ServiceManager");
      IBinder binder=(IBinder)sm.getMethod("getService",String.class).invoke(null,service);
      if(binder==null)return -1;
      Class<?> stub=Class.forName("com.hkmc.micom.ISystemService$Stub");
      Object iface=stub.getMethod("asInterface",IBinder.class).invoke(null,binder);
      Object res=iface.getClass().getMethod("getGearPStatus").invoke(iface);
      return ((Number)res).intValue();
    } catch(Throwable e) {return -1;}
  }
  private int stockGuidance() {
    Cursor c=null;
    try {
      c=getContentResolver().query(Uri.parse("content://com.mnsoft.navi/State_Table"),
        new String[]{"State_Destination"},null,null,null);
      if(c!=null&&c.moveToFirst()) {
        int idx=c.getColumnIndex("State_Destination");
        if(idx>=0)return c.getInt(idx);
      }
    } catch(Throwable e) {} finally {if(c!=null)c.close();}
    return -1;
  }
  private void requestSend() {
    if(!confirm.isChecked()){add("차단: 주차 상태 확인란을 선택하세요.");return;}
    if(busy||active){add("차단: 다른 송신 또는 시험 표시가 진행 중.");return;}
    final int p=parkState();
    final int n=stockGuidance();
    add("안전 확인: P="+p+" 순정 안내="+n);
    if(p>=0&&p!=1){add("차단: 기어 P 아님.");return;}
    if(n==1){add("차단: 순정 내비 길안내 진행 중.");return;}
    new AlertDialog.Builder(this)
      .setTitle("우회전 300m HUD 시험")
      .setMessage("정차(P), 주차브레이크, 순정 길안내 종료를 확인했습니까?\n\n시험 안내를 5초간 전송하고 지웁니다. API는 아직 실차 검증되지 않았습니다.")
      .setNegativeButton("취소",null)
      .setPositiveButton("전송",new DialogInterface.OnClickListener() {
        @Override public void onClick(DialogInterface d,int w) {send();}
      }).show();
  }
  private static byte[] turnFrame(int icon) {
    byte[] frame=new byte[87];
    frame[0]=(byte)0xf2;frame[1]=3;frame[2]=(byte)icon;
    String word="HUD TEST";
    for(int i=0;i<word.length();i++){
      char c=word.charAt(i);frame[5+2*i]=(byte)c;frame[6+2*i]=(byte)(c>>8);
    }
    return frame;
  }
  private void write(boolean reset) throws Exception {
    Class<?> c=navClass();
    Object receiver=c.getConstructor(Context.class).newInstance(getApplicationContext());
    Method tbt=c.getMethod("updateTBTInfo",int.class,int.class,float.class,float.class,
      int.class,int.class,int.class,int.class,int.class);
    tbt.invoke(receiver,reset?0:TURN_RIGHT,0,0f,reset?0f:300f,
      reset?-3:0,0,0,0,0);
    c.getMethod("updateTPTurnListNew",byte[].class)
      .invoke(receiver,(Object)(reset?new byte[]{(byte)0xf2,0}:turnFrame(TURN_RIGHT)));
    try {
      Class<?> cn=Class.forName("com.hkmc.cluster.NavigationCluster$CrossroadName");
      Object name=cn.getConstructor().newInstance();
      cn.getField("name").set(name,reset?"":"HUD TEST");
      c.getMethod("updateTPCrossroadName",cn).invoke(receiver,name);
    } catch(Throwable optional) {post("도로명 선택 기능: "+error(optional));}
  }
  private void send(){
    if(busy||active)return;
    busy=true;active=true;
    ui.removeCallbacks(timeout);
    ui.postDelayed(timeout,5000);
    add("전송 시도: icon 0x43, 거리 300m. 5초 뒤 초기화 예정.");
    new Thread(new Runnable(){
      @Override public void run(){
        try {write(false);post("출력 API 호출 완료. 실제 HUD 표시는 육안으로 확인.");}
        catch(Throwable e){post("출력 예외: "+error(e));}
        finally {busy=false;}
      }
    },"hud-test-write").start();
  }
  private void clear(final String source){
    if(!active&&!busy){add(source+": 앱에서 생성한 시험 안내 없음.");return;}
    ui.removeCallbacks(timeout);
    new Thread(new Runnable(){
      @Override public void run(){
        try {write(true);post(source+": 표시 해제 API 호출.");}
        catch(Throwable e){post(source+": 해제 예외: "+error(e));}
        finally {active=false;busy=false;}
      }
    },"hud-test-clear").start();
  }
  @Override protected void onDestroy(){
    ui.removeCallbacks(timeout);
    if(active||busy)clear("앱 종료");
    super.onDestroy();
  }
}
