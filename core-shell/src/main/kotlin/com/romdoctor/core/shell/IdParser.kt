package com.romdoctor.core.shell

/** Parser output perintah `id`. */
object IdParser {
    private val UID0 = Regex("""(^|\s)uid=0(\(|\s|$)""")

    fun isUidZero(output: String): Boolean = UID0.containsMatchIn(output.trim())
}
