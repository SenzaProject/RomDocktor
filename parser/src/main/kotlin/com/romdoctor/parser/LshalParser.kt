package com.romdoctor.parser

data class HalEntry(
    /** HIDL: `android.hardware.audio@6.0::IDevicesFactory`; AIDL: `android.hardware.light.ILights`. */
    val fqName: String,
    val instance: String,
    /** Teks di kiri nama interface (flag kolom R/DM/dll), apa adanya. */
    val flags: String,
    val transport: String?,
    val pid: Int?,
    /** Judul seksi terakhir ("All binderized services ..."). */
    val section: String?,
    val rawLine: String,
)

/**
 * Parser `lshal` (tabel default). Baris dikenali dari token interface (HIDL `pkg@x.y::IFoo/inst`
 * atau AIDL `pkg.IFoo/inst`). Baris tanpa interface (header, legenda) masuk unparsedLines.
 */
object LshalParser {
    private const val HIDL = """[A-Za-z0-9_.]+@\d+\.\d+::[A-Za-z0-9_]+/[A-Za-z0-9_.\-]+"""
    private const val AIDL = """[A-Za-z][A-Za-z0-9_.]*\.I[A-Za-z0-9_]+/[A-Za-z0-9_.\-]+"""
    val INTERFACE = Regex("($HIDL)|($AIDL)")
    private val WS = Regex("\\s+")

    fun parse(text: String): ParseResult<List<HalEntry>> {
        var section: String? = null
        val entries = mutableListOf<HalEntry>()
        val unparsed = mutableListOf<String>()
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            val trimmed = line.trim()
            if (trimmed.startsWith("All ")) {
                section = trimmed
                continue
            }
            val m = INTERFACE.find(line)
            if (m == null) {
                unparsed += line
                continue
            }
            val token = m.value
            val slash = token.indexOf('/')
            val rest = line.substring(m.range.last + 1).trim().split(WS).filter { it.isNotEmpty() }
            val pid = if (rest.size >= 5 && rest[2].contains('/')) rest[4].toIntOrNull() else null
            entries += HalEntry(
                fqName = token.substring(0, slash),
                instance = token.substring(slash + 1),
                flags = line.substring(0, m.range.first).trim(),
                transport = rest.getOrNull(0),
                pid = pid,
                section = section,
                rawLine = line,
            )
        }
        return ParseResult(entries, unparsed)
    }
}
