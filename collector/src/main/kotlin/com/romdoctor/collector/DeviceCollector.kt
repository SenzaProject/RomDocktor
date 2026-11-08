package com.romdoctor.collector

import com.romdoctor.core.model.Capability
import com.romdoctor.core.model.CapabilityReport
import com.romdoctor.core.model.CheckResult
import com.romdoctor.core.model.OperationMode
import com.romdoctor.core.shell.ShellCommand
import com.romdoctor.core.shell.ShellExecutor
import com.romdoctor.core.shell.cmd
import kotlinx.coroutines.CancellationException

enum class PackageScope(val flag: String) { SYSTEM("-s"), THIRD_PARTY("-3") }

/**
 * Mengambil data mentah dari perangkat. Tidak mem-parse apa pun (itu tugas modul parser).
 * [rootAvailable] dan [fullLogcat] berasal dari hasil CapabilityDetector.
 */
class DeviceCollector(
    private val shell: ShellExecutor,
    private val rootAvailable: Boolean,
    private val fullLogcat: Boolean,
) {
    suspend fun getprop(): CollectResult = run(cmd("getprop"), root = false)

    suspend fun procMounts(): CollectResult = run(cmd("cat", "/proc/mounts"), root = false)

    suspend fun getenforce(): CollectResult = run(cmd("getenforce"), root = false)

    suspend fun serviceList(): CollectResult = run(cmd("service", "list"), root = rootAvailable)

    suspend fun lshal(): CollectResult = run(cmd("lshal"), root = rootAvailable)

    suspend fun dmesg(): CollectResult = run(cmd("dmesg"), root = rootAvailable)

    suspend fun pstoreList(): CollectResult = run(cmd("ls", "/sys/fs/pstore"), root = rootAvailable)

    suspend fun packages(scope: PackageScope): CollectResult =
        run(cmd("pm", "list", "packages", "-f", scope.flag), root = false, timeoutMs = 30_000)

    /** Logcat semua buffer. Tanpa root/READ_LOGS hanya log app sendiri, jadi ditolak jujur. */
    suspend fun logcat(lines: Int = 5000): CollectResult {
        if (!fullLogcat) {
            return CollectResult.Unavailable(
                "Logcat penuh tidak tersedia (butuh root atau izin READ_LOGS via adb)",
            )
        }
        return run(
            cmd("logcat", "-b", "all", "-d", "-v", "threadtime", "-t", lines.toString()),
            root = rootAvailable,
            timeoutMs = 30_000,
        )
    }

    private suspend fun run(command: ShellCommand, root: Boolean, timeoutMs: Long = 15_000): CollectResult =
        try {
            val r = shell.run(command, root, timeoutMs)
            val label = (if (root) "su -c " else "") + command.display()
            when {
                r.startError != null -> CollectResult.Unavailable("$label: ${r.startError}")
                r.timedOut -> CollectResult.Unavailable("$label: timeout $timeoutMs ms")
                r.exitCode != 0 -> {
                    val err = r.stderr.trim().take(200)
                    CollectResult.Unavailable("$label: exit ${r.exitCode}" + if (err.isNotEmpty()) ": $err" else "")
                }
                else -> CollectResult.Success(
                    RawCapture(
                        command = command.display(),
                        asRoot = root,
                        stdout = r.stdout,
                        stderr = r.stderr,
                        exitCode = r.exitCode,
                        truncated = r.truncated,
                        durationMs = r.durationMs,
                    ),
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CollectResult.Failure(e)
        }

    companion object {
        fun from(shell: ShellExecutor, report: CapabilityReport): DeviceCollector = DeviceCollector(
            shell = shell,
            rootAvailable = report.mode == OperationMode.ROOT,
            fullLogcat = report.results[Capability.LOGCAT_FULL] is CheckResult.Ok,
        )
    }
}
