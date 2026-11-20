from pathlib import Path
root = Path("MyWebViewer_v1")
css_file = root / "app/src/main/assets/site-layout.css"
css = css_file.read_text(encoding="utf-8")
needle = "html.mwv-layout-active .banner-box,"
assert needle in css
css = css.replace(needle, needle + "\nhtml.mwv-layout-active.mwv-page-play #advertiseInPlayer,", 1)
css += """
/* v1.2: verified from user-exported playback HTML. */
html.mwv-layout-active.mwv-page-play #bofang_box {
  width:100% !important; max-width:100% !important;
  min-width:0 !important; box-sizing:border-box !important;
}
html.mwv-layout-active.mwv-page-play #bofang_box .MacPlayer,
html.mwv-layout-active.mwv-page-play #bofang_box .MacPlayer table,
html.mwv-layout-active.mwv-page-play #playleft {
  width:100% !important; max-width:100% !important;
  min-width:0 !important; box-sizing:border-box !important;
}
html.mwv-layout-active.mwv-page-play .play-select {
  width:100% !important; max-width:100% !important; box-sizing:border-box !important;
}
/* Original .utility-button remains between player and episode list. */
"""
css_file.write_text(css, encoding="utf-8")
js_file = root / "app/src/main/assets/site-layout.js"
js = js_file.read_text(encoding="utf-8")
start = js.index("    if (!isPlaying) return;")
end = js.index("\n  }\n  rearrange();", start)
js = js[:start] + """    /* Original DOM sequence: player / utility buttons / hidden banner / episode list.
       Do not move original elements to preserve player event handlers. */""" + js[end:]
js_file.write_text(js, encoding="utf-8")
gradle_file = root / "app/build.gradle.kts"
g = gradle_file.read_text(encoding="utf-8")
assert 'versionCode = 2' in g and 'versionName = "1.1"' in g
g = g.replace("versionCode = 2", "versionCode = 3").replace('versionName = "1.1"', 'versionName = "1.2"')
gradle_file.write_text(g, encoding="utf-8")
activity = root / "app/src/main/java/com/jooh/mywebviewer/MainActivity.java"
s = activity.read_text(encoding="utf-8").replace('setTitle("내 웹뷰 1.1")', 'setTitle("내 웹뷰 1.2")')
s = s.replace("재생 페이지 플레이어 구조는 HTML 확인 전까지 보수적으로 처리합니다.", "재생 화면의 플레이어 너비를 확대하며 원본 이전화 및 회차 버튼은 유지합니다.")
activity.write_text(s, encoding="utf-8")
settings = root / "app/src/main/assets/layout-settings.json"
s = settings.read_text(encoding="utf-8").replace('"playerOnPlaybackPage": []','"playerOnPlaybackPage": ["#bofang_box", ".MacPlayer", "#playleft"]')
settings.write_text(s, encoding="utf-8")
print("MyWebViewer 1.2 patches applied")
