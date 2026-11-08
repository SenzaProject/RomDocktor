package com.romdoctor.core.model

/** Kemampuan yang diuji CapabilityDetector. Tiap item diuji dengan perintah/API nyata. */
enum class Capability(val label: String, val usedFor: String) {
    ROOT("Akses root (su)", "dmesg, lshal, logcat penuh, pstore, tombstone"),
    GETPROP("getprop", "Analyzer Prop (ro.build.*, ro.product.*, ro.vendor.*)"),
    GETENFORCE("getenforce", "Status SELinux (SEL-002)"),
    PROC_MOUNTS("/proc/mounts", "Analyzer Partisi dan fstab"),
    LOGCAT_FULL("logcat semua buffer", "Analyzer Log (HAL died, avc denied, crash)"),
    DMESG("dmesg", "Boot, AVB/verity, firmware, kernel panic"),
    LSHAL("lshal", "Analyzer HAL (HAL mati/tidak terdaftar)"),
    SERVICE_LIST("service list", "Daftar service (camera provider, dll)"),
    PSTORE("/sys/fs/pstore", "Log kernel panic sesi sebelumnya"),
    PACKAGE_MANAGER("PackageManager", "Analyzer App dan Feature"),
    SENSOR_MANAGER("SensorManager", "Analyzer Sensor"),
    BATTERY_MANAGER("BatteryManager", "Analyzer Daya"),
}

enum class OperationMode { ROOT, NON_ROOT }

data class CapabilityReport(
    val results: Map<Capability, CheckResult>,
    val mode: OperationMode,
    val detectedAtMillis: Long,
)
