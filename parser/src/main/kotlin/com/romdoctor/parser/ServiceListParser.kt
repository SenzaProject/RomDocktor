package com.romdoctor.parser

data class ServiceEntry(val index: Int, val name: String, val descriptor: String)

data class ServiceList(val declaredCount: Int?, val services: List<ServiceEntry>)

/** Parser `service list`: header `Found N services:` lalu `idx<TAB>name: [descriptor]`. */
object ServiceListParser {
    private val HEADER = Regex("""^Found (\d+) services?:$""")
    private val ENTRY = Regex("""^(\d+)\s+(\S+): \[(.*)\]$""")

    fun parse(text: String): ParseResult<ServiceList> {
        var declared: Int? = null
        val services = mutableListOf<ServiceEntry>()
        val unparsed = mutableListOf<String>()
        for (line in text.lineSequence()) {
            if (line.isBlank()) continue
            val h = HEADER.matchEntire(line.trim())
            if (h != null) {
                declared = h.groupValues[1].toInt()
                continue
            }
            val e = ENTRY.matchEntire(line.trim())
            if (e != null) {
                services += ServiceEntry(e.groupValues[1].toInt(), e.groupValues[2], e.groupValues[3])
            } else {
                unparsed += line
            }
        }
        return ParseResult(ServiceList(declared, services), unparsed)
    }
}
