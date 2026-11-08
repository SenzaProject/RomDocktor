package com.romdoctor.core.shell

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorManager
import android.os.BatteryManager
import com.romdoctor.core.model.Capability
import com.romdoctor.core.model.CapabilityReport
import com.romdoctor.core.model.CheckResult
import com.romdoctor.core.model.Evidence
import com.romdoctor.core.model.OperationMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Menguji kemampuan NYATA saat dipanggil: setiap capability dites dengan menjalankan
 * perintah/API sungguhan. Gagal = Unavailable(alasan dari sistem), bukan dianggap aman.
 */
class CapabilityDetector(
    private val context: Context,
    private val shell: ShellExecutor,
) {
    suspend fun detect(): CapabilityReport = withContext(Dispatchers.IO) {
        val results = LinkedHashMap<Capability, CheckResult>()

        val root = commandProbe(cmd("id"), asRoot = true, timeoutMs = 15_000) {
            if (IdParser.isUidZero(it.stdout)) null else "su berjalan tetapi bukan uid 0"
        }
        results[Capability.ROOT] = root
        val isRoot = root is CheckResult.Ok

        results[Capability.GETPROP] =
            commandProbe(cmd("getprop", "ro.build.version.sdk"), false) {
                if (it.stdout.trim().toIntOrNull() == null) "output bukan angka SDK" else null
            }

        results[Capability.GETENFORCE] = commandProbe(cmd("getenforce"), false) {
            if (it.stdout.trim() in setOf("Enforcing", "Permissive", "Disabled")) null
            else "output tidak dikenali"
        }

        results[Capability.PROC_MOUNTS] = commandProbe(cmd("cat", "/proc/mounts"), false) {
            if (it.stdout.lines().any { l -> l.trim().split(' ').size >= 4 }) null
            else "format /proc/mounts tidak dikenali"
        }

        val hasReadLogs = context.checkSelfPermission(Manifest.permission.READ_LOGS) ==
            PackageManager.PERMISSION_GRANTED
        results[Capability.LOGCAT_FULL] =
            if (!isRoot && !hasReadLogs) {
                CheckResult.Unavailable(
                    "Butuh root atau izin READ_LOGS. Jalankan: adb shell pm grant " +
                        "${context.packageName} android.permission.READ_LOGS",
                )
            } else {
                commandProbe(cmd("logcat", "-b", "all", "-d", "-t", "1"), asRoot = isRoot)
            }

        results[Capability.DMESG] = commandProbe(cmd("dmesg"), asRoot = isRoot)
        results[Capability.LSHAL] = commandProbe(cmd("lshal"), asRoot = isRoot)
        results[Capability.SERVICE_LIST] = commandProbe(cmd("service", "list"), asRoot = isRoot)
        results[Capability.PSTORE] =
            commandProbe(cmd("ls", "/sys/fs/pstore"), asRoot = isRoot, allowEmpty = true)

        results[Capability.PACKAGE_MANAGER] = apiProbe { probePackages() }
        results[Capability.SENSOR_MANAGER] = apiProbe { probeSensors() }
        results[Capability.BATTERY_MANAGER] = apiProbe { probeBattery() }

        CapabilityReport(
            results = results,
            mode = if (isRoot) OperationMode.ROOT else OperationMode.NON_ROOT,
            detectedAtMillis = System.currentTimeMillis(),
        )
    }

    private suspend fun commandProbe(
        command: ShellCommand,
        asRoot: Boolean,
        timeoutMs: Long = 10_000,
        allowEmpty: Boolean = false,
        validate: (ShellResult) -> String? = { null },
    ): CheckResult = try {
        val r = shell.run(command, asRoot, timeoutMs)
        val label = (if (asRoot) "su -c " else "") + command.display()
        when {
            !r.ok -> CheckResult.Unavailable(reasonFor(r))
            r.stdout.isBlank() && !allowEmpty -> CheckResult.Unavailable("output kosong")
            else -> validate(r)?.let { CheckResult.Unavailable(it) }
                ?: CheckResult.Ok(Evidence.of(label, r.stdout, r.exitCode))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        CheckResult.Error(e)
    }

    private fun reasonFor(r: ShellResult): String {
        val startError = r.startError
        if (startError != null) return startError
        if (r.timedOut) return "timeout (${r.durationMs} ms)"
        val err = r.stderr.trim().take(200)
        return "exit code ${r.exitCode}" + if (err.isNotEmpty()) ": $err" else ""
    }

    private fun apiProbe(block: () -> CheckResult): CheckResult = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        CheckResult.Error(e)
    }

    private fun probePackages(): CheckResult {
        val n = context.packageManager.getInstalledPackages(0).size
        return if (n < 10) {
            CheckResult.Unavailable("Hanya $n paket terlihat (pembatasan package visibility)")
        } else {
            CheckResult.Ok(Evidence("PackageManager.getInstalledPackages(0)", "$n paket terlihat"))
        }
    }

    private fun probeSensors(): CheckResult {
        val sm = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
            ?: return CheckResult.Unavailable("SensorManager tidak tersedia")
        val sensors = sm.getSensorList(Sensor.TYPE_ALL)
        return if (sensors.isEmpty()) {
            CheckResult.Unavailable("Tidak ada sensor terdeteksi")
        } else {
            CheckResult.Ok(
                Evidence(
                    "SensorManager.getSensorList(TYPE_ALL)",
                    "${sensors.size} sensor: " + sensors.take(5).joinToString { it.name },
                ),
            )
        }
    }

    private fun probeBattery(): CheckResult {
        val i = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            ?: return CheckResult.Unavailable("ACTION_BATTERY_CHANGED tidak mengembalikan data")
        val level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
        val scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
        return if (level < 0 || scale <= 0) {
            CheckResult.Unavailable("level/scale baterai tidak valid (level=$level, scale=$scale)")
        } else {
            CheckResult.Ok(Evidence("ACTION_BATTERY_CHANGED", "level=$level scale=$scale"))
        }
    }
}
