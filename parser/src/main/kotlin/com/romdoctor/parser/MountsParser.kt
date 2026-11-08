package com.romdoctor.parser

data class MountEntry(
    val device: String,
    val mountPoint: String,
    val fsType: String,
    val options: List<String>,
    val dump: Int?,
    val pass: Int?,
)

/** Parser `/proc/mounts` (6 kolom; escape oktal `\040` dibuka). */
object MountsParser {
    private val WS = Regex("\\s+")
    private val OCTAL = Regex("""\\([0-7]{3})""")

    fun parse(text: String): ParseResult<List<MountEntry>> {
        val entries = mutableListOf<MountEntry>()
        val unparsed = mutableListOf<String>()
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            val t = line.trim().split(WS)
            if (t.size < 4) {
                unparsed += line
                continue
            }
            entries += MountEntry(
                device = unescape(t[0]),
                mountPoint = unescape(t[1]),
                fsType = t[2],
                options = t[3].split(','),
                dump = t.getOrNull(4)?.toIntOrNull(),
                pass = t.getOrNull(5)?.toIntOrNull(),
            )
        }
        return ParseResult(entries, unparsed)
    }

    private fun unescape(s: String): String =
        OCTAL.replace(s) { it.groupValues[1].toInt(8).toChar().toString() }
}
