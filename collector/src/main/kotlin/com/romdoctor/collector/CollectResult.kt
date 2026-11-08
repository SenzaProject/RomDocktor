package com.romdoctor.collector

import com.romdoctor.core.model.Evidence

/** Output mentah satu perintah, lengkap dengan konteks eksekusi. */
data class RawCapture(
    val command: String,
    val asRoot: Boolean,
    val stdout: String,
    val stderr: String,
    val exitCode: Int?,
    val truncated: Boolean,
    val durationMs: Long,
) {
    val label: String get() = (if (asRoot) "su -c " else "") + command

    fun toEvidence(): Evidence = Evidence.of(label, stdout, exitCode)
}

/** Collector tidak pernah memalsukan data: gagal = Unavailable (alasan nyata) atau Failure. */
sealed interface CollectResult {
    data class Success(val raw: RawCapture) : CollectResult
    data class Unavailable(val reason: String) : CollectResult
    data class Failure(val throwable: Throwable) : CollectResult
}
