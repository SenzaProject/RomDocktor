package com.romdoctor.core.shell

/** Hanya binary di daftar ini yang boleh dijalankan ShellExecutor. */
object CommandWhitelist {
    val DEFAULT: Set<String> = setOf(
        "id", "getprop", "getenforce", "cat", "ls", "df", "mount",
        "dmesg", "logcat", "lshal", "service", "dumpsys", "pm",
    )
}
