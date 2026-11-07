# Naver Notification Probe (debug test APK)

Standalone Android application reading ONLY `com.nhn.android.nmap` notifications after the user grants Notification Access. Does not modify vehicle firmware, NEMESIS, Leafia Plus, or Android Auto.

- No INTERNET, location, overlay, or accessibility permissions.
- Keeps logs in app-private storage; use Copy button to inspect/share after removing location information.
- Build: from the `naver-probe` directory run `gradle :app:assembleDebug`.
- Install: enable notification access for **네이버지도 알림 분석**, start Naver Map navigation, return to this app.
- Expected fields: `android.title`, `android.text`, `android.bigText`, any text extras and `since_previous_posted`.
- Android Auto connection and actual device behavior must still be validated.
