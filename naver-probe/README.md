# Naver Map overlay + notification probe v1.2
The notification listener still reads Naver Map turn/distance as before.
A separate **optional** AccessibilityService listens only to Naver Map events and saves visible text or contentDescription of Naver Map nodes, deduplicated and limited to the app's private storage. It never requests INTERNET permission.
Install via adb if trusted; debug signing may differ so uninstall old package first.
Enable Settings > Accessibility > Installed apps > "네이버지도 안내창 분석".
Start Naver Map navigation, press Home to show the floating small guidance window, then come back to this app and copy "안내창 기록".
If the overlay is a custom-drawn Surface rather than accessible text nodes, output will say "텍스트 노드 없음".
**Accessibility permissions are powerful**. Disable the service after the test.
Do not interact with the phone while driving.
