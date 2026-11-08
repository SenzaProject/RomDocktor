# Fixture perangkat nyata

Satu folder per perangkat: `testing/fixtures/<device>_a<android>/`.
Dibuat otomatis oleh `tools/capture_fixtures.sh` (jalankan di Termux, HP yang sudah root).

Berkas: getprop.txt, proc_mounts.txt, service_list.txt, lshal.txt, logcat.txt, dmesg.txt,
getenforce.txt, pm_system.txt, pm_third.txt, meta.txt.

PENTING: logcat/dmesg bisa memuat data pribadi. Skrip menyamarkan serial/IMEI/MAC dan email,
tetapi tetap periksa isinya sebelum di-push, dan gunakan repo PRIVATE.
Tes parser (RealFixtureTest) otomatis memakai semua folder di sini; tanpa fixture, tesnya di-skip.
