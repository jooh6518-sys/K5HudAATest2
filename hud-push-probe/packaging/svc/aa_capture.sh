#!/system/bin/sh
# K5 AA navigation-status log collector.
# Root service: reads only Android logcat and package process list when the user
# presses the capture button. Nothing is transmitted or changed in /system.
BASE=/data/data/kr.k5.hudprobe/files

write_state() {
  echo "$1" > "$BASE/aa.status"
  chmod 644 "$BASE/aa.status" 2>/dev/null
}
while [ ! -d "$BASE" ]; do sleep 2; done
write_state "IDLE - ready for 90s manual AA capture"

while true; do
  if [ -f "$BASE/aa-request.start" ]; then
    rm -f "$BASE/aa-request.start" "$BASE/aa-request.stop"
    rm -f "$BASE/aa-capture.log" "$BASE/aa-capture.log.1" "$BASE/aa-capture.log.2"
    ps > "$BASE/aa.processes" 2>/dev/null
    chmod 644 "$BASE/aa.processes" 2>/dev/null
    write_state "CAPTURING - started $(date '+%m-%d %H:%M:%S') (90s max)"

    # -f/-r/-n are supported by legacy Android logcat. Rotate raw data at
    # 256 KiB per file so the recorder cannot fill the head unit.
    logcat -v threadtime -b main -b system -f "$BASE/aa-capture.log" -r 256 -n 2 &
    CAP_PID=$!
    sleep 2
    if ! kill -0 "$CAP_PID" 2>/dev/null; then
      # Some vendor builds disallow combined buffers; main buffer fallback.
      logcat -v time -b main -f "$BASE/aa-capture.log" -r 256 -n 2 &
      CAP_PID=$!
      sleep 2
      if ! kill -0 "$CAP_PID" 2>/dev/null; then
        write_state "ERROR - logcat capture not supported; check NEMESIS service log"
        sleep 1
        continue
      fi
    fi

    SECONDS_LEFT=90
    REASON="90 seconds reached"
    while [ "$SECONDS_LEFT" -gt 0 ]; do
      if [ -f "$BASE/aa-request.stop" ]; then
        REASON="manually stopped"
        break
      fi
      sleep 1
      SECONDS_LEFT=$((SECONDS_LEFT-1))
    done
    kill "$CAP_PID" 2>/dev/null
    wait "$CAP_PID" 2>/dev/null
    chmod 644 "$BASE/aa-capture.log" "$BASE/aa-capture.log.1" "$BASE/aa-capture.log.2" 2>/dev/null
    rm -f "$BASE/aa-request.stop"
    write_state "FINISHED - $REASON at $(date '+%m-%d %H:%M:%S')"
  fi
  sleep 1
done
