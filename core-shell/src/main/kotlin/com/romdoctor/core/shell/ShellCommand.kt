package com.romdoctor.core.shell

/**
 * Perintah dengan argumen TERPISAH (bukan string shell). Tanpa root: argumen dikirim
 * langsung ke ProcessBuilder (tidak ada interpretasi shell). Dengan root: tiap argumen
 * di-single-quote sebelum diberikan ke `su -c`.
 */
class ShellCommand(val binary: String, val args: List<String> = emptyList()) {
    init {
        require(BINARY_REGEX.matches(binary)) { "Nama binary tidak valid: $binary" }
        require(args.none { it.contains('\u0000') }) { "Argumen tidak boleh mengandung NUL" }
    }

    fun toArgv(): List<String> = listOf(binary) + args

    fun toRootScript(): String = (listOf(binary) + args.map(::quote)).joinToString(" ")

    fun display(): String = toArgv().joinToString(" ")

    companion object {
        private val BINARY_REGEX = Regex("^[A-Za-z0-9_.-]+$")

        fun quote(arg: String): String = "'" + arg.replace("'", "'\\''") + "'"
    }
}

fun cmd(binary: String, vararg args: String): ShellCommand = ShellCommand(binary, args.toList())
