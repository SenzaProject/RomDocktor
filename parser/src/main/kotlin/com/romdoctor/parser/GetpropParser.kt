package com.romdoctor.parser

/** Parser output `getprop`: baris `[key]: [value]`, nilai multi-baris didukung. */
object GetpropParser {
    private val SINGLE = Regex("""^\[(.+?)\]: \[(.*)\]$""")
    private val START = Regex("""^\[(.+?)\]: \[(.*)$""")

    fun parse(text: String): ParseResult<Map<String, String>> {
        val props = LinkedHashMap<String, String>()
        val unparsed = mutableListOf<String>()
        var pendingKey: String? = null
        val pending = StringBuilder()

        for (line in text.lineSequence()) {
            val key = pendingKey
            if (key != null) {
                if (line.endsWith("]")) {
                    pending.append('\n').append(line.dropLast(1))
                    props[key] = pending.toString()
                    pendingKey = null
                    pending.clear()
                } else {
                    pending.append('\n').append(line)
                }
                continue
            }
            if (line.isBlank()) continue
            val single = SINGLE.matchEntire(line)
            if (single != null) {
                props[single.groupValues[1]] = single.groupValues[2]
                continue
            }
            val start = START.matchEntire(line)
            if (start != null) {
                pendingKey = start.groupValues[1]
                pending.clear()
                pending.append(start.groupValues[2])
                continue
            }
            unparsed += line
        }
        val leftover = pendingKey
        if (leftover != null) unparsed += "[$leftover]: [$pending"
        return ParseResult(props, unparsed)
    }
}
