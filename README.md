# ROM Doctor (RomDocktor)

Aplikasi Android untuk mendiagnosis ROM hasil port. Status: **Tahap 1 — Fondasi**.

## Isi Tahap 1
- `core-model`: Evidence, CheckResult (Ok / Problem / Unavailable / Error), Severity, Finding, Capability.
- `core-shell`: ShellExecutor (timeout, kill, stdout/stderr/exit code, whitelist, anti-injection), CapabilityDetector.
- `app`: layar yang menampilkan mode (ROOT / NON-ROOT) dan kemampuan hasil uji nyata, lengkap dengan bukti mentah.

## Build
Push ke `main`; GitHub Actions menjalankan unit test lalu `assembleDebug`.
APK ada di tab Actions → run terbaru → Artifacts → `RomDoctor-debug`.

## Izin logcat tanpa root
    adb shell pm grant com.romdoctor android.permission.READ_LOGS

## Batasan
- Tanpa root, dmesg/lshal/pstore umumnya tidak terbaca; aplikasi menandainya "Tidak dapat diperiksa", bukan "aman".
