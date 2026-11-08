#!/data/data/com.termux/files/usr/bin/sh
# Ambil output ASLI perangkat sebagai fixture. Jalankan dari root repo:  sh tools/capture_fixtures.sh
set -u
DEV=$(getprop ro.product.device | tr -c 'A-Za-z0-9\n_-' '_')
REL=$(getprop ro.build.version.release | tr -c 'A-Za-z0-9\n_-' '_')
OUT="testing/fixtures/${DEV}_a${REL}"
mkdir -p "$OUT"

SED_EMAIL='s/[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\.[A-Za-z0-9-]+)*\.(com|net|org|id|co|io|edu)\b/REDACTED_EMAIL/g'

# getprop (tanpa root) + samarkan serial/IMEI/MAC dll
getprop | sed -E 's/^\[([^]]*(serial|imei|meid|mac|address|android_id|iccid|imsi)[^]]*)\]: \[.*\]$/[\1]: [REDACTED]/I' > "$OUT/getprop.txt"
echo "OK    getprop.txt ($(wc -l < "$OUT/getprop.txt") baris)"

capture() {
  name=$1; shift
  tmp="$OUT/.$name.tmp"; err="$OUT/.$name.err"
  if su -c "$*" > "$tmp" 2> "$err"; then
    sed -E "$SED_EMAIL" "$tmp" > "$OUT/$name"
    echo "OK    $name ($(wc -l < "$OUT/$name") baris)"
  else
    echo "GAGAL $name -> $(head -c 150 "$err")"
  fi
  rm -f "$tmp" "$err"
}

capture proc_mounts.txt   cat /proc/mounts
capture service_list.txt  service list
capture lshal.txt         lshal
capture getenforce.txt    getenforce
capture dmesg.txt         dmesg
capture logcat.txt        logcat -b all -d -v threadtime -t 5000
capture pm_system.txt     pm list packages -f -s
capture pm_third.txt      pm list packages -f -3

{
  echo "tanggal: $(date)"
  echo "model: $(getprop ro.product.model)"
  echo "android: $(getprop ro.build.version.release) (sdk $(getprop ro.build.version.sdk))"
  echo "fingerprint: $(getprop ro.build.fingerprint)"
} > "$OUT/meta.txt"

echo
echo "Selesai -> $OUT"
echo "Periksa isinya (data pribadi!) sebelum git add/push."
