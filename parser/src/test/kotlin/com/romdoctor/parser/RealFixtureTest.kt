package com.romdoctor.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Invarian terhadap output PERANGKAT NYATA. Tidak ada nilai yang ditulis tangan:
 * yang diuji adalah konsistensi parser dengan data asli (jumlah baris vs hasil, fakta yang
 * pasti ada di setiap Android).
 */
class RealFixtureTest {

    @Test
    fun getprop() = withFixtures("getprop.txt") { dev, text ->
        val r = GetpropParser.parse(text)
        val expected = text.lines().count { it.startsWith("[") && it.contains("]: [") }
        assertEquals("[$dev] jumlah prop", expected, r.value.size)
        assertTrue("[$dev] baris tak terparse: ${r.unparsedLines.take(3)}", r.unparsedLines.isEmpty())
        assertNotNull("[$dev] ro.build.version.sdk", r.value["ro.build.version.sdk"]?.toIntOrNull())
        assertTrue("[$dev] ro.build.fingerprint kosong", !r.value["ro.build.fingerprint"].isNullOrBlank())
    }

    @Test
    fun procMounts() = withFixtures("proc_mounts.txt") { dev, text ->
        val r = MountsParser.parse(text)
        assertTrue("[$dev] baris tak terparse: ${r.unparsedLines.take(3)}", r.unparsedLines.isEmpty())
        assertEquals("[$dev] jumlah entri", text.lines().count { it.isNotBlank() }, r.value.size)
        assertTrue("[$dev] tidak ada /data", r.value.any { it.mountPoint == "/data" })
        assertTrue("[$dev] tidak ada proc", r.value.any { it.fsType == "proc" && it.mountPoint == "/proc" })
    }

    @Test
    fun serviceList() = withFixtures("service_list.txt") { dev, text ->
        val r = ServiceListParser.parse(text)
        assertNotNull("[$dev] header 'Found N services' tidak ada", r.value.declaredCount)
        assertEquals("[$dev] jumlah service vs header", r.value.declaredCount, r.value.services.size)
        assertTrue("[$dev] baris tak terparse: ${r.unparsedLines.take(3)}", r.unparsedLines.isEmpty())
        assertTrue("[$dev] service 'activity' tidak ada", r.value.services.any { it.name == "activity" })
    }

    @Test
    fun lshal() = withFixtures("lshal.txt") { dev, text ->
        val r = LshalParser.parse(text)
        val expected = text.lines().count { LshalParser.INTERFACE.containsMatchIn(it) }
        assertEquals("[$dev] jumlah entri HAL", expected, r.value.size)
        assertTrue("[$dev] tidak ada HAL terbaca", r.value.isNotEmpty())
    }

    @Test
    fun logcat() = withFixtures("logcat.txt") { dev, text ->
        val r = LogcatParser.parse(text)
        val total = r.value.size + r.unparsedLines.size
        assertTrue("[$dev] logcat kosong", total > 0)
        val ratio = r.value.size.toDouble() / total
        assertTrue(
            "[$dev] hanya ${"%.0f".format(ratio * 100)}% baris terparse; contoh gagal: ${r.unparsedLines.take(3)}",
            ratio >= 0.9,
        )
    }

    @Test
    fun dmesg() = withFixtures("dmesg.txt") { dev, text ->
        val r = DmesgParser.parse(text)
        val total = r.value.size + r.unparsedLines.size
        assertTrue("[$dev] dmesg kosong", total > 0)
        val ratio = r.value.size.toDouble() / total
        assertTrue(
            "[$dev] hanya ${"%.0f".format(ratio * 100)}% baris terparse; contoh gagal: ${r.unparsedLines.take(3)}",
            ratio >= 0.9,
        )
    }

    @Test
    fun getenforce() = withFixtures("getenforce.txt") { dev, text ->
        assertNotNull("[$dev] output tidak dikenali: '$text'", GetenforceParser.parse(text))
    }

    @Test
    fun pmSystem() = withFixtures("pm_system.txt") { dev, text ->
        val r = PmListParser.parse(text)
        assertTrue("[$dev] baris tak terparse: ${r.unparsedLines.take(3)}", r.unparsedLines.isEmpty())
        assertTrue("[$dev] paket 'android' tidak ada", r.value.any { it.name == "android" })
    }

    @Test
    fun pmThirdParty() = withFixtures("pm_third.txt") { dev, text ->
        val r = PmListParser.parse(text)
        assertTrue("[$dev] baris tak terparse: ${r.unparsedLines.take(3)}", r.unparsedLines.isEmpty())
    }
}
