package com.romdoctor.parser

import org.junit.Assume
import java.io.File

/** Fixture nyata ada di testing/fixtures/<perangkat>/ (dibuat oleh tools/capture_fixtures.sh). */
object Fixtures {
    private val root = File("../testing/fixtures")

    fun devices(): List<File> =
        root.listFiles { f -> f.isDirectory }?.sortedBy { it.name }.orEmpty()

    fun read(device: File, name: String): String? =
        File(device, name).takeIf { it.isFile && it.length() > 0 }?.readText()
}

/** Jalankan [block] untuk tiap perangkat yang punya fixture [name]; di-skip jika tidak ada satu pun. */
fun withFixtures(name: String, block: (device: String, text: String) -> Unit) {
    val found = Fixtures.devices().mapNotNull { d -> Fixtures.read(d, name)?.let { d.name to it } }
    Assume.assumeTrue(
        "Tidak ada fixture $name di testing/fixtures (jalankan tools/capture_fixtures.sh)",
        found.isNotEmpty(),
    )
    found.forEach { (device, text) -> block(device, text) }
}
