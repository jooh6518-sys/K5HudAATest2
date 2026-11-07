# Signing policy

This is a public source repository. NEVER COMMIT A PRIVATE KEY OR PASSWORD.

GitHub Actions produces an UNSIGNED release APK. To upgrade versions without uninstalling, sign each release using the SAME developer signing keystore and alias, and increment versionCode. Keep the keystore securely backed up. The first transition from a temporary Gradle debug certificate to this developer certificate requires uninstalling the old application.

Version 1.3.1 has versionCode=6, packageName=com.example.navernotificationprobe.
