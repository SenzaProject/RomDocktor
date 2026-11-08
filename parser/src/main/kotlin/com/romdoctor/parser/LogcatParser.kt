package com.romdoctor.parser

enum class LogLevel(val char: Char) {
    VERBOSE('V'), DEBUG('D'), INFO('I'), WARN('W'), ERROR('E'), FATAL('F'), SILENT('S');

    companion object {
        fun fromChar(c: Char): LogLevel? = values().firstOrNull { it.char == c }
    }
}

data class LogLine(
    val timestamp: String,
    val pid: Int,
    val tid: Int,
    val level: LogLevel,
    val tag: String,
    val message: String,
    /** Buffer dari penanda `--------- beginning of X`, null jika belum ada penanda. */
    val buffer: String?,
)

/** Parser `logcat -v threadtime`. */
object LogcatParser {
    private val LINE = Regex(
        """^(\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3})\s+(\d+)\s+(\d+)\s+([VDIWEFS])\s+(.*?)\s*:\s?(.*)$""",
    )
    private val MARKER = Regex("""^-+ beginning of (\S+)\s*$""")

    fun parse(text: String): ParseResult<List<LogLine>> {
        var buffer: String? = null
        val lines = mutableListOf<LogLine>()
        val unparsed = mutableListOf<String>()
        for (raw in text.lineSequence()) {
            if (raw.isBlank()) continue
            val marker = MARKER.matchEntire(raw.trim())
            if (marker != null) {
                buffer = marker.groupValues[1]
                continue
            }
            val m = LINE.matchEntire(raw)
            val level = m?.let { LogLevel.fromChar(it.groupValues[4][0]) }
            if (m == null || level == null) {
                unparsed += raw
                continue
            }
            lines += LogLine(
                timestamp = m.groupValues[1],
                pid = m.groupValues[2].toInt(),
                tid = m.groupValues[3].toInt(),
                level = level,
                tag = m.groupValues[5],
                message = m.groupValues[6],
                buffer = buffer,
            )
        }
        return ParseResult(lines, unparsed)
    }
}
