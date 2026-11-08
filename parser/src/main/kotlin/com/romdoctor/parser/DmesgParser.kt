package com.romdoctor.parser

data class DmesgLine(val level: Int?, val seconds: Double, val message: String)

/** Parser `dmesg`: `[  12.345678] pesan`, awalan `<6>` (dmesg -r) didukung. */
object DmesgParser {
    private val LINE = Regex("""^(?:<(\d+)>)?\[\s*(\d+\.\d+)\]\s?(.*)$""")

    fun parse(text: String): ParseResult<List<DmesgLine>> {
        val lines = mutableListOf<DmesgLine>()
        val unparsed = mutableListOf<String>()
        for (raw in text.lineSequence()) {
            if (raw.isBlank()) continue
            val m = LINE.matchEntire(raw)
            if (m == null) {
                unparsed += raw
                continue
            }
            lines += DmesgLine(
                level = m.groupValues[1].toIntOrNull(),
                seconds = m.groupValues[2].toDouble(),
                message = m.groupValues[3],
            )
        }
        return ParseResult(lines, unparsed)
    }
}
