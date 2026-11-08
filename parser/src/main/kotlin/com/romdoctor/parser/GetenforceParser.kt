package com.romdoctor.parser

enum class SelinuxMode { ENFORCING, PERMISSIVE, DISABLED }

object GetenforceParser {
    /** null jika output tidak dikenali (jangan ditebak). */
    fun parse(text: String): SelinuxMode? = when (text.trim()) {
        "Enforcing" -> SelinuxMode.ENFORCING
        "Permissive" -> SelinuxMode.PERMISSIVE
        "Disabled" -> SelinuxMode.DISABLED
        else -> null
    }
}
