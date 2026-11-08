package com.romdoctor.core.shell

data class ShellResult(
    /** null jika proses tidak jalan atau di-kill karena timeout. */
    val exitCode: Int?,
    val stdout: String,
    val stderr: String,
    val timedOut: Boolean,
    val startError: String?,
    val durationMs: Long,
    val truncated: Boolean = false,
) {
    val ok: Boolean get() = startError == null && !timedOut && exitCode == 0
}
