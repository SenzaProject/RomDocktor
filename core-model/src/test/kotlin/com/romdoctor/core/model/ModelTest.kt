package com.romdoctor.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ModelTest {
    @Test
    fun evidenceSnippetIsTrimmedAndLimited() {
        val e = Evidence.of("getprop", "  " + "x".repeat(2000) + "  ", 0)
        assertEquals(Evidence.MAX_SNIPPET, e.output.length)
    }

    @Test
    fun findingWithoutEvidenceIsRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            Finding("X-001", "t", Severity.LOW, emptyList())
        }
    }
}
