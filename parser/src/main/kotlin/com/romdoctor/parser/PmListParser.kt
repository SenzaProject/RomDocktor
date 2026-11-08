package com.romdoctor.parser

data class PackageEntry(val name: String, val apkPath: String?, val versionCode: Long?)

/** Parser `pm list packages [-f] [--show-versioncode]`. */
object PmListParser {
    private val LINE = Regex("""^package:(?:(.+)=)?([A-Za-z0-9_.]+)(?:\s+versionCode:(\d+))?$""")

    fun parse(text: String): ParseResult<List<PackageEntry>> {
        val entries = mutableListOf<PackageEntry>()
        val unparsed = mutableListOf<String>()
        for (raw in text.lineSequence()) {
            if (raw.isBlank()) continue
            val m = LINE.matchEntire(raw.trim())
            if (m == null) {
                unparsed += raw
                continue
            }
            entries += PackageEntry(
                name = m.groupValues[2],
                apkPath = m.groupValues[1].ifEmpty { null },
                versionCode = m.groupValues[3].toLongOrNull(),
            )
        }
        return ParseResult(entries, unparsed)
    }
}
