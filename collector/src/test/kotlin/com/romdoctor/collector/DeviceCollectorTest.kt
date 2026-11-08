package com.romdoctor.collector

import com.romdoctor.core.model.Capability
import com.romdoctor.core.model.CapabilityReport
import com.romdoctor.core.model.CheckResult
import com.romdoctor.core.model.OperationMode
import com.romdoctor.core.shell.ShellCommand
import com.romdoctor.core.shell.ShellExecutor
import com.romdoctor.core.shell.ShellResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Menguji pengkabelan (argumen, flag root, pemetaan gagal) memakai executor palsu; bukan data parser. */
class DeviceCollectorTest {

    private class FakeShell(private val responder: (ShellCommand, Boolean) -> ShellResult) : ShellExecutor {
        val calls = mutableListOf<Pair<ShellCommand, Boolean>>()
        override suspend fun run(command: ShellCommand, asRoot: Boolean, timeoutMs: Long): ShellResult {
            calls += command to asRoot
            return responder(command, asRoot)
        }
    }

    private fun ok(out: String = "x") = ShellResult(0, out, "", false, null, 1)

    @Test
    fun logcatUsesExpectedArgsAndRootFlag() = runBlocking {
        val shell = FakeShell { _, _ -> ok() }
        val result = DeviceCollector(shell, rootAvailable = true, fullLogcat = true).logcat(100)
        assertTrue(result is CollectResult.Success)
        val (c, root) = shell.calls.single()
        assertEquals(listOf("logcat", "-b", "all", "-d", "-v", "threadtime", "-t", "100"), c.toArgv())
        assertTrue(root)
    }

    @Test
    fun logcatWithoutFullAccessDoesNotRunShell() = runBlocking {
        val shell = FakeShell { _, _ -> ok() }
        val result = DeviceCollector(shell, rootAvailable = false, fullLogcat = false).logcat()
        assertTrue(result is CollectResult.Unavailable)
        assertTrue(shell.calls.isEmpty())
    }

    @Test
    fun nonRootCollectorNeverRequestsRoot() = runBlocking {
        val shell = FakeShell { _, _ -> ok() }
        val c = DeviceCollector(shell, rootAvailable = false, fullLogcat = false)
        c.getprop(); c.procMounts(); c.getenforce(); c.dmesg(); c.lshal(); c.serviceList()
        assertTrue(shell.calls.none { it.second })
    }

    @Test
    fun nonZeroExitBecomesUnavailableWithStderr() = runBlocking {
        val shell = FakeShell { _, _ -> ShellResult(1, "", "Operation not permitted", false, null, 1) }
        val r = DeviceCollector(shell, false, false).dmesg()
        assertTrue(r is CollectResult.Unavailable)
        assertTrue((r as CollectResult.Unavailable).reason.contains("Operation not permitted"))
    }

    @Test
    fun startErrorAndTimeoutBecomeUnavailable() = runBlocking {
        val start = FakeShell { _, _ -> ShellResult(null, "", "", false, "gagal start", 1) }
        assertTrue(DeviceCollector(start, true, true).lshal() is CollectResult.Unavailable)
        val slow = FakeShell { _, _ -> ShellResult(null, "", "", true, null, 99) }
        assertTrue(DeviceCollector(slow, true, true).lshal() is CollectResult.Unavailable)
    }

    @Test
    fun exceptionBecomesFailure() = runBlocking {
        val shell = FakeShell { _, _ -> throw IllegalStateException("boom") }
        assertTrue(DeviceCollector(shell, true, true).getprop() is CollectResult.Failure)
    }

    @Test
    fun packagesScopeFlag() = runBlocking {
        val shell = FakeShell { _, _ -> ok() }
        DeviceCollector(shell, false, false).packages(PackageScope.THIRD_PARTY)
        assertEquals(listOf("pm", "list", "packages", "-f", "-3"), shell.calls.single().first.toArgv())
    }

    @Test
    fun fromReportDerivesFlags() {
        val report = CapabilityReport(
            mapOf(Capability.ROOT to CheckResult.Ok(), Capability.LOGCAT_FULL to CheckResult.Unavailable("x")),
            OperationMode.ROOT,
            0,
        )
        val shell = FakeShell { _, _ -> ok() }
        val collector = DeviceCollector.from(shell, report)
        runBlocking { collector.dmesg(); collector.logcat() }
        assertEquals(1, shell.calls.size)       // logcat ditolak karena LOGCAT_FULL tidak Ok
        assertTrue(shell.calls[0].second)       // dmesg pakai root
    }
}
