package com.romdoctor.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Tes MEKANIK parser (kasus tepi format). Input di sini buatan tangan dan HANYA menguji
 * logika parser; bukan pengganti RealFixtureTest.
 */
class ParserMechanicsTest {
    @Test
    fun getpropMultilineAndEmptyValue() {
        val r = GetpropParser.parse("[a]: [x]\n[b]: [line1\nline2]\n[c]: []\noops\n")
        assertEquals("x", r.value["a"])
        assertEquals("line1\nline2", r.value["b"])
        assertEquals("", r.value["c"])
        assertEquals(listOf("oops"), r.unparsedLines)
    }

    @Test
    fun mountsOctalEscapeAndOptions() {
        val r = MountsParser.parse("/dev/block/dm-0 /data ext4 rw,seclabel,noatime 0 0\nsrc /a\\040b tmpfs rw 0 0\nrusak\n")
        assertEquals(2, r.value.size)
        assertEquals(listOf("rw", "seclabel", "noatime"), r.value[0].options)
        assertEquals("/a b", r.value[1].mountPoint)
        assertEquals(listOf("rusak"), r.unparsedLines)
    }

    @Test
    fun logcatMarkerLevelAndGarbage() {
        val text = "--------- beginning of main\n" +
            "11-08 22:11:05.123  1234  5678 E AndroidRuntime: FATAL EXCEPTION: main\n" +
            "bukan baris log\n"
        val r = LogcatParser.parse(text)
        assertEquals(1, r.value.size)
        assertEquals(LogLevel.ERROR, r.value[0].level)
        assertEquals("AndroidRuntime", r.value[0].tag)
        assertEquals("main", r.value[0].buffer)
        assertEquals(listOf("bukan baris log"), r.unparsedLines)
    }

    @Test
    fun pmPathMayContainEqualsSign() {
        val r = PmListParser.parse("package:/data/app/~~ab==/com.x-cd==/base.apk=com.x versionCode:12\npackage:com.y\n")
        assertEquals("com.x", r.value[0].name)
        assertEquals("/data/app/~~ab==/com.x-cd==/base.apk", r.value[0].apkPath)
        assertEquals(12L, r.value[0].versionCode)
        assertEquals("com.y", r.value[1].name)
        assertNull(r.value[1].apkPath)
    }

    @Test
    fun dmesgWithAndWithoutLevelPrefix() {
        val r = DmesgParser.parse("[    1.000001] Booting\n<6>[  12.5] hello\nxx\n")
        assertEquals(2, r.value.size)
        assertEquals(6, r.value[1].level)
        assertEquals(listOf("xx"), r.unparsedLines)
    }

    @Test
    fun getenforceUnknownIsNull() {
        assertNull(GetenforceParser.parse("Operation not permitted"))
        assertEquals(SelinuxMode.ENFORCING, GetenforceParser.parse("Enforcing\n"))
    }

    @Test
    fun lshalFindsHidlAndAidlTokens() {
        val text = "All binderized services (registered services through hwservicemanager)\n" +
            "R Interface                      Transport Arch  Thread Use PID Clients\n" +
            "  android.hardware.audio@6.0::IDevicesFactory/default hwbinder 64 0/1 1 543 1\n" +
            "  android.hardware.light.ILights/default hwbinder 64 0/1 1 600 1\n"
        val r = LshalParser.parse(text)
        assertEquals(2, r.value.size)
        assertEquals("android.hardware.audio@6.0::IDevicesFactory", r.value[0].fqName)
        assertEquals("default", r.value[0].instance)
        assertEquals(543, r.value[0].pid)
        assertEquals(1, r.unparsedLines.size)
    }

    @Test
    fun serviceListHeaderAndEntry() {
        val r = ServiceListParser.parse("Found 2 services:\n0\tactivity: [android.app.IActivityManager]\n1\tx: []\n")
        assertEquals(2, r.value.declaredCount)
        assertEquals(2, r.value.services.size)
        assertEquals("activity", r.value.services[0].name)
    }
}
