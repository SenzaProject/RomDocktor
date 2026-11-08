package com.romdoctor.core.shell

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.runInterruptible
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.util.concurrent.TimeUnit

/**
 * Executor berbasis ProcessBuilder: timeout per perintah, kill proses saat timeout/cancel,
 * capture stdout/stderr/exit code, batas ukuran output, whitelist binary.
 */
class ProcessShellExecutor(
    private val whitelist: Set<String> = CommandWhitelist.DEFAULT,
    private val suBinary: String = "su",
    private val maxOutputBytes: Int = 1_000_000,
) : ShellExecutor {

    override suspend fun run(command: ShellCommand, asRoot: Boolean, timeoutMs: Long): ShellResult =
        withContext(Dispatchers.IO) {
            val started = System.nanoTime()
            fun elapsed() = (System.nanoTime() - started) / 1_000_000

            if (command.binary !in whitelist) {
                return@withContext failure("Binary '${command.binary}' tidak ada di whitelist", elapsed())
            }
            val argv = if (asRoot) listOf(suBinary, "-c", command.toRootScript()) else command.toArgv()

            val process = try {
                ProcessBuilder(argv).start()
            } catch (e: IOException) {
                return@withContext failure("Gagal menjalankan ${argv.first()}: ${e.message}", elapsed())
            }

            try {
                runCatching { process.outputStream.close() }
                coroutineScope {
                    val out = async { readBounded(process.inputStream) }
                    val err = async { readBounded(process.errorStream) }
                    val finished = runInterruptible { process.waitFor(timeoutMs, TimeUnit.MILLISECONDS) }
                    if (!finished) {
                        process.destroyForcibly()
                        runCatching { process.inputStream.close() }
                        runCatching { process.errorStream.close() }
                    }
                    val (o, oTrunc) = out.await()
                    val (e, eTrunc) = err.await()
                    ShellResult(
                        exitCode = if (finished) process.exitValue() else null,
                        stdout = o,
                        stderr = e,
                        timedOut = !finished,
                        startError = null,
                        durationMs = elapsed(),
                        truncated = oTrunc || eTrunc,
                    )
                }
            } finally {
                if (process.isAlive) process.destroyForcibly()
            }
        }

    private fun failure(message: String, durationMs: Long) = ShellResult(
        exitCode = null, stdout = "", stderr = "", timedOut = false,
        startError = message, durationMs = durationMs,
    )

    private fun readBounded(stream: InputStream): Pair<String, Boolean> {
        val buf = ByteArray(8192)
        val acc = ByteArrayOutputStream()
        var truncated = false
        try {
            while (true) {
                val n = stream.read(buf)
                if (n < 0) break
                val room = maxOutputBytes - acc.size()
                if (room > 0) acc.write(buf, 0, minOf(n, room))
                if (n > room) truncated = true
            }
        } catch (_: IOException) {
            // stream ditutup karena proses di-kill; pakai output yang sudah terbaca
        }
        return String(acc.toByteArray(), Charsets.UTF_8) to truncated
    }
}
