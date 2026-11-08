package com.romdoctor.core.model

/** Bukti mentah dari pemeriksaan: perintah yang dijalankan + cuplikan output asli. */
data class Evidence(
    val command: String,
    val output: String,
    val exitCode: Int? = null,
) {
    companion object {
        const val MAX_SNIPPET = 600

        fun of(command: String, rawOutput: String, exitCode: Int?): Evidence =
            Evidence(command, rawOutput.trim().take(MAX_SNIPPET), exitCode)
    }
}
