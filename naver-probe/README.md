# Naver Map notification and overlay probe v1.3
- NotificationListener: turn and distance updates from Naver One UI notification.
- AccessibilityService: reacts to Naver Map events and scans the Naver Map windows every 1 second. No OCR.
- Includes each matching window's accessibility type and screen bounds so a small floating window can be distinguished from the full navigation screen.
- Only Naver Map package nodes are recorded. Internet and external data transmissions are not present.
- The "accessibility enabled" label is derived from Android settings every 500ms, independently of log copy actions.
- Does not capture on other apps without Naver accessibility windows, cannot guarantee ability to read second turn.
- Uninstall previous debug versions if signatures differ, then install, re-enable Notification Access and Accessibility.
- Stop accessibility service after testing to avoid battery overhead and sensitive permission usage.
- No modifications to NEMESIS / HUD / Android Auto.
