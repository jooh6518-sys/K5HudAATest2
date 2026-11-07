#!/system/bin/sh
product="$(getprop ro.build.product)"
if [ "$product" != 'DL3HEV.KOR.0000.V039.001.260902' ]; then
  echo 'REFUSED: package for DL3HEV.KOR.0000.V039.001.260902 only.'
  echo "Found: $product"
  exit 30
fi
if [ "$(getprop ro.build.version.release)" != '4.4.2' ]; then
  echo 'REFUSED: requires Android 4.4.2'
  exit 31
fi
exit 0
