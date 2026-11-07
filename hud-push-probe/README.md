# K5 AA Nav Log Probe v1.1
K5 DL3HEV V039.001.260902; Android 4.4.2; NEMESIS 1.16.7.

The verified v1.0 HUD test (manual right turn 300m, automatic clearing at 5s) remains unchanged.
v1.1 adds an optional **90-second read-only Android Auto receiver log capture**.
NEMESIS starts one idle root service. It does not record anything until the in-app
"AA 로그 90초 수집 시작" button is clicked. The service stores capped rolling
Android logcat files only inside the probe APK's private data folder; the app
copies only matching GPM/NavigationStatus/projection lines to clipboard.
No Internet permission, no binary patch, no maps and no GPM code injection.

## Test
1. Park, parking brake, stock navigation guidance off. Connect Android Auto.
2. Open NEMESIS Manager -> My Packages -> K5 HUD + AA Navigation Probe -> Launch.
3. Tick P gear / parking brake. Tap "AA 로그 90초 수집 시작".
4. Start Naver Map guidance inside Android Auto, ideally change route direction while
   vehicle remains parked. (Do not interact with device while moving.)
5. After 90 seconds or manual stop, open probe and tap "AA 관련 로그 복사".
6. Inspect for personal road names and copy the curated log to the chat.

## Interpreting results
Presence of native GPM/navigation log lines proves only that logs exist, not that the
user's Naver Map turn payload is available. NO_MATCH is NOT proof that Android Auto
has no data: vendor release builds may omit all callback logs. A targeted,
car-firmware-specific instrumentation/hook may then be necessary.

No other NEMESIS packages and no /system files are changed. The service monitors
control files and logs only when requested; remove this test package via NEMESIS Manager
when finished.
