package com.romdoctor.parser

/**
 * Hasil parsing. [unparsedLines] berisi baris yang tidak dikenali parser, supaya tidak ada
 * data yang diam-diam hilang (anti-fake).
 */
data class ParseResult<out T>(
    val value: T,
    val unparsedLines: List<String> = emptyList(),
)
