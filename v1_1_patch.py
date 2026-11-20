from pathlib import Path
root = Path('MyWebViewer_v1')
p = root/'app/src/main/java/com/jooh/mywebviewer/MainActivity.java'
s = p.read_text(encoding='utf-8')
def rep(a,b):
    global s
    if a not in s: raise RuntimeError('Missing anchor: '+a[:100])
    s=s.replace(a,b,1)
rep('import android.content.ActivityNotFoundException;', '''import android.content.ActivityNotFoundException;
import java.io.OutputStream;
import org.json.JSONArray;
import java.text.SimpleDateFormat;
import java.util.Date;''')
rep('    private static final int MENU_ABOUT = 9;', '''    private static final int MENU_ABOUT = 9;
    private static final int MENU_EXPORT_HTML = 10;
    private static final int REQUEST_HTML_SAVE = 203;
    private String pendingHtml;''')
rep('menu.add(0, MENU_LAYOUT, 4, "레이아웃 적용 준비: "', 'menu.add(0, MENU_LAYOUT, 4, "레이아웃 적용: "')
rep('menu.add(0, MENU_RESTORE, 5,', '''menu.add(0, MENU_EXPORT_HTML, 5, "현재 페이지 HTML 저장");
        menu.add(0, MENU_RESTORE, 6,''')
rep('menu.add(0, MENU_AWAKE, 6,', 'menu.add(0, MENU_AWAKE, 7,')
rep('menu.add(0, MENU_BROWSER, 7,', 'menu.add(0, MENU_BROWSER, 8,')
rep('menu.add(0, MENU_ABOUT, 8,', 'menu.add(0, MENU_ABOUT, 9,')
rep('                toast("레이아웃 적용 준비: " + (newLayout ? "켜짐" : "꺼짐"));', '''                if (!newLayout) webView.reload(); // restore any relocated nodes
                toast("레이아웃 적용: " + (newLayout ? "켜짐" : "꺼짐"));''')
rep('            case MENU_RESTORE:\n', '''            case MENU_EXPORT_HTML:
                exportCurrentPageHtml();
                return true;
            case MENU_RESTORE:
''')
rep('                        .setTitle("내 웹뷰 1.0")', '                        .setTitle("내 웹뷰 1.1")')
rep('                                "광고·추천영상 숨김과 회차 재배치 규칙은 실제 페이지 DOM 확인 후 적용할 예정이며 지금은 적용되지 않습니다.")', '''                                "광고·추천영상·푸터 숨김을 적용합니다. 재생 페이지 플레이어 구조는 HTML 확인 전까지 보수적으로 처리합니다.\\n\\n" +
                                "HTML 저장 기능은 민감정보를 최대한 제거한 현재 페이지 구조를 보관합니다.")''')
addition = '''    private void exportCurrentPageHtml() {
        if (webView == null || !isHttpsUrl(webView.getUrl())) {
            toast("먼저 페이지를 열어주세요.");
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("현재 페이지 HTML 저장")
                .setMessage("현재 페이지의 HTML 구조를 파일로 저장합니다. 링크 및 스크립트 등 민감할 수 있는 속성을 제거하지만 저장한 파일은 공유 전에 확인하세요. 외부 iframe 내부 내용은 포함하지 않습니다.")
                .setNegativeButton("취소", null)
                .setPositiveButton("추출", (d, which) -> {
                    try {
                        String script = readAsset("page-snapshot.js");
                        webView.evaluateJavascript(script, value -> {
                            try {
                                String json = new JSONArray("[" + value + "]").getString(0);
                                JSONObject data = new JSONObject(json);
                                if (data.has("error")) {
                                    toast("추출 실패: " + data.optString("error"));
                                    return;
                                }
                                pendingHtml = "<!-- Source: " + data.optString("url") + " -->\\n" + data.getString("html");
                                Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
                                intent.addCategory(Intent.CATEGORY_OPENABLE);
                                intent.setType("text/html");
                                String filename = "webview-page-" + new SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(new Date()) + ".html";
                                intent.putExtra(Intent.EXTRA_TITLE, filename);
                                startActivityForResult(intent, REQUEST_HTML_SAVE);
                            } catch (Exception e) {
                                toast("HTML 처리 오류가 발생했습니다.");
                            }
                        });
                    } catch (IOException e) {
                        toast("HTML 추출 파일을 불러오지 못했습니다.");
                    }
                }).show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_HTML_SAVE) return;
        if (resultCode != RESULT_OK || data == null || data.getData() == null || pendingHtml == null) {
            pendingHtml = null;
            return;
        }
        try (OutputStream stream = getContentResolver().openOutputStream(data.getData())) {
            if (stream == null) throw new IOException("No output stream");
            stream.write(pendingHtml.getBytes(StandardCharsets.UTF_8));
            stream.flush();
            toast("HTML 파일을 저장했습니다.");
        } catch (Exception e) {
            toast("HTML 저장에 실패했습니다.");
        } finally {
            pendingHtml = null;
        }
    }

'''
rep('    private String readAsset(String name) throws IOException {', addition+'    private String readAsset(String name) throws IOException {')
p.write_text(s,encoding='utf-8')
gfile=root/'app/build.gradle.kts'
g=gfile.read_text(encoding='utf-8')
assert 'versionCode = 1' in g and 'versionName = "1.0"' in g
g=g.replace('versionCode = 1','versionCode = 2').replace('versionName = "1.0"','versionName = "1.1"')
gfile.write_text(g,encoding='utf-8')
print('Android source updated to v1.1')
