# Naver Notification Probe v1.1

Small standalone Android test APK for the Naver Map (`com.nhn.android.nmap`) Samsung One UI ongoing-activity notification.

- Uses only Notification Listener permission. No network, location, overlay, accessibility, or vehicle privileges.
- Reads `android.ongoingActivityNoti.primaryInfo` and `android.ongoingActivityNoti.secondaryInfo`.
- Displays current instruction + normalized remaining distance (m), last notification time, notification count, and count of changes.
- Stores only value changes (direction or distance) in internal app-private log.
- Test first with Naver Map navigation on phone; then connect Android Auto and repeat. **Do not use the phone while driving.**
- For sharing, review the log for sensitive place names.
- Building: `gradle -p naver-probe :app:assembleDebug`.
- This is a distinct debug-signing run from v1.0: uninstall v1.0 before installing v1.1 if package signature conflicts occur.
- No HUD integration yet; this version verifies the data source.
