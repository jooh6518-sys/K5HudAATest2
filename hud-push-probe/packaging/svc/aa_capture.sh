#!/system/bin/sh
# Android Auto Dalvik method sampling, parked-car manual trigger only.
# Unmodified OEM GPM/APK/ODEX/SO. No hook map or binary writes.
BASE=/data/data/kr.k5.hudprobe/files
TARGET=com.google.android.projection.sink
TRACE=/data/local/tmp/k5-aa-method.trace

state() {
  echo "$1" > "$BASE/aa.status"
  chmod 644 "$BASE/aa.status" 2>/dev/null
}
stop_profile() {
  am profile stop "$TARGET" > "$BASE/aa-profile-stop.txt" 2>&1
  return $?
}
trap 'stop_profile >/dev/null 2>&1; exit 0' 15 2 1

while [ ! -d "$BASE" ]; do sleep 2; done
state "IDLE - press AA method trace button for 90 seconds"

while true; do
  if [ -f "$BASE/aa-request.start" ]; then
    rm -f "$BASE/aa-request.start" "$BASE/aa-request.stop"
    rm -f "$BASE/aa-method.trace" "$TRACE"
    ps > "$BASE/aa.processes" 2>/dev/null
    chmod 644 "$BASE/aa.processes" 2>/dev/null
    state "STARTING - method trace requested"

    # On KitKat: am profile start --sampling INTERVAL(us) PROCESS FILE
    # Sampling instead of instrument-every-method reduces load on old hardware.
    am profile start --sampling 1000 "$TARGET" "$TRACE" > "$BASE/aa-profile-start.txt" 2>&1
    CODE=$?
    chmod 644 "$BASE/aa-profile-start.txt" 2>/dev/null

    if [ "$CODE" -ne 0 ]; then
      state "ERROR - am profile start failed (see aa-profile-start.txt, code $CODE)"
      continue
    fi
    if grep -i 'PROFILE FAILED' "$BASE/aa-profile-start.txt" >/dev/null 2>&1; then
      state "ERROR - profile refused by Android (see aa-profile-start.txt)"
      continue
    fi
    if grep -i 'Error:' "$BASE/aa-profile-start.txt" >/dev/null 2>&1; then
      state "ERROR - profile rejected (see aa-profile-start.txt)"
      continue
    fi

    state "CAPTURING - 90s 1ms sampling; switch to Naver Map AA now"
    LEFT=90
    REASON="90-second timeout"
    while [ "$LEFT" -gt 0 ]; do
      if [ -f "$BASE/aa-request.stop" ]; then
        REASON="manual stop"
        break
      fi
      sleep 1
      LEFT=$((LEFT - 1))
    done

    stop_profile
    STOPCODE=$?
    sleep 2
    chmod 644 "$BASE/aa-profile-stop.txt" 2>/dev/null
    rm -f "$BASE/aa-request.stop"

    if [ -s "$TRACE" ]; then
      cp "$TRACE" "$BASE/aa-method.trace"
      COPYCODE=$?
      if [ "$COPYCODE" -eq 0 ]; then
        chmod 644 "$BASE/aa-method.trace" 2>/dev/null
        state "FINISHED - method trace saved ($REASON). Export to USB."
      else
        state "ERROR - trace generated but copy failed; see NEMESIS service log"
      fi
      rm -f "$TRACE"
    else
      state "ERROR - no trace file (stop code $STOPCODE); inspect am profile logs"
    fi
  fi
  sleep 1
done
