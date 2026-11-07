# K5 Android Auto Method Trace v1.4
## Scope
2021 K5 DL3HEV, Android 4.4.2, V039.001.260902. APK is updated
from v1.1 using the SAME package ID and original developer signature key.
NEMESIS package ID k5.hud.probe, version 1.4.

The user's uploaded factory GPM ODEX and native receiver library were reviewed
locally and are NOT in this repository or distributable package.
Verified factory GPM symbols:
- com.google.android.projection.protocol.NavigationStatus.navigationNextTurnCallback(String,int,int,byte[],int,int)
- com.google.android.projection.protocol.NavigationStatus.navigationNextTurnDistanceCallback(int,int,int,int)
- com.google.android.projection.service.GalConnectionSession$4.onNextTurn(...)
- com.google.android.projection.sink.navigation.GpmNaviTBTManager.onNextTurn(String,int,int,int,int)
- com.google.android.projection.sink.navigation.GpmNaviTBTManager.onNextTurnDistance(int,int,int)

## Function
NEMESIS root service waits for the same manual AA-start request as the app.
It calls Android 4.4's built-in 'am profile start --sampling 1000'
on com.google.android.projection.sink for up to 90 seconds.
The trace contains observed method names, not argument values.
The service stops profiling, saves the binary trace in the app's
private files and waits idle. It never changes /system, GPM ODEX/APK
or the receiver .so. A separate read-only USB export retrieves the trace.

## Parked vehicle usage
1. Install the NEMESIS update without uninstalling the v1.1 app.
2. Connect Android Auto and start Naver Map guidance while parked.
3. Open updated app; check the parked-car checkbox and press
   "AA 메서드 추적 90초 시작".
4. Return to Naver Map; wait for 'FINISHED' or press stop.
5. Disconnect AA, run K5-AA-METHOD-TRACE-EXPORT USB utility, attach
   aa-method.trace + capture-status.txt to the chat.

## Interpretation / limits
- This is *sampling*, not a guaranteed record of all method calls. A very brief
  callback may not appear even when it executed.
- The method trace proves observed Java callbacks but DOES NOT capture input
  argument values or instrument native C++ callbacks.
- A crash, UI performance degradation or Android command rejection is possible
  on the aging head unit. Do not run while driving.
- If profiling is refused, status and diagnostic output are exported.
- The previous manual HUD test remains independent and unchanged.
